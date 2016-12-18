package com.aprz.danmaku;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;


/**
 * Created by aprz on 16-12-18.
 * --
 */
public class DanmakuItem extends LinearLayout {

    private ImageView mHeadImage;
    private TextView mText;
    private DanmakuBean mDanmakuBean;
    private boolean mReady;
    private RelativeLayout mItemBg;

    public DanmakuItem(Context context) {
        this(context, null);
    }

    public DanmakuItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        createItem(context);
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        setDrawingCacheBackgroundColor(Color.TRANSPARENT);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void createItem(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.danmuku_item, this, true);
        mHeadImage = (ImageView) view.findViewById(R.id.header_image);
        mItemBg = (RelativeLayout) view.findViewById(R.id.danmaku_item_bg);
        mText = (TextView) view.findViewById(R.id.comment);
    }

    public void initItem() {
        if (mDanmakuBean.getImgUrl() == null) {
            mReady = true;
            mHeadImage.setVisibility(INVISIBLE);
        } else {
            mReady = false;
            mHeadImage.setVisibility(VISIBLE);
            mReady = true;
            ImageLoader.getInstance().displayImage(
                    mDanmakuBean.getImgUrl(),
                    mHeadImage,
                    new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            mReady = false;
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            mReady = false;
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            mReady = true;
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            mReady = false;
                        }
                    });
        }
        if (mDanmakuBean.getSource() == DanmakuBean.SOURCE.MY) {
            mText.setTextColor(Color.BLUE);
        } else {
            mText.setTextColor(Color.WHITE);
        }
        mText.setText(mDanmakuBean.getWords());
    }

    /**
     * 图片是否加载成功
     *
     * @return true 加载完成/失败
     */
    public boolean isReady() {
        return mReady;
    }

    public DanmakuBean getDanmakuBean() {
        return mDanmakuBean;
    }

    public void setDanmakuBean(DanmakuBean danmakuBean) {
        mDanmakuBean = danmakuBean;
    }

    /**
     * 方法说明：给view截图；
     * 注意当 v是viewGroup时 最好设置下layoutParams，若不设置会使用默认的，Bitmap大小会不准确；
     *
     * @param v targetView
     * @return bitmap
     */
    public static Bitmap createBitmapFromView(View v) {
        if (v.getMeasuredHeight() == 0 || v.getMeasuredWidth() == 0) {
            v.measure(MeasureSpec.makeMeasureSpec(DeviceInfo.HEIGHT / 2, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(DeviceInfo.WIDTH / 2, MeasureSpec.AT_MOST));
        }

        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }

    public void onlySetText(String text) {
        mHeadImage.setVisibility(GONE);
        mText.setText(text);
    }

    public void setItemBackground(int itemBackground) {
        if (mItemBg != null && itemBackground != -1) {
            mItemBg.setBackgroundResource(itemBackground);
        }
    }
}
