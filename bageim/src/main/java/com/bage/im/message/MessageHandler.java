package com.bage.im.message;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.ConversationDbManager;
import com.bage.im.db.MsgDbManager;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageSyncMsg;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.interfaces.IReceivedMsgListener;
import com.bage.im.manager.CMDManager;
import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.message.type.BageMsgType;
import com.bage.im.protocol.BageBaseMsg;
import com.bage.im.protocol.BageConnectAckMsg;
import com.bage.im.protocol.BageDisconnectMsg;
import com.bage.im.protocol.BagePongMsg;
import com.bage.im.protocol.BageReceivedAckMsg;
import com.bage.im.protocol.BageSendAckMsg;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;
import com.bage.im.utils.BageTypeUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.xsocket.connection.INonBlockingConnection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.BufferOverflowException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * 5/21/21 11:25 AM
 * msg handler
 */
public class MessageHandler {
    private final String TAG = "MessageHandler";

    private MessageHandler() {
    }

    private static class MessageHandlerBinder {
        static final MessageHandler handler = new MessageHandler();
    }

    public static MessageHandler getInstance() {
        return MessageHandlerBinder.handler;
    }

    private final List<BageReceivedAckMsg> receivedAckMsgList = Collections.synchronizedList(new ArrayList<>());

    int sendMessage(INonBlockingConnection connection, BageBaseMsg msg) {
        if (msg == null) {
            return 1;
        }
        byte[] bytes = BageProto.getInstance().encodeMsg(msg);
        if (bytes == null || bytes.length == 0) {
            BageLoggerUtils.getInstance().e(TAG, "发送了非法包:" + msg.packetType);
            return 1;
        }

        if (connection != null && connection.isOpen()) {
            try {
                connection.write(bytes, 0, bytes.length);
                connection.flush();
                return 1;
            } catch (BufferOverflowException e) {
                BageLoggerUtils.getInstance().e(TAG, "发消息异常 BufferOverflowException"
                        + e.getMessage());
                return 0;
            } catch (ClosedChannelException e) {
                BageLoggerUtils.getInstance().e(TAG, "发消息异常 ClosedChannelException"
                        + e.getMessage());
                return 0;
            } catch (SocketTimeoutException e) {
                BageLoggerUtils.getInstance().e(TAG, "发消息异常 SocketTimeoutException"
                        + e.getMessage());
                return 0;
            } catch (IOException e) {
                BageLoggerUtils.getInstance().e(TAG, "发消息异常 IOException" + e.getMessage());
                return 0;
            }
        } else {
            BageLoggerUtils.getInstance().e("发消息异常:"
                    + connection);
            return 0;
        }
    }


    private volatile List<BageSyncMsg> receivedMsgList;
    private final Object receivedMsgListLock = new Object();
    private final ReentrantLock cacheLock = new ReentrantLock(true); // 使用公平锁
    private static final long LOCK_TIMEOUT = 2000; // 2秒超时
    private byte[] cacheData = null;
    private int available_len;

    public void clearCacheData() {
        boolean locked = false;
        try {
            // 尝试获取锁，最多等待3秒
            locked = cacheLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            if (locked) {
                cacheData = null;
                available_len = 0;
            } else {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，clearCacheData失败");
            }
        } catch (InterruptedException e) {
            BageLoggerUtils.getInstance().e(TAG, "clearCacheData等待锁被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                cacheLock.unlock();
            }
        }
    }


    synchronized void handlerOnlineBytes(INonBlockingConnection iNonBlockingConnection) {
        boolean locked = false;
        try {
            locked = cacheLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，handlerOnlineBytes失败");
                return;
            }
            
            try {
                // 获取可用数据长度
                available_len = iNonBlockingConnection.available();

                // 安全检查
                if (available_len <= 0) {
                    return;
                }

                // 限制单次最大读取大小为150kb
                int bufLen = 1024 / 2;

                // 分批读取数据
                while (available_len > 0) {
                    // 计算本次应该读取的长度
                    int readLen = Math.min(bufLen, available_len);
                    if (readLen <= 0) break;
                    // 读取数据前确保连接仍然有效
                    if (!iNonBlockingConnection.isOpen()) {
                        BageLoggerUtils.getInstance().e(TAG, "读取数据时连接关闭");
                        break;
                    }
                    // 读取数据
                    byte[] buffBytes = iNonBlockingConnection.readBytesByLength(readLen);
                    if (buffBytes != null && buffBytes.length > 0) {
                        BageConnection.getInstance().receivedData(buffBytes);
                        available_len -= buffBytes.length;
                    } else {
                        BageLoggerUtils.getInstance().e(TAG, "读取数据失败或收到空数据");
                        break;
                    }
                    // 给一个很小的延迟，避免过快读取
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (IOException e) {
                BageLoggerUtils.getInstance().e(TAG, "处理接收到的数据异常:" + e.getMessage());
                clearCacheData();
            } catch (Exception e) {
                BageLoggerUtils.getInstance().e(TAG, "onData 中发生意外错误: " + e.getMessage());
                clearCacheData();
            }
        } catch (InterruptedException e) {
            BageLoggerUtils.getInstance().e(TAG, "handlerOnlineBytes等待锁被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                cacheLock.unlock();
            }
        }
    }

    synchronized void cutBytes(byte[] available_bytes,
                               IReceivedMsgListener mIReceivedMsgListener) {
        boolean locked = false;
        try {
            locked = cacheLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，cutBytes失败");
                return;
            }

            if (cacheData == null || cacheData.length == 0) cacheData = available_bytes;
            else {
                //如果上次还存在未解析完的消息将新数据追加到缓存数据中
                byte[] temp = new byte[available_bytes.length + cacheData.length];
                try {
                    System.arraycopy(cacheData, 0, temp, 0, cacheData.length);
                    System.arraycopy(available_bytes, 0, temp, cacheData.length, available_bytes.length);
                    cacheData = temp;
                } catch (Exception e) {
                    BageLoggerUtils.getInstance().e(TAG, "处理粘包消息异常" + e.getMessage());
                    clearCacheData();
                    return;
                }
            }
            byte[] lastMsgBytes = cacheData;
            int readLength = 0;

            while (lastMsgBytes.length > 0 && readLength != lastMsgBytes.length) {
                readLength = lastMsgBytes.length;
                int packetType = BageTypeUtils.getInstance().getHeight4(lastMsgBytes[0]);
                // 是否不持久化：0。 是否显示红点：1。是否只同步一次：0
                //是否持久化[是否保存在数据库]
                int no_persist = BageTypeUtils.getInstance().getBit(lastMsgBytes[0], 0);
                //是否显示红点
                int red_dot = BageTypeUtils.getInstance().getBit(lastMsgBytes[0], 1);
                //是否只同步一次
                int sync_once = BageTypeUtils.getInstance().getBit(lastMsgBytes[0], 2);
                if (BageIM.getInstance().isDebug()) {
                    String packetTypeStr = "[其他]";
                    switch (packetType) {
                        case BageMsgType.CONNACK:
                            packetTypeStr = "[连接状态包]";
                            break;
                        case BageMsgType.SEND:
                            packetTypeStr = "[发送包]";
                            break;
                        case BageMsgType.RECEIVED:
                            packetTypeStr = "[收到消息包]";
                            break;
                        case BageMsgType.DISCONNECT:
                            packetTypeStr = "[断开连接包]";
                            break;
                        case BageMsgType.SENDACK:
                            packetTypeStr = "[发送回执包]";
                            break;
                        case BageMsgType.PONG:
                            packetTypeStr = "[心跳包]";
                            break;
                    }
                    String info = "是否不持续化：" + no_persist + "，是否显示红点：" + red_dot + "，是否只同步一次：" + sync_once;
                    BageLoggerUtils.getInstance().e(TAG, "收到包类型" + packetType + " " + packetTypeStr + "|" + info);
                }
                if (packetType == BageMsgType.REVACK || packetType == BageMsgType.SEND || packetType == BageMsgType.Reserved) {
                    BageConnection.getInstance().forcedReconnection();
                    return;
                }
                if (packetType == BageMsgType.PONG) {
                    //心跳ack
                    mIReceivedMsgListener.pongMsg(new BagePongMsg());
                    BageLoggerUtils.getInstance().e(TAG, "pong...");
                    byte[] bytes = Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length);
                    cacheData = lastMsgBytes = bytes;
                } else {
                    if (packetType < 10) {
                        // 2019-12-21 计算剩余长度
                        if (lastMsgBytes.length < 5) {
                            cacheData = lastMsgBytes;
                            break;
                        }
                        //其他消息类型
                        int remainingLength = BageTypeUtils.getInstance().getRemainingLength(Arrays.copyOfRange(lastMsgBytes, 1, lastMsgBytes.length));
                        if (remainingLength == -1) {
                            //剩余长度被分包
                            cacheData = lastMsgBytes;
                            break;
                        }
                        if (remainingLength > 1 << 21) {
                            cacheData = null;
                            break;
                        }
                        byte[] bytes = BageTypeUtils.getInstance().getRemainingLengthByte(remainingLength);
                        if (remainingLength + 1 + bytes.length > lastMsgBytes.length) {
                            //半包情况
                            cacheData = lastMsgBytes;
                        } else {
                            byte[] msg = Arrays.copyOfRange(lastMsgBytes, 0, remainingLength + 1 + bytes.length);
                            acceptMsg(msg, no_persist, sync_once, red_dot, mIReceivedMsgListener);
                            byte[] temps = Arrays.copyOfRange(lastMsgBytes, msg.length, lastMsgBytes.length);
                            cacheData = lastMsgBytes = temps;
                        }

                    } else {
                        cacheData = null;
                        mIReceivedMsgListener.reconnect();
                        break;
                    }
                }
            }
            saveReceiveMsg();
        } catch (InterruptedException e) {
            BageLoggerUtils.getInstance().e(TAG, "cutBytes等待锁被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                cacheLock.unlock();
            }
        }
    }

    private void acceptMsg(byte[] bytes, int no_persist, int sync_once, int red_dot,
                           IReceivedMsgListener mIReceivedMsgListener) {

        if (bytes != null && bytes.length > 0) {
            BageBaseMsg g_msg;
            g_msg = BageProto.getInstance().decodeMessage(bytes);
            if (g_msg != null) {
                //连接ack
                if (g_msg.packetType == BageMsgType.CONNACK) {
                    BageConnectAckMsg loginStatusMsg = (BageConnectAckMsg) g_msg;
                    mIReceivedMsgListener.loginStatusMsg(loginStatusMsg);
                } else if (g_msg.packetType == BageMsgType.SENDACK) {
                    //发送ack
                    BageSendAckMsg sendAckMsg = (BageSendAckMsg) g_msg;
                    BageMsg bageMsg = null;
                    if (no_persist == 0) {
                        bageMsg = MsgDbManager.getInstance().updateMsgSendStatus(sendAckMsg.clientSeq, sendAckMsg.messageSeq, sendAckMsg.messageID, sendAckMsg.reasonCode);
                    }
                    if (bageMsg == null) {
                        bageMsg = new BageMsg();
                        bageMsg.clientSeq = sendAckMsg.clientSeq;
                        bageMsg.messageID = sendAckMsg.messageID;
                        bageMsg.status = sendAckMsg.reasonCode;
                        bageMsg.messageSeq = (int) sendAckMsg.messageSeq;
                    }
                    BageIM.getInstance().getMsgManager().setSendMsgAck(bageMsg);

                    mIReceivedMsgListener
                            .sendAckMsg(sendAckMsg);
                } else if (g_msg.packetType == BageMsgType.RECEIVED) {
                    //收到消息
                    BageMsg message = BageProto.getInstance().baseMsg2BageMsg(g_msg);
                    message.header.noPersist = no_persist == 1;
                    message.header.redDot = red_dot == 1;
                    message.header.syncOnce = sync_once == 1;
                    handleReceiveMsg(message);
                    // mIReceivedMsgListener.receiveMsg(message);
                } else if (g_msg.packetType == BageMsgType.DISCONNECT) {
                    //被踢消息
                    BageDisconnectMsg disconnectMsg = (BageDisconnectMsg) g_msg;
                    mIReceivedMsgListener.kickMsg(disconnectMsg);
                } else if (g_msg.packetType == BageMsgType.PONG) {
                    mIReceivedMsgListener.pongMsg((BagePongMsg) g_msg);
                }
            } else {
                mIReceivedMsgListener.reconnect();
            }
        }
    }

    private void handleReceiveMsg(BageMsg message) {
        message = parsingMsg(message);
        if (message.type != BageMsgContentType.Bage_INSIDE_MSG) {
            addReceivedMsg(message);
        } else {
            BageReceivedAckMsg receivedAckMsg = getReceivedAckMsg(message);
            receivedAckMsgList.add(receivedAckMsg);
        }
    }

    private BageReceivedAckMsg getReceivedAckMsg(BageMsg message) {
        BageReceivedAckMsg receivedAckMsg = new BageReceivedAckMsg();
        receivedAckMsg.messageID = message.messageID;
        receivedAckMsg.messageSeq = message.messageSeq;
        receivedAckMsg.no_persist = message.header.noPersist;
        receivedAckMsg.red_dot = message.header.redDot;
        receivedAckMsg.sync_once = message.header.syncOnce;
        return receivedAckMsg;
    }

    private void addReceivedMsg(BageMsg msg) {
        synchronized (receivedMsgListLock) {
            if (receivedMsgList == null) {
                receivedMsgList = new ArrayList<>();
            }
            BageSyncMsg syncMsg = new BageSyncMsg();
            syncMsg.no_persist = msg.header.noPersist ? 1 : 0;
            syncMsg.sync_once = msg.header.syncOnce ? 1 : 0;
            syncMsg.red_dot = msg.header.redDot ? 1 : 0;
            syncMsg.bageMsg = msg;
            receivedMsgList.add(syncMsg);
        }
    }

    public void saveReceiveMsg() {
        List<BageSyncMsg> tempList = null;
        synchronized (receivedMsgListLock) {
            if (BageCommonUtils.isNotEmpty(receivedMsgList)) {
                tempList = new ArrayList<>(receivedMsgList);
                receivedMsgList.clear();
            }
        }

        if (tempList != null) {
            saveSyncMsg(tempList);
            synchronized (receivedAckMsgList) {
                for (BageSyncMsg syncMsg : tempList) {
                    BageReceivedAckMsg receivedAckMsg = getReceivedAckMsg(syncMsg.bageMsg);
                    receivedAckMsgList.add(receivedAckMsg);
                }
            }
        }
        sendAck();
    }

    private final Handler sendAckHandler = new Handler(Looper.getMainLooper());
    private final Runnable sendAckRunnable = new Runnable() {
        @Override
        public void run() {
            // 检查连接状态
            if (BageConnection.getInstance().connectionIsNull() || BageConnection.getInstance().isReConnecting) {
                // 连接断开，取消所有待发送的消息
                sendAckHandler.removeCallbacks(this);
                return;
            }

            synchronized (receivedAckMsgList) {
                if (!receivedAckMsgList.isEmpty()) {
                    BageLoggerUtils.getInstance().i(TAG,"发送received ack");                    BageConnection.getInstance().sendMessage(receivedAckMsgList.get(0));
                    receivedAckMsgList.remove(0);
                    // 如果列表不为空，继续发送下一条
                    if (!receivedAckMsgList.isEmpty()) {
                        sendAckHandler.postDelayed(this, 100);
                    }
                }
            }
        }
    };

    //回复消息ack
    public void sendAck() {
        if (BageConnection.getInstance().connectionIsNull() || BageConnection.getInstance().isReConnecting) {
            return;
        }
        synchronized (receivedAckMsgList) {
            if (receivedAckMsgList.isEmpty()) {
                return;
            }
            if (receivedAckMsgList.size() == 1) {
                BageLoggerUtils.getInstance().i(TAG,"发送received ack");
                BageConnection.getInstance().sendMessage(receivedAckMsgList.get(0));
                receivedAckMsgList.clear();
                return;
            }
            // 移除所有待发送的消息，避免重复发送
            sendAckHandler.removeCallbacks(sendAckRunnable);
            // 开始发送消息
            sendAckHandler.post(sendAckRunnable);
        }
    }

    // 在需要清理资源的地方（比如onDestroy）调用此方法
    public void destroy() {
        if (sendAckHandler != null) {
            sendAckHandler.removeCallbacks(sendAckRunnable);
        }
    }

    /**
     * 保存同步消息
     *
     * @param list 同步消息对象
     */
    public synchronized void saveSyncMsg(List<BageSyncMsg> list) {
        List<BageMsg> saveMsgList = new ArrayList<>();
        List<BageMsg> allList = new ArrayList<>();
        for (BageSyncMsg mMsg : list) {
            if (mMsg.no_persist == 0 && mMsg.sync_once == 0) {
                saveMsgList.add(mMsg.bageMsg);
            }
            allList.add(mMsg.bageMsg);
        }
        MsgDbManager.getInstance().insertMsgs(saveMsgList);
        //将消息push给UI
        BageIM.getInstance().getMsgManager().pushNewMsg(allList);
        groupMsg(list);
    }

    private void groupMsg(List<BageSyncMsg> list) {
        LinkedHashMap<String, SavedMsg> savedList = new LinkedHashMap<>();
        //再将消息分组
        for (int i = 0, size = list.size(); i < size; i++) {
            BageMsg lastMsg = null;
            int count;

            if (list.get(i).bageMsg.channelType == BageChannelType.PERSONAL) {
                //如果是单聊先将channelId改成发送者ID
                if (!TextUtils.isEmpty(list.get(i).bageMsg.channelID) && !TextUtils.isEmpty(list.get(i).bageMsg.fromUID) && list.get(i).bageMsg.channelID.equals(BageIMApplication.getInstance().getUid())) {
                    list.get(i).bageMsg.channelID = list.get(i).bageMsg.fromUID;
                }
            }

            //将要存库的最后一条消息更新到会话记录表
            if (list.get(i).no_persist == 0
                    && list.get(i).bageMsg.type != BageMsgContentType.Bage_INSIDE_MSG
                    && list.get(i).bageMsg.isDeleted == 0) {
                lastMsg = list.get(i).bageMsg;
            }
            count = list.get(i).red_dot;
            if (lastMsg == null) {
                continue;
            }

            lastMsg = parsingMsg(lastMsg);
            boolean isSave = false;
            if (lastMsg.baseContentMsgModel != null && lastMsg.baseContentMsgModel.mentionAll == 1 && list.get(i).red_dot == 1) {
                isSave = true;
            } else {
                if (lastMsg.baseContentMsgModel != null && lastMsg.baseContentMsgModel.mentionInfo != null && BageCommonUtils.isNotEmpty(lastMsg.baseContentMsgModel.mentionInfo.uids) && count == 1) {
                    for (int j = 0, len = lastMsg.baseContentMsgModel.mentionInfo.uids.size(); j < len; j++) {
                        if (!TextUtils.isEmpty(lastMsg.baseContentMsgModel.mentionInfo.uids.get(j)) && !TextUtils.isEmpty(BageIMApplication.getInstance().getUid()) && lastMsg.baseContentMsgModel.mentionInfo.uids.get(j).equalsIgnoreCase(BageIMApplication.getInstance().getUid())) {
                            isSave = true;
                        }
                    }
                }
            }
            if (isSave) {
                //如果存在艾特情况直接将消息存储
                BageUIConversationMsg conversationMsg = ConversationDbManager.getInstance().insertOrUpdateWithMsg(lastMsg, 1);
                BageIM.getInstance().getConversationManager().setOnRefreshMsg(conversationMsg, "cutData");
                continue;
            }

            SavedMsg savedMsg = null;
            if (savedList.containsKey(lastMsg.channelID + "_" + lastMsg.channelType)) {
                savedMsg = savedList.get(lastMsg.channelID + "_" + lastMsg.channelType);
            }
            if (savedMsg == null) {
                savedMsg = new SavedMsg(lastMsg, count);
            } else {
                savedMsg.bageMsg = lastMsg;
                savedMsg.redDot = savedMsg.redDot + count;
            }
            savedList.put(lastMsg.channelID + "_" + lastMsg.channelType, savedMsg);
        }

        List<BageUIConversationMsg> refreshList = new ArrayList<>();
        // TODO: 4/27/21 这里未开事物是因为消息太多太快。事物来不及关闭
        for (Map.Entry<String, SavedMsg> entry : savedList.entrySet()) {
            BageUIConversationMsg conversationMsg = ConversationDbManager.getInstance().insertOrUpdateWithMsg(entry.getValue().bageMsg, entry.getValue().redDot);
            if (conversationMsg != null) {
                refreshList.add(conversationMsg);
            }
        }
//        for (int i = 0, size = refreshList.size(); i < size; i++) {
//            ConversationManager.getInstance().setOnRefreshMsg(refreshList.get(i), i == refreshList.size() - 1, "groupMsg");
//        }
        BageIM.getInstance().getConversationManager().setOnRefreshMsg(refreshList, "groupMsg");
    }

    public BageMsg parsingMsg(BageMsg message) {
        if (message.type == BageMsgContentType.Bage_SIGNAL_DECRYPT_ERROR || message.type == BageMsgContentType.Bage_CONTENT_FORMAT_ERROR) {
            return message;
        }
        JSONObject json = null;
        try {
            if (TextUtils.isEmpty(message.content)) return message;
            json = new JSONObject(message.content);
            if (json.has("type")) {
                message.content = json.toString();
                message.type = json.optInt("type");
            }
            if (TextUtils.isEmpty(message.fromUID)) {
                if (json.has("from_uid")) {
                    message.fromUID = json.optString("from_uid");
                } else {
                    message.fromUID = message.channelID;
                }
            }
            if (json.has("flame")) {
                message.flame = json.optInt("flame");
            }
            if (json.has("flame_second")) {
                message.flameSecond = json.optInt("flame_second");
            }
            if (json.has("root_id")) {
                message.robotID = json.optString("root_id");
            }
        } catch (JSONException e) {
            message.type = BageMsgContentType.Bage_CONTENT_FORMAT_ERROR;
            BageLoggerUtils.getInstance().e(TAG, "消息体非json");
        }

        if (json == null) {
            if (message.type != BageMsgContentType.Bage_SIGNAL_DECRYPT_ERROR)
                message.type = BageMsgContentType.Bage_CONTENT_FORMAT_ERROR;
        }

        if (message.type == BageMsgContentType.Bage_INSIDE_MSG) {
            CMDManager.getInstance().handleCMD(json, message.channelID, message.channelType);
            return message;
        }

        message.baseContentMsgModel = BageIM.getInstance().getMsgManager().getMsgContentModel(message.type, json);
        //如果是单聊先将channelId改成发送者ID
        if (!TextUtils.isEmpty(message.channelID)
                && !TextUtils.isEmpty(message.fromUID)
                && message.channelType == BageChannelType.PERSONAL
                && message.channelID.equals(BageIMApplication.getInstance().getUid())) {
            message.channelID = message.fromUID;
        }
        return message;
    }

    public void updateLastSendingMsgFail() {
        MsgDbManager.getInstance().updateAllMsgSendFail();
    }
}
