package com.bage.im.manager;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.ConversationDbManager;
import com.bage.im.db.MsgDbManager;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsg;
import com.bage.im.entity.BageConversationMsgExtra;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgExtra;
import com.bage.im.entity.BageMsgReaction;
import com.bage.im.entity.BageSyncChat;
import com.bage.im.entity.BageSyncConvMsgExtra;
import com.bage.im.entity.BageSyncRecent;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.interfaces.IAllConversations;
import com.bage.im.interfaces.IDeleteConversationMsg;
import com.bage.im.interfaces.IRefreshConversationMsg;
import com.bage.im.interfaces.IRefreshConversationMsgList;
import com.bage.im.interfaces.ISyncConversationChat;
import com.bage.im.interfaces.ISyncConversationChatBack;
import com.bage.im.message.type.BageConnectStatus;
import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.utils.DispatchQueuePool;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/21/21 12:12 PM
 * 最近会话管理
 */
public class ConversationManager extends BaseManager {
    private final DispatchQueuePool dispatchQueuePool = new DispatchQueuePool(3);

    private final String TAG = "ConversationManager";

    private ConversationManager() {
    }

    private static class ConversationManagerBinder {
        static final ConversationManager manager = new ConversationManager();
    }

    public static ConversationManager getInstance() {
        return ConversationManagerBinder.manager;
    }

    //监听刷新最近会话
    private ConcurrentHashMap<String, IRefreshConversationMsg> refreshMsgMap;
    private ConcurrentHashMap<String, IRefreshConversationMsgList> refreshMsgListMap;

    //移除某个会话
    private ConcurrentHashMap<String, IDeleteConversationMsg> iDeleteMsgList;
    // 同步最近会话
    private ISyncConversationChat iSyncConversationChat;

    /**
     * 查询会话记录消息
     *
     * @return 最近会话集合
     */
    public List<BageUIConversationMsg> getAll() {
        return ConversationDbManager.getInstance().queryAll();
    }

    public void getAll(IAllConversations iAllConversations) {
        if (iAllConversations == null) {
            return;
        }
        dispatchQueuePool.execute(() -> {
            try {
                List<BageUIConversationMsg> list = ConversationDbManager.getInstance().queryAll();
                iAllConversations.onResult(list);
            } catch (Throwable t) {
                // Bugly#33246 防御：DB 关闭竞态导致的后台线程崩溃
                BageLoggerUtils.getInstance().e("ConversationManager", "getAll aborted: " + t.getMessage());
                iAllConversations.onResult(new java.util.ArrayList<>());
            }
        });
    }

    public List<BageConversationMsg> getWithChannelType(byte channelType) {
        return ConversationDbManager.getInstance().queryWithChannelType(channelType);
    }

    public List<BageUIConversationMsg> getWithChannelIds(List<String> channelIds) {
        return ConversationDbManager.getInstance().queryWithChannelIds(channelIds);
    }

    /**
     * 查询某条消息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return BageConversationMsg
     */
    public BageConversationMsg getWithChannel(String channelID, byte channelType) {
        return ConversationDbManager.getInstance().queryWithChannel(channelID, channelType);
    }

    public void updateWithMsg(BageConversationMsg mConversationMsg) {
        BageMsg msg = MsgDbManager.getInstance().queryMaxOrderSeqMsgWithChannel(mConversationMsg.channelID, mConversationMsg.channelType);
        if (msg != null) {
            mConversationMsg.lastClientMsgNO = msg.clientMsgNO;
            mConversationMsg.lastMsgSeq = msg.messageSeq;
        }
        ConversationDbManager.getInstance().updateMsg(mConversationMsg.channelID, mConversationMsg.channelType, mConversationMsg.lastClientMsgNO, mConversationMsg.lastMsgSeq, mConversationMsg.unreadCount);
    }

    /**
     * 删除某个会话记录信息
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public boolean deleteWitchChannel(String channelId, byte channelType) {
        return ConversationDbManager.getInstance().deleteWithChannel(channelId, channelType, 1);
    }

    /**
     * 清除所有最近会话
     */
    public boolean clearAll() {
        return ConversationDbManager.getInstance().clearEmpty();
    }

    public void addOnRefreshMsgListListener(String key, IRefreshConversationMsgList listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (refreshMsgListMap == null) {
            refreshMsgListMap = new ConcurrentHashMap<>();
        }
        refreshMsgListMap.put(key, listener);
    }

    public void removeOnRefreshMsgListListener(String key) {
        if (TextUtils.isEmpty(key) || refreshMsgListMap == null) return;
        refreshMsgListMap.remove(key);
    }

    /**
     * 监听刷新最近会话
     *
     * @param listener 回调
     */
    public void addOnRefreshMsgListener(String key, IRefreshConversationMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (refreshMsgMap == null)
            refreshMsgMap = new ConcurrentHashMap<>();
        refreshMsgMap.put(key, listener);
    }

    public void removeOnRefreshMsgListener(String key) {
        if (TextUtils.isEmpty(key) || refreshMsgMap == null) return;
        refreshMsgMap.remove(key);
    }

    /**
     * 设置刷新最近会话
     */
//    public void setOnRefreshMsg(BageUIConversationMsg conversationMsg, boolean isEnd, String from) {
//        if (refreshMsgMap != null && !refreshMsgMap.isEmpty() && conversationMsg != null) {
//            runOnMainThread(() -> {
//                for (Map.Entry<String, IRefreshConversationMsg> entry : refreshMsgMap.entrySet()) {
//                    entry.getValue().onRefreshConversationMsg(conversationMsg, isEnd);
//                }
//            });
//        }
//    }
    public void setOnRefreshMsg(BageUIConversationMsg msg, String from) {
        List<BageUIConversationMsg> list = new ArrayList<>();
        list.add(msg);
        this.setOnRefreshMsg(list, from);
    }

    public void setOnRefreshMsg(List<BageUIConversationMsg> list, String from) {
        if (BageCommonUtils.isEmpty(list)) return;
        if (refreshMsgMap != null && !refreshMsgMap.isEmpty()) {
            runOnMainThread(() -> {
                for (int i = 0, size = list.size(); i < size; i++) {
                    for (Map.Entry<String, IRefreshConversationMsg> entry : refreshMsgMap.entrySet()) {
                        entry.getValue().onRefreshConversationMsg(list.get(i), i == list.size() - 1);
                    }
                }
            });
        }
        if (refreshMsgListMap != null && !refreshMsgListMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshConversationMsgList> entry : refreshMsgListMap.entrySet()) {
                    entry.getValue().onRefresh(list);
                }
            });
        }
    }

    //监听删除最近会话监听
    public void addOnDeleteMsgListener(String key, IDeleteConversationMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (iDeleteMsgList == null) iDeleteMsgList = new ConcurrentHashMap<>();
        iDeleteMsgList.put(key, listener);
    }

    public void removeOnDeleteMsgListener(String key) {
        if (TextUtils.isEmpty(key) || iDeleteMsgList == null) return;
        iDeleteMsgList.remove(key);
    }

    // 删除某个最近会话
    public void setDeleteMsg(String channelID, byte channelType) {
        if (iDeleteMsgList != null && !iDeleteMsgList.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IDeleteConversationMsg> entry : iDeleteMsgList.entrySet()) {
                    entry.getValue().onDelete(channelID, channelType);
                }
            });
        }
    }

    public void updateRedDot(String channelID, byte channelType, int redDot) {
        boolean result = ConversationDbManager.getInstance().updateRedDot(channelID, channelType, redDot);
        if (result) {
            BageUIConversationMsg msg = getUIConversationMsg(channelID, channelType);
            setOnRefreshMsg(msg, "updateRedDot");
        }
    }

    public BageConversationMsgExtra getMsgExtraWithChannel(String channelID, byte channelType) {
        return ConversationDbManager.getInstance().queryMsgExtraWithChannel(channelID, channelType);
    }

    public void updateMsgExtra(BageConversationMsgExtra extra) {
        boolean result = ConversationDbManager.getInstance().insertOrUpdateMsgExtra(extra);
        if (result) {
            BageUIConversationMsg msg = getUIConversationMsg(extra.channelID, extra.channelType);
            List<BageUIConversationMsg> list = new ArrayList<>();
            list.add(msg);
            setOnRefreshMsg(list, "updateMsgExtra");
        }
    }

    public BageUIConversationMsg updateWithBageMsg(BageMsg msg) {
        if (msg == null || TextUtils.isEmpty(msg.channelID)) return null;
        return ConversationDbManager.getInstance().insertOrUpdateWithMsg(msg, 0);
    }

    public BageUIConversationMsg getUIConversationMsg(String channelID, byte channelType) {
        BageConversationMsg msg = ConversationDbManager.getInstance().queryWithChannel(channelID, channelType);
        if (msg == null) {
            return null;
        }
        return ConversationDbManager.getInstance().getUIMsg(msg);
    }

    public long getMsgExtraMaxVersion() {
        return ConversationDbManager.getInstance().queryMsgExtraMaxVersion();
    }

    public void saveSyncMsgExtras(List<BageSyncConvMsgExtra> list) {
        List<BageConversationMsgExtra> msgExtraList = new ArrayList<>();
        for (BageSyncConvMsgExtra msg : list) {
            msgExtraList.add(syncConvMsgExtraToConvMsgExtra(msg));
        }
        ConversationDbManager.getInstance().insertMsgExtras(msgExtraList);
    }

    private BageConversationMsgExtra syncConvMsgExtraToConvMsgExtra(BageSyncConvMsgExtra extra) {
        BageConversationMsgExtra msg = new BageConversationMsgExtra();
        msg.channelID = extra.channel_id;
        msg.channelType = extra.channel_type;
        msg.draft = extra.draft;
        msg.keepOffsetY = extra.keep_offset_y;
        msg.keepMessageSeq = extra.keep_message_seq;
        msg.version = extra.version;
        msg.browseTo = extra.browse_to;
        msg.draftUpdatedAt = extra.draft_updated_at;
        return msg;
    }


    public void addOnSyncConversationListener(ISyncConversationChat iSyncConvChatListener) {
        this.iSyncConversationChat = iSyncConvChatListener;
    }

    public void setSyncConversationListener(ISyncConversationChatBack iSyncConversationChatBack) {
        if (iSyncConversationChat != null) {
            long version = ConversationDbManager.getInstance().queryMaxVersion();
            String lastMsgSeqStr = ConversationDbManager.getInstance().queryLastMsgSeqs();
            runOnMainThread(() -> iSyncConversationChat.syncConversationChat(lastMsgSeqStr, 10, version, syncChat -> {
                dispatchQueuePool.execute(() -> {
                    try {
                        saveSyncChat(syncChat, () -> iSyncConversationChatBack.onBack(syncChat));
                    } catch (Throwable t) {
                        // Bugly#33246 防御：DB 关闭竞态导致的后台线程崩溃
                        BageLoggerUtils.getInstance().e("ConversationManager", "saveSyncChat aborted: " + t.getMessage());
                    }
                });
            }));
        } else {
            BageLoggerUtils.getInstance().e("未设置同步最近会话事件");
        }
    }


    interface ISaveSyncChatBack {
        void onBack();
    }


    private void saveSyncChat(BageSyncChat syncChat, final ISaveSyncChatBack iSaveSyncChatBack) {
        if (syncChat == null) {
            iSaveSyncChatBack.onBack();
            return;
        }
        List<BageConversationMsg> conversationMsgList = new ArrayList<>();
        List<BageMsg> msgList = new ArrayList<>();
        List<BageMsgReaction> msgReactionList = new ArrayList<>();
        List<BageMsgExtra> msgExtraList = new ArrayList<>();
        if (BageCommonUtils.isNotEmpty(syncChat.conversations)) {
            for (int i = 0, size = syncChat.conversations.size(); i < size; i++) {
                //最近会话消息对象
                BageConversationMsg conversationMsg = new BageConversationMsg();
                byte channelType = syncChat.conversations.get(i).channel_type;
                String channelID = syncChat.conversations.get(i).channel_id;
                if (channelType == BageChannelType.COMMUNITY_TOPIC) {
                    String[] str = channelID.split("@");
                    conversationMsg.parentChannelID = str[0];
                    conversationMsg.parentChannelType = BageChannelType.COMMUNITY;
                }
                conversationMsg.channelID = syncChat.conversations.get(i).channel_id;
                conversationMsg.channelType = syncChat.conversations.get(i).channel_type;
                conversationMsg.lastMsgSeq = syncChat.conversations.get(i).last_msg_seq;
                conversationMsg.lastClientMsgNO = syncChat.conversations.get(i).last_client_msg_no;
                conversationMsg.lastMsgTimestamp = syncChat.conversations.get(i).timestamp;
                conversationMsg.unreadCount = syncChat.conversations.get(i).unread;
                conversationMsg.version = syncChat.conversations.get(i).version;
                //聊天消息对象
                if (syncChat.conversations.get(i).recents != null && BageCommonUtils.isNotEmpty(syncChat.conversations)) {
                    for (BageSyncRecent bageSyncRecent : syncChat.conversations.get(i).recents) {
                        BageMsg msg = MsgManager.getInstance().BageSyncRecent2BageMsg(bageSyncRecent);
                        if (msg.type == BageMsgContentType.Bage_INSIDE_MSG) {
                            continue;
                        }
                        if (BageCommonUtils.isNotEmpty(msg.reactionList)) {
                            msgReactionList.addAll(msg.reactionList);
                        }
                        //判断会话列表的fromUID
                        if (conversationMsg.lastClientMsgNO.equals(msg.clientMsgNO)) {
                            conversationMsg.isDeleted = msg.isDeleted;
                        }
                        if (bageSyncRecent.message_extra != null) {
                            BageMsgExtra extra = MsgManager.getInstance().BageSyncExtraMsg2BageMsgExtra(msg.channelID, msg.channelType, bageSyncRecent.message_extra);
                            msgExtraList.add(extra);
                        }
                        msgList.add(msg);
                    }
                }

                conversationMsgList.add(conversationMsg);
            }
        }
        if (BageCommonUtils.isNotEmpty(msgExtraList)) {
            MsgDbManager.getInstance().insertOrReplaceExtra(msgExtraList);
        }
        List<BageUIConversationMsg> uiMsgList = new ArrayList<>();
        if (BageCommonUtils.isNotEmpty(conversationMsgList)) {
            if (BageCommonUtils.isNotEmpty(msgList)) {
                MsgDbManager.getInstance().insertMsgs(msgList);
            }
            try {
                if (BageCommonUtils.isNotEmpty(conversationMsgList)) {
                    List<ContentValues> cvList = new ArrayList<>();
                    for (int i = 0, size = conversationMsgList.size(); i < size; i++) {
                        ContentValues cv = ConversationDbManager.getInstance().getInsertSyncCV(conversationMsgList.get(i));
                        cvList.add(cv);
                        BageUIConversationMsg uiMsg = ConversationDbManager.getInstance().getUIMsg(conversationMsgList.get(i));
                        if (uiMsg != null) {
                            uiMsgList.add(uiMsg);
                        }
                    }
                    net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
                    if (db != null) {
                        db.beginTransaction();
                        for (ContentValues cv : cvList) {
                            ConversationDbManager.getInstance().insertSyncMsg(cv);
                        }
                        db.setTransactionSuccessful();
                    }
                }
            } catch (Exception ignored) {
                BageLoggerUtils.getInstance().e(TAG, "Save synchronization session message exception");
            } finally {
                try {
                    net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
                    if (db != null && db.inTransaction()) {
                        db.endTransaction();
                    }
                } catch (Exception ignored2) {
                }
            }
            if (BageCommonUtils.isNotEmpty(msgReactionList)) {
                MsgManager.getInstance().saveMsgReactions(msgReactionList);
            }
            // fixme 离线消息应该不能push给UI
            if (BageCommonUtils.isNotEmpty(msgList)) {
                HashMap<String, List<BageMsg>> allMsgMap = new HashMap<>();
                for (BageMsg bageMsg : msgList) {
                    if (TextUtils.isEmpty(bageMsg.channelID)) continue;
                    List<BageMsg> list;
                    if (allMsgMap.containsKey(bageMsg.channelID)) {
                        list = allMsgMap.get(bageMsg.channelID);
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                    } else {
                        list = new ArrayList<>();
                    }
                    list.add(bageMsg);
                    allMsgMap.put(bageMsg.channelID, list);
                }

//                for (Map.Entry<String, List<BageMsg>> entry : allMsgMap.entrySet()) {
//                    List<BageMsg> channelMsgList = entry.getValue();
//                    if (channelMsgList != null && channelMsgList.size() < 20) {
//                        Collections.sort(channelMsgList, new Comparator<BageMsg>() {
//                            @Override
//                            public int compare(BageMsg o1, BageMsg o2) {
//                                return Long.compare(o1.messageSeq, o2.messageSeq);
//                            }
//                        });
//                        MsgManager.getInstance().pushNewMsg(channelMsgList);
//                    }
//                }


            }
            if (BageCommonUtils.isNotEmpty(uiMsgList)) {
                setOnRefreshMsg(uiMsgList, "saveSyncChat");
//                for (int i = 0, size = uiMsgList.size(); i < size; i++) {
//                    BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == uiMsgList.size() - 1, "saveSyncChat");
//                }
            }
        }

        if (BageCommonUtils.isNotEmpty(syncChat.cmds)) {
            try {
                for (int i = 0, size = syncChat.cmds.size(); i < size; i++) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cmd", syncChat.cmds.get(i).cmd);
                    JSONObject json = new JSONObject(syncChat.cmds.get(i).param);
                    jsonObject.put("param", json);
                    CMDManager.getInstance().handleCMD(jsonObject);
                }
            } catch (JSONException e) {
                BageLoggerUtils.getInstance().e(TAG, "saveSyncChat cmd not json struct");
            }
        }
        BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.syncCompleted, "");
        iSaveSyncChatBack.onBack();
    }
}
