package com.bage.im.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class BageMsgHeader implements Parcelable {
    //是否持久化[是否不保存在数据库]
    public boolean noPersist;
    //对方是否显示红点
    public boolean redDot = true;
    //消息是否只同步一次
    public boolean syncOnce;

    BageMsgHeader() {

    }

    protected BageMsgHeader(Parcel in) {
        noPersist = in.readByte() != 0;
        redDot = in.readByte() != 0;
        syncOnce = in.readByte() != 0;
    }

    public static final Creator<BageMsgHeader> CREATOR = new Creator<BageMsgHeader>() {
        @Override
        public BageMsgHeader createFromParcel(Parcel in) {
            return new BageMsgHeader(in);
        }

        @Override
        public BageMsgHeader[] newArray(int size) {
            return new BageMsgHeader[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (noPersist ? 1 : 0));
        parcel.writeByte((byte) (redDot ? 1 : 0));
        parcel.writeByte((byte) (syncOnce ? 1 : 0));
    }
}
