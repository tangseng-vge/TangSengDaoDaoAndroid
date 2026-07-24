package com.bage.im.db;

import static com.bage.im.db.BageDBColumns.TABLE.reminders;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.entity.BageReminder;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.interfaces.IReminderResult;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ReminderDBManager {
    private static final String TAG = "ReminderDBManager";

    private ReminderDBManager() {
    }

    private static class ReminderDBManagerBinder {
        final static ReminderDBManager binder = new ReminderDBManager();
    }

    public static ReminderDBManager getInstance() {
        return ReminderDBManagerBinder.binder;
    }

    public void doneWithReminderIds(List<Long> ids) {
        ContentValues cv = new ContentValues();
        cv.put("done", 1);
        String[] strings = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            strings[i] = ids.get(i) + "";
        }
        BageIMApplication.getInstance().getDbHelper().update(reminders, cv, "reminder_id in (" + BageCursor.getPlaceholders(ids.size()) + ")", strings);
    }

    public long queryMaxVersion() {
        String sql = "select * from " + reminders + " order by version desc limit 1";
        long version = 0;
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                version = BageCursor.readLong(cursor, "version");
            }
        }
        return version;
    }

    /**
     * 同步查询（仅供后台线程使用）
     *
     * @deprecated 建议使用 queryWithChannelAndDoneAsync 避免 ANR
     */
    @Deprecated
    public List<BageReminder> queryWithChannelAndDone(String channelID, byte channelType, int done) {
        String sql = "select * from " + reminders + " where channel_id=? and channel_type=? and done=? order by message_seq desc";
        List<BageReminder> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, done})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        }
        return list;
    }

    /**
     * 异步查询（推荐使用，避免 ANR）
     */
    public void queryWithChannelAndDoneAsync(String channelID, byte channelType, int done, IReminderResult callback) {
        String sql = "select * from " + reminders + " where channel_id=? and channel_type=? and done=? order by message_seq desc";
        BageIMApplication.getInstance().getDbHelper().rawQueryAsync(sql, new Object[]{channelID, channelType, done},
                new BageDBHelper.QueryCallback<List<BageReminder>>() {
                    @Override
                    public List<BageReminder> onQuery(Cursor cursor) {
                        // 在后台线程处理 Cursor
                        List<BageReminder> list = new ArrayList<>();
                        if (cursor == null) {
                            return list;
                        }
                        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                            BageReminder reminder = serializeReminder(cursor);
                            list.add(reminder);
                        }
                        return list;
                    }

                    @Override
                    public void onResult(List<BageReminder> result) {
                        // 在主线程回调结果
                        if (callback != null) {
                            callback.onResult(result);
                        }
                    }
                });
    }


    public List<BageReminder> queryWithChannelAndTypeAndDone(String channelID, byte channelType, int type, int done) {
        String sql = "select * from " + reminders + " where channel_id=? and channel_type=? and done=? and type =? order by message_seq desc";
        List<BageReminder> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, done, type})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        }
        return list;
    }

    private List<BageReminder> queryWithIds(List<Long> ids) {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0, size = ids.size(); i < size; i++) {
            if (!TextUtils.isEmpty(stringBuffer)) {
                stringBuffer.append(",");
            }
            stringBuffer.append(ids.get(i));
        }
        String sql = "select * from " + reminders + " where reminder_id in (" + stringBuffer + ")";
        List<BageReminder> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private List<BageReminder> queryWithChannelIds(List<String> channelIds) {
        List<BageReminder> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper()
                .select(reminders, "channel_id in (" + BageCursor.getPlaceholders(channelIds.size()) + ")", channelIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<BageReminder> insertOrUpdateReminders(List<BageReminder> list) {
        List<Long> ids = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            boolean isAdd = true;
            for (String channelId : channelIds) {
                if (!TextUtils.isEmpty(list.get(i).channelID) && channelId.equals(list.get(i).channelID)) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) channelIds.add(list.get(i).channelID);
            ids.add(list.get(i).reminderID);

        }
        List<ContentValues> insertCVs = new ArrayList<>();
        List<ContentValues> updateCVs = new ArrayList<>();
        List<BageReminder> allList = queryWithIds(ids);
        for (int i = 0, size = list.size(); i < size; i++) {
            boolean isAdd = true;
            for (BageReminder reminder : allList) {
                if (reminder.reminderID == list.get(i).reminderID) {
                    updateCVs.add(BageSqlContentValues.getCVWithReminder(list.get(i)));
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                insertCVs.add(BageSqlContentValues.getCVWithReminder(list.get(i)));
            }
        }
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return new ArrayList<>();
        try {
            db.beginTransaction();
            if (!insertCVs.isEmpty()) {
                for (ContentValues cv : insertCVs) {
                    BageIMApplication.getInstance().getDbHelper().insert(reminders, cv);
                }
            }
            if (!updateCVs.isEmpty()) {
                for (ContentValues cv : updateCVs) {
                    String[] update = new String[1];
                    update[0] = cv.getAsString("reminder_id");
                    BageIMApplication.getInstance().getDbHelper()
                            .update(reminders, cv, "reminder_id=?", update);
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

        List<BageReminder> reminderList = queryWithChannelIds(channelIds);
        HashMap<String, List<BageReminder>> maps = listToMap(reminderList);
        List<BageUIConversationMsg> uiMsgList = ConversationDbManager.getInstance().queryWithChannelIds(channelIds);
        for (int i = 0, size = uiMsgList.size(); i < size; i++) {
            String key = uiMsgList.get(i).channelID + "_" + uiMsgList.get(i).channelType;
            if (maps.containsKey(key)) {
                uiMsgList.get(i).setReminderList(maps.get(key));
            }
            // BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == list.size() - 1, "saveReminders");
        }
        BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsgList, "saveReminders");
        return reminderList;
    }

    private HashMap<String, List<BageReminder>> listToMap(List<BageReminder> list) {
        HashMap<String, List<BageReminder>> map = new HashMap<>();
        if (list == null || list.isEmpty()) {
            return map;
        }
        for (BageReminder reminder : list) {
            String key = reminder.channelID + "_" + reminder.channelType;
            List<BageReminder> tempList = null;
            if (map.containsKey(key)) {
                tempList = map.get(key);
            }
            if (tempList == null) tempList = new ArrayList<>();
            tempList.add(reminder);
            map.put(key, tempList);
        }
        return map;
    }

    private BageReminder serializeReminder(Cursor cursor) {
        BageReminder reminder = new BageReminder();
        reminder.type = BageCursor.readInt(cursor, "type");
        reminder.reminderID = BageCursor.readLong(cursor, "reminder_id");
        reminder.messageID = BageCursor.readString(cursor, "message_id");
        reminder.messageSeq = BageCursor.readLong(cursor, "message_seq");
        reminder.isLocate = BageCursor.readInt(cursor, "is_locate");
        reminder.channelID = BageCursor.readString(cursor, "channel_id");
        reminder.channelType = (byte) BageCursor.readInt(cursor, "channel_type");
        reminder.text = BageCursor.readString(cursor, "text");
        reminder.version = BageCursor.readLong(cursor, "version");
        reminder.done = BageCursor.readInt(cursor, "done");
        String data = BageCursor.readString(cursor, "data");
        reminder.needUpload = BageCursor.readInt(cursor, "need_upload");
        reminder.publisher = BageCursor.readString(cursor, "publisher");
        if (!TextUtils.isEmpty(data)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            try {
                JSONObject jsonObject = new JSONObject(data);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    hashMap.put(key, jsonObject.opt(key));
                }
            } catch (JSONException e) {
                BageLoggerUtils.getInstance().e(TAG, "serializeReminder error");
            }
            reminder.data = hashMap;
        }
        return reminder;
    }
}
