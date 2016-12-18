package com.aprz.danmaku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by aprz on 16-12-18.
 * --
 */
public class DanmakuItemManager {

    private static final int ROWS = 3;

    private final int mScreenWidth;
    private final int mItemBgResourceId;

    private Context mContext;

    private ReentrantLock mLock = new ReentrantLock();
    private ArrayList<Integer> mIntegers = new ArrayList<>(ROWS);

    private Boolean[] mAvailableRow = new Boolean[ROWS];

    private LinkedList<DanmakuBean> mWaitingDanmakuBean = new LinkedList<>();
    private LinkedList<DrawerBean> mRunningBean = new LinkedList<>();
    private LinkedList<DanmakuItem> mDanmakuItems = new LinkedList<>();

    private boolean mLoop;
    private Paint mBitmapPaint;

    // 上一次运行弹幕时，因为图片没加载完成，所以保存其引用，以免下次又创建新的
    private DrawerBean mPreDrawerBean;

    private DanmakuView.OnNeedMoreItemListener mOnNeedMoreItemListener;

    public DanmakuItemManager(Context context, int screenWidth, int bgResourceId) {
        this.mContext = context;
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setDither(true);
        this.mScreenWidth = screenWidth;

        for (int i = 0; i < ROWS; i++) {
            mAvailableRow[i] = true;
            mIntegers.add(i);
        }

        // 同步集合
        Collections.synchronizedList(mWaitingDanmakuBean);
        this.mItemBgResourceId = bgResourceId;
    }

    public void drawOnCanvas(Canvas canvas) {

        try {
            mLock.lock();
            if (mRunningBean.isEmpty()) {
                return;
            }
            for (DrawerBean drawerBean : mRunningBean) {
                canvas.drawBitmap(drawerBean.getBitmap(), drawerBean.getX(), drawerBean.getY(), mBitmapPaint);
                drawerBean.moveToNextPosition();
            }

            removeDoneItem();
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 创建一个弹幕实体对象
     */
    private DrawerBean createBean(DanmakuBean bean) {
        DanmakuItem danmakuItem = getItem();
        danmakuItem.setDanmakuBean(bean);
        return new DrawerBean(2, danmakuItem, mScreenWidth);
    }

    // 取出的用完了 一定要放回来
    private DanmakuItem getItem() {
        if (mDanmakuItems.isEmpty()) {
            DanmakuItem item = new DanmakuItem(mContext);
            item.setItemBackground(mItemBgResourceId);
            mDanmakuItems.offer(item);
        }

        return mDanmakuItems.pop();
    }

    /**
     * 是否有可用的行
     */
    private boolean hasAvailableRow() {
        for (Boolean b : mAvailableRow) {
            if (b) {
                return true;
            }
        }

        return false;
    }

    /**
     * 运行下一个弹幕
     * 当 mPreDrawerBean 不为 null 并且 isReady 为false的时候，
     * 就加载不出下个弹幕了...
     * 是没有回调
     */
    public void runNextItem() {
        if (!hasNextItem() && hasAvailableRow()) {
            return;
        }

        DrawerBean drawerBean;
        if (mPreDrawerBean == null) {
            DanmakuBean danmakuBean = mWaitingDanmakuBean.peek();
            drawerBean = createBean(danmakuBean);
            mPreDrawerBean = drawerBean;
        } else {
            drawerBean = mPreDrawerBean;
        }

        if (!drawerBean.getDanmakuItem().isReady()) {
            // 头像还没加载出来
            return;
        } else {
            mPreDrawerBean = null;
        }

        // 随机选取一行
        Collections.shuffle(mIntegers);
        for (Integer i : mIntegers) {
            if (mAvailableRow[i]) {
                drawerBean.setRow(i);
                drawerBean.setY((drawerBean.getDanmakuItemHeight() + 30) * i);
                mAvailableRow[i] = false;
                break;
            }
        }

        if (drawerBean.getRow() == -1) {
            // 暂时没有空余的行来运行弹幕
            mDanmakuItems.offer(drawerBean.getDanmakuItem());
            return;
        }

        drawerBean.createBitmap();

        mWaitingDanmakuBean.pop();
        if (mOnNeedMoreItemListener != null &&
                (mWaitingDanmakuBean.size() == 10 || mWaitingDanmakuBean.size() == 0)) {
            mOnNeedMoreItemListener.onNeedMoreItem();
        }

        try {
            mLock.lock();
            mRunningBean.add(drawerBean);
        } finally {
            mLock.unlock();
        }

    }

    /**
     * bean 完全移动出去时，清除bitmap
     * 若循环，重新放入等待集合
     */
    private void removeDoneItem() {
        Iterator<DrawerBean> iterator = mRunningBean.iterator();
        while (iterator.hasNext()) {
            DrawerBean bean = iterator.next();
            if (bean.isDone()) {
                mDanmakuItems.offer(bean.getDanmakuItem());
                if (mLoop) {
                    mWaitingDanmakuBean.offer(bean.getDanmakuItem().getDanmakuBean());
                }
                mAvailableRow[bean.getRow()] = true;//先放到这里, 原来在release里面
                bean.clear();
                iterator.remove();
            } else {
                // 从前往后遍历的，一个未完成，后面的就不用遍历了
                break;
            }
        }
    }

    public boolean hasNextItem() {
        return !mWaitingDanmakuBean.isEmpty();
    }

    public void initWaitBean(ArrayList<DanmakuBean> beanList) {
        for (DanmakuBean bean : beanList) {
            mWaitingDanmakuBean.offer(bean);
        }
    }

    /**
     * 弹幕停止
     */
    public void stop() {
        for (int i = 0; i < 3; i++) {
            mAvailableRow[i] = true;
        }
        try {
            mLock.lock();
            for (DrawerBean bean : mRunningBean) {
                // 将正在运行的弹幕放回准备弹幕中
                mWaitingDanmakuBean.offer(bean.getDanmakuItem().getDanmakuBean());
                // 将item放回
                mDanmakuItems.offer(bean.getDanmakuItem());
                bean.clear();
            }
            mRunningBean.clear();
        } finally {
            mLock.unlock();
        }
    }

    public void destroy() {
        mPreDrawerBean = null;
        stop();
        mWaitingDanmakuBean.clear();
        mDanmakuItems.clear();
    }

    /**
     * 弹幕是否循环
     *
     * @param loop
     */
    public void setLoop(boolean loop) {
        mLoop = loop;
    }

    /**
     * 添加一条弹幕，添加到集合的最前面
     *
     * @param bean 弹幕实体
     */
    public void addDanmakuBean(DanmakuBean bean) {
        // 必须要置null
        mPreDrawerBean = null;
        mWaitingDanmakuBean.push(bean);
    }

    public void setOnNeedMoreItemListener(DanmakuView.OnNeedMoreItemListener onNeedMoreItemListener) {
        this.mOnNeedMoreItemListener = onNeedMoreItemListener;
    }
}
