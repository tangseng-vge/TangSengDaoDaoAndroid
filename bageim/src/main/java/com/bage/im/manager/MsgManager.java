package com.bage.im.manager;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.ConversationDbManager;
import com.bage.im.db.MsgDbManager;
import com.bage.im.db.BageDBColumns;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsg;
import com.bage.im.entity.BageMentionInfo;
import com.bage.im.entity.BageMessageGroupByDate;
import com.bage.im.entity.BageMessageSearchResult;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgExtra;
import com.bage.im.entity.BageMsgReaction;
import com.bage.im.entity.BageMsgSetting;
import com.bage.im.entity.BageSendOptions;
import com.bage.im.entity.BageSyncExtraMsg;
import com.bage.im.entity.BageSyncMsg;
import com.bage.im.entity.BageSyncMsgReaction;
import com.bage.im.entity.BageSyncRecent;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.interfaces.IClearMsgListener;
import com.bage.im.interfaces.IDeleteMsgListener;
import com.bage.im.interfaces.IGetOrSyncHistoryMsgBack;
import com.bage.im.interfaces.IMessageStoreBeforeIntercept;
import com.bage.im.interfaces.INewMsgListener;
import com.bage.im.interfaces.IRefreshMsg;
import com.bage.im.interfaces.ISendACK;
import com.bage.im.interfaces.ISendMsgCallBackListener;
import com.bage.im.interfaces.ISyncChannelMsgBack;
import com.bage.im.interfaces.ISyncChannelMsgListener;
import com.bage.im.interfaces.ISyncOfflineMsgBack;
import com.bage.im.interfaces.ISyncOfflineMsgListener;
import com.bage.im.interfaces.IUploadAttacResultListener;
import com.bage.im.interfaces.IUploadAttachmentListener;
import com.bage.im.interfaces.IUploadMsgExtraListener;
import com.bage.im.message.MessageHandler;
import com.bage.im.message.BageConnection;
import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.message.type.BageSendMsgResult;
import com.bage.im.msgmodel.BageFormatErrorContent;
import com.bage.im.msgmodel.BageImageContent;
import com.bage.im.msgmodel.BageMessageContent;
import com.bage.im.msgmodel.BageMsgEntity;
import com.bage.im.msgmodel.BageReply;
import com.bage.im.msgmodel.BageTextContent;
import com.bage.im.msgmodel.BageVideoContent;
import com.bage.im.msgmodel.BageVoiceContent;
import com.bage.im.utils.DateUtils;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;
import com.bage.im.utils.BageTypeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/20/21 5:38 PM
 * 消息管理
 */
public class MsgManager extends BaseManager {
    private final String TAG = "MsgManager";

    private MsgManager() {
    }

    private static class MsgManagerBinder {
        static final MsgManager msgManager = new MsgManager();
    }

    public static MsgManager getInstance() {
        return MsgManagerBinder.msgManager;
    }

    private final long bageOrderSeqFactor = 1000L;
    // 消息修改
    private ConcurrentHashMap<String, IRefreshMsg> refreshMsgListenerMap;
    // 监听发送消息回调
    private ConcurrentHashMap<String, ISendMsgCallBackListener> sendMsgCallBackListenerHashMap;
    // 删除消息监听
    private ConcurrentHashMap<String, IDeleteMsgListener> deleteMsgListenerMap;
    // 发送消息ack监听
    private ConcurrentHashMap<String, ISendACK> sendAckListenerMap;
    // 新消息监听
    private ConcurrentHashMap<String, INewMsgListener> newMsgListenerMap;
    // 清空消息
    private ConcurrentHashMap<String, IClearMsgListener> clearMsgMap;
    // 上传文件附件
    private IUploadAttachmentListener iUploadAttachmentListener;
    // 同步离线消息
    private ISyncOfflineMsgListener iOfflineMsgListener;
    // 同步channel内消息
    private ISyncChannelMsgListener iSyncChannelMsgListener;

    // 消息存库拦截器
    private IMessageStoreBeforeIntercept messageStoreBeforeIntercept;
    // 自定义消息model
    private List<java.lang.Class<? extends BageMessageContent>> customContentMsgList;
    // 上传消息扩展
    private IUploadMsgExtraListener iUploadMsgExtraListener;
    private Timer checkMsgNeedUploadTimer;

    // 初始化默认消息model
    public void initNormalMsg() {
        if (customContentMsgList == null) {
            customContentMsgList = new ArrayList<>();
            customContentMsgList.add(BageTextContent.class);
            customContentMsgList.add(BageImageContent.class);
            customContentMsgList.add(BageVideoContent.class);
            customContentMsgList.add(BageVoiceContent.class);
        }
    }

    /**
     * 注册消息module
     *
     * @param contentMsg 消息
     */
    public void registerContentMsg(java.lang.Class<? extends BageMessageContent> contentMsg) {
        if (BageCommonUtils.isEmpty(customContentMsgList))
            initNormalMsg();
        try {
            boolean isAdd = true;
            for (int i = 0, size = customContentMsgList.size(); i < size; i++) {
                if (customContentMsgList.get(i).getDeclaredConstructor().newInstance().type == contentMsg.newInstance().type) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd)
                customContentMsgList.add(contentMsg);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            BageLoggerUtils.getInstance().e(TAG, "registerContentMsg error " + e.getLocalizedMessage());
        }

    }

    // 通过json获取消息model
    public BageMessageContent getMsgContentModel(JSONObject jsonObject) {
        int type = jsonObject.optInt("type");
        BageMessageContent messageContent = getMsgContentModel(type, jsonObject);
        return messageContent;
    }

    public BageMessageContent getMsgContentModel(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) {
            return new BageFormatErrorContent();
        }
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonStr);
        } catch (JSONException e) {
            BageLoggerUtils.getInstance().e(TAG, "getMsgContentModel The parameter is not a JSON");
        }
        if (jsonObject == null) {
            return new BageFormatErrorContent();
        }
        return getMsgContentModel(jsonObject);
    }

    public BageMessageContent getMsgContentModel(int contentType, JSONObject jsonObject) {
        if (jsonObject == null) jsonObject = new JSONObject();
        BageMessageContent baseContentMsgModel = getContentMsgModel(contentType, jsonObject);
        if (baseContentMsgModel == null) {
            baseContentMsgModel = new BageMessageContent();
        }
        //解析@成员列表
        if (jsonObject.has("mention")) {
            JSONObject tempJson = jsonObject.optJSONObject("mention");
            if (tempJson != null) {
                //是否@所有人
                if (tempJson.has("all"))
                    baseContentMsgModel.mentionAll = tempJson.optInt("all");
                JSONArray uidList = tempJson.optJSONArray("uids");

                if (uidList != null && uidList.length() > 0) {
                    BageMentionInfo mentionInfo = new BageMentionInfo();
                    List<String> mentionInfoUIDs = new ArrayList<>();
                    for (int i = 0, size = uidList.length(); i < size; i++) {
                        String uid = uidList.optString(i);
                        if (uid.equals(BageIMApplication.getInstance().getUid())) {
                            mentionInfo.isMentionMe = true;
                        }
                        mentionInfoUIDs.add(uid);
                    }
                    mentionInfo.uids = mentionInfoUIDs;
                    if (baseContentMsgModel.mentionAll == 1) {
                        mentionInfo.isMentionMe = true;
                    }
                    baseContentMsgModel.mentionInfo = mentionInfo;
                }
            }
        }

        if (jsonObject.has("from_uid"))
            baseContentMsgModel.fromUID = jsonObject.optString("from_uid");
        if (jsonObject.has("flame"))
            baseContentMsgModel.flame = jsonObject.optInt("flame");
        if (jsonObject.has("flame_second"))
            baseContentMsgModel.flameSecond = jsonObject.optInt("flame_second");
        if (jsonObject.has("robot_id"))
            baseContentMsgModel.robotID = jsonObject.optString("robot_id");

        //判断消息中是否包含回复情况
        if (jsonObject.has("reply")) {
            baseContentMsgModel.reply = new BageReply();
            JSONObject replyJson = jsonObject.optJSONObject("reply");
            if (replyJson != null) {
                baseContentMsgModel.reply = baseContentMsgModel.reply.decodeMsg(replyJson);
            }
        }

        if (jsonObject.has("entities")) {
            JSONArray jsonArray = jsonObject.optJSONArray("entities");
            if (jsonArray != null && jsonArray.length() > 0) {
                List<BageMsgEntity> list = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    BageMsgEntity entity = new BageMsgEntity();
                    JSONObject jo = jsonArray.optJSONObject(i);
                    entity.type = jo.optString("type");
                    entity.offset = jo.optInt("offset");
                    entity.length = jo.optInt("length");
                    entity.value = jo.optString("value");
                    list.add(entity);
                }
                baseContentMsgModel.entities = list;
            }
        }
        return baseContentMsgModel;
    }

    /**
     * 将json消息转成对于的消息model
     *
     * @param type       content type
     * @param jsonObject content json
     * @return model
     */
    private BageMessageContent getContentMsgModel(int type, JSONObject jsonObject) {
        java.lang.Class<? extends BageMessageContent> baseMsg = null;
        if (customContentMsgList != null && !customContentMsgList.isEmpty()) {
            try {
                for (int i = 0, size = customContentMsgList.size(); i < size; i++) {
                    if (customContentMsgList.get(i).getDeclaredConstructor().newInstance().type == type) {
                        baseMsg = customContentMsgList.get(i);
                        break;
                    }
                }
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                     InvocationTargetException e) {
                BageLoggerUtils.getInstance().e(TAG, "getContentMsgModel error" + e.getLocalizedMessage());
                return null;
            }
        }
        try {
            // 注册的消息model必须提供无参的构造方法
            if (baseMsg != null) {
                return baseMsg.newInstance().decodeMsg(jsonObject);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            BageLoggerUtils.getInstance().e(TAG, "getContentMsgModel decodeMsg error");
            return null;
        }
        return null;
    }

    private long getOrNearbyMsgSeq(long orderSeq) {
        if (orderSeq % bageOrderSeqFactor == 0) {
            return orderSeq / bageOrderSeqFactor;
        }
        return (orderSeq - orderSeq % bageOrderSeqFactor) / bageOrderSeqFactor;
    }

    /**
     * 查询或同步某个频道消息
     *
     * @param channelId                频道ID
     * @param channelType              频道类型
     * @param oldestOrderSeq           最后一次消息大orderSeq 第一次进入聊天传入0
     * @param contain                  是否包含 oldestOrderSeq 这条消息
     * @param pullMode                 拉取模式 0:向下拉取 1:向上拉取
     * @param aroundMsgOrderSeq        查询此消息附近消息
     * @param limit                    每次获取数量
     * @param iGetOrSyncHistoryMsgBack 请求返还
     */
    public void getOrSyncHistoryMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, int pullMode, int limit, long aroundMsgOrderSeq, final IGetOrSyncHistoryMsgBack iGetOrSyncHistoryMsgBack) {
        new Thread(() -> {
            try {
            int tempPullMode = pullMode;
            long tempOldestOrderSeq = oldestOrderSeq;
            boolean tempContain = contain;
            if (aroundMsgOrderSeq != 0) {
//                    long maxMsgSeq = getMaxMessageSeqWithChannel(channelId, channelType);
                long maxMsgSeq =
                        MsgDbManager.getInstance().queryMaxMessageSeqNotDeletedWithChannel(channelId, channelType);
                long aroundMsgSeq = getOrNearbyMsgSeq(aroundMsgOrderSeq);

                if (maxMsgSeq >= aroundMsgSeq && maxMsgSeq - aroundMsgSeq <= limit) {
                    // 显示最后一页数据
//                oldestOrderSeq = 0;
                    tempOldestOrderSeq = getMaxOrderSeqWithChannel(channelId, channelType);
//                    tempOldestOrderSeq = getMessageOrderSeq(maxMsgSeq, channelId, channelType);
                    if (tempOldestOrderSeq < aroundMsgOrderSeq) {
                        tempOldestOrderSeq = aroundMsgOrderSeq;
                    }
                    tempContain = true;
                    tempPullMode = 0;
                } else {
                    long minOrderSeq = MsgDbManager.getInstance().queryOrderSeq(channelId, channelType, aroundMsgOrderSeq, 3);
                    if (minOrderSeq == 0) {
                        tempOldestOrderSeq = aroundMsgOrderSeq;
                    } else {
                        if (minOrderSeq + limit < aroundMsgOrderSeq) {
                            if (aroundMsgOrderSeq % bageOrderSeqFactor == 0) {
                                tempOldestOrderSeq = (aroundMsgOrderSeq / bageOrderSeqFactor - 3) * bageOrderSeqFactor;
                            } else
                                tempOldestOrderSeq = aroundMsgOrderSeq - 3;
//                        oldestOrderSeq = aroundMsgOrderSeq;
                        } else {
                            // todo 这里只会查询3条数据  oldestOrderSeq = minOrderSeq
                            long startOrderSeq = MsgDbManager.getInstance().queryOrderSeq(channelId, channelType, aroundMsgOrderSeq, limit);
                            if (startOrderSeq == 0) {
                                tempOldestOrderSeq = aroundMsgOrderSeq;
                            } else
                                tempOldestOrderSeq = startOrderSeq;
                        }
                    }
                    tempPullMode = 1;
                    tempContain = true;
                }
            }
            MsgDbManager.getInstance().queryOrSyncHistoryMessages(channelId, channelType, tempOldestOrderSeq, tempContain, tempPullMode, limit, iGetOrSyncHistoryMsgBack);
            } catch (Throwable t) {
                // Bugly#33246 防御：DB 关闭竞态 / 登出路径导致的连接池关闭等异步崩溃，在此兜底
                com.bage.im.utils.BageLoggerUtils.getInstance().e("MsgManager", "getOrSyncHistoryMessages aborted: " + t.getMessage());
            }
        }).start();
    }

    public List<BageMsg> getAll() {
        return MsgDbManager.getInstance().queryAll();
    }

    public List<BageMsg> getWithFromUID(String channelID, byte channelType, String fromUID, long oldestOrderSeq, int limit) {
        return MsgDbManager.getInstance().queryWithFromUID(channelID, channelType, fromUID, oldestOrderSeq, limit);
    }

    /**
     * 批量删除消息
     *
     * @param clientMsgNos 消息编号集合
     */
    public void deleteWithClientMsgNos(List<String> clientMsgNos) {
        if (BageCommonUtils.isEmpty(clientMsgNos)) return;
        List<BageMsg> list = new ArrayList<>();
        net.zetetic.database.sqlcipher.SQLiteDatabase db = BageIMApplication.getInstance().getDbHelper().getDb();
        if (db == null) return;
        try {
            db.beginTransaction();
            for (int i = 0, size = clientMsgNos.size(); i < size; i++) {
                BageMsg msg = MsgDbManager.getInstance().deleteWithClientMsgNo(clientMsgNos.get(i));
                if (msg != null) {
                    list.add(msg);
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
        List<BageMsg> deleteMsgList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            setDeleteMsg(list.get(i));
            boolean isAdd = true;
            for (int j = 0, len = deleteMsgList.size(); j < len; j++) {
                if (deleteMsgList.get(j).channelID.equals(list.get(i).channelID)
                        && deleteMsgList.get(j).channelType == list.get(i).channelType) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) deleteMsgList.add(list.get(i));
        }
        List<BageUIConversationMsg> uiMsgList = new ArrayList<>();
        for (int i = 0, size = deleteMsgList.size(); i < size; i++) {
            BageMsg msg = MsgDbManager.getInstance().queryMaxOrderSeqMsgWithChannel(deleteMsgList.get(i).channelID, deleteMsgList.get(i).channelType);
            if (msg != null) {
                BageUIConversationMsg uiMsg = BageIM.getInstance().getConversationManager().updateWithBageMsg(msg);
                if (uiMsg != null) {
                    uiMsgList.add(uiMsg);
//                    BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsg, i == deleteMsgList.size()
//                            - 1, "deleteWithClientMsgNOList");
                }
            }
        }
        BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsgList, "deleteWithClientMsgNOList");
    }

    public List<BageMsg> getExpireMessages(int limit) {
        long time = DateUtils.getInstance().getCurrentSeconds();
        return MsgDbManager.getInstance().queryExpireMessages(time, limit);
    }

    /**
     * 删除某条消息
     *
     * @param client_seq 客户端序列号
     */
    public boolean deleteWithClientSeq(long client_seq) {
        return MsgDbManager.getInstance().deleteWithClientSeq(client_seq);
    }

    /**
     * 查询某条消息所在行
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param clientMsgNo 客户端消息ID
     * @return int
     */
    public int getRowNoWithOrderSeq(String channelID, byte channelType, String clientMsgNo) {
        BageMsg msg = MsgDbManager.getInstance().queryWithClientMsgNo(clientMsgNo);
        return MsgDbManager.getInstance().queryRowNoWithOrderSeq(channelID, channelType, msg == null ? 0 : msg.orderSeq);
    }

    public int getRowNoWithMessageID(String channelID, byte channelType, String messageID) {
        BageMsg msg = MsgDbManager.getInstance().queryWithMessageID(messageID, false);
        return MsgDbManager.getInstance().queryRowNoWithOrderSeq(channelID, channelType, msg == null ? 0 : msg.orderSeq);
    }

    public void deleteWithClientMsgNO(String clientMsgNo) {
        BageMsg msg = MsgDbManager.getInstance().deleteWithClientMsgNo(clientMsgNo);
        if (msg != null) {
            setDeleteMsg(msg);
            BageConversationMsg conversationMsg = BageIM.getInstance().getConversationManager().getWithChannel(msg.channelID, msg.channelType);
            if (conversationMsg != null && conversationMsg.lastClientMsgNO.equals(clientMsgNo)) {
                BageMsg tempMsg = MsgDbManager.getInstance().queryMaxOrderSeqMsgWithChannel(msg.channelID, msg.channelType);
                if (tempMsg != null) {
                    BageUIConversationMsg uiMsg = ConversationDbManager.getInstance().insertOrUpdateWithMsg(tempMsg, 0);
                    BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsg, "deleteWithClientMsgNO");
                }
            }
        }
    }


    public boolean deleteWithMessageID(String messageID) {
        return MsgDbManager.getInstance().deleteWithMessageID(messageID);
    }

    public BageMsg getWithMessageID(String messageID) {
        return MsgDbManager.getInstance().queryWithMessageID(messageID, true);
    }

    public List<BageMsg> getWithMessageIDs(List<String> msgIds) {
        return MsgDbManager.getInstance().queryWithMsgIds(msgIds);
    }

    public int isDeletedMsg(JSONObject jsonObject) {
        int isDelete = 0;
        //消息可见数组
        if (jsonObject != null && jsonObject.has("visibles")) {
            boolean isIncludeLoginUser = false;
            JSONArray jsonArray = jsonObject.optJSONArray("visibles");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0, size = jsonArray.length(); i < size; i++) {
                    if (jsonArray.optString(i).equals(BageIMApplication.getInstance().getUid())) {
                        isIncludeLoginUser = true;
                        break;
                    }
                }
            }
            isDelete = isIncludeLoginUser ? 0 : 1;
        }
        return isDelete;
    }

    public List<BageMsg> getWithFlame() {
        return MsgDbManager.getInstance().queryWithFlame();
    }

    public long getMessageOrderSeq(long messageSeq, String channelID, byte channelType) {
        if (messageSeq == 0) {
            long tempOrderSeq = MsgDbManager.getInstance().queryMaxOrderSeqWithChannel(channelID, channelType);
            return tempOrderSeq + 1;
        }
        return messageSeq * bageOrderSeqFactor;
    }

    public long getMessageSeq(long messageOrderSeq) {
        if (messageOrderSeq % bageOrderSeqFactor == 0) {
            return messageOrderSeq / bageOrderSeqFactor;
        }
        return 0;
    }

    public long getReliableMessageSeq(long messageOrderSeq) {
        return messageOrderSeq / bageOrderSeqFactor;
    }

    /**
     * use getMaxReactionSeqWithChannel
     *
     * @param channelID   channelId
     * @param channelType channelType
     * @return channel reaction max seq version
     */
    @Deprecated
    public long getMaxSeqWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().getMaxReactionSeqWithChannel(channelID, channelType);
    }

    public long getMaxReactionSeqWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().getMaxReactionSeqWithChannel(channelID, channelType);
    }


    public void saveMessageReactions(List<BageSyncMsgReaction> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        List<BageMsgReaction> reactionList = new ArrayList<>();
        List<String> msgIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            BageMsgReaction reaction = new BageMsgReaction();
            reaction.messageID = list.get(i).message_id;
            reaction.channelID = list.get(i).channel_id;
            reaction.channelType = list.get(i).channel_type;
            reaction.uid = list.get(i).uid;
            reaction.name = list.get(i).name;
            reaction.seq = list.get(i).seq;
            reaction.emoji = list.get(i).emoji;
            reaction.isDeleted = list.get(i).is_deleted;
            reaction.createdAt = list.get(i).created_at;
            msgIds.add(list.get(i).message_id);
            reactionList.add(reaction);
        }
        saveMsgReactions(reactionList);
        List<BageMsg> msgList = MsgDbManager.getInstance().queryWithMsgIds(msgIds);
        getMsgReactionsAndRefreshMsg(msgIds, msgList);
    }

    public int getMaxMessageSeq() {
        return MsgDbManager.getInstance().queryMaxMessageSeqWithChannel();
    }

    public int getMaxMessageSeqWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().queryMaxMessageSeqWithChannel(channelID, channelType);
    }

    public int getMaxOrderSeqWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().queryMaxMessageOrderSeqWithChannel(channelID, channelType);
    }

    public int getMinMessageSeqWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().queryMinMessageSeqWithChannel(channelID, channelType);
    }


    public List<BageMsgReaction> getMsgReactions(String messageID) {
        List<String> ids = new ArrayList<>();
        ids.add(messageID);
        return MsgDbManager.getInstance().queryMsgReactionWithMsgIds(ids);
    }

    private void getMsgReactionsAndRefreshMsg(List<String> messageIds, List<BageMsg> updatedMsgList) {
        List<BageMsgReaction> reactionList = MsgDbManager.getInstance().queryMsgReactionWithMsgIds(messageIds);
        for (int i = 0, size = updatedMsgList.size(); i < size; i++) {
            for (int j = 0, len = reactionList.size(); j < len; j++) {
                if (updatedMsgList.get(i).messageID.equals(reactionList.get(j).messageID)) {
                    if (updatedMsgList.get(i).reactionList == null)
                        updatedMsgList.get(i).reactionList = new ArrayList<>();
                    updatedMsgList.get(i).reactionList.add(reactionList.get(j));
                }
            }
            setRefreshMsg(updatedMsgList.get(i), i == updatedMsgList.size() - 1);
        }
    }


    public synchronized long getClientSeq() {
        return MsgDbManager.getInstance().queryMaxMessageSeqWithChannel();
    }

    /**
     * 修改消息的扩展字段
     *
     * @param clientMsgNo 客户端ID
     * @param hashExtra   扩展字段
     */
    public boolean updateLocalExtraWithClientMsgNO(String clientMsgNo, HashMap<String, Object> hashExtra) {
        if (hashExtra != null) {
            JSONObject jsonObject = new JSONObject();
            for (String key : hashExtra.keySet()) {
                try {
                    jsonObject.put(key, hashExtra.get(key));
                } catch (JSONException e) {
                    BageLoggerUtils.getInstance().e(TAG, "updateLocalExtraWithClientMsgNO local_extra is not a JSON");
                }
            }
            return MsgDbManager.getInstance().updateFieldWithClientMsgNo(clientMsgNo, BageDBColumns.BageMessageColumns.extra, jsonObject.toString(), true);
        }

        return false;
    }

    /**
     * 查询按日期分组的消息数量
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<BageMessageGroupByDate>
     */
    public List<BageMessageGroupByDate> getMessageGroupByDateWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().queryMessageGroupByDateWithChannel(channelID, channelType);
    }

    public void clearAll() {
        MsgDbManager.getInstance().clearEmpty();
    }

    public void saveMsg(BageMsg msg) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(msg.clientMsgNO)) {
            BageMsg tempMsg = MsgDbManager.getInstance().queryWithClientMsgNo(msg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (msg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, msg.channelID, msg.channelType);
            msg.orderSeq = tempOrderSeq + 1;
        }
        msg.clientSeq = MsgDbManager.getInstance().insert(msg);
        if (refreshType == 0)
            pushNewMsg(msg);
        else setRefreshMsg(msg, true);
    }

    /**
     * 本地插入一条消息并更新会话记录表且未读消息数量加一
     *
     * @param bageMsg      消息对象
     * @param addRedDots 是否显示红点
     */
    public void saveAndUpdateConversationMsg(BageMsg bageMsg, boolean addRedDots) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(bageMsg.clientMsgNO)) {
            BageMsg tempMsg = MsgDbManager.getInstance().queryWithClientMsgNo(bageMsg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (bageMsg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, bageMsg.channelID, bageMsg.channelType);
            bageMsg.orderSeq = tempOrderSeq + 1;
        }
        bageMsg.clientSeq = MsgDbManager.getInstance().insert(bageMsg);
        if (refreshType == 0)
            pushNewMsg(bageMsg);
        else setRefreshMsg(bageMsg, true);
        BageUIConversationMsg msg = ConversationDbManager.getInstance().insertOrUpdateWithMsg(bageMsg, addRedDots ? 1 : 0);
        BageIM.getInstance().getConversationManager().setOnRefreshMsg(msg, "insertAndUpdateConversationMsg");
    }

    /**
     * 查询某个频道的固定类型消息
     *
     * @param channelID      频道ID
     * @param channelType    频道列席
     * @param oldestOrderSeq 最后一次消息大orderSeq
     * @param limit          每次获取数量
     * @param contentTypes   消息内容类型
     * @return List<BageMsg>
     */
    public List<BageMsg> searchMsgWithChannelAndContentTypes(String channelID, byte channelType, long oldestOrderSeq, int limit, int[] contentTypes) {
        return MsgDbManager.getInstance().searchWithChannelAndContentTypes(channelID, channelType, oldestOrderSeq, limit, contentTypes);
    }

    /**
     * 搜索某个频道到消息
     *
     * @param searchKey   关键字
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<BageMsg>
     */
    public List<BageMsg> searchWithChannel(String searchKey, String channelID, byte channelType) {
        return MsgDbManager.getInstance().searchWithChannel(searchKey, channelID, channelType);
    }

    public List<BageMessageSearchResult> search(String searchKey) {
        return MsgDbManager.getInstance().search(searchKey);
    }

    /**
     * 修改语音是否已读
     *
     * @param clientMsgNo 客户端ID
     * @param isReaded    1：已读
     */
    public boolean updateVoiceReadStatus(String clientMsgNo, int isReaded, boolean isRefreshUI) {
        return MsgDbManager.getInstance().updateFieldWithClientMsgNo(clientMsgNo, BageDBColumns.BageMessageColumns.voice_status, String.valueOf(isReaded), isRefreshUI);
    }

    /**
     * 清空某个会话信息
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public boolean clearWithChannel(String channelId, byte channelType) {
        boolean result = MsgDbManager.getInstance().deleteWithChannel(channelId, channelType);
        if (result) {
            if (clearMsgMap != null && !clearMsgMap.isEmpty()) {
                runOnMainThread(() -> {
                    for (Map.Entry<String, IClearMsgListener> entry : clearMsgMap.entrySet()) {
                        entry.getValue().clear(channelId, channelType, "");
                    }
                });

            }
        }
        return result;
    }

    public boolean clearWithChannelAndFromUID(String channelId, byte channelType, String fromUID) {
        boolean result = MsgDbManager.getInstance().deleteWithChannelAndFromUID(channelId, channelType, fromUID);
        if (result) {
            if (clearMsgMap != null && !clearMsgMap.isEmpty()) {
                runOnMainThread(() -> {
                    for (Map.Entry<String, IClearMsgListener> entry : clearMsgMap.entrySet()) {
                        entry.getValue().clear(channelId, channelType, fromUID);
                    }
                });

            }
        }
        return result;
    }


    public boolean updateContentAndRefresh(String clientMsgNo, String content, boolean isRefreshUI) {
        return MsgDbManager.getInstance().updateFieldWithClientMsgNo(clientMsgNo, BageDBColumns.BageMessageColumns.content, content, isRefreshUI);
    }


    public boolean updateContentAndRefresh(String clientMsgNo, BageMessageContent model, boolean isRefreshUI) {
        JSONObject jsonObject = model.encodeMsg();
        try {
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            jsonObject.put("type", model.type);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return updateContentAndRefresh(clientMsgNo, jsonObject.toString(), isRefreshUI);
    }


    public void updateViewedAt(int viewed, long viewedAt, String clientMsgNo) {
        MsgDbManager.getInstance().updateViewedAt(viewed, viewedAt, clientMsgNo);
    }

    /**
     * 获取某个类型的聊天数据
     *
     * @param type            消息类型
     * @param oldestClientSeq 最后一次消息客户端ID
     * @param limit           数量
     * @return list
     */
    public List<BageMsg> getWithContentType(int type, long oldestClientSeq, int limit) {
        return MsgDbManager.getInstance().queryWithContentType(type, oldestClientSeq, limit);
    }

    public void saveAndUpdateConversationMsg(BageMsg msg) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(msg.clientMsgNO)) {
            BageMsg tempMsg = MsgDbManager.getInstance().queryWithClientMsgNo(msg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (msg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, msg.channelID, msg.channelType);
            msg.orderSeq = tempOrderSeq + 1;
        }
        MsgDbManager.getInstance().insert(msg);
        if (refreshType == 0)
            pushNewMsg(msg);
        else setRefreshMsg(msg, true);
        ConversationDbManager.getInstance().insertOrUpdateWithMsg(msg, 0);
    }


    public long getMsgExtraMaxVersionWithChannel(String channelID, byte channelType) {
        return MsgDbManager.getInstance().queryMsgExtraMaxVersionWithChannel(channelID, channelType);
    }

    public BageMsg getWithClientMsgNO(String clientMsgNo) {
        return MsgDbManager.getInstance().queryWithClientMsgNo(clientMsgNo);
    }


    public void saveRemoteExtraMsg(BageChannel channel, List<BageSyncExtraMsg> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        List<BageMsgExtra> extraList = new ArrayList<>();
        List<String> messageIds = new ArrayList<>();
        List<String> deleteMsgIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            if (TextUtils.isEmpty(list.get(i).message_id)) {
                continue;
            }
            BageMsgExtra extra = BageSyncExtraMsg2BageMsgExtra(channel.channelID, channel.channelType, list.get(i));
            extraList.add(extra);
            messageIds.add(list.get(i).message_id);
            if (extra.isMutualDeleted == 1) {
                deleteMsgIds.add(list.get(i).message_id);
            }
        }
        List<BageMsg> updatedMsgList = MsgDbManager.getInstance().insertOrReplaceExtra(extraList);
        if (!deleteMsgIds.isEmpty()) {
            boolean isSuccess = MsgDbManager.getInstance().deleteWithMessageIDs(deleteMsgIds);
            if (!isSuccess) {
                BageLoggerUtils.getInstance().e(TAG, "saveRemoteExtraMsg delete message error");
            }
            String deletedMsgId = "";
            BageConversationMsg conversationMsg = ConversationDbManager.getInstance().queryWithChannel(channel.channelID, channel.channelType);
            if (conversationMsg != null && !TextUtils.isEmpty(conversationMsg.lastClientMsgNO)) {
                BageMsg msg = getWithClientMsgNO(conversationMsg.lastClientMsgNO);
                if (msg != null && !TextUtils.isEmpty(msg.messageID) && msg.messageSeq != 0) {
                    for (String msgId : deleteMsgIds) {
                        if (msg.messageID.equals(msgId)) {
                            deletedMsgId = msgId;
                            break;
                        }
                    }
                }
            }
            if (!TextUtils.isEmpty(deletedMsgId) && conversationMsg != null) {
                int rowNo = BageIM.getInstance().getMsgManager().getRowNoWithMessageID(channel.channelID, channel.channelType, deletedMsgId);
                if (rowNo < conversationMsg.unreadCount) {
                    conversationMsg.unreadCount--;
                }
                BageIM.getInstance().getConversationManager().updateWithMsg(conversationMsg);
                BageUIConversationMsg bageuiConversationMsg = BageIM.getInstance().getConversationManager().getUIConversationMsg(channel.channelID, channel.channelType);
                BageIM.getInstance().getConversationManager().setOnRefreshMsg(bageuiConversationMsg, TAG + " saveRemoteExtraMsg");
            }
        }
        getMsgReactionsAndRefreshMsg(messageIds, updatedMsgList);
    }

    public void addOnSyncOfflineMsgListener(ISyncOfflineMsgListener iOfflineMsgListener) {
        this.iOfflineMsgListener = iOfflineMsgListener;
    }

    //添加删除消息监听
    public void addOnDeleteMsgListener(String key, IDeleteMsgListener iDeleteMsgListener) {
        if (iDeleteMsgListener == null || TextUtils.isEmpty(key)) return;
        if (deleteMsgListenerMap == null) deleteMsgListenerMap = new ConcurrentHashMap<>();
        deleteMsgListenerMap.put(key, iDeleteMsgListener);
    }

    public void removeDeleteMsgListener(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (deleteMsgListenerMap != null) deleteMsgListenerMap.remove(key);
    }

    //设置删除消息
    public void setDeleteMsg(BageMsg msg) {
        if (deleteMsgListenerMap != null && !deleteMsgListenerMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IDeleteMsgListener> entry : deleteMsgListenerMap.entrySet()) {
                    entry.getValue().onDeleteMsg(msg);
                }
            });
        }
    }


    void saveMsgReactions(List<BageMsgReaction> list) {
        MsgDbManager.getInstance().insertMsgReactions(list);
    }


    public void setSyncOfflineMsg(ISyncOfflineMsgBack iSyncOfflineMsgBack) {
        syncOfflineMsg(iSyncOfflineMsgBack);
    }

    private void syncOfflineMsg(ISyncOfflineMsgBack iSyncOfflineMsgBack) {
        if (iOfflineMsgListener != null) {
            runOnMainThread(() -> {
                long max_message_seq = getMaxMessageSeq();
                iOfflineMsgListener.getOfflineMsgs(max_message_seq, (isEnd, list) -> {
                    //保存同步消息
                    saveSyncMsg(list);
                    if (isEnd) {
                        iSyncOfflineMsgBack.onBack(isEnd, null);
                    } else {
                        syncOfflineMsg(iSyncOfflineMsgBack);
                    }
                });
            });
        } else iSyncOfflineMsgBack.onBack(true, null);
    }


    public void setSendMsgCallback(BageMsg msg) {
        if (sendMsgCallBackListenerHashMap != null && !sendMsgCallBackListenerHashMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ISendMsgCallBackListener> entry : sendMsgCallBackListenerHashMap.entrySet()) {
                    entry.getValue().onInsertMsg(msg);
                }
            });
        }
    }

    public void addOnSendMsgCallback(String key, ISendMsgCallBackListener iSendMsgCallBackListener) {
        if (TextUtils.isEmpty(key)) return;
        if (sendMsgCallBackListenerHashMap == null) {
            sendMsgCallBackListenerHashMap = new ConcurrentHashMap<>();
        }
        sendMsgCallBackListenerHashMap.put(key, iSendMsgCallBackListener);
    }

    public void removeSendMsgCallBack(String key) {
        if (sendMsgCallBackListenerHashMap != null) {
            sendMsgCallBackListenerHashMap.remove(key);
        }
    }


    //监听同步频道消息
    public void addOnSyncChannelMsgListener(ISyncChannelMsgListener listener) {
        this.iSyncChannelMsgListener = listener;
    }

    public void setSyncChannelMsgListener(String channelID, byte channelType, long startMessageSeq, long endMessageSeq, int limit, int pullMode, ISyncChannelMsgBack iSyncChannelMsgBack) {
        if (this.iSyncChannelMsgListener != null) {
            runOnMainThread(() -> iSyncChannelMsgListener.syncChannelMsgs(channelID, channelType, startMessageSeq, endMessageSeq, limit, pullMode, syncChannelMsg -> {
                // DB写入和后续查询移至后台线程，避免主线程SQLCipher连接池竞争ANR
                new Thread(() -> {
                    try {
                        if (syncChannelMsg != null && BageCommonUtils.isNotEmpty(syncChannelMsg.messages)) {
                            saveSyncChannelMSGs(syncChannelMsg.messages);
                        }
                        iSyncChannelMsgBack.onBack(syncChannelMsg);
                    } catch (Throwable t) {
                        BageLoggerUtils.getInstance().e("MsgManager", "saveSyncChannelMSGs aborted: " + t.getMessage());
                        iSyncChannelMsgBack.onBack(null);
                    }
                }).start();
            }));
        }
    }

    public void saveSyncChannelMSGs(List<BageSyncRecent> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        List<BageMsg> msgList = new ArrayList<>();
        List<BageMsgExtra> msgExtraList = new ArrayList<>();
        List<BageMsgReaction> reactionList = new ArrayList<>();
        List<String> msgIds = new ArrayList<>();
        for (int j = 0, len = list.size(); j < len; j++) {
            BageMsg bageMsg = BageSyncRecent2BageMsg(list.get(j));
            if (bageMsg.type == BageMsgContentType.Bage_INSIDE_MSG) {
                continue;
            }
            msgList.add(bageMsg);
            if (!TextUtils.isEmpty(bageMsg.messageID)) {
                msgIds.add(bageMsg.messageID);
            }
            if (list.get(j).message_extra != null) {
                BageMsgExtra extra = BageSyncExtraMsg2BageMsgExtra(bageMsg.channelID, bageMsg.channelType, list.get(j).message_extra);
                msgExtraList.add(extra);
            }
            if (BageCommonUtils.isNotEmpty(bageMsg.reactionList)) {
                reactionList.addAll(bageMsg.reactionList);
            }
        }
        if (BageCommonUtils.isNotEmpty(msgExtraList)) {
            MsgDbManager.getInstance().insertOrReplaceExtra(msgExtraList);
        }
        if (BageCommonUtils.isNotEmpty(msgList)) {
            MsgDbManager.getInstance().insertMsgs(msgList);
        }
        if (BageCommonUtils.isNotEmpty(reactionList)) {
            MsgDbManager.getInstance().insertMsgReactions(reactionList);
        }
        // Bugly#30231 OOM 优化：复用 msgList 而不是再次全量 queryWithMsgIds，
        // 避免 200-500 条消息同时在堆里持有两份（50-200MB 重复占用）
        // reactionList 清空让 getMsgReactionsAndRefreshMsg 从 DB 重建，行为与原 saveList 路径等价
        if (BageCommonUtils.isNotEmpty(msgList)) {
            for (int i = 0, size = msgList.size(); i < size; i++) {
                msgList.get(i).reactionList = null;
            }
            getMsgReactionsAndRefreshMsg(msgIds, msgList);
        }
    }

    public void addOnSendMsgAckListener(String key, ISendACK iSendACKListener) {
        if (iSendACKListener == null || TextUtils.isEmpty(key)) return;
        if (sendAckListenerMap == null) sendAckListenerMap = new ConcurrentHashMap<>();
        sendAckListenerMap.put(key, iSendACKListener);
    }

    public void setSendMsgAck(BageMsg msg) {
        if (sendAckListenerMap != null && !sendAckListenerMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ISendACK> entry : sendAckListenerMap.entrySet()) {
                    entry.getValue().msgACK(msg);
                }
            });

        }
    }

    public void removeSendMsgAckListener(String key) {
        if (!TextUtils.isEmpty(key) && sendAckListenerMap != null) {
            sendAckListenerMap.remove(key);
        }
    }

    public void addOnUploadAttachListener(IUploadAttachmentListener iUploadAttachmentListener) {
        this.iUploadAttachmentListener = iUploadAttachmentListener;
    }

    public void setUploadAttachment(BageMsg msg, IUploadAttacResultListener resultListener) {
        if (iUploadAttachmentListener != null) {
            runOnMainThread(() -> {
                iUploadAttachmentListener.onUploadAttachmentListener(msg, resultListener);
            });
        }
    }

    public void addMessageStoreBeforeIntercept(IMessageStoreBeforeIntercept iMessageStoreBeforeInterceptListener) {
        messageStoreBeforeIntercept = iMessageStoreBeforeInterceptListener;
    }

    public boolean setMessageStoreBeforeIntercept(BageMsg msg) {
        return messageStoreBeforeIntercept == null || messageStoreBeforeIntercept.isSaveMsg(msg);
    }

    //添加消息修改
    public void addOnRefreshMsgListener(String key, IRefreshMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (refreshMsgListenerMap == null) refreshMsgListenerMap = new ConcurrentHashMap<>();
        refreshMsgListenerMap.put(key, listener);
    }


    public void removeRefreshMsgListener(String key) {
        if (!TextUtils.isEmpty(key) && refreshMsgListenerMap != null) {
            refreshMsgListenerMap.remove(key);
        }
    }

    public void setRefreshMsg(BageMsg msg, boolean left) {
        if (refreshMsgListenerMap != null && !refreshMsgListenerMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshMsg> entry : refreshMsgListenerMap.entrySet()) {
                    entry.getValue().onRefresh(msg, left);
                }
            });

        }
    }

    public void addOnNewMsgListener(String key, INewMsgListener iNewMsgListener) {
        if (TextUtils.isEmpty(key) || iNewMsgListener == null) return;
        if (newMsgListenerMap == null)
            newMsgListenerMap = new ConcurrentHashMap<>();
        newMsgListenerMap.put(key, iNewMsgListener);
    }

    public void removeNewMsgListener(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (newMsgListenerMap != null) newMsgListenerMap.remove(key);
    }

    public void addOnClearMsgListener(String key, IClearMsgListener iClearMsgListener) {
        if (TextUtils.isEmpty(key) || iClearMsgListener == null) return;
        if (clearMsgMap == null) clearMsgMap = new ConcurrentHashMap<>();
        clearMsgMap.put(key, iClearMsgListener);
    }

    public void removeClearMsg(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (clearMsgMap != null) clearMsgMap.remove(key);
    }


    BageMsgExtra BageSyncExtraMsg2BageMsgExtra(String channelID, byte channelType, BageSyncExtraMsg extraMsg) {
        BageMsgExtra extra = new BageMsgExtra();
        extra.channelID = channelID;
        extra.channelType = channelType;
        extra.unreadCount = extraMsg.unread_count;
        extra.readedCount = extraMsg.readed_count;
        extra.readed = extraMsg.readed;
        extra.messageID = extraMsg.message_id;
        extra.isMutualDeleted = extraMsg.is_mutual_deleted;
        extra.isPinned = extraMsg.is_pinned;
        extra.extraVersion = extraMsg.extra_version;
        extra.revoke = extraMsg.revoke;
        extra.revoker = extraMsg.revoker;
        extra.needUpload = 0;
        if (extraMsg.content_edit != null) {
            JSONObject jsonObject = new JSONObject(extraMsg.content_edit);
            extra.contentEdit = jsonObject.toString();
        }

        extra.editedAt = extraMsg.edited_at;
        return extra;
    }

    BageMsg BageSyncRecent2BageMsg(BageSyncRecent bageSyncRecent) {
        BageMsg msg = new BageMsg();
        msg.channelID = bageSyncRecent.channel_id;
        msg.channelType = bageSyncRecent.channel_type;
        msg.messageID = bageSyncRecent.message_id;
        msg.messageSeq = bageSyncRecent.message_seq;
        msg.clientMsgNO = bageSyncRecent.client_msg_no;
        msg.fromUID = bageSyncRecent.from_uid;
        msg.timestamp = bageSyncRecent.timestamp;
        msg.orderSeq = msg.messageSeq * bageOrderSeqFactor;
        msg.voiceStatus = bageSyncRecent.voice_status;
        msg.isDeleted = bageSyncRecent.is_deleted;
        msg.status = BageSendMsgResult.send_success;
        msg.remoteExtra = new BageMsgExtra();
        msg.remoteExtra.revoke = bageSyncRecent.revoke;
        msg.remoteExtra.revoker = bageSyncRecent.revoker;
        msg.remoteExtra.unreadCount = bageSyncRecent.unread_count;
        msg.remoteExtra.readedCount = bageSyncRecent.readed_count;
        msg.remoteExtra.readed = bageSyncRecent.readed;
        msg.expireTime = bageSyncRecent.expire;
        msg.expireTimestamp = msg.expireTime + msg.timestamp;
        // msg.reactionList = bageSyncRecent.reactions;
        // msg.receipt = bageSyncRecent.receipt;
        msg.remoteExtra.extraVersion = bageSyncRecent.extra_version;
        //处理消息设置
        byte[] setting = BageTypeUtils.getInstance().intToByte(bageSyncRecent.setting);
        msg.setting = BageTypeUtils.getInstance().getMsgSetting(setting[0]);
        //如果是单聊先将channelId改成发送者ID
        if (!TextUtils.isEmpty(msg.channelID)
                && !TextUtils.isEmpty(msg.fromUID)
                && msg.channelType == BageChannelType.PERSONAL
                && msg.channelID.equals(BageIMApplication.getInstance().getUid())) {
            msg.channelID = msg.fromUID;
        }

        if (bageSyncRecent.payload != null) {
            JSONObject jsonObject = new JSONObject(bageSyncRecent.payload);
            msg.content = jsonObject.toString();
        }
        // 处理消息回应
        if (BageCommonUtils.isNotEmpty(bageSyncRecent.reactions)) {
            msg.reactionList = getMsgReaction(bageSyncRecent);
        }
        msg = MessageHandler.getInstance().parsingMsg(msg);
        return msg;
    }

    private List<BageMsgReaction> getMsgReaction(BageSyncRecent bageSyncRecent) {
        List<BageMsgReaction> list = new ArrayList<>();
        for (int i = 0, size = bageSyncRecent.reactions.size(); i < size; i++) {
            BageMsgReaction reaction = new BageMsgReaction();
            reaction.channelID = bageSyncRecent.reactions.get(i).channel_id;
            reaction.channelType = bageSyncRecent.reactions.get(i).channel_type;
            reaction.uid = bageSyncRecent.reactions.get(i).uid;
            reaction.name = bageSyncRecent.reactions.get(i).name;
            reaction.emoji = bageSyncRecent.reactions.get(i).emoji;
            reaction.seq = bageSyncRecent.reactions.get(i).seq;
            reaction.isDeleted = bageSyncRecent.reactions.get(i).is_deleted;
            reaction.messageID = bageSyncRecent.reactions.get(i).message_id;
            reaction.createdAt = bageSyncRecent.reactions.get(i).created_at;
            list.add(reaction);
        }
        return list;
    }

    public void saveSyncMsg(List<BageSyncMsg> bageSyncMsgs) {
        if (BageCommonUtils.isEmpty(bageSyncMsgs)) return;
        for (int i = 0, size = bageSyncMsgs.size(); i < size; i++) {
            bageSyncMsgs.get(i).bageMsg = MessageHandler.getInstance().parsingMsg(bageSyncMsgs.get(i).bageMsg);
            if (bageSyncMsgs.get(i).bageMsg.timestamp != 0)
                bageSyncMsgs.get(i).bageMsg.orderSeq = bageSyncMsgs.get(i).bageMsg.timestamp;
            else
                bageSyncMsgs.get(i).bageMsg.orderSeq = getMessageOrderSeq(bageSyncMsgs.get(i).bageMsg.messageSeq, bageSyncMsgs.get(i).bageMsg.channelID, bageSyncMsgs.get(i).bageMsg.channelType);
        }
        MessageHandler.getInstance().saveSyncMsg(bageSyncMsgs);
    }


    public void updateMsgEdit(String msgID, String channelID, byte channelType, String content) {
        BageMsgExtra bageMsgExtra = MsgDbManager.getInstance().queryMsgExtraWithMsgID(msgID);
        if (bageMsgExtra == null) {
            bageMsgExtra = new BageMsgExtra();
        }
        bageMsgExtra.messageID = msgID;
        bageMsgExtra.channelID = channelID;
        bageMsgExtra.channelType = channelType;
        bageMsgExtra.editedAt = DateUtils.getInstance().getCurrentSeconds();
        bageMsgExtra.contentEdit = content;
        bageMsgExtra.needUpload = 1;
        List<BageMsgExtra> list = new ArrayList<>();
        list.add(bageMsgExtra);
        List<BageMsg> bageMsgList = MsgDbManager.getInstance().insertOrReplaceExtra(list);
        List<String> messageIds = new ArrayList<>();
        messageIds.add(msgID);
        if (BageCommonUtils.isNotEmpty(bageMsgList)) {
            getMsgReactionsAndRefreshMsg(messageIds, bageMsgList);
            setUploadMsgExtra(bageMsgExtra);
        }
    }

    private synchronized void startCheckTimer() {
        if (checkMsgNeedUploadTimer == null) {
            checkMsgNeedUploadTimer = new Timer();
        }
        checkMsgNeedUploadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<BageMsgExtra> list = MsgDbManager.getInstance().queryMsgExtraWithNeedUpload(1);
                if (BageCommonUtils.isNotEmpty(list)) {
                    for (BageMsgExtra extra : list) {
                        if (iUploadMsgExtraListener != null) {
                            iUploadMsgExtraListener.onUpload(extra);
                        }
                    }
                } else {
                    checkMsgNeedUploadTimer.cancel();
                    checkMsgNeedUploadTimer.purge();
                    checkMsgNeedUploadTimer = null;
                }
            }
        }, 1000 * 5, 1000 * 5);
    }

    private void setUploadMsgExtra(BageMsgExtra extra) {
        if (iUploadMsgExtraListener != null) {
            iUploadMsgExtraListener.onUpload(extra);
        }
        startCheckTimer();
    }

    public void addOnUploadMsgExtraListener(IUploadMsgExtraListener iUploadMsgExtraListener) {
        this.iUploadMsgExtraListener = iUploadMsgExtraListener;
    }

    public void pushNewMsg(List<BageMsg> bageMsgList) {
        if (newMsgListenerMap != null && !newMsgListenerMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, INewMsgListener> entry : newMsgListenerMap.entrySet()) {
                    entry.getValue().newMsg(bageMsgList);
                }
            });
        }
    }

    /**
     * push新消息
     *
     * @param msg 消息
     */
    public void pushNewMsg(BageMsg msg) {
        if (msg == null) return;
        List<BageMsg> msgs = new ArrayList<>();
        msgs.add(msg);
        pushNewMsg(msgs);
    }

    /**
     * Deprecated 后续版本将会移除
     *
     * @param messageContent 消息体
     * @param channelID      频道ID
     * @param channelType    频道类型
     */
    @Deprecated
    public void sendMessage(BageMessageContent messageContent, String channelID, byte channelType) {
        send(messageContent, new BageChannel(channelID, channelType));
    }

    /**
     * Deprecated 后续版本将会移除
     *
     * @param messageContent 消息体
     * @param setting        消息设置
     * @param channelID      频道ID
     * @param channelType    频道类型
     */
    @Deprecated
    public void sendMessage(BageMessageContent messageContent, BageMsgSetting setting, String channelID, byte channelType) {
        BageSendOptions options = new BageSendOptions();
        options.setting = setting;
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
        if (channel == null) {
            channel = new BageChannel(channelID, channelType);
        }
        sendWithOptions(messageContent, channel, options);
    }

    /**
     * 发送消息
     *
     * @param msg 消息对象
     */
    public void sendMessage(@NonNull BageMsg msg) {
        BageConnection.getInstance().sendMessage(msg);
    }

    /**
     * 发送消息
     *
     * @param contentModel 消息体
     * @param channel      频道
     */
    public void send(@NonNull BageMessageContent contentModel, @NonNull BageChannel channel) {
        sendWithOptions(contentModel, channel, new BageSendOptions());
    }

    /**
     * 发送消息
     *
     * @param contentModel 消息体
     * @param channel      频道
     * @param options      高级设置
     */
    public void sendWithOptions(@NonNull BageMessageContent contentModel, @NonNull BageChannel channel, @NonNull BageSendOptions options) {
        final BageMsg bageMsg = new BageMsg();
        bageMsg.type = contentModel.type;
        bageMsg.channelID = channel.channelID;
        bageMsg.channelType = channel.channelType;
        bageMsg.baseContentMsgModel = contentModel;
        bageMsg.flame = options.flame;
        bageMsg.flameSecond = options.flameSecond;
        bageMsg.expireTime = options.expire;
        if (!TextUtils.isEmpty(options.topicID)) {
            bageMsg.topicID = options.topicID;
        }
        if (!TextUtils.isEmpty(options.robotID)) {
            bageMsg.robotID = options.robotID;
        }
        bageMsg.setting = options.setting;
        bageMsg.header = options.header;
        sendMessage(bageMsg);
    }

    public String createClientMsgNO() {
        String deviceId = BageIM.getInstance().getDeviceID();
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = "unknown";
        }
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return uuid + "_" + deviceId + "_1";
    }
}
