package com.chat.uikit.message;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.BageBaseModel;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.db.BageBaseCMD;
import com.chat.base.db.BageBaseCMDManager;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.ICommonListener;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.base.net.ud.BageDownloader;
import com.chat.base.net.ud.BageProgressManager;
import com.chat.base.net.ud.BageUploader;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.uikit.BageUIKitApplication;
import com.chat.uikit.enity.SensitiveWords;
import com.chat.uikit.enity.BageSyncReminder;
import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.BageDBColumns;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelState;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsg;
import com.bage.im.entity.BageConversationMsgExtra;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageReminder;
import com.bage.im.entity.BageSyncChannelMsg;
import com.bage.im.entity.BageSyncChat;
import com.bage.im.entity.BageSyncConvMsgExtra;
import com.bage.im.entity.BageSyncExtraMsg;
import com.bage.im.entity.BageSyncMsg;
import com.bage.im.interfaces.ISyncChannelMsgBack;
import com.bage.im.interfaces.ISyncConversationChatBack;
import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.message.type.BageSendMsgResult;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * 2019-11-24 14:18
 * 消息管理
 */
public class MsgModel extends BageBaseModel {
    private MsgModel() {

    }
   public List<BageChannelState> channelStatus;
    private int last_message_seq;

    private static class MsgModelBinder {
        final static MsgModel msgModel = new MsgModel();
    }

    public static MsgModel getInstance() {
        return MsgModelBinder.msgModel;
    }

    private Timer timer;

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public synchronized void startCheckFlameMsgTimer() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    deleteFlameMsg();
                }
            }, 100, 1000);
        }
    }

    public void deleteFlameMsg() {
        if (!BageConstants.isLogin()) return;
        List<BageMsg> list = BageIM.getInstance().getMsgManager().getWithFlame();
        if (BageReader.isEmpty(list)) return;
        List<String> deleteClientMsgNoList = new ArrayList<>();
        List<BageMsg> deleteMsgList = new ArrayList<>();
        boolean isStopTimer = true;
        for (BageMsg msg : list) {
            if (msg.flame == 1 && msg.viewed == 1) {
                long time = BageTimeUtils.getInstance().getCurrentMills() - msg.viewedAt;
                if (time / 1000 > msg.flameSecond || msg.flameSecond == 0) {
                    deleteClientMsgNoList.add(msg.clientMsgNO);
                    deleteMsgList.add(msg);
                }
                isStopTimer = false;
            }
        }
        if (isStopTimer && timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        deleteMsg(deleteMsgList, null);
        BageIM.getInstance().getMsgManager().deleteWithClientMsgNos(deleteClientMsgNoList);
    }

    private void ackMsg() {
        request(createService(MsgService.class).ackMsg(last_message_seq), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {

            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    /**
     * 删除消息
     */
    public void deleteMsg(List<BageMsg> list, final ICommonListener iCommonListener) {
        if (BageReader.isEmpty(list)) return;
        JSONArray jsonArray = new JSONArray();
        for (BageMsg msg : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message_id", msg.messageID);
            jsonObject.put("channel_id", msg.channelID);
            jsonObject.put("channel_type", msg.channelType);
            jsonObject.put("message_seq", msg.messageSeq);
            jsonArray.add(jsonObject);
        }
        request(createService(MsgService.class).deleteMsg(jsonArray), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void offsetMsg(String channelID, byte channelType, ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channel_id", channelID);
        jsonObject.put("channel_type", channelType);
        int msgSeq = BageIM.getInstance().getMsgManager().getMaxMessageSeqWithChannel(channelID, channelType);
        jsonObject.put("message_seq", msgSeq);
        request(createService(MsgService.class).offsetMsg(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 撤回消息
     *
     * @param msgId           消息ID
     * @param channelID       频道ID
     * @param channelType     频道类型
     * @param iCommonListener 返回
     */
    public void revokeMsg(String msgId, String channelID, byte channelType, String clientMsgNo, final ICommonListener iCommonListener) {
        request(createService(MsgService.class).revokeMsg(msgId, channelID, channelType, clientMsgNo), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 同步红点
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public void clearUnread(String channelId, byte channelType, int unreadCount, ICommonListener iCommonListener) {
        if (unreadCount < 0) unreadCount = 0;
        BageIM.getInstance().getConversationManager().updateRedDot(channelId, channelType, unreadCount);
        com.alibaba.fastjson.JSONObject jsonObject = new com.alibaba.fastjson.JSONObject();
        jsonObject.put("channel_id", channelId);
        jsonObject.put("channel_type", channelType);
        jsonObject.put("unread", unreadCount);
        request(createService(MsgService.class).clearUnread(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
            }
        });
    }

    /**
     * 修改语音已读
     *
     * @param messageID 服务器消息ID
     */
    public void updateVoiceStatus(String messageID, String channel_id, byte channel_type, int message_seq) {
        if (TextUtils.isEmpty(messageID)) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message_id", messageID);
        jsonObject.put("channel_id", channel_id);
        jsonObject.put("channel_type", channel_type);
        jsonObject.put("message_seq", message_seq);
        request(createService(MsgService.class).updateVoiceStatus(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
            }

            @Override
            public void onFail(int code, String msg) {
            }
        });
    }

    public void getChatIp(IChatIp iChatIp) {
        // SDK 的连接地址等待窗口是 6 秒，这个请求必须更早结束并在所有分支回调，
        // 否则 SDK 会一直停留在“正在连接”状态并反复重置。
        request(createService(MsgService.class)
                .getImIp(BageConfig.getInstance().getUid())
                .timeout(5, TimeUnit.SECONDS), new IRequestResultListener<>() {
            @Override
            public void onSuccess(Ipentity result) {
                if (result != null && !TextUtils.isEmpty(result.tcp_addr)) {
                    String[] strings = result.tcp_addr.split(":");
                    if (strings.length == 2 && !TextUtils.isEmpty(strings[0]) && !TextUtils.isEmpty(strings[1])) {
                        iChatIp.onResult(HttpResponseCode.success, strings[0], strings[1]);
                        return;
                    }
                }
                iChatIp.onResult(-1, "", "0");
            }

            @Override
            public void onFail(int code, String msg) {
                iChatIp.onResult(code, "", "0");
            }
        });
    }

    public interface IChatIp {
        void onResult(int code, String ip, String port);
    }

    public void favoriteMessage(BageMsg msg, boolean favorite, ICommonListener listener) {
        if (msg == null || msg.type != BageMsgContentType.Bage_TEXT
                || TextUtils.isEmpty(msg.messageID) || msg.messageSeq <= 0
                || TextUtils.isEmpty(msg.channelID)) {
            if (listener != null) listener.onResult(-1, "");
            return;
        }
        favoriteMessage(msg.messageID, msg.messageSeq, msg.channelID, msg.channelType,
                favorite, listener);
    }

    public void favoriteMessage(String messageID, int messageSeq, String channelID,
                                byte channelType, boolean favorite, ICommonListener listener) {
        JSONObject body = new JSONObject();
        body.put("message_id", messageID);
        body.put("message_seq", messageSeq);
        body.put("channel_id", channelID);
        body.put("channel_type", channelType);
        request(favorite
                        ? createService(MsgService.class).favoriteMessage(body)
                        : createService(MsgService.class).unfavoriteMessage(body),
                new IRequestResultListener<>() {
                    @Override
                    public void onSuccess(CommonResponse result) {
                        if (listener != null) listener.onResult(HttpResponseCode.success,
                                result == null ? "" : result.msg);
                    }

                    @Override
                    public void onFail(int code, String msg) {
                        if (listener != null) listener.onResult(code, msg);
                    }
                });
    }

    public void getFavoriteMessages(int pageIndex, int pageSize, IFavoriteMessages listener) {
        JSONObject body = new JSONObject();
        body.put("page_index", Math.max(1, pageIndex));
        body.put("page_size", Math.min(100, Math.max(1, pageSize)));
        request(createService(MsgService.class).favoriteMessages(body),
                new IRequestResultListener<>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (listener != null) listener.onResult(HttpResponseCode.success, "", result);
                    }

                    @Override
                    public void onFail(int code, String msg) {
                        if (listener != null) listener.onResult(code, msg, null);
                    }
                });
    }

    public interface IFavoriteMessages {
        void onResult(int code, String msg, JSONObject result);
    }

    public void typing(String channelID, byte channelType) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channel_id", channelID);
        jsonObject.put("channel_type", channelType);
        request(createService(MsgService.class).typing(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {

            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    private BageSyncMsg getBageSyncMsg(SyncMsg syncMsg) {
        BageMsg msg = new BageMsg();
        BageSyncMsg BageSyncMsg = new BageSyncMsg();
        msg.status = BageSendMsgResult.send_success;
        msg.messageID = syncMsg.message_id;
        msg.messageSeq = syncMsg.message_seq;
        msg.clientMsgNO = syncMsg.client_msg_no;
        msg.fromUID = syncMsg.from_uid;
        msg.channelID = syncMsg.channel_id;
        msg.channelType = syncMsg.channel_type;
        msg.voiceStatus = syncMsg.voice_status;
        msg.timestamp = syncMsg.timestamp;
        msg.isDeleted = syncMsg.is_delete;
        msg.remoteExtra.unreadCount = syncMsg.unread_count;
        msg.remoteExtra.readedCount = syncMsg.readed_count;
        msg.remoteExtra.extraVersion = syncMsg.extra_version;
        if (syncMsg.payload != null)
            msg.content = JSONObject.toJSONString(syncMsg.payload);
        if (syncMsg.payload != null && syncMsg.payload.containsKey("type")) {
            Object typeObject = syncMsg.payload.get("type");
            if (typeObject != null)
                msg.type = (int) typeObject;
        }
        BageSyncMsg.bageMsg = msg;
        BageSyncMsg.red_dot = syncMsg.header.red_dot;
        BageSyncMsg.sync_once = syncMsg.header.sync_once;
        BageSyncMsg.no_persist = syncMsg.header.no_persist;
        return BageSyncMsg;
    }

    /**
     * 同步会话
     *
     * @param last_msg_seqs 最后一条消息的msgseq数组
     * @param msg_count     同步消息条数
     * @param version       最大版本号
     */
    public void syncChat(String last_msg_seqs, int msg_count, long version, ISyncConversationChatBack iSyncConversationChatBack) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("last_msg_seqs", last_msg_seqs);
        jsonObject.put("msg_count", msg_count);
        jsonObject.put("version", version);
        jsonObject.put("device_uuid", BageConstants.getDeviceUUID());
        request(createService(MsgService.class).syncChat(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(BageSyncChat result) {
                if (result != null && !TextUtils.isEmpty(result.uid) && result.uid.equals(BageConfig.getInstance().getUid())) {
                    if (BageReader.isNotEmpty(result.conversations)) {
                        BageUIKitApplication.getInstance().isRefreshChatActivityMessage = true;
                    }
                    channelStatus = result.channel_status;
                    iSyncConversationChatBack.onBack(result);
                    last_message_seq = 0;
                    syncCmdMsgs(0);
                    ackDeviceUUID();
                    syncReminder();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> EndpointManager.getInstance().invoke("refresh_conversation_calling",null),300);
                } else {
                    iSyncConversationChatBack.onBack(null);
                }
            }

            @Override
            public void onFail(int code, String msg) {
                iSyncConversationChatBack.onBack(null);
            }
        });

    }

    public void ackDeviceUUID() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("device_uuid", BageConstants.getDeviceUUID());
        request(createService(MsgService.class).ackCoverMsg(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {

            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    /**
     * 同步某个频道的消息
     *
     * @param channelID           频道ID
     * @param channelType         频道类型
     * @param startMessageSeq     最小messageSeq
     * @param endMessageSeq       最大messageSeq
     * @param limit               获取条数
     * @param pullMode            拉取模式 0:向下拉取 1:向上拉取
     * @param iSyncChannelMsgBack 返回
     */
    public void syncChannelMsg(String channelID, byte channelType, long startMessageSeq, long endMessageSeq, int limit, int pullMode, final ISyncChannelMsgBack iSyncChannelMsgBack) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channel_id", channelID);
        jsonObject.put("channel_type", channelType);
        jsonObject.put("start_message_seq", startMessageSeq);
        jsonObject.put("end_message_seq", endMessageSeq);
        jsonObject.put("limit", limit);
        jsonObject.put("pull_mode", pullMode);
        jsonObject.put("device_uuid", BageConstants.getDeviceUUID());
        request(createPriorityService(MsgService.class).syncChannelMsg(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(BageSyncChannelMsg result) {
                iSyncChannelMsgBack.onBack(result);
                ackDeviceUUID();
            }

            @Override
            public void onFail(int code, String msg) {
                iSyncChannelMsgBack.onBack(null);
            }
        });
    }

    /**
     * 只读取本地频道消息，不触发 SDK 的历史消息网络补齐。
     * <p>
     * SDK 的 getOrSyncHistoryMessages 在本地不足一页时会等网络同步完成才回调；
     * 首次登录本地已有会话同步下来的最近消息时，先用这里把它们立即显示出来。
     */
    public void getLocalChannelMessages(String channelID, byte channelType, int limit,
                                        ILocalChannelMessagesBack back) {
        final int safeLimit = Math.max(1, Math.min(limit, 100));
        new Thread(() -> {
            List<BageMsg> result = new ArrayList<>();
            List<String> messageIDs = new ArrayList<>();
            List<String> clientMsgNos = new ArrayList<>();
            String table = BageDBColumns.TABLE.message;
            String sql = "SELECT " + BageDBColumns.BageMessageColumns.message_id + ","
                    + BageDBColumns.BageMessageColumns.client_msg_no
                    + " FROM " + table
                    + " WHERE " + BageDBColumns.BageMessageColumns.channel_id + "=?"
                    + " AND " + BageDBColumns.BageMessageColumns.channel_type + "=?"
                    + " AND " + BageDBColumns.BageMessageColumns.type + "<>0"
                    + " AND " + BageDBColumns.BageMessageColumns.type + "<>99"
                    + " AND " + BageDBColumns.BageMessageColumns.is_deleted + "=0"
                    + " ORDER BY " + BageDBColumns.BageMessageColumns.order_seq + " DESC"
                    + " LIMIT " + safeLimit;
            try (Cursor cursor = BageIMApplication.getInstance().getDbHelper()
                    .rawQuery(sql, new Object[]{channelID, channelType})) {
                if (cursor != null) {
                    int messageIDIndex = cursor.getColumnIndex(BageDBColumns.BageMessageColumns.message_id);
                    int clientMsgNoIndex = cursor.getColumnIndex(BageDBColumns.BageMessageColumns.client_msg_no);
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        String messageID = cursor.getString(messageIDIndex);
                        String clientMsgNo = cursor.getString(clientMsgNoIndex);
                        if (!TextUtils.isEmpty(messageID)) {
                            messageIDs.add(messageID);
                        } else if (!TextUtils.isEmpty(clientMsgNo)) {
                            clientMsgNos.add(clientMsgNo);
                        }
                    }
                }
                if (BageReader.isNotEmpty(messageIDs)) {
                    result.addAll(BageIM.getInstance().getMsgManager().getWithMessageIDs(messageIDs));
                }
                for (String clientMsgNo : clientMsgNos) {
                    BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                    if (msg != null) result.add(msg);
                }
                result.removeIf(msg -> msg == null || msg.isDeleted == 1
                        || (msg.remoteExtra != null && msg.remoteExtra.isMutualDeleted == 1));
                result.sort(Comparator.comparingLong(msg -> msg.orderSeq));
            } catch (Throwable throwable) {
                BageLogUtils.e("读取本地聊天记录失败：" + throwable.getMessage());
                result.clear();
            }
            List<BageMsg> localMessages = result;
            new Handler(Looper.getMainLooper()).post(() -> back.onBack(localMessages));
        }, "local-channel-messages").start();
    }

    public interface ILocalChannelMessagesBack {
        void onBack(List<BageMsg> messages);
    }

    /**
     * 同步cmd消息
     *
     * @param max_message_seq 最大消息编号
     */
    private void syncCmdMsgs(long max_message_seq) {

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("limit", 500);
        jsonObject1.put("max_message_seq", max_message_seq);
        request(createService(MsgService.class).syncMsg(jsonObject1), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<SyncMsg> list) {
                if (BageReader.isNotEmpty(list)) {
                    List<BageBaseCMD> cmdList = new ArrayList<>();
                    for (int i = 0, size = list.size(); i < size; i++) {
                        BageSyncMsg BageSyncMsg = getBageSyncMsg(list.get(i));
                        BageBaseCMD BageBaseCmd = new BageBaseCMD();
                        if (BageSyncMsg.bageMsg.type == BageMsgContentType.Bage_INSIDE_MSG) {
                            BageBaseCmd.client_msg_no = BageSyncMsg.bageMsg.clientMsgNO;
                            BageBaseCmd.created_at = BageSyncMsg.bageMsg.createdAt;
                            BageBaseCmd.message_id = BageSyncMsg.bageMsg.messageID;
                            BageBaseCmd.message_seq = BageSyncMsg.bageMsg.messageSeq;
                            BageBaseCmd.timestamp = BageSyncMsg.bageMsg.timestamp;
                            try {
                                org.json.JSONObject jsonObject = new org.json.JSONObject(BageSyncMsg.bageMsg.content);
                                if (jsonObject.has("cmd")) {
                                    BageBaseCmd.cmd = jsonObject.optString("cmd");
                                }
                                if (jsonObject.has("sign")) {
                                    BageBaseCmd.sign = jsonObject.optString("sign");
                                }
                                if (jsonObject.has("param")) {
                                    org.json.JSONObject paramJson = jsonObject.optJSONObject("param");
                                    if (paramJson != null) {
                                        if (!paramJson.has("channel_id") && !TextUtils.isEmpty(BageSyncMsg.bageMsg.channelID)) {
                                            paramJson.put("channel_id", BageSyncMsg.bageMsg.channelID);
                                        }
                                        if (!paramJson.has("channel_type")) {
                                            paramJson.put("channel_type", BageSyncMsg.bageMsg.channelType);
                                        }
                                        BageBaseCmd.param = paramJson.toString();
                                    }
                                }
                            } catch (JSONException e) {
                                BageLogUtils.e("MsgModel", "cmd messages not json struct");
                            }
                            cmdList.add(BageBaseCmd);
                        }
                        if (BageSyncMsg.bageMsg.messageSeq > last_message_seq) {
                            last_message_seq = BageSyncMsg.bageMsg.messageSeq;
                        }
                    }
                    //保存cmd
                    BageBaseCMDManager.getInstance().addCmd(cmdList);
                    if (last_message_seq != 0) {
                        ackMsg();
                    }
                    AndroidUtilities.runOnUIThread(() -> syncCmdMsgs(last_message_seq),1000);

                } else {
                    if (last_message_seq != 0) {
                        ackMsg();
                    }
                    //处理cmd
                    BageBaseCMDManager.getInstance().handleCmd();
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (last_message_seq != 0) {
                    ackMsg();
                    BageBaseCMDManager.getInstance().handleCmd();
                }
            }
        });
    }

    /**
     * 同步某个会话的扩展消息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     */
    public void syncExtraMsg(String channelID, byte channelType) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channel_id", channelID);
        jsonObject.put("channel_type", channelType);
        long maxExtraVersion = BageIM.getInstance().getMsgManager().getMsgExtraMaxVersionWithChannel(channelID, channelType);
        jsonObject.put("extra_version", maxExtraVersion);
        jsonObject.put("limit", 100);
        String deviceUUID = BageConstants.getDeviceUUID();
        jsonObject.put("source", deviceUUID);
        request(createService(MsgService.class).syncExtraMsg(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<BageSyncExtraMsg> result) {
                if (BageReader.isNotEmpty(result)) {
                    // 更改扩展消息
                    BageIM.getInstance().getMsgManager().saveRemoteExtraMsg(new BageChannel(channelID, channelType), result);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> syncExtraMsg(channelID, channelType), 500);
                }
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }


    // 同步敏感词
    public void syncSensitiveWords() {
        if (TextUtils.isEmpty(BageConfig.getInstance().getToken())) return;
        long version = BageSharedPreferencesUtil.getInstance().getLong("bage_sensitive_words_version");
        request(createService(MsgService.class).syncSensitiveWords(version), new IRequestResultListener<>() {
            @Override
            public void onSuccess(SensitiveWords result) {
                BageSharedPreferencesUtil.getInstance().putLong("bage_sensitive_words_version", result.version);
                if (!TextUtils.isEmpty(result.tips)) {
                    BageUIKitApplication.getInstance().sensitiveWords = result;
                    String json = JSON.toJSONString(result);
                    BageSharedPreferencesUtil.getInstance().putSP("bage_sensitive_words", json);
                }
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    public void editMsg(String msgID, int msgSeq, String channelID, byte channelType, String content, ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message_id", msgID);
        jsonObject.put("message_seq", msgSeq);
        jsonObject.put("channel_id", channelID);
        jsonObject.put("channel_type", channelType);
        jsonObject.put("content_edit", content);
        request(createService(MsgService.class).editMsg(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (iCommonListener != null)
                    iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null)
                    iCommonListener.onResult(code, msg);
            }
        });
    }

    public void syncReminder() {
        long version = BageIM.getInstance().getReminderManager().getMaxVersion();
        List<String> channelIDs = new ArrayList<>();
        List<BageConversationMsg> list = BageIM.getInstance().getConversationManager().getWithChannelType(BageChannelType.GROUP);
        for (BageConversationMsg mConversationMsg : list) {
            channelIDs.add(mConversationMsg.channelID);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", version);
        jsonObject.put("limit", 200);
        jsonObject.put("channel_ids", channelIDs);
        request(createService(MsgService.class).syncReminder(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<BageSyncReminder> result) {
                if (BageReader.isNotEmpty(result)) {
                    String loginUID = BageConfig.getInstance().getUid();
                    List<BageReminder> list = new ArrayList<>();
                    for (BageSyncReminder reminder : result) {
                        BageReminder BageReminder = syncReminderToReminder(reminder);
                        if (!TextUtils.isEmpty(reminder.publisher) && reminder.publisher.equals(loginUID)) {
                            BageReminder.done = 1;
                        }
                        list.add(BageReminder);
                    }
                    BageIM.getInstance().getReminderManager().saveOrUpdateReminders(list);

                }

            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    public void doneReminder(List<Long> list) {
        if (BageReader.isEmpty(list)) return;
        request(createService(MsgService.class).doneReminder(list), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {

            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    public void updateCoverExtra(String channelID, byte channelType, long browseTo, long keepMsgSeq, int keepOffsetY, String draft) {
        BageConversationMsgExtra extra = new BageConversationMsgExtra();
        extra.draft = draft;
        extra.keepOffsetY = keepOffsetY;
        extra.keepMessageSeq = keepMsgSeq;
        extra.channelID = channelID;
        extra.channelType = channelType;
        extra.browseTo = browseTo;
        if (!TextUtils.isEmpty(draft)) {
            extra.draftUpdatedAt = BageTimeUtils.getInstance().getCurrentSeconds();
        }
        BageIM.getInstance().getConversationManager().updateMsgExtra(extra);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("browse_to", browseTo);
        jsonObject.put("keep_message_seq", keepMsgSeq);
        jsonObject.put("keep_offset_y", keepOffsetY);
        jsonObject.put("draft", draft);
        request(createService(MsgService.class).updateCoverExtra(channelID, channelType, jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {

            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    public void syncCoverExtra() {
        long version = BageIM.getInstance().getConversationManager().getMsgExtraMaxVersion();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", version);
        request(createService(MsgService.class).syncCoverExtra(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<BageSyncConvMsgExtra> result) {
                BageIM.getInstance().getConversationManager().saveSyncMsgExtras(result);
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    private BageReminder syncReminderToReminder(BageSyncReminder syncReminder) {
        BageReminder reminder = new BageReminder();
        reminder.reminderID = syncReminder.id;
        reminder.channelID = syncReminder.channel_id;
        reminder.channelType = syncReminder.channel_type;
        reminder.messageSeq = syncReminder.message_seq;
        reminder.type = syncReminder.reminder_type;
        reminder.isLocate = syncReminder.is_locate;
        reminder.text = syncReminder.text;
        reminder.version = syncReminder.version;
        reminder.messageID = syncReminder.message_id;
        reminder.uid = syncReminder.uid;
        reminder.done = syncReminder.done;
        reminder.data = syncReminder.data;
        reminder.publisher = syncReminder.publisher;
        return reminder;
    }

    public void backupMsg(String filePath, ICommonListener iCommonListener) {
        String url = BageApiConfig.baseUrl + "message/backup";
        BageUploader.getInstance().upload(url, filePath, new BageUploader.IUploadBack() {
            @Override
            public void onSuccess(String url) {
                iCommonListener.onResult(HttpResponseCode.success, "");
            }

            @Override
            public void onError() {
                iCommonListener.onResult(HttpResponseCode.error, "");
            }
        });
    }

    public void recovery(final IRecovery iRecovery) {
        String uid = BageConfig.getInstance().getUid();
        String url = BageApiConfig.baseUrl + "message/recovery";
        String path = BageConstants.messageBackupDir + uid + "_recovery.json";
        BageDownloader.Companion.getInstance().download(url, path, new BageProgressManager.IProgress() {
            @Override
            public void onProgress(@Nullable Object tag, int progress) {

            }

            @Override
            public void onSuccess(@Nullable Object tag, @Nullable String path) {
                iRecovery.onSuccess(path);
            }

            @Override
            public void onFail(@Nullable Object tag, @Nullable String msg) {
                iRecovery.onFail();
            }
        });
    }

    public interface IRecovery {
        void onSuccess(String path);

        void onFail();
    }
}
