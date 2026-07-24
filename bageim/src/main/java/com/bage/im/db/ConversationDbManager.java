package com.bage.im.db;

import static com.bage.im.db.BageDBColumns.TABLE.channel;
import static com.bage.im.db.BageDBColumns.TABLE.conversation;
import static com.bage.im.db.BageDBColumns.TABLE.conversationExtra;
import static com.bage.im.db.BageDBColumns.TABLE.message;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsg;
import com.bage.im.entity.BageConversationMsgExtra;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgExtra;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.manager.ConversationManager;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 5/21/21 12:14 PM
 * 最近会话
 */
public class ConversationDbManager {
    private final String TAG = "ConversationDbManager";
    private final String extraCols = "IFNULL(" + conversationExtra + ".browse_to,0) AS browse_to,IFNULL(" + conversationExtra + ".keep_message_seq,0) AS keep_message_seq,IFNULL(" + conversationExtra + ".keep_offset_y,0) AS keep_offset_y,IFNULL(" + conversationExtra + ".draft,'') AS draft,IFNULL(" + conversationExtra + ".version,0) AS extra_version";
    private final String channelCols = channel + ".channel_remark," +
            channel + ".channel_name," +
            channel + ".top," +
            channel + ".mute," +
            channel + ".save," +
            channel + ".status as channel_status," +
            channel + ".forbidden," +
            channel + ".invite," +
            channel + ".follow," +
            channel + ".is_deleted as channel_is_deleted," +
            channel + ".show_nick," +
            channel + ".avatar," +
            channel + ".avatar_cache_key," +
            channel + ".online," +
            channel + ".last_offline," +
            channel + ".category," +
            channel + ".receipt," +
            channel + ".robot," +
            channel + ".parent_channel_id AS c_parent_channel_id," +
            channel + ".parent_channel_type AS c_parent_channel_type," +
            channel + ".version AS channel_version," +
            channel + ".remote_extra AS channel_remote_extra," +
            channel + ".extra AS channel_extra";

    private ConversationDbManager() {
    }

    private static class ConversationDbManagerBinder {
        static final ConversationDbManager db = new ConversationDbManager();
    }

    public static ConversationDbManager getInstance() {
        return ConversationDbManagerBinder.db;
    }

    public synchronized List<BageUIConversationMsg> queryAll() {
        List<BageUIConversationMsg> list = new ArrayList<>();
        if (BageIMApplication.getInstance().getDbHelper() == null || BageIMApplication.getInstance().getDbHelper().getDb() == null) {
            return list;
        }

        String sql = "SELECT " + conversation + ".*," + channelCols + "," + extraCols + " FROM "
                + conversation + " LEFT JOIN " + channel + " ON "
                + conversation + ".channel_id = " + channel + ".channel_id AND "
                + conversation + ".channel_type = " + channel + ".channel_type LEFT JOIN " + conversationExtra + " ON " + conversation + ".channel_id=" + conversationExtra + ".channel_id AND " + conversation + ".channel_type=" + conversationExtra + ".channel_type where " + conversation + ".is_deleted=0 order by "
                + BageDBColumns.BageCoverMessageColumns.last_msg_timestamp + " desc";
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            List<String> clientMsgNos = new ArrayList<>();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageConversationMsg msg = serializeMsg(cursor);
                if (msg.isDeleted == 0) {
                    BageUIConversationMsg uiMsg = getUIMsg(msg, cursor);
                    list.add(uiMsg);
                    clientMsgNos.add(uiMsg.clientMsgNo);
                }
            }
            if (!clientMsgNos.isEmpty()) {
                List<BageMsg> msgList = queryWithClientMsgNos(clientMsgNos);
                List<String> msgIds = new ArrayList<>();
                if (BageCommonUtils.isNotEmpty(msgList)) {
                    for (BageUIConversationMsg uiMsg : list) {
                        for (BageMsg msg : msgList) {
                            if (uiMsg.clientMsgNo.equals(msg.clientMsgNO)) {
                                uiMsg.setBageMsg(msg);
                                if (!TextUtils.isEmpty(msg.messageID)) {
                                    msgIds.add(msg.messageID);
                                }
                                break;
                            }
                        }
                    }
                }
                List<BageMsgExtra> extraList = queryWithMsgIds(msgIds);
                if (BageCommonUtils.isNotEmpty(extraList)) {
                    for (BageUIConversationMsg uiMsg : list) {
                        for (BageMsgExtra extra : extraList) {
                            if (uiMsg.getBageMsg() != null && !TextUtils.isEmpty(uiMsg.getBageMsg().messageID) && uiMsg.getBageMsg().messageID.equals(extra.messageID)) {
                                uiMsg.getBageMsg().remoteExtra = extra;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "queryAll error");
        }
        return list;
    }

    private List<BageMsgExtra> queryWithMsgIds(List<String> msgIds) {
        List<BageMsgExtra> msgExtraList = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (int i = 0, size = msgIds.size(); i < size; i++) {
            if (ids.size() == 200) {
                List<BageMsgExtra> list = MsgDbManager.getInstance().queryMsgExtrasWithMsgIds(ids);
                if (BageCommonUtils.isNotEmpty(list)) {
                    msgExtraList.addAll(list);
                }
                ids.clear();
            }
            ids.add(msgIds.get(i));
        }
        if (!ids.isEmpty()) {
            List<BageMsgExtra> list = MsgDbManager.getInstance().queryMsgExtrasWithMsgIds(ids);
            if (BageCommonUtils.isNotEmpty(list)) {
                msgExtraList.addAll(list);
            }
        }
        return msgExtraList;
    }

    private List<BageMsg> queryWithClientMsgNos(List<String> clientMsgNos) {
        List<BageMsg> msgList = new ArrayList<>();
        List<String> nos = new ArrayList<>();
        for (int i = 0, size = clientMsgNos.size(); i < size; i++) {
            if (nos.size() == 200) {
                List<BageMsg> list = MsgDbManager.getInstance().queryWithClientMsgNos(nos);
                if (BageCommonUtils.isNotEmpty(list)) {
                    msgList.addAll(list);
                }
                nos.clear();
            }
            nos.add(clientMsgNos.get(i));
        }
        if (!nos.isEmpty()) {
            List<BageMsg> list = MsgDbManager.getInstance().queryWithClientMsgNos(nos);
            if (BageCommonUtils.isNotEmpty(list)) {
                msgList.addAll(list);
            }
            nos.clear();
        }
        return msgList;
    }

    public List<BageUIConversationMsg> queryWithChannelIds(List<String> channelIds) {
        List<BageUIConversationMsg> list = new ArrayList<>();
        if (channelIds == null || channelIds.isEmpty()) return list;
        String sql = "select " + conversation + ".*," + channelCols + "," + extraCols + " from " + conversation + " left join " + channel + " on " + conversation + ".channel_id=" + channel + ".channel_id and " + conversation + ".channel_type=" + channel + ".channel_type left join " + conversationExtra + " on " + conversation + ".channel_id=" + conversationExtra + ".channel_id and " + conversation + ".channel_type=" + conversationExtra + ".channel_type where " + conversation + ".is_deleted=0 and " + conversation + ".channel_id in (" + BageCursor.getPlaceholders(channelIds.size()) + ")";
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql, channelIds.toArray(new String[0]))) {
            if (cursor == null) {
                return list;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageConversationMsg msg = serializeMsg(cursor);
                BageUIConversationMsg uiMsg = getUIMsg(msg, cursor);
                list.add(uiMsg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<BageConversationMsg> queryWithChannelType(byte channelType) {
        List<BageConversationMsg> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .select(conversation, "channel_type=?", new String[]{String.valueOf(channelType)}, null)) {
            if (cursor == null) {
                return list;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageConversationMsg msg = serializeMsg(cursor);
                list.add(msg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private BageUIConversationMsg getUIMsg(BageConversationMsg msg, Cursor cursor) {
        BageUIConversationMsg uiMsg = getUIMsg(msg);
        BageChannel channel = ChannelDBManager.getInstance().serializableChannel(cursor);
        if (channel != null) {
            String extra = BageCursor.readString(cursor, "channel_extra");
            channel.localExtra = BageCommonUtils.str2HashMap(extra);
            String remoteExtra = BageCursor.readString(cursor, "channel_remote_extra");
            channel.remoteExtraMap = BageCommonUtils.str2HashMap(remoteExtra);
            channel.status = BageCursor.readInt(cursor, "channel_status");
            channel.version = BageCursor.readLong(cursor, "channel_version");
            channel.parentChannelID = BageCursor.readString(cursor, "c_parent_channel_id");
            channel.parentChannelType = BageCursor.readByte(cursor, "c_parent_channel_type");
            channel.channelID = msg.channelID;
            channel.channelType = msg.channelType;
            uiMsg.setBageChannel(channel);
        }
        return uiMsg;
    }

    public long queryMaxVersion() {
        long maxVersion = 0;
        String sql = "select max(version) version from " + conversation + " limit 0, 1";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxVersion = BageCursor.readLong(cursor, BageDBColumns.BageCoverMessageColumns.version);
            }
            cursor.close();
        }
        return maxVersion;
    }

    public synchronized ContentValues getInsertSyncCV(BageConversationMsg conversationMsg) {
        return BageSqlContentValues.getContentValuesWithCoverMsg(conversationMsg, true);
    }

    public synchronized void insertSyncMsg(ContentValues cv) {
        BageIMApplication.getInstance().getDbHelper().insertSql(conversation, cv);
    }

    public synchronized String queryLastMsgSeqs() {
        String lastMsgSeqs = "";
        String sql = "select GROUP_CONCAT(channel_id||':'||channel_type||':'|| last_seq,'|') synckey from (select *,(select max(message_seq) from " + message + " where " + message + ".channel_id=" + conversation + ".channel_id and " + message + ".channel_type=" + conversation + ".channel_type limit 1) last_seq from " + conversation + ") cn where channel_id<>'' AND is_deleted=0";
        Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql);
        if (cursor == null) {
            return lastMsgSeqs;
        }
        if (cursor.moveToFirst()) {
            lastMsgSeqs = BageCursor.readString(cursor, "synckey");
        }
        cursor.close();

        return lastMsgSeqs;
    }

    public synchronized boolean updateRedDot(String channelID, byte channelType, int redDot) {
        if (BageIMApplication.getInstance().getDbHelper() == null || BageIMApplication.getInstance().getDbHelper().getDb() == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(BageDBColumns.BageCoverMessageColumns.unread_count, redDot);
        return BageIMApplication.getInstance().getDbHelper().update(conversation, BageDBColumns.BageCoverMessageColumns.channel_id + "='" + channelID + "' and " + BageDBColumns.BageCoverMessageColumns.channel_type + "=" + channelType, cv);
    }

    public synchronized void updateMsg(String channelID, byte channelType, String clientMsgNo, long lastMsgSeq, int count) {
        String[] update = new String[2];
        update[0] = channelID;
        update[1] = String.valueOf(channelType);
        ContentValues cv = new ContentValues();
        try {
            cv.put(BageDBColumns.BageCoverMessageColumns.last_client_msg_no, clientMsgNo);
            cv.put(BageDBColumns.BageCoverMessageColumns.last_msg_seq, lastMsgSeq);
            cv.put(BageDBColumns.BageCoverMessageColumns.unread_count, count);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "updateMsg error");
        }
        BageIMApplication.getInstance().getDbHelper()
                .update(conversation, cv, BageDBColumns.BageCoverMessageColumns.channel_id + "=? and " + BageDBColumns.BageCoverMessageColumns.channel_type + "=?", update);
    }

    public BageConversationMsg queryWithChannel(String channelID, byte channelType) {
        String sql = "select " + conversation + ".*," + channelCols + "," + extraCols + " from " + conversation + " left join " + channel + " on " + conversation + ".channel_id=" + channel + ".channel_id and " + conversation + ".channel_type=" + channel + ".channel_type left join " + conversationExtra + " on " + conversation + ".channel_id=" + conversationExtra + ".channel_id and " + conversation + ".channel_type=" + conversationExtra + ".channel_type where " + conversation + ".channel_id=? and " + conversation + ".channel_type=? and " + conversation + ".is_deleted=0";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql, new Object[]{channelID, channelType});
        BageConversationMsg conversationMsg = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                conversationMsg = serializeMsg(cursor);
            }
            cursor.close();
        }
        return conversationMsg;
    }

    public synchronized boolean deleteWithChannel(String channelID, byte channelType, int isDeleted) {
        String[] update = new String[2];
        update[0] = channelID;
        update[1] = String.valueOf(channelType);
        ContentValues cv = new ContentValues();
        try {
            cv.put(BageDBColumns.BageCoverMessageColumns.is_deleted, isDeleted);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "deleteWithChannel error");
        }

        boolean result = BageIMApplication.getInstance().getDbHelper()
                .update(conversation, cv, BageDBColumns.BageCoverMessageColumns.channel_id + "=? and " + BageDBColumns.BageCoverMessageColumns.channel_type + "=?", update);
        if (result) {
            ConversationManager.getInstance().setDeleteMsg(channelID, channelType);
        }
        return result;

    }

    public synchronized BageUIConversationMsg insertOrUpdateWithMsg(BageMsg msg, int unreadCount) {
        if (msg.channelID.equals(BageIMApplication.getInstance().getUid())) return null;
        BageConversationMsg bageConversationMsg = new BageConversationMsg();
        if (msg.channelType == BageChannelType.COMMUNITY_TOPIC && !TextUtils.isEmpty(msg.channelID)) {
            if (msg.channelID.contains("@")) {
                String[] str = msg.channelID.split("@");
                bageConversationMsg.parentChannelID = str[0];
                bageConversationMsg.parentChannelType = BageChannelType.COMMUNITY;
            }
        }
        bageConversationMsg.channelID = msg.channelID;
        bageConversationMsg.channelType = msg.channelType;
//        bageConversationMsg.localExtraMap = msg.localExtraMap;
        bageConversationMsg.lastMsgTimestamp = msg.timestamp;
        bageConversationMsg.lastClientMsgNO = msg.clientMsgNO;
        bageConversationMsg.lastMsgSeq = msg.messageSeq;
        bageConversationMsg.unreadCount = unreadCount;
        return insertOrUpdateWithConvMsg(bageConversationMsg);// 插入消息列表数据表
    }

    public synchronized BageUIConversationMsg insertOrUpdateWithConvMsg(BageConversationMsg conversationMsg) {
        boolean result;
        BageConversationMsg lastMsg = queryWithChannelId(conversationMsg.channelID, conversationMsg.channelType);
        if (lastMsg == null || TextUtils.isEmpty(lastMsg.channelID)) {
            //如果服务器自增id为0则表示是本地数据|直接保存
            result = insert(conversationMsg);
        } else {
            conversationMsg.unreadCount = lastMsg.unreadCount + conversationMsg.unreadCount;
            result = update(conversationMsg);
        }
        if (result) {
            return getUIMsg(conversationMsg);
        }
        return null;
    }

    private synchronized boolean insert(BageConversationMsg msg) {
        ContentValues cv = new ContentValues();
        try {
            cv = BageSqlContentValues.getContentValuesWithCoverMsg(msg, false);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "insert error");
        }
        long result = -1;
        try {
            result = BageIMApplication.getInstance().getDbHelper()
                    .insert(conversation, cv);
        } catch (Exception ignored) {
        }
        return result > 0;
    }

    /**
     * 更新会话记录消息
     *
     * @param msg 会话消息
     * @return 修改结果
     */
    private synchronized boolean update(BageConversationMsg msg) {
        String[] update = new String[2];
        update[0] = msg.channelID;
        update[1] = String.valueOf(msg.channelType);
        ContentValues cv = new ContentValues();
        try {
            cv = BageSqlContentValues.getContentValuesWithCoverMsg(msg, false);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "update error");
        }
        return BageIMApplication.getInstance().getDbHelper()
                .update(conversation, cv, BageDBColumns.BageCoverMessageColumns.channel_id + "=? and " + BageDBColumns.BageCoverMessageColumns.channel_type + "=?", update);
    }

    private synchronized BageConversationMsg queryWithChannelId(String channelId, byte channelType) {
        BageConversationMsg msg = null;
        String selection = BageDBColumns.BageCoverMessageColumns.channel_id + " = ? and " + BageDBColumns.BageCoverMessageColumns.channel_type + "=?";
        String[] selectionArgs = new String[]{channelId, String.valueOf(channelType)};
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .select(conversation, selection, selectionArgs,
                        null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                msg = serializeMsg(cursor);
            }
            cursor.close();
        }
        return msg;
    }


    public synchronized boolean clearEmpty() {
        return BageIMApplication.getInstance().getDbHelper()
                .delete(conversation, null, null);
    }

    public BageConversationMsgExtra queryMsgExtraWithChannel(String channelID, byte channelType) {
        BageConversationMsgExtra msgExtra = null;
        String selection = "channel_id=? and channel_type=?";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(conversationExtra, selection, new String[]{channelID, String.valueOf(channelType)}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                msgExtra = serializeMsgExtra(cursor);
            }
            cursor.close();
        }
        return msgExtra;
    }

    private List<BageConversationMsgExtra> queryWithExtraChannelIds(List<String> channelIds) {
        List<BageConversationMsgExtra> list = new ArrayList<>();
        if (channelIds == null || channelIds.isEmpty()) return list;
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().select(conversationExtra, "channel_id in (" + BageCursor.getPlaceholders(channelIds.size()) + ")", channelIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageConversationMsgExtra extra = serializeMsgExtra(cursor);
                list.add(extra);
            }
        }
        return list;
    }

    public synchronized boolean insertOrUpdateMsgExtra(BageConversationMsgExtra extra) {
        BageConversationMsgExtra msgExtra = queryMsgExtraWithChannel(extra.channelID, extra.channelType);
        boolean isAdd = true;
        if (msgExtra != null) {
            extra.version = msgExtra.version;
            isAdd = false;
        }
        ContentValues cv = BageSqlContentValues.getCVWithExtra(extra);
        if (isAdd) {
            return BageIMApplication.getInstance().getDbHelper().insert(conversationExtra, cv) > 0;
        }
        return BageIMApplication.getInstance().getDbHelper().update(conversationExtra, "channel_id='" + extra.channelID + "' and channel_type=" + extra.channelType, cv);
    }

    public synchronized void insertMsgExtras(List<BageConversationMsgExtra> list) {
        List<String> channelIds = new ArrayList<>();
        for (BageConversationMsgExtra extra : list) {
            boolean isAdd = true;
            for (String channelID : channelIds) {
                if (channelID.equals(extra.channelID)) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) channelIds.add(extra.channelID);
        }
        List<ContentValues> insertCVList = new ArrayList<>();
        List<ContentValues> updateCVList = new ArrayList<>();
        List<BageConversationMsgExtra> existList = queryWithExtraChannelIds(channelIds);
        for (BageConversationMsgExtra extra : list) {
            boolean isAdd = true;
            for (BageConversationMsgExtra existExtra : existList) {
                if (existExtra.channelID.equals(extra.channelID) && existExtra.channelType == extra.channelType) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                insertCVList.add(BageSqlContentValues.getCVWithExtra(extra));
            } else {
                updateCVList.add(BageSqlContentValues.getCVWithExtra(extra));
            }
        }

        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            if (BageCommonUtils.isNotEmpty(insertCVList)) {
                for (ContentValues cv : insertCVList) {
                    BageIMApplication.getInstance().getDbHelper()
                            .insert(conversationExtra, cv);
                }
            }
            if (BageCommonUtils.isNotEmpty(updateCVList)) {
                for (ContentValues cv : updateCVList) {
                    String[] sv = new String[2];
                    sv[0] = cv.getAsString("channel_id");
                    sv[1] = cv.getAsString("channel_type");
                    BageIMApplication.getInstance().getDbHelper()
                            .update(conversationExtra, cv, "channel_id=? and channel_type=?", sv);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            try {
                if (db.inTransaction()) db.endTransaction();
            } catch (Exception ignored) {
            }
        }
        List<BageUIConversationMsg> uiMsgList = ConversationDbManager.getInstance().queryWithChannelIds(channelIds);
//        for (int i = 0, size = uiMsgList.size(); i < size; i++) {
//            BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == uiMsgList.size() - 1, "saveMsgExtras");
//        }
        BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsgList,"saveMsgExtras");
    }

    public long queryMsgExtraMaxVersion() {
        long maxVersion = 0;
        String sql = "select max(version) version from " + conversationExtra;
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxVersion = BageCursor.readLong(cursor, "version");
            }
            cursor.close();
        }
        return maxVersion;
    }

    private synchronized BageConversationMsgExtra serializeMsgExtra(Cursor cursor) {
        BageConversationMsgExtra extra = new BageConversationMsgExtra();
        extra.channelID = BageCursor.readString(cursor, "channel_id");
        extra.channelType = (byte) BageCursor.readInt(cursor, "channel_type");
        extra.keepMessageSeq = BageCursor.readLong(cursor, "keep_message_seq");
        extra.keepOffsetY = BageCursor.readInt(cursor, "keep_offset_y");
        extra.draft = BageCursor.readString(cursor, "draft");
        extra.browseTo = BageCursor.readLong(cursor, "browse_to");
        extra.draftUpdatedAt = BageCursor.readLong(cursor, "draft_updated_at");
        extra.version = BageCursor.readLong(cursor, "version");
        if (cursor.getColumnIndex("extra_version") > 0) {
            extra.version = BageCursor.readLong(cursor, "extra_version");
        }
        return extra;
    }

    private synchronized BageConversationMsg serializeMsg(Cursor cursor) {
        BageConversationMsg msg = new BageConversationMsg();
        msg.channelID = BageCursor.readString(cursor, BageDBColumns.BageCoverMessageColumns.channel_id);
        msg.channelType = BageCursor.readByte(cursor, BageDBColumns.BageCoverMessageColumns.channel_type);
        msg.lastMsgTimestamp = BageCursor.readLong(cursor, BageDBColumns.BageCoverMessageColumns.last_msg_timestamp);
        msg.unreadCount = BageCursor.readInt(cursor, BageDBColumns.BageCoverMessageColumns.unread_count);
        msg.isDeleted = BageCursor.readInt(cursor, BageDBColumns.BageCoverMessageColumns.is_deleted);
        msg.version = BageCursor.readLong(cursor, BageDBColumns.BageCoverMessageColumns.version);
        msg.lastClientMsgNO = BageCursor.readString(cursor, BageDBColumns.BageCoverMessageColumns.last_client_msg_no);
        msg.lastMsgSeq = BageCursor.readLong(cursor, BageDBColumns.BageCoverMessageColumns.last_msg_seq);
        msg.parentChannelID = BageCursor.readString(cursor, BageDBColumns.BageCoverMessageColumns.parent_channel_id);
        msg.parentChannelType = BageCursor.readByte(cursor, BageDBColumns.BageCoverMessageColumns.parent_channel_type);
        String extra = BageCursor.readString(cursor, BageDBColumns.BageCoverMessageColumns.extra);
        if (!TextUtils.isEmpty(extra)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            try {
                JSONObject jsonObject = new JSONObject(extra);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    hashMap.put(key, jsonObject.opt(key));
                }
            } catch (JSONException e) {
                BageLoggerUtils.getInstance().e(TAG, "serializeMsg error");
            }
            msg.localExtraMap = hashMap;
        }
        msg.msgExtra = serializeMsgExtra(cursor);
        return msg;
    }

    public BageUIConversationMsg getUIMsg(BageConversationMsg conversationMsg) {
        BageUIConversationMsg msg = new BageUIConversationMsg();
        msg.lastMsgSeq = conversationMsg.lastMsgSeq;
        msg.clientMsgNo = conversationMsg.lastClientMsgNO;
        msg.unreadCount = conversationMsg.unreadCount;
        msg.lastMsgTimestamp = conversationMsg.lastMsgTimestamp;
        msg.channelID = conversationMsg.channelID;
        msg.channelType = conversationMsg.channelType;
        msg.isDeleted = conversationMsg.isDeleted;
        msg.parentChannelID = conversationMsg.parentChannelID;
        msg.parentChannelType = conversationMsg.parentChannelType;
        msg.setRemoteMsgExtra(conversationMsg.msgExtra);
        return msg;
    }
}
