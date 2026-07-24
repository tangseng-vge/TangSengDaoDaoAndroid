package com.bage.im.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.bage.im.message.type.BageMsgContentType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:45
 * 内置语音消息model
 */
public class BageVoiceContent extends BageMediaMessageContent {
    public int timeTrad;
    public String waveform;

    public BageVoiceContent(String localPath, int timeTrad) {
        this.type = BageMsgContentType.Bage_VOICE;
        this.timeTrad = timeTrad;
        this.localPath = localPath;
    }

    // 无参构造必须提供
    public BageVoiceContent() {
        this.type = BageMsgContentType.Bage_VOICE;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("localPath", localPath);
            jsonObject.put("timeTrad", timeTrad);
            jsonObject.put("url", url);
            if (waveform != null)
                jsonObject.put("waveform", waveform);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public BageMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("timeTrad"))
            timeTrad = jsonObject.optInt("timeTrad");
        if (jsonObject.has("localPath"))
            localPath = jsonObject.optString("localPath");
        if (jsonObject.has("url"))
            url = jsonObject.optString("url");
        if (jsonObject.has("waveform"))
            waveform = jsonObject.optString("waveform");
        return this;
    }


    protected BageVoiceContent(Parcel in) {
        super(in);
        timeTrad = in.readInt();
        url = in.readString();
        localPath = in.readString();
        waveform = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(timeTrad);
        dest.writeString(url);
        dest.writeString(localPath);
        dest.writeString(waveform);
    }


    public static final Parcelable.Creator<BageVoiceContent> CREATOR = new Parcelable.Creator<BageVoiceContent>() {
        @Override
        public BageVoiceContent createFromParcel(Parcel in) {
            return new BageVoiceContent(in);
        }

        @Override
        public BageVoiceContent[] newArray(int size) {
            return new BageVoiceContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDisplayContent() {
        return "[语音]";
    }

    @Override
    public String getSearchableWord() {
        return "[语音]";
    }
}
