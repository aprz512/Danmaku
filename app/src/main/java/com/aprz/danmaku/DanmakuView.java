package com.aprz.danmaku;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by aprz on 16-12-18.
 * --
 */
public class DanmakuView extends TextureView implements TextureView.SurfaceTextureListener {

    private int mWidth;
    private int mBackgroundId;
    private int mItemSpacingTime;
    private DanmakuItemManager mItemManager;
    private DanmakuDrawThread mDrawThread;
    private Timer mTimer;
    private final Object mLock = new Object();

    public DanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DanmakuView);
        mBackgroundId = a.getResourceId(R.styleable.DanmakuView_item_background,
                R.drawable.danmaku_bg);
        mItemSpacingTime = a.getInteger(R.styleable.DanmakuView_item_spacing_time, 3000);
        a.recycle();
        initView();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        if (mItemManager == null) {
            mItemManager = new DanmakuItemManager(getContext(), w, mBackgroundId);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        pause();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void initView() {
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    private void startTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemManager.hasNextItem()) {
                                mItemManager.runNextItem();
                            }
                        }
                    });

                }
            }, 0, mItemSpacingTime);
        }
    }

    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    /**
     * 开始弹幕
     */
    public void start() {
        if (mDrawThread == null) {
            mDrawThread = new DanmakuDrawThread();
            mDrawThread.start();
        }

        startTimer();
    }

    /**
     * 暂停弹幕
     * 并不清除弹幕内容
     */
    public void pause() {
        if (mItemManager != null) {
            mItemManager.stop();
        }
        cancelTimer();
        stopDrawTread();
    }

    /**
     * 释放对象集合
     */
    public void release() {
        if (mItemManager != null) {
            mItemManager.destroy();
        }
        cancelTimer();
        stopDrawTread();
    }

    public boolean isDrawing() {
        return mDrawThread != null && mDrawThread.isDrawing();
    }

    private void stopDrawTread() {
        if (mDrawThread != null && mDrawThread.isDrawing()) {
            mDrawThread.stopDrawing();
            mDrawThread = null;
        }
    }

    public void loadDanmaku(ArrayList<DanmakuBean> beanList) {
        // 清除之前的弹幕
        release();
        mItemManager.initWaitBean(beanList);
    }

    public void setLoop(boolean loop) {
        mItemManager.setLoop(loop);
    }

    public void addDanmaku(DanmakuBean bean) {
        mItemManager.addDanmakuBean(bean);
    }

    // ----------------------
    // ----------------------

    public void setOnNeedMoreItemListener(OnNeedMoreItemListener listener) {
        if (mItemManager != null) {
            mItemManager.setOnNeedMoreItemListener(listener);
        }
    }

    public interface OnNeedMoreItemListener {
        void onNeedMoreItem();
    }

    public void addToWaitQueue(ArrayList<DanmakuBean> beanList) {
        if (mItemManager != null) {
            mItemManager.initWaitBean(beanList);
        }
    }

    //---------------- 渲染线程 -------------------
    //---------------- 渲染线程 -------------------

    private class DanmakuDrawThread extends Thread {

        private AtomicBoolean isRunning = new AtomicBoolean(false);

        @Override
        public void run() {

            isRunning.set(true);

            Canvas canvas = null;
            long frameDuration;
            while (isRunning.get()) {
                try {
                    synchronized (mLock) {
                        frameDuration = System.currentTimeMillis();

                        /**
                         * 日 不传 dirty 区域 似乎稳定了很多
                         */
                        canvas = lockCanvas();
                        if (canvas == null) {
                            continue;
                        }

                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        mItemManager.drawOnCanvas(canvas);
                        frameDuration = System.currentTimeMillis() - frameDuration;
                        /**
                         * 为了避免抖动 所以减少了每次绘制的间距 即弹幕的移动速度
                         *
                         * 所以这里需要减少睡眠时间,即提高绘制频率
                         *
                         * 但是也不要提的太高了
                         */
                        try {
                            if (12 - frameDuration > 0) {
                                Thread.sleep(12 - frameDuration);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        unlockCanvasAndPost(canvas);
                    }
                }

            }

        }

        public void stopDrawing() {
            isRunning.set(false);
        }

        public boolean isDrawing() {
            return isRunning.get();
        }
    }
}
