package com.bage.im.msgmodel;

import android.os.Parcel;

/**
 * 2020-04-04 10:39
 * 多媒体消息。如果自定义消息带附件需继承该类
 */
public abstract class BageMediaMessageContent extends BageMessageContent {
    public String localPath;//本地地址
    public String url;//网络地址

    public BageMediaMessageContent() {
    }

    protected BageMediaMessageContent(Parcel in) {
        super(in);
    }
}
