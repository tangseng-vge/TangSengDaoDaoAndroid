package com.bage.im.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:42
 * 图片消息
 */
public class BageImageContent extends BageMediaMessageContent {
    private final String TAG = "BageImageContent";
    public int width;
    public int height;

    public BageImageContent(String localPath) {
        this.localPath = localPath;
        this.type = BageMsgContentType.Bage_IMAGE;
    }

    // 无参构造必须提供
    public BageImageContent() {
        this.type = BageMsgContentType.Bage_IMAGE;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("url", url);
            jsonObject.put("width", width);
            jsonObject.put("height", height);
            jsonObject.put("localPath", localPath);
        } catch (JSONException e) {
            BageLoggerUtils.getInstance().e(TAG, "encodeMsg error");
        }
        return jsonObject;
    }

    @Override
    public BageMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("url"))
            this.url = jsonObject.optString("url");
        if (jsonObject.has("localPath"))
            this.localPath = jsonObject.optString("localPath");
        if (jsonObject.has("height"))
            this.height = jsonObject.optInt("height");
        if (jsonObject.has("width"))
            this.width = jsonObject.optInt("width");
        return this;
    }


    protected BageImageContent(Parcel in) {
        super(in);
        width = in.readInt();
        height = in.readInt();
        url = in.readString();
        localPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(url);
        dest.writeString(localPath);
    }


    public static final Parcelable.Creator<BageImageContent> CREATOR = new Parcelable.Creator<BageImageContent>() {
        @Override
        public BageImageContent createFromParcel(Parcel in) {
            return new BageImageContent(in);
        }

        @Override
        public BageImageContent[] newArray(int size) {
            return new BageImageContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDisplayContent() {
        return "[图片]";
    }

    @Override
    public String getSearchableWord() {
        return "[图片]";
    }
}
