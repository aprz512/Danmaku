package com.aprz.danmaku;

import android.graphics.Bitmap;
import android.view.View;


/**
 * Created by aprz on 16-12-18.
 * --
 */
public class DrawerBean {

    private float mX;
    private float mY;
    private float mTargetX;
    private float mStartX;
    private float mSpeed;
    private Bitmap mBitmap;
    private DanmakuItem mDanmakuItem;
    private int mRow = -1;

    public DrawerBean(float speed, DanmakuItem item, int screenWidth) {
        item.initItem();
        // 测量时 限制了弹幕的宽和高 宽 <= DeviceInfo.HEIGHT / 2 || 高 <= DeviceInfo.WIDTH / 2
        item.measure(View.MeasureSpec.makeMeasureSpec(DeviceInfo.HEIGHT / 2, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(DeviceInfo.WIDTH / 2, View.MeasureSpec.AT_MOST));
        item.requestLayout();

        mX = screenWidth;
        mY = 0;
        mTargetX = -item.getMeasuredWidth();
        mStartX = mX;
        mSpeed = speed;
        mDanmakuItem = item;
    }

    public DanmakuItem getDanmakuItem() {
        return mDanmakuItem;
    }

    public int getRow() {
        return mRow;
    }

    public void setRow(int row) {
        mRow = row;
    }

    public int getDanmakuItemHeight() {
        return mDanmakuItem.getMeasuredHeight();
    }

    public float getDistance() {
        return mTargetX - mStartX;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void moveToNextPosition() {
        mX -= mSpeed;
    }

    public boolean isDone() {
        return mX <= mTargetX - 20;
    }

    public void setY(float y) {
        mY = y;
    }

    public void clear() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mDanmakuItem = null;
    }

    public void createBitmap() {
        mBitmap = DanmakuItem.createBitmapFromView(mDanmakuItem);
    }
}
