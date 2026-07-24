package com.chat.uikit.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;

import com.chat.base.BageBaseApplication;
import com.chat.base.db.BageCursor;
import com.chat.base.utils.BageReader;
import com.chat.uikit.enity.MailListEntity;

import java.util.ArrayList;
import java.util.List;

public class BageContactsDB {
    private BageContactsDB() {

    }

    private static class ContactsDBBinder {
        static BageContactsDB db = new BageContactsDB();
    }

    public static BageContactsDB getInstance() {
        return ContactsDBBinder.db;
    }

    public List<MailListEntity> query() {
        List<MailListEntity> list = new ArrayList<>();
        Cursor cursor = BageBaseApplication
                .getInstance()
                .getDbHelper()
                .rawQuery(
                        "select * from user_contact", null);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serialize(cursor));
        }
        cursor.close();
        return list;
    }

    @SuppressLint("Range")
    private MailListEntity serialize(Cursor cursor) {
        MailListEntity entity = new MailListEntity();
        entity.phone = BageCursor.readString(cursor, "phone");
        entity.zone = BageCursor.readString(cursor, "zone");
        entity.name = BageCursor.readString(cursor, "name");
        entity.uid = BageCursor.readString(cursor, "uid");
        entity.vercode = BageCursor.readString(cursor, "vercode");
        entity.is_friend = BageCursor.readInt(cursor, "is_friend");
        return entity;
    }

    public void save(List<MailListEntity> list) {
        if (BageReader.isEmpty(list)) return;
        try {
            BageBaseApplication.getInstance().getDbHelper().getDB().beginTransaction();
            for (int i = 0, size = list.size(); i < size; i++) {
                boolean isAdd = true;
                if (isExist(list.get(i))) {
                    isAdd = delete(list.get(i));
                }
                if (isAdd)
                    insert(list.get(i));
            }
            BageBaseApplication.getInstance().getDbHelper().getDB().setTransactionSuccessful();
        } finally {
            BageBaseApplication.getInstance().getDbHelper().getDB().endTransaction();
        }
    }

    private boolean delete(MailListEntity entity) {
        String[] strings = new String[2];
        strings[0] = entity.phone;
        strings[1] = entity.name;
        return BageBaseApplication.getInstance().getDbHelper().delete("user_contact", "phone=? and name=?", strings);
    }

    public void updateFriendStatus(String uid, int isFriend) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_friend", isFriend);
        String[] strings = new String[1];
        strings[0] = uid;
        BageBaseApplication.getInstance().getDbHelper().update("user_contact", contentValues, "uid=?", strings);
    }

    private void insert(MailListEntity entity) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("phone", entity.phone);
        contentValues.put("uid", entity.uid);
        contentValues.put("zone", entity.zone);
        contentValues.put("name", entity.name);
        contentValues.put("vercode", entity.vercode);
        contentValues.put("is_friend", entity.is_friend);
        BageBaseApplication.getInstance().getDbHelper().insert("user_contact", contentValues);
    }

    private boolean isExist(MailListEntity entity) {
        boolean isExist = false;
        String sql = "select * from user_contact where phone=" + "\"" + entity.phone + "\"" + " and name=" + "\"" + entity.name + "\"";
        Cursor cursor = BageBaseApplication.getInstance().getDbHelper().rawQuery(sql, null);
        if (cursor != null && cursor.moveToNext()) {
            isExist = true;
        }
        if (cursor != null)
            cursor.close();
        return isExist;
    }
}
