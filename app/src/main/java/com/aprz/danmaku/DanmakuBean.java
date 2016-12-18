package com.aprz.danmaku;

/**
 * Created by aprz on 16-12-18.
 * --
 *
 * 是弹幕类型 自己发的弹幕 别人发的弹幕
 */
public class DanmakuBean {

    public enum SOURCE {
        MY, OTHERS
    }

    private SOURCE mSource;
    private String mWords;
    private String mImgUrl;

    public DanmakuBean(SOURCE source, String words, String imgUrl) {
        mSource = source;
        mWords = words;
        mImgUrl = imgUrl;
    }

    public SOURCE getSource() {
        return mSource;
    }

    public void setSource(SOURCE source) {
        mSource = source;
    }

    public String getWords() {
        return mWords;
    }

    public void setWords(String words) {
        mWords = words;
    }

    public String getImgUrl() {
        return mImgUrl;
    }

    public void setImgUrl(String imgUrl) {
        mImgUrl = imgUrl;
    }
}
