package com.bage.im.db;

import static com.bage.im.db.BageDBColumns.TABLE.robot;
import static com.bage.im.db.BageDBColumns.TABLE.robotMenu;

import android.content.ContentValues;
import android.database.Cursor;

import com.bage.im.BageIMApplication;
import com.bage.im.entity.BageRobot;
import com.bage.im.entity.BageRobotMenu;
import com.bage.im.utils.BageCommonUtils;

import java.util.ArrayList;
import java.util.List;

public class RobotDBManager {

    private RobotDBManager() {
    }

    private static class RobotDBManagerBinder {
        private final static RobotDBManager db = new RobotDBManager();
    }

    public static RobotDBManager getInstance() {
        return RobotDBManagerBinder.db;
    }

    public void insertOrUpdateMenus(List<BageRobotMenu> list) {
        for (BageRobotMenu menu : list) {
            if (isExitMenu(menu.robotID, menu.cmd)) {
                update(menu);
            } else {
                BageIMApplication.getInstance().getDbHelper().insert(robotMenu, getCV(menu));
            }
        }
    }

    public boolean isExitMenu(String robotID, String cmd) {
        boolean isExist = false;
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(robotMenu, "robot_id =? and cmd=?", new String[]{robotID, cmd}, null)) {
            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    private void update(BageRobotMenu menu) {
        String[] updateKey = new String[3];
        String[] updateValue = new String[3];
        updateKey[0] = "type";
        updateValue[0] = menu.type;
        updateKey[1] = "remark";
        updateValue[1] = menu.remark;
        updateKey[2] = "updated_at";
        updateValue[2] = menu.updatedAT;
        String where = "robot_id=? and cmd=?";
        String[] whereValue = new String[2];
        whereValue[0] = menu.robotID;
        whereValue[1] = menu.cmd;
        BageIMApplication.getInstance().getDbHelper()
                .update(robotMenu, updateKey, updateValue, where, whereValue);
    }

    public void insertOrUpdateRobots(List<BageRobot> list) {
        for (BageRobot robot : list) {
            if (isExist(robot.robotID)) {
                update(robot);
            } else {
                insert(robot);
            }
        }
    }

    public boolean isExist(String robotID) {
        boolean isExist = false;
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(robot, "robot_id=?", new String[]{robotID}, null)) {
            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    private void update(BageRobot bageRobot) {
        String[] updateKey = new String[6];
        String[] updateValue = new String[6];
        updateKey[0] = "status";
        updateValue[0] = String.valueOf(bageRobot.status);
        updateKey[1] = "version";
        updateValue[1] = String.valueOf(bageRobot.version);
        updateKey[2] = "updated_at";
        updateValue[2] = String.valueOf(bageRobot.updatedAT);
        updateKey[3] = "username";
        updateValue[3] = bageRobot.username;
        updateKey[4] = "placeholder";
        updateValue[4] = bageRobot.placeholder;
        updateKey[5] = "inline_on";
        updateValue[5] = String.valueOf(bageRobot.inlineOn);

        String where = "robot_id=?";
        String[] whereValue = new String[1];
        whereValue[0] = bageRobot.robotID;
        BageIMApplication.getInstance().getDbHelper()
                .update(robot, updateKey, updateValue, where, whereValue);

    }

    private void insert(BageRobot robot1) {
        ContentValues cv = getCV(robot1);
        BageIMApplication.getInstance().getDbHelper().insert(robot, cv);
    }

    public void insertRobots(List<BageRobot> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        List<ContentValues> cvList = new ArrayList<>();
        for (BageRobot robot : list) {
            cvList.add(getCV(robot));
        }
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            for (ContentValues cv : cvList) {
                BageIMApplication.getInstance().getDbHelper().insert(robot, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            try {
                if (db.inTransaction()) db.endTransaction();
            } catch (Exception ignored) {
            }
        }
    }

    public BageRobot query(String robotID) {
        BageRobot bageRobot = null;
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(robot, "robot_id =?", new String[]{robotID}, null)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                bageRobot = serializeRobot(cursor);
            }
        }
        return bageRobot;
    }

    public BageRobot queryWithUsername(String username) {
        BageRobot bageRobot = null;
        try (Cursor cursor = BageIMApplication
                .getInstance()
                .getDbHelper().select(robot, "username=?", new String[]{username}, null)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                bageRobot = serializeRobot(cursor);
            }
        }
        return bageRobot;
    }

    public List<BageRobot> queryRobots(List<String> robotIds) {
        List<BageRobot> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().select(robot, "robot_id in (" + BageCursor.getPlaceholders(robotIds.size()) + ")", robotIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageRobot robot = serializeRobot(cursor);
                list.add(robot);
            }
        }
        return list;
    }

    public List<BageRobotMenu> queryRobotMenus(List<String> robotIds) {
        List<BageRobotMenu> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().select(robotMenu, "robot_id in (" + BageCursor.getPlaceholders(robotIds.size()) + ")", robotIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageRobotMenu robotMenu = serializeRobotMenu(cursor);
                list.add(robotMenu);
            }
        }
        return list;
    }

    public List<BageRobotMenu> queryRobotMenus(String robotID) {
        List<BageRobotMenu> list = new ArrayList<>();
        try (Cursor cursor = BageIMApplication.getInstance().getDbHelper().select(robotMenu, "robot_id=?", new String[]{robotID}, null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageRobotMenu robotMenu = serializeRobotMenu(cursor);
                list.add(robotMenu);
            }
        }
        return list;
    }

    public void insertMenus(List<BageRobotMenu> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        List<ContentValues> cvList = new ArrayList<>();
        for (BageRobotMenu robot : list) {
            cvList.add(getCV(robot));
        }
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            for (ContentValues cv : cvList) {
                BageIMApplication.getInstance().getDbHelper().insert(robotMenu, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            try {
                if (db.inTransaction()) db.endTransaction();
            } catch (Exception ignored) {
            }
        }
    }

    private BageRobot serializeRobot(Cursor cursor) {
        BageRobot robot = new BageRobot();
        robot.robotID = BageCursor.readString(cursor, "robot_id");
        robot.status = BageCursor.readInt(cursor, "status");
        robot.version = BageCursor.readLong(cursor, "version");
        robot.username = BageCursor.readString(cursor, "username");
        robot.inlineOn = BageCursor.readInt(cursor, "inline_on");
        robot.placeholder = BageCursor.readString(cursor, "placeholder");
        robot.createdAT = BageCursor.readString(cursor, "created_at");
        robot.updatedAT = BageCursor.readString(cursor, "updated_at");
        return robot;
    }

    private BageRobotMenu serializeRobotMenu(Cursor cursor) {
        BageRobotMenu robot = new BageRobotMenu();
        robot.robotID = BageCursor.readString(cursor, "robot_id");
        robot.type = BageCursor.readString(cursor, "type");
        robot.cmd = BageCursor.readString(cursor, "cmd");
        robot.remark = BageCursor.readString(cursor, "remark");
        robot.createdAT = BageCursor.readString(cursor, "created_at");
        robot.updatedAT = BageCursor.readString(cursor, "updated_at");
        return robot;
    }

    private ContentValues getCV(BageRobot bageRobot) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("robot_id", bageRobot.robotID);
        contentValues.put("inline_on", bageRobot.inlineOn);
        contentValues.put("username", bageRobot.username);
        contentValues.put("placeholder", bageRobot.placeholder);
        contentValues.put("status", bageRobot.status);
        contentValues.put("version", bageRobot.version);
        contentValues.put("created_at", bageRobot.createdAT);
        contentValues.put("updated_at", bageRobot.updatedAT);
        return contentValues;
    }

    private ContentValues getCV(BageRobotMenu robotMenu) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("robot_id", robotMenu.robotID);
        contentValues.put("cmd", robotMenu.cmd);
        contentValues.put("remark", robotMenu.remark);
        contentValues.put("type", robotMenu.type);
        contentValues.put("created_at", robotMenu.createdAT);
        contentValues.put("updated_at", robotMenu.updatedAT);
        return contentValues;
    }
}
