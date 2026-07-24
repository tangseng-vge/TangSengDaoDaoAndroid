package com.bage.im.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

public class BageMsgEntity implements Parcelable {
    public int offset;
    public int length;
    public String type;
    public String value;

    public BageMsgEntity() {
    }

    protected BageMsgEntity(Parcel in) {
        offset = in.readInt();
        length = in.readInt();
        type = in.readString();
        value = in.readString();
    }

    public static final Creator<BageMsgEntity> CREATOR = new Creator<BageMsgEntity>() {
        @Override
        public BageMsgEntity createFromParcel(Parcel in) {
            return new BageMsgEntity(in);
        }

        @Override
        public BageMsgEntity[] newArray(int size) {
            return new BageMsgEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(offset);
        parcel.writeInt(length);
        parcel.writeString(type);
        parcel.writeString(value);
    }
}
