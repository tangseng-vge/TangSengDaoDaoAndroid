package com.chat.base.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.bage.im.BageIM;
import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.msgmodel.BageImageContent;
import com.bage.im.msgmodel.BageMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

/** Image message with thumbnail, preview and original OSS objects. */
public class BageChatImageContent extends BageImageContent {
    public String previewUrl;
    public String originalUrl;
    public long originalSize;

    public BageChatImageContent() {
        super();
        type = BageMsgContentType.Bage_IMAGE;
    }

    public BageChatImageContent(String localPath) {
        super(localPath);
    }

    protected BageChatImageContent(Parcel in) {
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

    public static final Parcelable.Creator<BageChatImageContent> CREATOR = new Parcelable.Creator<BageChatImageContent>() {
        @Override
        public BageChatImageContent createFromParcel(Parcel in) {
            return new BageChatImageContent(in);
        }

        @Override
        public BageChatImageContent[] newArray(int size) {
            return new BageChatImageContent[size];
        }
    };

    /**
     * BageIM 1.5.3 rejects registrations that reuse a built-in type. Replace
     * only the type-2 decoder so old and new image payloads share one type.
     */
    @SuppressWarnings("unchecked")
    public static void registerDecoder() {
        try {
            Object manager = BageIM.getInstance().getMsgManager();
            manager.getClass().getMethod("initNormalMsg").invoke(manager);
            Field field = manager.getClass().getDeclaredField("customContentMsgList");
            field.setAccessible(true);
            List<Class<? extends BageMessageContent>> decoders =
                    (List<Class<? extends BageMessageContent>>) field.get(manager);
            for (int i = 0; i < decoders.size(); i++) {
                BageMessageContent content = decoders.get(i).getDeclaredConstructor().newInstance();
                if (content.type == BageMsgContentType.Bage_IMAGE) {
                    decoders.set(i, BageChatImageContent.class);
                    return;
                }
            }
            decoders.add(BageChatImageContent.class);
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
    public BageMessageContent decodeMsg(JSONObject json) {
        super.decodeMsg(json);
        previewUrl = json.optString("preview_url", url);
        originalUrl = json.optString("original_url", previewUrl);
        originalSize = json.optLong("original_size", 0);
        return this;
    }
}
