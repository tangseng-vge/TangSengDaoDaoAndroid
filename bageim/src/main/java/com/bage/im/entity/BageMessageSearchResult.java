package com.bage.im.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 2020-05-10 22:26
 * 消息搜索结果
 */
public class BageMessageSearchResult implements Parcelable {
    //消息对应的频道信息
    public BageChannel bageChannel;
    //包含关键字的信息
    public String searchableWord;
    //条数
    public int messageCount;

    public BageMessageSearchResult() {
    }

    protected BageMessageSearchResult(Parcel in) {
        bageChannel = in.readParcelable(BageChannel.class.getClassLoader());
        searchableWord = in.readString();
        messageCount = in.readInt();
    }

    public static final Creator<BageMessageSearchResult> CREATOR = new Creator<BageMessageSearchResult>() {
        @Override
        public BageMessageSearchResult createFromParcel(Parcel in) {
            return new BageMessageSearchResult(in);
        }

        @Override
        public BageMessageSearchResult[] newArray(int size) {
            return new BageMessageSearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(bageChannel, flags);
        dest.writeString(searchableWord);
        dest.writeInt(messageCount);
    }
}
