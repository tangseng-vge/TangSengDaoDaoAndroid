package com.bage.im.db;

import static com.bage.im.db.BageDBColumns.TABLE.channel;
import static com.bage.im.db.BageDBColumns.TABLE.channelMembers;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bage.im.BageIMApplication;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelSearchResult;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 5/20/21 5:53 PM
 * channel DB manager
 */
public class ChannelDBManager {
    private static final String TAG = "ChannelDBManager";

    private ChannelDBManager() {
    }

    private static class ChannelDBManagerBinder {
        static final ChannelDBManager channelDBManager = new ChannelDBManager();
    }

    public static ChannelDBManager getInstance() {
        return ChannelDBManagerBinder.channelDBManager;
    }

    public List<BageChannel> queryWithChannelIds(List<String> channelIDs) {
        List<BageChannel> list = new ArrayList<>();
        if (BageIMApplication
                .getInstance()
                .getDbHelper() == null) {
            return list;
        }
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .select(channel, "channel_id in (" + BageCursor.getPlaceholders(channelIDs.size()) + ")", channelIDs.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageChannel channel = serializableChannel(cursor);
                list.add(channel);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<BageChannel> queryWithChannelIdsAndChannelType(List<String> channelIDs, byte channelType) {
        List<BageChannel> list = new ArrayList<>();
        if (BageIMApplication
                .getInstance()
                .getDbHelper() == null) {
            return list;
        }
        List<String> args = new ArrayList<>(channelIDs);
        args.add(String.valueOf(channelType));
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .select(channel, "channel_id in (" + BageCursor.getPlaceholders(channelIDs.size()) + ") and channel_type=?", args.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageChannel channel = serializableChannel(cursor);
                list.add(channel);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public synchronized BageChannel query(String channelId, int channelType) {
        String selection = BageDBColumns.BageChannelColumns.channel_id + "=? and " + BageDBColumns.BageChannelColumns.channel_type + "=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelId;
        selectionArgs[1] = String.valueOf(channelType);
        Cursor cursor = null;
        BageChannel bageChannel = null;
        if (BageIMApplication
                .getInstance()
                .getDbHelper() == null) {
            return null;
        }
        try {
            cursor = BageIMApplication
                    .getInstance()
                    .getDbHelper()
                    .select(channel, selection, selectionArgs,
                            null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    bageChannel = serializableChannel(cursor);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return bageChannel;
    }

    private boolean isExist(String channelId, int channelType) {
        String selection = BageDBColumns.BageChannelColumns.channel_id + "=? and " + BageDBColumns.BageChannelColumns.channel_type + "=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelId;
        selectionArgs[1] = String.valueOf(channelType);
        Cursor cursor = null;
        boolean isExist = false;
        try {
            if (BageIMApplication
                    .getInstance()
                    .getDbHelper() == null) {
                return false;
            }
            cursor = BageIMApplication
                    .getInstance()
                    .getDbHelper()
                    .select(channel, selection, selectionArgs,
                            null);
            if (cursor != null && cursor.moveToNext()) {
                isExist = true;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return isExist;
    }

    public synchronized void insertChannels(List<BageChannel> list) {
        List<ContentValues> newCVList = new ArrayList<>();
        for (BageChannel channel : list) {
            ContentValues cv = BageSqlContentValues.getContentValuesWithChannel(channel);
            newCVList.add(cv);
        }
        if (BageIMApplication.getInstance().getDbHelper() == null) return;
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            for (ContentValues cv : newCVList) {
                BageIMApplication.getInstance().getDbHelper()
                        .insert(channel, cv);
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

    public synchronized void insertOrUpdate(BageChannel channel) {
        if (isExist(channel.channelID, channel.channelType)) {
            update(channel);
        } else {
            insert(channel);
        }
    }

    private synchronized void insert(BageChannel bageChannel) {
        ContentValues cv = new ContentValues();
        try {
            cv = BageSqlContentValues.getContentValuesWithChannel(bageChannel);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "insert channel error");
        }
        if (BageIMApplication.getInstance().getDbHelper() == null) {
            return;
        }
        BageIMApplication.getInstance().getDbHelper()
                .insert(channel, cv);
    }

    public synchronized void update(BageChannel bageChannel) {
        String[] update = new String[2];
        update[0] = bageChannel.channelID;
        update[1] = String.valueOf(bageChannel.channelType);
        ContentValues cv = new ContentValues();
        try {
            cv = BageSqlContentValues.getContentValuesWithChannel(bageChannel);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "update channel error");
        }
        if (BageIMApplication.getInstance().getDbHelper() == null) {
            return;
        }
        BageIMApplication.getInstance().getDbHelper()
                .update(channel, cv, BageDBColumns.BageChannelColumns.channel_id + "=? and " + BageDBColumns.BageChannelColumns.channel_type + "=?", update);

    }

    /**
     * 查询频道
     *
     * @param channelType 频道类型
     * @param follow      是否关注 好友或陌生人
     * @param status      状态 正常或黑名单
     * @return List<BageChannel>
     */
    public synchronized List<BageChannel> queryWithFollowAndStatus(byte channelType, int follow, int status) {
        String[] args = new String[3];
        args[0] = String.valueOf(channelType);
        args[1] = String.valueOf(follow);
        args[2] = String.valueOf(status);
        String selection = BageDBColumns.BageChannelColumns.channel_type + "=? and " + BageDBColumns.BageChannelColumns.follow + "=? and " + BageDBColumns.BageChannelColumns.status + "=? and is_deleted=0";
        List<BageChannel> channels = new ArrayList<>();
        if (BageIMApplication
                .getInstance()
                .getDbHelper() != null) {
            try (Cursor cursor = BageIMApplication
                    .getInstance()
                    .getDbHelper().select(channel, selection, args, null)) {
                if (cursor == null) {
                    return channels;
                }
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    channels.add(serializableChannel(cursor));
                }
            }
        }

        return channels;
    }

    /**
     * 查下指定频道类型和频道状态的频道
     *
     * @param channelType 频道类型
     * @param status      状态[sdk不维护状态]
     * @return List<BageChannel>
     */
    public synchronized List<BageChannel> queryWithStatus(byte channelType, int status) {
        String[] args = new String[2];
        args[0] = String.valueOf(channelType);
        args[1] = String.valueOf(status);
        String selection = BageDBColumns.BageChannelColumns.channel_type + "=? and " + BageDBColumns.BageChannelColumns.status + "=?";
        List<BageChannel> channels = new ArrayList<>();
        if (BageIMApplication.getInstance().getDbHelper() == null) {
            return channels;
        }
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(channel, selection, args, null)) {
            if (cursor == null) {
                return channels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                channels.add(serializableChannel(cursor));
            }
        }
        return channels;
    }

    public synchronized List<BageChannelSearchResult> search(String searchKey) {
        List<BageChannelSearchResult> list = new ArrayList<>();
        Object[] args = new Object[4];
        args[0] = "%" + searchKey + "%";
        args[1] = "%" + searchKey + "%";
        args[2] = "%" + searchKey + "%";
        args[3] = "%" + searchKey + "%";
        String sql = " select t.*,cm.member_name,cm.member_remark from (\n" +
                " select " + channel + ".*,max(" + channelMembers + ".id) mid from " + channel + "," + channelMembers + " " +
                "where " + channel + ".channel_id=" + channelMembers + ".channel_id and " + channel + ".channel_type=" + channelMembers + ".channel_type" +
                " and (" + channel + ".channel_name like ? or " + channel + ".channel_remark" +
                " like ? or " + channelMembers + ".member_name like ? or " + channelMembers + ".member_remark like ?)\n" +
                " group by " + channel + ".channel_id," + channel + ".channel_type\n" +
                " ) t," + channelMembers + " cm where t.channel_id=cm.channel_id and t.channel_type=cm.channel_type and t.mid=cm.id";
        Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, args);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String member_name = BageCursor.readString(cursor, "member_name");
            String member_remark = BageCursor.readString(cursor, "member_remark");
            BageChannel channel = serializableChannel(cursor);
            BageChannelSearchResult result = new BageChannelSearchResult();
            result.bageChannel = channel;
            if (!TextUtils.isEmpty(member_remark)) {
                //优先显示备注名称
                if (member_remark.toUpperCase().contains(searchKey.toUpperCase())) {
                    result.containMemberName = member_remark;
                }
            }
            if (TextUtils.isEmpty(result.containMemberName)) {
                if (!TextUtils.isEmpty(member_name)) {
                    if (member_name.toUpperCase().contains(searchKey.toUpperCase())) {
                        result.containMemberName = member_name;
                    }
                }
            }
            list.add(result);
        }
        cursor.close();
        return list;
    }

    public synchronized List<BageChannel> searchWithChannelType(String searchKey, byte channelType) {
        List<BageChannel> list = new ArrayList<>();
        Object[] args = new Object[3];
        args[0] = "%" + searchKey + "%";
        args[1] = "%" + searchKey + "%";
        args[2] = channelType;
        String sql = "select * from " + channel + " where (" + BageDBColumns.BageChannelColumns.channel_name + " LIKE ? or " + BageDBColumns.BageChannelColumns.channel_remark + " LIKE ?) and " + BageDBColumns.BageChannelColumns.channel_type + "=?";
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                list.add(serializableChannel(cursor));
            }
        }
        return list;
    }

    public synchronized List<BageChannel> searchWithChannelTypeAndFollow(String searchKey, byte channelType, int follow) {
        List<BageChannel> list = new ArrayList<>();
        Object[] args = new Object[4];
        args[0] = "%" + searchKey + "%";
        args[1] = "%" + searchKey + "%";
        args[2] = channelType;
        args[3] = follow;
        String sql = "select * from " + channel + " where (" + BageDBColumns.BageChannelColumns.channel_name + " LIKE ? or " + BageDBColumns.BageChannelColumns.channel_remark + " LIKE ?) and " + BageDBColumns.BageChannelColumns.channel_type + "=? and " + BageDBColumns.BageChannelColumns.follow + "=?";
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                list.add(serializableChannel(cursor));
            }
        }
        return list;
    }

    public synchronized List<BageChannel> queryWithChannelTypeAndFollow(byte channelType, int follow) {
        String[] args = new String[2];
        args[0] = String.valueOf(channelType);
        args[1] = String.valueOf(follow);
        String selection = BageDBColumns.BageChannelColumns.channel_type + "=? and " + BageDBColumns.BageChannelColumns.follow + "=?";
        List<BageChannel> channels = new ArrayList<>();
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(channel, selection, args, null)) {
            if (cursor == null) {
                return channels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                channels.add(serializableChannel(cursor));
            }
        }
        return channels;
    }

    public synchronized void updateWithField(String channelID, byte channelType, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = BageDBColumns.BageChannelColumns.channel_id + "=? and " + BageDBColumns.BageChannelColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelID;
        whereValue[1] = String.valueOf(channelType);
        BageIMApplication.getInstance().getDbHelper()
                .update(channel, updateKey, updateValue, where, whereValue);
    }

    public BageChannel serializableChannel(Cursor cursor) {
        BageChannel channel = new BageChannel();
        channel.id = BageCursor.readLong(cursor, BageDBColumns.BageChannelColumns.id);
        channel.channelID = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.channel_id);
        channel.channelType = BageCursor.readByte(cursor, BageDBColumns.BageChannelColumns.channel_type);
        channel.channelName = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.channel_name);
        channel.channelRemark = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.channel_remark);
        channel.showNick = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.show_nick);
        channel.top = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.top);
        channel.mute = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.mute);
        channel.isDeleted = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.is_deleted);
        channel.forbidden = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.forbidden);
        channel.status = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.status);
        channel.follow = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.follow);
        channel.invite = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.invite);
        channel.version = BageCursor.readLong(cursor, BageDBColumns.BageChannelColumns.version);
        channel.createdAt = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.created_at);
        channel.updatedAt = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.updated_at);
        channel.avatar = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.avatar);
        channel.online = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.online);
        channel.lastOffline = BageCursor.readLong(cursor, BageDBColumns.BageChannelColumns.last_offline);
        channel.category = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.category);
        channel.receipt = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.receipt);
        channel.robot = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.robot);
        channel.username = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.username);
        channel.avatarCacheKey = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.avatar_cache_key);
        channel.flame = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.flame);
        channel.flameSecond = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.flame_second);
        channel.deviceFlag = BageCursor.readInt(cursor, BageDBColumns.BageChannelColumns.device_flag);
        channel.parentChannelID = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.parent_channel_id);
        channel.parentChannelType = BageCursor.readByte(cursor, BageDBColumns.BageChannelColumns.parent_channel_type);
        String extra = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.localExtra);
        String remoteExtra = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.remote_extra);
        channel.localExtra = BageCommonUtils.str2HashMap(extra);
        channel.remoteExtraMap = BageCommonUtils.str2HashMap(remoteExtra);
        return channel;
    }
}
