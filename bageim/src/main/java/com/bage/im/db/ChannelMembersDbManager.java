package com.bage.im.db;

import static com.bage.im.db.BageDBColumns.TABLE.channel;
import static com.bage.im.db.BageDBColumns.TABLE.channelMembers;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bage.im.BageIMApplication;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.manager.ChannelMembersManager;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 2019-11-10 14:06
 * 频道成员数据管理
 */
public class ChannelMembersDbManager {
    private static final String TAG = "ChannelMembersDbManager";
    final String channelCols = channel + ".channel_remark," + channel + ".channel_name," + channel + ".avatar," + channel + ".avatar_cache_key";

    private ChannelMembersDbManager() {
    }

    private static class ChannelMembersManagerBinder {
        private final static ChannelMembersDbManager channelMembersManager = new ChannelMembersDbManager();
    }

    public static ChannelMembersDbManager getInstance() {
        return ChannelMembersManagerBinder.channelMembersManager;
    }

    public synchronized List<BageChannelMember> search(String channelId, byte channelType, String keyword, int page, int size) {
        int queryPage = (page - 1) * size;
        Object[] args = new Object[6];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = "%" + keyword + "%";
        args[3] = "%" + keyword + "%";
        args[4] = "%" + keyword + "%";
        args[5] = "%" + keyword + "%";
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=0 and " + channelMembers + ".status=1 and (member_name like ? or member_remark like ? or channel_name like ? or channel_remark like ?) order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.created_at + " asc limit " + queryPage + "," + size;
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized List<BageChannelMember> queryWithPage(String channelId, byte channelType, int page, int size) {
        int queryPage = (page - 1) * size;
        Object[] args = new Object[2];
        args[0] = channelId;
        args[1] = channelType;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=0 and " + channelMembers + ".status=1 order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.created_at + " asc limit " + queryPage + "," + size;
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * 查询某个频道的所有成员
     *
     * @param channelId 频道ID
     * @return List<BageChannelMember>
     */
    public synchronized List<BageChannelMember> query(String channelId, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelId;
        args[1] = channelType;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=0 and " + channelMembers + ".status=1 order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.created_at + " asc";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized List<BageChannelMember> queryDeleted(String channelId, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelId;
        args[1] = channelType;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=1 and " + channelMembers + ".status=1 order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.created_at + " asc";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized boolean isExist(String channelId, byte channelType, String uid) {
        boolean isExist = false;
        Object[] args = new Object[3];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = uid;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " left join " + channel + " on " + channelMembers + ".member_uid = " + channel + ".channel_id AND " + channel + ".channel_type=1 where (" + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.channel_id + "=? and " + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.channel_type + "=? and " + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.member_uid + "=?)";
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args)) {

            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    public List<BageChannelMember> queryWithUIDs(String channelID, byte channelType, List<String> uidList) {
        List<String> args = new ArrayList<>();
        args.add(channelID);
        args.add(String.valueOf(channelType));
        args.addAll(uidList);
        uidList.add(String.valueOf(channelType));
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(channelMembers, "channel_id =? and channel_type=? and member_uid in (" + BageCursor.getPlaceholders(uidList.size()) + ")", args.toArray(new String[0]), null);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * 查询单个频道成员
     *
     * @param channelId 频道ID
     * @param uid       用户ID
     */
    public synchronized BageChannelMember query(String channelId, byte channelType, String uid) {
        BageChannelMember bageChannelMember = null;
        Object[] args = new Object[3];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = uid;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " left join " + channel + " on " + channelMembers + ".member_uid = " + channel + ".channel_id AND " + channel + ".channel_type=1 where (" + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.channel_id + "=? and " + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.channel_type + "=? and " + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.member_uid + "=?)";
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                bageChannelMember = serializableChannelMember(cursor);
            }
        }
        return bageChannelMember;
    }

    public synchronized void insert(BageChannelMember channelMember) {
        if (TextUtils.isEmpty(channelMember.channelID) || TextUtils.isEmpty(channelMember.memberUID))
            return;
        ContentValues cv = new ContentValues();
        try {
            cv = BageSqlContentValues.getContentValuesWithChannelMember(channelMember);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "insert error");
        }
        BageIMApplication.getInstance().getDbHelper()
                .insert(channelMembers, cv);
    }

    /**
     * 批量插入频道成员
     *
     * @param list List<BageChannelMember>
     */
    public void insertMembers(List<BageChannelMember> list) {
        List<ContentValues> newCVList = new ArrayList<>();
        for (BageChannelMember member : list) {
            ContentValues cv = BageSqlContentValues.getContentValuesWithChannelMember(member);
            newCVList.add(cv);
        }
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            if (BageCommonUtils.isNotEmpty(newCVList)) {
                for (ContentValues cv : newCVList) {
                    BageIMApplication.getInstance().getDbHelper().insert(channelMembers, cv);
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

    public void insertMembers(List<BageChannelMember> allMemberList, List<BageChannelMember> existList) {
        List<ContentValues> insertCVList = new ArrayList<>();
//        List<ContentValues> updateCVList = new ArrayList<>();
        for (BageChannelMember channelMember : allMemberList) {
//            boolean isAdd = true;
//            for (BageChannelMember cm : existList) {
//                if (channelMember.memberUID.equals(cm.memberUID)) {
//                    isAdd = false;
//                    updateCVList.add(BageSqlContentValues.getContentValuesWithChannelMember(channelMember));
//                    break;
//                }
//            }
//            if (isAdd) {
            insertCVList.add(BageSqlContentValues.getContentValuesWithChannelMember(channelMember));
//            }
        }
        net.zetetic.database.sqlcipher.SQLiteDatabase db2 = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db2 == null) return;
        try {
            db2.beginTransaction();
            if (BageCommonUtils.isNotEmpty(insertCVList)) {
                for (ContentValues cv : insertCVList) {
                    BageIMApplication.getInstance().getDbHelper().insert(channelMembers, cv);
                }
            }
            db2.setTransactionSuccessful();
        } finally {
            try {
                if (db2.inTransaction()) db2.endTransaction();
            } catch (Exception ignored) {
            }
        }
    }

    public void insertOrUpdate(BageChannelMember channelMember) {
        if (channelMember == null) return;
        if (isExist(channelMember.channelID, channelMember.channelType, channelMember.memberUID)) {
            update(channelMember);
        } else {
            insert(channelMember);
        }
    }

    /**
     * 修改某个频道的某个成员信息
     *
     * @param channelMember 成员
     */
    public synchronized void update(BageChannelMember channelMember) {
        String[] update = new String[3];
        update[0] = channelMember.channelID;
        update[1] = String.valueOf(channelMember.channelType);
        update[2] = channelMember.memberUID;
        ContentValues cv = new ContentValues();
        try {
            cv = BageSqlContentValues.getContentValuesWithChannelMember(channelMember);
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "update error");
        }
        BageIMApplication.getInstance().getDbHelper()
                .update(channelMembers, cv, BageDBColumns.BageChannelMembersColumns.channel_id + "=? and " + BageDBColumns.BageChannelMembersColumns.channel_type + "=? and " + BageDBColumns.BageChannelMembersColumns.member_uid + "=?", update);
    }

    /**
     * 根据字段修改频道成员
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param field       字段
     * @param value       值
     */
    public synchronized boolean updateWithField(String channelID, byte channelType, String uid, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = BageDBColumns.BageChannelMembersColumns.channel_id + "=? and " + BageDBColumns.BageChannelMembersColumns.channel_type + "=? and " + BageDBColumns.BageChannelMembersColumns.member_uid + "=?";
        String[] whereValue = new String[3];
        whereValue[0] = channelID;
        whereValue[1] = String.valueOf(channelType);
        whereValue[2] = uid;
        int row = BageIMApplication.getInstance().getDbHelper()
                .update(channelMembers, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            BageChannelMember channelMember = query(channelID, channelType, uid);
            if (channelMember != null)
                //刷新频道成员信息
                ChannelMembersManager.getInstance().setRefreshChannelMember(channelMember, true);
        }
        return row > 0;
    }

    public void deleteWithChannel(String channelID, byte channelType) {
        String selection = "channel_id=? and channel_type=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelID;
        selectionArgs[1] = String.valueOf(channelType);
        BageIMApplication.getInstance().getDbHelper().delete(channelMembers, selection, selectionArgs);
    }

    /**
     * 批量删除频道成员
     *
     * @param list 频道成员
     */
    public synchronized void deleteMembers(List<BageChannelMember> list) {
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            if (BageCommonUtils.isNotEmpty(list)) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    insertOrUpdate(list.get(i));
                }
                db.setTransactionSuccessful();
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (db.inTransaction()) db.endTransaction();
            } catch (Exception ignored2) {
            }
        }
        ChannelMembersManager.getInstance().setOnRemoveChannelMember(list);
    }

    public long queryMaxVersion(String channelID, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelID;
        args[1] = channelType;
        String sql = "select max(version) version from " + channelMembers + " where channel_id =? and channel_type=? limit 0, 1";
        long version = 0;
        try {
            if (BageIMApplication.getInstance().getDbHelper() != null) {
                Cursor cursor = BageIMApplication
                        .getInstance()
                        .getDbHelper()
                        .rawQuery(sql, args);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        version = BageCursor.readLong(cursor, "version");
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return version;
    }

    @Deprecated
    public synchronized BageChannelMember queryMaxVersionMember(String channelID, byte channelType) {
        BageChannelMember channelMember = null;
        Object[] args = new Object[2];
        args[0] = channelID;
        args[1] = channelType;
        String sql = "select * from " + channelMembers + " where " + BageDBColumns.BageChannelMembersColumns.channel_id + "=? and " + BageDBColumns.BageChannelMembersColumns.channel_type + "=? order by " + BageDBColumns.BageChannelMembersColumns.version + " desc limit 0,1";
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                channelMember = serializableChannelMember(cursor);
            }
        }
        return channelMember;
    }

    public synchronized List<BageChannelMember> queryRobotMembers(String channelId, byte channelType) {
        String selection = "channel_id=? and channel_type=? and robot=1 and is_deleted=0";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(channelMembers, selection, new String[]{channelId, String.valueOf(channelType)}, null);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public List<BageChannelMember> queryWithRole(String channelId, byte channelType, int role) {
        String selection = "channel_id=? AND channel_type=? AND role=? AND is_deleted=0";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(channelMembers, selection, new String[]{channelId, String.valueOf(channelType), String.valueOf(role)}, null);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized List<BageChannelMember> queryWithStatus(String channelId, byte channelType, int status) {
        Object[] args = new Object[3];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = status;
        String sql = "select " + channelMembers + ".*," + channel + ".channel_name," + channel + ".channel_remark," + channel + ".avatar from " + channelMembers + " left Join " + channel + " where " + channelMembers + ".member_uid = " + channel + ".channel_id AND " + channel + ".channel_type=1 AND " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".status=? order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + BageDBColumns.BageChannelMembersColumns.created_at + " asc";
        Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql, args);
        List<BageChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized int queryCount(String channelID, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelID;
        args[1] = channelType;
        String sql = "select count(*) from " + channelMembers
                + " where (" + BageDBColumns.BageChannelMembersColumns.channel_id + "=? and "
                + BageDBColumns.BageChannelMembersColumns.channel_type + "=? and " + BageDBColumns.BageChannelMembersColumns.is_deleted + "=0 and " + BageDBColumns.BageChannelMembersColumns.status + "=1)";
        Cursor cursor = null;
        try {
            if (BageIMApplication.getInstance().getDbHelper() == null) return 0;
            cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, args);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            BageLoggerUtils.getInstance().e(TAG, "queryCount error: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    /**
     * 序列化频道成员
     *
     * @param cursor Cursor
     * @return BageChannelMember
     */
    private BageChannelMember serializableChannelMember(Cursor cursor) {
        BageChannelMember channelMember = new BageChannelMember();
        channelMember.id = BageCursor.readLong(cursor, BageDBColumns.BageChannelMembersColumns.id);
        channelMember.status = BageCursor.readInt(cursor, BageDBColumns.BageChannelMembersColumns.status);
        channelMember.channelID = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.channel_id);
        channelMember.channelType = (byte) BageCursor.readInt(cursor, BageDBColumns.BageChannelMembersColumns.channel_type);
        channelMember.memberUID = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.member_uid);
        channelMember.memberName = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.member_name);
        channelMember.memberAvatar = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.member_avatar);
        channelMember.memberRemark = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.member_remark);
        channelMember.role = BageCursor.readInt(cursor, BageDBColumns.BageChannelMembersColumns.role);
        channelMember.isDeleted = BageCursor.readInt(cursor, BageDBColumns.BageChannelMembersColumns.is_deleted);
        channelMember.version = BageCursor.readLong(cursor, BageDBColumns.BageChannelMembersColumns.version);
        channelMember.createdAt = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.created_at);
        channelMember.updatedAt = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.updated_at);
        channelMember.memberInviteUID = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.member_invite_uid);
        channelMember.robot = BageCursor.readInt(cursor, BageDBColumns.BageChannelMembersColumns.robot);
        channelMember.forbiddenExpirationTime = BageCursor.readLong(cursor, BageDBColumns.BageChannelMembersColumns.forbidden_expiration_time);
        String channelName = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.channel_name);
        if (!TextUtils.isEmpty(channelName)) channelMember.memberName = channelName;
        channelMember.remark = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.channel_remark);
        channelMember.memberAvatar = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.avatar);
        String avatarCache = BageCursor.readString(cursor, BageDBColumns.BageChannelColumns.avatar_cache_key);
        if (!TextUtils.isEmpty(avatarCache)) {
            channelMember.memberAvatarCacheKey = avatarCache;
        } else {
            channelMember.memberAvatarCacheKey = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.memberAvatarCacheKey);
        }
        String extra = BageCursor.readString(cursor, BageDBColumns.BageChannelMembersColumns.extra);
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
                BageLoggerUtils.getInstance().e(TAG, "serializableChannelMember extra error");
            }
            channelMember.extraMap = hashMap;
        }
        return channelMember;
    }
}
