package com.bage.im.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.bage.im.msgmodel.BageMessageContent;

public class BageMsgExtra implements Parcelable {
    public String messageID;
    public String channelID;
    public byte channelType;
    public int readed;
    public int readedCount;
    public int unreadCount;
    public int revoke;
    public int isMutualDeleted;
    public String revoker;
    public long extraVersion;
    public long editedAt;
    public String contentEdit;
    public int needUpload;
    public int isPinned;
    public BageMessageContent contentEditMsgModel;

    public BageMsgExtra() {
    }

    protected BageMsgExtra(Parcel in) {
        messageID = in.readString();
        channelID = in.readString();
        channelType = in.readByte();
        readed = in.readInt();
        readedCount = in.readInt();
        unreadCount = in.readInt();
        revoke = in.readInt();
        isMutualDeleted = in.readInt();
        revoker = in.readString();
        extraVersion = in.readLong();
        editedAt = in.readLong();
        contentEdit = in.readString();
        needUpload = in.readInt();
        isPinned = in.readInt();
        contentEditMsgModel = in.readParcelable(BageMessageContent.class.getClassLoader());
    }

    public static final Creator<BageMsgExtra> CREATOR = new Creator<BageMsgExtra>() {
        @Override
        public BageMsgExtra createFromParcel(Parcel in) {
            return new BageMsgExtra(in);
        }

        @Override
        public BageMsgExtra[] newArray(int size) {
            return new BageMsgExtra[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(messageID);
        parcel.writeString(channelID);
        parcel.writeByte(channelType);
        parcel.writeInt(readed);
        parcel.writeInt(readedCount);
        parcel.writeInt(unreadCount);
        parcel.writeInt(revoke);
        parcel.writeInt(isMutualDeleted);
        parcel.writeString(revoker);
        parcel.writeLong(extraVersion);
        parcel.writeLong(editedAt);
        parcel.writeString(contentEdit);
        parcel.writeInt(needUpload);
        parcel.writeInt(isPinned);
        parcel.writeParcelable(contentEditMsgModel, i);
    }
}
