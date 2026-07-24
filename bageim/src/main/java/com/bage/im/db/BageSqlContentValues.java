package com.bage.im.db;

import android.content.ContentValues;
import android.text.TextUtils;

import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageConversationMsg;
import com.bage.im.entity.BageConversationMsgExtra;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgExtra;
import com.bage.im.entity.BageMsgReaction;
import com.bage.im.entity.BageMsgSetting;
import com.bage.im.entity.BageReminder;
import com.bage.im.utils.BageLoggerUtils;
import com.bage.im.utils.BageTypeUtils;

import org.json.JSONException;
import org.json.JSONObject;


class BageSqlContentValues {
    private final static String TAG = "BageSqlContentValues";

    static ContentValues getContentValuesWithMsg(BageMsg msg) {
        ContentValues contentValues = new ContentValues();
        if (msg == null) {
            return contentValues;
        }
        if (msg.setting == null) {
            msg.setting = new BageMsgSetting();
        }
        contentValues.put(BageDBColumns.BageMessageColumns.message_id, msg.messageID);
        contentValues.put(BageDBColumns.BageMessageColumns.message_seq, msg.messageSeq);

        contentValues.put(BageDBColumns.BageMessageColumns.order_seq, msg.orderSeq);
        contentValues.put(BageDBColumns.BageMessageColumns.timestamp, msg.timestamp);
        contentValues.put(BageDBColumns.BageMessageColumns.from_uid, msg.fromUID);
        contentValues.put(BageDBColumns.BageMessageColumns.channel_id, msg.channelID);
        contentValues.put(BageDBColumns.BageMessageColumns.channel_type, msg.channelType);
        contentValues.put(BageDBColumns.BageMessageColumns.is_deleted, msg.isDeleted);
        contentValues.put(BageDBColumns.BageMessageColumns.type, msg.type);
        contentValues.put(BageDBColumns.BageMessageColumns.content, msg.content);
        contentValues.put(BageDBColumns.BageMessageColumns.status, msg.status);
        contentValues.put(BageDBColumns.BageMessageColumns.created_at, msg.createdAt);
        contentValues.put(BageDBColumns.BageMessageColumns.updated_at, msg.updatedAt);
        contentValues.put(BageDBColumns.BageMessageColumns.voice_status, msg.voiceStatus);
        contentValues.put(BageDBColumns.BageMessageColumns.client_msg_no, msg.clientMsgNO);
        contentValues.put(BageDBColumns.BageMessageColumns.flame, msg.flame);
        contentValues.put(BageDBColumns.BageMessageColumns.flame_second, msg.flameSecond);
        contentValues.put(BageDBColumns.BageMessageColumns.viewed, msg.viewed);
        contentValues.put(BageDBColumns.BageMessageColumns.viewed_at, msg.viewedAt);
        contentValues.put(BageDBColumns.BageMessageColumns.topic_id, msg.topicID);
        contentValues.put(BageDBColumns.BageMessageColumns.expire_time, msg.expireTime);
        contentValues.put(BageDBColumns.BageMessageColumns.expire_timestamp, msg.expireTimestamp);
        byte setting = BageTypeUtils.getInstance().getMsgSetting(msg.setting);
        contentValues.put(BageDBColumns.BageMessageColumns.setting, setting);
        if (msg.baseContentMsgModel != null) {
            contentValues.put(BageDBColumns.BageMessageColumns.searchable_word, msg.baseContentMsgModel.getSearchableWord());
        }
        contentValues.put(BageDBColumns.BageMessageColumns.extra, msg.getLocalMapExtraString());
        return contentValues;
    }

    static ContentValues getContentValuesWithCoverMsg(BageConversationMsg bageConversationMsg, boolean isSync) {
        ContentValues contentValues = new ContentValues();
        if (bageConversationMsg == null) {
            return contentValues;
        }
        contentValues.put(BageDBColumns.BageCoverMessageColumns.channel_id, bageConversationMsg.channelID);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.channel_type, bageConversationMsg.channelType);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.last_client_msg_no, bageConversationMsg.lastClientMsgNO);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.last_msg_timestamp, bageConversationMsg.lastMsgTimestamp);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.last_msg_seq, bageConversationMsg.lastMsgSeq);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.unread_count, bageConversationMsg.unreadCount);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.parent_channel_id, bageConversationMsg.parentChannelID);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.parent_channel_type, bageConversationMsg.parentChannelType);
        if (isSync) {
            contentValues.put(BageDBColumns.BageCoverMessageColumns.version, bageConversationMsg.version);
        }
        contentValues.put(BageDBColumns.BageCoverMessageColumns.is_deleted, bageConversationMsg.isDeleted);
        contentValues.put(BageDBColumns.BageCoverMessageColumns.extra, bageConversationMsg.getLocalExtraString());
        return contentValues;
    }

    static ContentValues getContentValuesWithChannel(BageChannel channel) {
        ContentValues contentValues = new ContentValues();
        if (channel == null) {
            return contentValues;
        }
        contentValues.put(BageDBColumns.BageChannelColumns.channel_id, channel.channelID);
        contentValues.put(BageDBColumns.BageChannelColumns.channel_type, channel.channelType);
        contentValues.put(BageDBColumns.BageChannelColumns.channel_name, channel.channelName);
        contentValues.put(BageDBColumns.BageChannelColumns.channel_remark, channel.channelRemark);
        contentValues.put(BageDBColumns.BageChannelColumns.avatar, channel.avatar);
        contentValues.put(BageDBColumns.BageChannelColumns.top, channel.top);
        contentValues.put(BageDBColumns.BageChannelColumns.save, channel.save);
        contentValues.put(BageDBColumns.BageChannelColumns.mute, channel.mute);
        contentValues.put(BageDBColumns.BageChannelColumns.forbidden, channel.forbidden);
        contentValues.put(BageDBColumns.BageChannelColumns.invite, channel.invite);
        contentValues.put(BageDBColumns.BageChannelColumns.status, channel.status);
        contentValues.put(BageDBColumns.BageChannelColumns.is_deleted, channel.isDeleted);
        contentValues.put(BageDBColumns.BageChannelColumns.follow, channel.follow);
        contentValues.put(BageDBColumns.BageChannelColumns.version, channel.version);
        contentValues.put(BageDBColumns.BageChannelColumns.show_nick, channel.showNick);
        contentValues.put(BageDBColumns.BageChannelColumns.created_at, channel.createdAt);
        contentValues.put(BageDBColumns.BageChannelColumns.updated_at, channel.updatedAt);
        contentValues.put(BageDBColumns.BageChannelColumns.online, channel.online);
        contentValues.put(BageDBColumns.BageChannelColumns.last_offline, channel.lastOffline);
        contentValues.put(BageDBColumns.BageChannelColumns.receipt, channel.receipt);
        contentValues.put(BageDBColumns.BageChannelColumns.robot, channel.robot);
        contentValues.put(BageDBColumns.BageChannelColumns.category, channel.category);
        contentValues.put(BageDBColumns.BageChannelColumns.username, channel.username);
        contentValues.put(BageDBColumns.BageChannelColumns.avatar_cache_key, TextUtils.isEmpty(channel.avatarCacheKey) ? "" : channel.avatarCacheKey);
        contentValues.put(BageDBColumns.BageChannelColumns.flame, channel.flame);
        contentValues.put(BageDBColumns.BageChannelColumns.flame_second, channel.flameSecond);
        contentValues.put(BageDBColumns.BageChannelColumns.device_flag, channel.deviceFlag);
        contentValues.put(BageDBColumns.BageChannelColumns.parent_channel_id, channel.parentChannelID);
        contentValues.put(BageDBColumns.BageChannelColumns.parent_channel_type, channel.parentChannelType);

        if (channel.localExtra != null) {
            JSONObject jsonObject = new JSONObject(channel.localExtra);
            contentValues.put(BageDBColumns.BageChannelColumns.localExtra, jsonObject.toString());
        }
        if (channel.remoteExtraMap != null) {
            JSONObject jsonObject = new JSONObject(channel.remoteExtraMap);
            contentValues.put(BageDBColumns.BageChannelColumns.remote_extra, jsonObject.toString());
        }
        return contentValues;
    }

    static ContentValues getContentValuesWithChannelMember(BageChannelMember channelMember) {
        ContentValues contentValues = new ContentValues();
        if (channelMember == null) {
            return contentValues;
        }
        contentValues.put(BageDBColumns.BageChannelMembersColumns.channel_id, channelMember.channelID);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.channel_type, channelMember.channelType);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.member_invite_uid, channelMember.memberInviteUID);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.member_uid, channelMember.memberUID);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.member_name, channelMember.memberName);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.member_remark, channelMember.memberRemark);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.member_avatar, channelMember.memberAvatar);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.memberAvatarCacheKey, TextUtils.isEmpty(channelMember.memberAvatarCacheKey) ? "" : channelMember.memberAvatarCacheKey);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.role, channelMember.role);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.is_deleted, channelMember.isDeleted);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.version, channelMember.version);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.status, channelMember.status);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.robot, channelMember.robot);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.forbidden_expiration_time, channelMember.forbiddenExpirationTime);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.created_at, channelMember.createdAt);
        contentValues.put(BageDBColumns.BageChannelMembersColumns.updated_at, channelMember.updatedAt);

        if (channelMember.extraMap != null) {
            JSONObject jsonObject = new JSONObject(channelMember.extraMap);
            contentValues.put(BageDBColumns.BageChannelMembersColumns.extra, jsonObject.toString());
        }

        return contentValues;
    }

    static ContentValues getContentValuesWithMsgReaction(BageMsgReaction reaction) {
        ContentValues contentValues = new ContentValues();
        if (reaction == null) {
            return contentValues;
        }
        contentValues.put("channel_id", reaction.channelID);
        contentValues.put("channel_type", reaction.channelType);
        contentValues.put("message_id", reaction.messageID);
        contentValues.put("uid", reaction.uid);
        contentValues.put("name", reaction.name);
        contentValues.put("is_deleted", reaction.isDeleted);
        contentValues.put("seq", reaction.seq);
        contentValues.put("emoji", reaction.emoji);
        contentValues.put("created_at", reaction.createdAt);
        return contentValues;
    }

    static ContentValues getCVWithMsgExtra(BageMsgExtra extra) {
        ContentValues cv = new ContentValues();
        cv.put("channel_id", extra.channelID);
        cv.put("channel_type", extra.channelType);
        cv.put("message_id", extra.messageID);
        cv.put("readed", extra.readed);
        cv.put("readed_count", extra.readedCount);
        cv.put("unread_count", extra.unreadCount);
        cv.put("revoke", extra.revoke);
        cv.put("revoker", extra.revoker);
        cv.put("extra_version", extra.extraVersion);
        cv.put("is_mutual_deleted", extra.isMutualDeleted);
        cv.put("content_edit", extra.contentEdit);
        cv.put("edited_at", extra.editedAt);
        cv.put("need_upload", extra.needUpload);
        cv.put("is_pinned", extra.isPinned);
        return cv;
    }

    static ContentValues getCVWithReminder(BageReminder reminder) {
        ContentValues cv = new ContentValues();
        cv.put("channel_id", reminder.channelID);
        cv.put("channel_type", reminder.channelType);
        cv.put("reminder_id", reminder.reminderID);
        cv.put("message_id", reminder.messageID);
        cv.put("message_seq", reminder.messageSeq);
        cv.put("uid", reminder.uid);
        cv.put("type", reminder.type);
        cv.put("is_locate", reminder.isLocate);
        cv.put("text", reminder.text);
        cv.put("version", reminder.version);
        cv.put("done", reminder.done);
        cv.put("need_upload", reminder.needUpload);
        cv.put("publisher", reminder.publisher);

        if (reminder.data != null) {
            JSONObject jsonObject = new JSONObject();
            for (Object key : reminder.data.keySet()) {
                try {
                    jsonObject.put(String.valueOf(key), reminder.data.get(key));
                } catch (JSONException e) {
                    BageLoggerUtils.getInstance().e(TAG, "getCVWithReminder error");
                }
            }
            cv.put("data", jsonObject.toString());
        }
        return cv;
    }

    static ContentValues getCVWithExtra(BageConversationMsgExtra extra) {
        ContentValues cv = new ContentValues();
        cv.put("channel_id", extra.channelID);
        cv.put("channel_type", extra.channelType);
        cv.put("browse_to", extra.browseTo);
        cv.put("keep_message_seq", extra.keepMessageSeq);
        cv.put("keep_offset_y", extra.keepOffsetY);
        cv.put("draft", extra.draft);
        cv.put("draft_updated_at", extra.draftUpdatedAt);
        cv.put("version", extra.version);
        return cv;
    }
}
