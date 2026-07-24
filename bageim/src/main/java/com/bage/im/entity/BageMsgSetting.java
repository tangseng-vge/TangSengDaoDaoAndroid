package com.bage.im.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class BageMsgSetting implements Parcelable {
    // 消息是否回执
    public int receipt;
    // 是否开启top
    public int topic;
    // 是否未流消息
    public int stream;

    public BageMsgSetting() {
    }

    protected BageMsgSetting(Parcel in) {
        receipt = in.readInt();
        topic = in.readInt();
        stream = in.readInt();
    }

    public static final Creator<BageMsgSetting> CREATOR = new Creator<BageMsgSetting>() {
        @Override
        public BageMsgSetting createFromParcel(Parcel in) {
            return new BageMsgSetting(in);
        }

        @Override
        public BageMsgSetting[] newArray(int size) {
            return new BageMsgSetting[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(receipt);
        dest.writeInt(topic);
        dest.writeInt(stream);
    }
}
