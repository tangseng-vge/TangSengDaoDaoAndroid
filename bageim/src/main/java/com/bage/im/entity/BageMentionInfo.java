package com.bage.im.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 2020-10-22 13:28
 * 提醒对象
 */
public class BageMentionInfo implements Parcelable {

    public boolean isMentionMe;
    public List<String> uids;

    public BageMentionInfo() {
    }

    protected BageMentionInfo(Parcel in) {
        isMentionMe = in.readByte() != 0;
        uids = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isMentionMe ? 1 : 0));
        dest.writeStringList(uids);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BageMentionInfo> CREATOR = new Creator<BageMentionInfo>() {
        @Override
        public BageMentionInfo createFromParcel(Parcel in) {
            return new BageMentionInfo(in);
        }

        @Override
        public BageMentionInfo[] newArray(int size) {
            return new BageMentionInfo[size];
        }
    };
}
