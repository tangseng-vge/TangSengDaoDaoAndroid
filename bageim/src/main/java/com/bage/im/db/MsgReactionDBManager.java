package com.bage.im.db;

import static com.bage.im.db.BageDBColumns.TABLE.messageReaction;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMsgReaction;
import com.bage.im.utils.BageCommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 4/16/21 1:46 PM
 * 消息回应
 */
class MsgReactionDBManager {
    private MsgReactionDBManager() {
    }

    private static class MessageReactionDBManagerBinder {
        final static MsgReactionDBManager manager = new MsgReactionDBManager();
    }

    public static MsgReactionDBManager getInstance() {
        return MessageReactionDBManagerBinder.manager;
    }

    public void insertReactions(List<BageMsgReaction> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        List<ContentValues> insertCVs = new ArrayList<>();
        for (BageMsgReaction reaction : list) {
            insertCVs.add(BageSqlContentValues.getContentValuesWithMsgReaction(reaction));
        }
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            if (!insertCVs.isEmpty()) {
                for (ContentValues cv : insertCVs) {
                    BageIMApplication.getInstance().getDbHelper().insert(messageReaction, cv);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception ignored) {
        } finally {
            try {
                if (db.inTransaction()) db.endTransaction();
            } catch (Exception ignored2) {
            }
        }
    }

    public List<BageMsgReaction> queryWithMessageId(String messageID) {
        List<BageMsgReaction> list = new ArrayList<>();
        String sql = "select * from " + messageReaction + " where message_id=? and is_deleted=0 ORDER BY created_at desc";
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, new Object[]{messageID})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageMsgReaction reaction = serializeReaction(cursor);
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(reaction.uid, BageChannelType.PERSONAL);
                if (channel != null) {
                    String showName = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
                    if (!TextUtils.isEmpty(showName))
                        reaction.name = showName;
                }
                list.add(reaction);
            }
        }
        return list;
    }

    public List<BageMsgReaction> queryWithMessageIds(List<String> messageIds) {
        List<BageMsgReaction> list = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(messageReaction, "message_id in (" + BageCursor.getPlaceholders(messageIds.size()) + ")", messageIds.toArray(new String[0]), "created_at desc")) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageMsgReaction msgReaction = serializeReaction(cursor);
                channelIds.add(msgReaction.uid);
                list.add(msgReaction);
            }
        } catch (Exception ignored) {
        }
        //查询用户备注
        List<BageChannel> channelList = ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(channelIds, BageChannelType.PERSONAL);
        for (int i = 0, size = list.size(); i < size; i++) {
            for (int j = 0, len = channelList.size(); j < len; j++) {
                if (channelList.get(j).channelID.equals(list.get(i).uid)) {
                    list.get(i).name = TextUtils.isEmpty(channelList.get(j).channelRemark) ? channelList.get(j).channelName : channelList.get(j).channelRemark;
                }
            }
        }
        return list;
    }

    public long queryMaxSeqWithChannel(String channelID, byte channelType) {
        int maxSeq = 0;
        String sql = "select max(seq) seq from " + messageReaction
                + " where channel_id=? and channel_type=? limit 0, 1";
        try {
            if (BageIMApplication.getInstance().getDbHelper() != null) {
                Cursor cursor = BageIMApplication
                        .getInstance()
                        .getDbHelper()
                        .rawQuery(sql, new Object[]{channelID, channelType});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        maxSeq = BageCursor.readInt(cursor, "seq");
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxSeq;
    }

    private BageMsgReaction serializeReaction(Cursor cursor) {
        BageMsgReaction reaction = new BageMsgReaction();
        reaction.channelID = BageCursor.readString(cursor, "channel_id");
        reaction.channelType = (byte) BageCursor.readInt(cursor, "channel_type");
        reaction.uid = BageCursor.readString(cursor, "uid");
        reaction.name = BageCursor.readString(cursor, "name");
        reaction.messageID = BageCursor.readString(cursor, "message_id");
        reaction.createdAt = BageCursor.readString(cursor, "created_at");
        reaction.seq = BageCursor.readLong(cursor, "seq");
        reaction.emoji = BageCursor.readString(cursor, "emoji");
        reaction.isDeleted = BageCursor.readInt(cursor, "is_deleted");
        return reaction;
    }
}
