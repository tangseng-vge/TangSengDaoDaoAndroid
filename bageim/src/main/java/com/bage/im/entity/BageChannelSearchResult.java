package com.bage.im.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 2020-05-10 19:16
 * 频道搜索结果
 */
public class BageChannelSearchResult implements Parcelable {
    //频道信息
    public BageChannel bageChannel;
    //包含的成员名称
    public String containMemberName;

    public BageChannelSearchResult() {
    }

    protected BageChannelSearchResult(Parcel in) {
        bageChannel = in.readParcelable(BageChannel.class.getClassLoader());
        containMemberName = in.readString();
    }

    public static final Creator<BageChannelSearchResult> CREATOR = new Creator<BageChannelSearchResult>() {
        @Override
        public BageChannelSearchResult createFromParcel(Parcel in) {
            return new BageChannelSearchResult(in);
        }

        @Override
        public BageChannelSearchResult[] newArray(int size) {
            return new BageChannelSearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(bageChannel, flags);
        dest.writeString(containMemberName);
    }
}
