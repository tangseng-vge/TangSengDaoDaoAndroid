package com.bage.im.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bage.im.message.type.BageMsgContentType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:35
 * 文本消息
 */
public class BageTextContent extends BageMessageContent {

    public BageTextContent(String content) {
        this.content = content;
        this.type = BageMsgContentType.Bage_TEXT;
    }

    // 无参构造必须提供
    public BageTextContent() {
        this.type = BageMsgContentType.Bage_TEXT;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (!TextUtils.isEmpty(content))
                jsonObject.put("content", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public BageMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject != null) {
            if (jsonObject.has("content"))
                this.content = jsonObject.optString("content");
        }
        return this;
    }

    @Override
    public String getSearchableWord() {
        return content;
    }

    @Override
    public String getDisplayContent() {
        return content;
    }

    protected BageTextContent(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }


    public static final Parcelable.Creator<BageTextContent> CREATOR = new Parcelable.Creator<BageTextContent>() {
        @Override
        public BageTextContent createFromParcel(Parcel in) {
            return new BageTextContent(in);
        }

        @Override
        public BageTextContent[] newArray(int size) {
            return new BageTextContent[size];
        }
    };
}
