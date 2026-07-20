package com.chat.base.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.message.type.WKMsgContentType;
import com.xinbida.wukongim.msgmodel.WKImageContent;
import com.xinbida.wukongim.msgmodel.WKMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

/** Image message with thumbnail, preview and original OSS objects. */
public class WKChatImageContent extends WKImageContent {
    public String previewUrl;
    public String originalUrl;
    public long originalSize;

    public WKChatImageContent() {
        super();
        type = WKMsgContentType.WK_IMAGE;
    }

    public WKChatImageContent(String localPath) {
        super(localPath);
    }

    protected WKChatImageContent(Parcel in) {
        super(in);
        previewUrl = in.readString();
        originalUrl = in.readString();
        originalSize = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(previewUrl);
        dest.writeString(originalUrl);
        dest.writeLong(originalSize);
    }

    public static final Parcelable.Creator<WKChatImageContent> CREATOR = new Parcelable.Creator<WKChatImageContent>() {
        @Override
        public WKChatImageContent createFromParcel(Parcel in) {
            return new WKChatImageContent(in);
        }

        @Override
        public WKChatImageContent[] newArray(int size) {
            return new WKChatImageContent[size];
        }
    };

    /**
     * WuKongIM 1.5.3 rejects registrations that reuse a built-in type. Replace
     * only the type-2 decoder so old and new image payloads share one type.
     */
    @SuppressWarnings("unchecked")
    public static void registerDecoder() {
        try {
            Object manager = WKIM.getInstance().getMsgManager();
            manager.getClass().getMethod("initNormalMsg").invoke(manager);
            Field field = manager.getClass().getDeclaredField("customContentMsgList");
            field.setAccessible(true);
            List<Class<? extends WKMessageContent>> decoders =
                    (List<Class<? extends WKMessageContent>>) field.get(manager);
            for (int i = 0; i < decoders.size(); i++) {
                WKMessageContent content = decoders.get(i).getDeclaredConstructor().newInstance();
                if (content.type == WKMsgContentType.WK_IMAGE) {
                    decoders.set(i, WKChatImageContent.class);
                    return;
                }
            }
            decoders.add(WKChatImageContent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to install three-level image decoder", e);
        }
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject json = super.encodeMsg();
        try {
            json.put("preview_url", previewUrl == null || previewUrl.isEmpty() ? url : previewUrl);
            json.put("original_url", originalUrl == null || originalUrl.isEmpty() ? (previewUrl == null ? url : previewUrl) : originalUrl);
            if (originalSize > 0) json.put("original_size", originalSize);
        } catch (JSONException ignored) {
        }
        return json;
    }

    @Override
    public WKMessageContent decodeMsg(JSONObject json) {
        super.decodeMsg(json);
        previewUrl = json.optString("preview_url", url);
        originalUrl = json.optString("original_url", previewUrl);
        originalSize = json.optLong("original_size", 0);
        return this;
    }
}
