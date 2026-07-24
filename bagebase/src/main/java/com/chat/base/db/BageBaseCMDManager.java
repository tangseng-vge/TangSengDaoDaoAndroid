package com.chat.base.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.chat.base.BageBaseApplication;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageReader;
import com.bage.im.BageIM;
import com.bage.im.entity.BageCMD;
import com.bage.im.entity.BageCMDKeys;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 2020-11-23 11:48
 * cmd管理
 */
public class BageBaseCMDManager {
    private BageBaseCMDManager() {
    }

    private static class BaseCMDManagerBinder {
        final static BageBaseCMDManager cmdManager = new BageBaseCMDManager();
    }

    public static BageBaseCMDManager getInstance() {
        return BaseCMDManagerBinder.cmdManager;
    }

    //添加
    public void addCmd(List<BageBaseCMD> list) {
        if (BageReader.isEmpty(list)) return;
        try {
            List<BageBaseCMD> tempList = new ArrayList<>();
            List<ContentValues> cvList = new ArrayList<>();
            List<String> clientMsgNos = new ArrayList<>();
            List<String> msgIds = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                clientMsgNos.add(list.get(i).client_msg_no);
                msgIds.add(list.get(i).message_id);
                // if (!isExistWithClientMsgNo(list.get(i).client_msg_no) && !isExistWithMessageID(list.get(i).message_id))
//                cvList.add(getContentValues(list.get(i)));
            }
            tempList.addAll(queryWithClientMsgNos(clientMsgNos));
            tempList.addAll(queryWithMsgIds(msgIds));
            boolean isCheck = BageReader.isNotEmpty(tempList);

            for (int i = 0; i < list.size(); i++) {
                boolean isAdd = true;
                if (isCheck) {
                    for (BageBaseCMD cmd : tempList) {
                        if (cmd.client_msg_no.equals(list.get(i).client_msg_no) || cmd.message_id.equals(list.get(i).message_id)) {
                            isAdd = false;
                            break;
                        }
                    }
                }
                if (isAdd) {
                    cvList.add(getContentValues(list.get(i)));
                }
            }
            BageBaseApplication.getInstance().getDbHelper().getDB()
                    .beginTransaction();
            for (ContentValues cv : cvList) {
                BageBaseApplication.getInstance().getDbHelper()
                        .insert("cmd", cv);
            }
            BageBaseApplication.getInstance().getDbHelper().getDB()
                    .setTransactionSuccessful();
        } catch (Exception ignored) {
        } finally {
            if (BageBaseApplication.getInstance().getDbHelper().getDB().inTransaction()) {
                BageBaseApplication.getInstance().getDbHelper().getDB()
                        .endTransaction();
            }
        }
    }

    private List<BageBaseCMD> queryWithClientMsgNos(List<String> clientMsgNos) {
        List<BageBaseCMD> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("select * from cmd where client_msg_no in (");
        for (int i = 0, size = clientMsgNos.size(); i < size; i++) {
            if (i != 0) sb.append(",");
            sb.append("'").append(clientMsgNos.get(i)).append("'");
        }
        sb.append(")");
        try (Cursor cursor = BageBaseApplication.getInstance().getDbHelper().rawQuery(sb.toString())) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageBaseCMD cmd = serializeCmd(cursor);
                list.add(cmd);
            }
        }
        return list;
    }

    private List<BageBaseCMD> queryWithMsgIds(List<String> clientMsgNos) {
        List<BageBaseCMD> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("select * from cmd where message_id in (");
        for (int i = 0, size = clientMsgNos.size(); i < size; i++) {
            if (i != 0) sb.append(",");
            sb.append("'").append(clientMsgNos.get(i)).append("'");
        }
        sb.append(")");
        try (Cursor cursor = BageBaseApplication.getInstance().getDbHelper().rawQuery(sb.toString())) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                BageBaseCMD cmd = serializeCmd(cursor);
                list.add(cmd);
            }
        }
        return list;
    }

    //是否存在某条cmd
    private boolean isExistWithClientMsgNo(String clientMsgNo) {
        String sql = "select * from cmd where client_msg_no = " + "\"" + clientMsgNo + "\"";
        Cursor cursor = BageBaseApplication.getInstance().getDbHelper().rawQuery(sql, null);
        boolean isExist;
        if (cursor == null) {
            isExist = false;
        } else {
            isExist = cursor.moveToLast();
            cursor.close();
        }
        return isExist;
    }

    //是否存在某条cmd
    private boolean isExistWithMessageID(String messageID) {
        String sql = "select * from cmd where message_id = " + "\"" + messageID + "\"";
        Cursor cursor = BageBaseApplication.getInstance().getDbHelper().rawQuery(sql, null);
        boolean isExist;
        if (cursor == null) {
            isExist = false;
        } else {
            isExist = cursor.moveToLast();
            cursor.close();
        }
        return isExist;
    }


    //删除某条cmd
    public void deleteCmd(String client_msg_no) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_deleted", 1);
        String[] update = new String[1];
        update[0] = client_msg_no;
        BageBaseApplication.getInstance().getDbHelper().update("cmd", contentValues, "client_msg_no=?", update);
    }

    //查询所有cmd
    private List<BageBaseCMD> queryAllCmd() {
        List<BageBaseCMD> list = new ArrayList<>();
        String sql = "select * from cmd where is_deleted=0";
        Cursor cursor = BageBaseApplication.getInstance().getDbHelper().rawQuery(sql, null);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializeCmd(cursor));
        }
        cursor.close();
        return list;
    }

    private ContentValues getContentValues(BageBaseCMD BageBaseCmd) {
        ContentValues contentValues = new ContentValues();
        if (BageBaseCmd == null) {
            return contentValues;
        }
        contentValues.put("client_msg_no", BageBaseCmd.client_msg_no);
        contentValues.put("cmd", BageBaseCmd.cmd);
        contentValues.put("sign", BageBaseCmd.sign);
        contentValues.put("created_at", BageBaseCmd.created_at);
        contentValues.put("message_id", BageBaseCmd.message_id);
        contentValues.put("message_seq", BageBaseCmd.message_seq);
        contentValues.put("param", BageBaseCmd.param);
        contentValues.put("timestamp", BageBaseCmd.timestamp);
        return contentValues;
    }

    @SuppressLint("Range")
    private BageBaseCMD serializeCmd(Cursor cursor) {
        BageBaseCMD BageBaseCmd = new BageBaseCMD();
        BageBaseCmd.client_msg_no = BageCursor.readString(cursor, "client_msg_no");
        BageBaseCmd.cmd = BageCursor.readString(cursor, "cmd");
        BageBaseCmd.created_at = BageCursor.readString(cursor, "created_at");
        BageBaseCmd.message_id = BageCursor.readString(cursor, "message_id");
        BageBaseCmd.message_seq = BageCursor.readLong(cursor, "message_seq");
        BageBaseCmd.param = BageCursor.readString(cursor, "param");
        BageBaseCmd.sign = BageCursor.readString(cursor, "sign");
        BageBaseCmd.timestamp = BageCursor.readLong(cursor, "timestamp");
        return BageBaseCmd;
    }

    private void handleRevokeCmd(List<BageBaseCMD> list) {
        final Timer[] timer = {new Timer()};
        final int[] i = {0};
        timer[0].schedule(new TimerTask() {
            @Override
            public void run() {
                if (i[0] == list.size() - 1) {
                    timer[0].cancel();
                    timer[0] = null;
                }
                BageIM.getInstance().getCMDManager().handleCMD(list.get(i[0]).cmd, list.get(i[0]).param, list.get(i[0]).sign);
                i[0]++;
            }
        }, 0, 100);
    }

    //处理cmd
    public void handleCmd() {
        List<BageCMD> rtcList = new ArrayList<>();
        List<BageBaseCMD> cmdList = queryAllCmd();
        if (BageReader.isEmpty(cmdList)) return;
        HashMap<String, List<BageBaseCMD>> revokeMap = new HashMap<>();
        for (BageBaseCMD BageBaseCmd : cmdList) {
            if (BageBaseCmd.is_deleted == 0 && !TextUtils.isEmpty(BageBaseCmd.cmd)) {
                if (BageBaseCmd.cmd.equals(BageCMDKeys.bage_messageRevoke)) {
                    if (!TextUtils.isEmpty(BageBaseCmd.param)) {
                        try {
                            String channelID = "";
                            byte channelType = 0;
                            JSONObject jsonObject = new JSONObject(BageBaseCmd.param);
                            if (jsonObject.has("channel_id")) {
                                channelID = jsonObject.optString("channel_id");
                            }
                            if (jsonObject.has("channel_type")) {
                                channelType = (byte) jsonObject.optInt("channel_type");
                            }
                            if (!TextUtils.isEmpty(channelID)) {
                                List<BageBaseCMD> list;
                                String key = String.format("%s,%s", channelID, channelType);
                                if (revokeMap.containsKey(key)) {
                                    list = revokeMap.get(key);
                                    if (list == null) list = new ArrayList<>();
                                } else {
                                    list = new ArrayList<>();
                                }
                                list.add(BageBaseCmd);
                                revokeMap.put(key, list);
                            }
                        } catch (JSONException e) {
                            BageLogUtils.e("处理cmd错误");
                        }
                    }
                } else if (BageBaseCmd.cmd.startsWith("rtc.p2p")) {
                    try {
                        JSONObject jsonObject = new JSONObject(BageBaseCmd.param);
                        rtcList.add(new BageCMD(BageBaseCmd.cmd, jsonObject));
                    } catch (JSONException e) {
                        BageLogUtils.e("解析cmd错误");
                    }
                } else
                    BageIM.getInstance().getCMDManager().handleCMD(BageBaseCmd.cmd, BageBaseCmd.param, BageBaseCmd.sign);
            }
        }
        if (BageReader.isNotEmpty(rtcList)) {
            EndpointManager.getInstance().invoke("rtc_offline_data", rtcList);
        }
        if (!revokeMap.isEmpty()) {
            List<BageBaseCMD> tempList = new ArrayList<>();
            for (String key : revokeMap.keySet()) {
                String channelID = key.split(",")[0];
                byte channelType = Byte.parseByte(key.split(",")[1]);
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
                //是否撤回提醒
                int revokeRemind = 0;
                if (channel != null && channel.localExtra != null && channel.localExtra.containsKey(BageChannelExtras.revokeRemind)) {
                    Object object = channel.localExtra.get(BageChannelExtras.revokeRemind);
                    if (object != null) {
                        revokeRemind = (int) object;
                    }
                }
                if (revokeRemind == 1) {
                    EndpointManager.getInstance().invoke("syncExtraMsg", new BageChannel(channelID, channelType));
                } else {
                    List<BageBaseCMD> list = revokeMap.get(key);
                    if (BageReader.isNotEmpty(list))
                        tempList.addAll(list);
                }
                if (BageReader.isNotEmpty(tempList)) {
                    new Thread(() -> handleRevokeCmd(tempList)).start();
                }
            }
        }
        try {
            BageBaseApplication.getInstance().getDbHelper().getDB()
                    .beginTransaction();
            for (int i = 0; i < cmdList.size(); i++) {
                deleteCmd(cmdList.get(i).client_msg_no);
            }
            BageBaseApplication.getInstance().getDbHelper().getDB()
                    .setTransactionSuccessful();
        } catch (Exception ignored) {
        } finally {
            if (BageBaseApplication.getInstance().getDbHelper() != null && BageBaseApplication.getInstance().getDbHelper().getDB().inTransaction()) {
                BageBaseApplication.getInstance().getDbHelper().getDB()
                        .endTransaction();
            }
        }
    }
}
