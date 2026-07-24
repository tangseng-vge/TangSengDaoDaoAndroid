package com.chat.uikit.chat.msgmodel;

import android.os.Parcel;
import android.text.TextUtils;

import com.chat.base.msgitem.BageContentType;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageReader;
import com.chat.uikit.R;
import com.chat.uikit.BageUIKitApplication;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageMsg;
import com.bage.im.msgmodel.BageMessageContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-09-22 10:01
 * 合并转发消息
 */
public class BageMultiForwardContent extends BageMessageContent {
    public byte channelType;
    public List<BageChannel> userList;
    public List<BageMsg> msgList;

    public BageMultiForwardContent() {
        type = BageContentType.Bage_MULTIPLE_FORWARD;
    }

    @Override
    public BageMessageContent decodeMsg(JSONObject jsonObject) {
        channelType = (byte) jsonObject.optInt("channel_type");
        JSONArray msgArr = jsonObject.optJSONArray("msgs");
        if (msgArr != null && msgArr.length() > 0) {
            msgList = new ArrayList<>();
            for (int i = 0, size = msgArr.length(); i < size; i++) {
                JSONObject msgJson = msgArr.optJSONObject(i);
                BageMsg msg = new BageMsg();
                JSONObject contentJson = msgJson.optJSONObject("payload");
                if (contentJson != null) {
                    msg.content = contentJson.toString();
                    msg.baseContentMsgModel = BageIM.getInstance().getMsgManager().getMsgContentModel(contentJson);
                    if (msg.baseContentMsgModel != null) {
                        msg.type = msg.baseContentMsgModel.type;
                    }
                } else {
                    msg.baseContentMsgModel = new BageMessageContent();
                    msg.type = BageContentType.unknown_msg;
                }
                msg.timestamp = msgJson.optLong("timestamp");
                msg.messageID = msgJson.optString("message_id");
                if (msgJson.has("from_uid")) {
                    msg.fromUID = msgJson.optString("from_uid");
//                    if (msg.baseContentMsgModel != null) {
//                        msg.baseContentMsgModel.fromUID = msg.fromUID;
//                    }
                }
                msgList.add(msg);
            }
        }
        JSONArray userArr = jsonObject.optJSONArray("users");
        if (userArr != null && userArr.length() > 0) {
            userList = new ArrayList<>();
            for (int i = 0, size = userArr.length(); i < size; i++) {
                JSONObject userJson = userArr.optJSONObject(i);
                BageChannel channel = new BageChannel();
                if (userJson.has("uid"))
                    channel.channelID = userJson.optString("uid");
                if (userJson.has("name"))
                    channel.channelName = userJson.optString("name");
                if (userJson.has("avatar"))
                    channel.avatar = userJson.optString("avatar");
                userList.add(channel);
            }
        }
        return this;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channel_type", channelType);
            JSONArray jsonArray = new JSONArray();
            for (int i = 0, size = msgList.size(); i < size; i++) {
                JSONObject json = new JSONObject();
                if (!TextUtils.isEmpty(msgList.get(i).content)) {
                    json.put("payload", new JSONObject(msgList.get(i).content));
                }
                json.put("timestamp", msgList.get(i).timestamp);
                json.put("message_id", msgList.get(i).messageID);
                json.put("from_uid", msgList.get(i).fromUID);
                jsonArray.put(json);
            }
            jsonObject.put("msgs", jsonArray);
            if (BageReader.isNotEmpty(userList)) {
                JSONArray userArr = new JSONArray();
                for (int i = 0, size = userList.size(); i < size; i++) {
                    JSONObject json = new JSONObject();
                    json.put("uid", userList.get(i).channelID);
                    json.put("name", userList.get(i).channelName);
                    json.put("avatar", userList.get(i).avatar);
                    userArr.put(json);
                }
                jsonObject.put("users", userArr);
            }
        } catch (JSONException e) {
            BageLogUtils.e("编码合并转发消息错误");
        }
        return jsonObject;
    }

    @Override
    public String getDisplayContent() {
        return BageUIKitApplication.getInstance().getContext().getString(R.string.last_msg_chat_record);
    }

    @Override
    public String getSearchableWord() {
        return BageUIKitApplication.getInstance().getContext().getString(R.string.last_msg_chat_record);
    }

    public BageMultiForwardContent(Parcel in) {
        super(in);
        channelType = in.readByte();
        userList = in.createTypedArrayList(BageChannel.CREATOR);
        msgList = in.createTypedArrayList(BageMsg.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte(channelType);
        dest.writeTypedList(userList);
        dest.writeTypedList(msgList);
    }

    public static final Creator<BageMultiForwardContent> CREATOR = new Creator<BageMultiForwardContent>() {
        @Override
        public BageMultiForwardContent createFromParcel(Parcel in) {
            return new BageMultiForwardContent(in);
        }

        @Override
        public BageMultiForwardContent[] newArray(int size) {
            return new BageMultiForwardContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
