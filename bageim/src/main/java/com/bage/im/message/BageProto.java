package com.bage.im.message;

import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.BageDBColumns;
import com.bage.im.entity.BageMsg;
import com.bage.im.message.type.BageMsgType;
import com.bage.im.message.type.BageSendMsgResult;
import com.bage.im.msgmodel.BageMediaMessageContent;
import com.bage.im.msgmodel.BageMessageContent;
import com.bage.im.msgmodel.BageMsgEntity;
import com.bage.im.protocol.BageBaseMsg;
import com.bage.im.protocol.BageConnectAckMsg;
import com.bage.im.protocol.BageConnectMsg;
import com.bage.im.protocol.BageDisconnectMsg;
import com.bage.im.protocol.BagePingMsg;
import com.bage.im.protocol.BagePongMsg;
import com.bage.im.protocol.BageReceivedAckMsg;
import com.bage.im.protocol.BageReceivedMsg;
import com.bage.im.protocol.BageSendAckMsg;
import com.bage.im.protocol.BageSendMsg;
import com.bage.im.utils.CryptoUtils;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;
import com.bage.im.utils.BageTypeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * 5/21/21 11:28 AM
 * 收发消息转换
 */
class BageProto {
    private final String TAG = "BageProto";

    private BageProto() {
    }

    private static class MessageConvertHandlerBinder {
        static final BageProto msgConvert = new BageProto();
    }

    public static BageProto getInstance() {
        return MessageConvertHandlerBinder.msgConvert;
    }

    byte[] encodeMsg(BageBaseMsg msg) {
        byte[] bytes = null;
        if (msg.packetType == BageMsgType.CONNECT) {
            // 连接
            bytes = BageProto.getInstance().enConnectMsg((BageConnectMsg) msg);
//            String str = Arrays.toString(bytes);
//            BageLoggerUtils.getInstance().e(str);
        } else if (msg.packetType == BageMsgType.REVACK) {
            // 收到消息回执
            bytes = BageProto.getInstance().enReceivedAckMsg((BageReceivedAckMsg) msg);
        } else if (msg.packetType == BageMsgType.SEND) {
            // 发送聊天消息
            bytes = BageProto.getInstance().enSendMsg((BageSendMsg) msg);
        } else if (msg.packetType == BageMsgType.PING) {
            // 发送心跳
            bytes = BageProto.getInstance().enPingMsg((BagePingMsg) msg);
            BageLoggerUtils.getInstance().e("ping...");
        }
        return bytes;
    }

    byte[] enConnectMsg(BageConnectMsg connectMsg) {
        CryptoUtils.getInstance().initKey();
        byte[] remainingBytes = BageTypeUtils.getInstance().getRemainingLengthByte(connectMsg.getRemainingLength());
        int totalLen = connectMsg.getTotalLen();
        BageWrite bageWrite = new BageWrite(totalLen);
        try {
            bageWrite.writeByte(BageTypeUtils.getInstance().getHeader(connectMsg.packetType, connectMsg.flag, 0, 0));
            bageWrite.writeBytes(remainingBytes);
            bageWrite.writeByte(BageIMApplication.getInstance().protocolVersion);
            bageWrite.writeByte(connectMsg.deviceFlag);
            bageWrite.writeString(connectMsg.deviceID);
            bageWrite.writeString(BageIMApplication.getInstance().getUid());
            bageWrite.writeString(BageIMApplication.getInstance().getToken());
            bageWrite.writeLong(connectMsg.clientTimestamp);
            bageWrite.writeString(CryptoUtils.getInstance().getPublicKey());
        } catch (UnsupportedEncodingException e) {
            BageLoggerUtils.getInstance().e(TAG, "编码连接包错误");
        }
        return bageWrite.getWriteBytes();
    }

    synchronized byte[] enReceivedAckMsg(BageReceivedAckMsg receivedAckMsg) {
        byte[] remainingBytes = BageTypeUtils.getInstance().getRemainingLengthByte(8 + 4);

        int totalLen = 1 + remainingBytes.length + 8 + 4;
        BageWrite bageWrite = new BageWrite(totalLen);
        bageWrite.writeByte(BageTypeUtils.getInstance().getHeader(receivedAckMsg.packetType, receivedAckMsg.no_persist ? 1 : 0, receivedAckMsg.red_dot ? 1 : 0, receivedAckMsg.sync_once ? 1 : 0));
        bageWrite.writeBytes(remainingBytes);
        BigInteger bigInteger = new BigInteger(receivedAckMsg.messageID);
        bageWrite.writeLong(bigInteger.longValue());
        bageWrite.writeInt(receivedAckMsg.messageSeq);
        return bageWrite.getWriteBytes();
    }

    byte[] enPingMsg(BagePingMsg pingMsg) {
        BageWrite bageWrite = new BageWrite(1);
        bageWrite.writeByte(BageTypeUtils.getInstance().getHeader(pingMsg.packetType, pingMsg.flag, 0, 0));
        return bageWrite.getWriteBytes();
    }

    byte[] enSendMsg(BageSendMsg sendMsg) {
        // 先加密内容
        String sendContent = sendMsg.getSendContent();
        String msgKeyContent = sendMsg.getMsgKey();
        byte[] remainingBytes = BageTypeUtils.getInstance().getRemainingLengthByte(sendMsg.getRemainingLength());
        int totalLen = sendMsg.getTotalLength();
        BageWrite bageWrite = new BageWrite(totalLen);
        try {
            bageWrite.writeByte(BageTypeUtils.getInstance().getHeader(sendMsg.packetType, sendMsg.no_persist ? 1 : 0, sendMsg.red_dot ? 1 : 0, sendMsg.sync_once ? 1 : 0));
            bageWrite.writeBytes(remainingBytes);
            bageWrite.writeByte(BageTypeUtils.getInstance().getMsgSetting(sendMsg.setting));
            bageWrite.writeInt(sendMsg.clientSeq);
            bageWrite.writeString(sendMsg.clientMsgNo);
            bageWrite.writeString(sendMsg.channelId);
            bageWrite.writeByte(sendMsg.channelType);
            if (BageIMApplication.getInstance().protocolVersion >= 3) {
                bageWrite.writeInt(sendMsg.expire);
            }
            bageWrite.writeString(msgKeyContent);
            if (sendMsg.setting.topic == 1) {
                bageWrite.writeString(sendMsg.topicID);
            }
            bageWrite.writePayload(sendContent);

        } catch (UnsupportedEncodingException e) {
            BageLoggerUtils.getInstance().e(TAG, "编码发送包错误");
        }
        return bageWrite.getWriteBytes();
    }

    private BageConnectAckMsg deConnectAckMsg(BageRead bageRead, int hasServerVersion) {
        BageConnectAckMsg connectAckMsg = new BageConnectAckMsg();
        try {
            if (hasServerVersion == 1) {
                connectAckMsg.serviceProtoVersion = bageRead.readByte();
               // byte serverVersion = bageRead.readByte();
                if (connectAckMsg.serviceProtoVersion != 0) {
                    BageIMApplication.getInstance().protocolVersion = (byte) Math.min(connectAckMsg.serviceProtoVersion, BageIMApplication.getInstance().protocolVersion);
                }
            }
            long time = bageRead.readLong();
            short reasonCode = bageRead.readByte();
            String serverKey = bageRead.readString();
            String salt = bageRead.readString();
            if (connectAckMsg.serviceProtoVersion >= 4){
                connectAckMsg.nodeId = (int) bageRead.readLong();
            }
            connectAckMsg.serverKey = serverKey;
            connectAckMsg.salt = salt;
            //保存公钥和安全码
            CryptoUtils.getInstance().setServerKeyAndSalt(connectAckMsg.serverKey, connectAckMsg.salt);
            connectAckMsg.timeDiff = time;
            connectAckMsg.reasonCode = reasonCode;
        } catch (IOException e) {
            BageLoggerUtils.getInstance().e(TAG, "解码连接ack包错误");
        }

        return connectAckMsg;
    }

    private BageSendAckMsg deSendAckMsg(BageRead bageRead) {
        BageSendAckMsg sendAckMsg = new BageSendAckMsg();
        try {
            sendAckMsg.messageID = bageRead.readMsgID();
            sendAckMsg.clientSeq = bageRead.readInt();
            sendAckMsg.messageSeq = bageRead.readInt();
            sendAckMsg.reasonCode = bageRead.readByte();
        } catch (IOException e) {
            BageLoggerUtils.getInstance().e(TAG, "解码发送ack错误");
        }
        return sendAckMsg;
    }

    private BageDisconnectMsg deDisconnectMsg(BageRead bageRead) {
        BageDisconnectMsg disconnectMsg = new BageDisconnectMsg();
        try {
            disconnectMsg.reasonCode = bageRead.readByte();
            disconnectMsg.reason = bageRead.readString();
            BageLoggerUtils.getInstance().e(TAG, "断开消息code:" + disconnectMsg.reasonCode + ",reason:" + disconnectMsg.reason);
            return disconnectMsg;
        } catch (IOException e) {
            BageLoggerUtils.getInstance().e(TAG, "解码断开包错误");
        }
        return disconnectMsg;
    }

    private BageReceivedMsg deReceivedMsg(BageRead bageRead) {
        BageReceivedMsg receivedMsg = new BageReceivedMsg();
        try {
            byte settingByte = bageRead.readByte();
            receivedMsg.setting = BageTypeUtils.getInstance().getMsgSetting(settingByte);
            receivedMsg.msgKey = bageRead.readString();
            receivedMsg.fromUID = bageRead.readString();
            receivedMsg.channelID = bageRead.readString();
            receivedMsg.channelType = bageRead.readByte();
            if (BageIMApplication.getInstance().protocolVersion >= 3) {
                receivedMsg.expire = bageRead.readInt();
            }
            receivedMsg.clientMsgNo = bageRead.readString();
            if (receivedMsg.setting.stream == 1) {
                receivedMsg.streamNO = bageRead.readString();
                receivedMsg.streamSeq = bageRead.readInt();
                receivedMsg.streamFlag = bageRead.readByte();
            }
            receivedMsg.messageID = bageRead.readMsgID();
            receivedMsg.messageSeq = bageRead.readInt();
            receivedMsg.messageTimestamp = bageRead.readInt();
            if (receivedMsg.setting.topic == 1) {
                receivedMsg.topicID = bageRead.readString();
            }
            String content = bageRead.readPayload();
            String msgKey = receivedMsg.messageID
                    + receivedMsg.messageSeq
                    + receivedMsg.clientMsgNo
                    + receivedMsg.messageTimestamp
                    + receivedMsg.fromUID
                    + receivedMsg.channelID
                    + receivedMsg.channelType
                    + content;
            byte[] result = CryptoUtils.getInstance().aesEncrypt(msgKey);
            if (result == null) {
                return null;
            }
            String base64Result = CryptoUtils.getInstance().base64Encode(result);
            String localMsgKey = CryptoUtils.getInstance().digestMD5(base64Result);
            if (!localMsgKey.equals(receivedMsg.msgKey)) {
                BageLoggerUtils.getInstance().e("非法消息,本地消息key:" + localMsgKey + ",期望key:" + msgKey);
                return null;
            }
            receivedMsg.payload = CryptoUtils.getInstance().aesDecrypt(CryptoUtils.getInstance().base64Decode(content));
            BageLoggerUtils.getInstance().e(receivedMsg.toString());
            return receivedMsg;
        } catch (IOException e) {
            BageLoggerUtils.getInstance().e(TAG, "解码收到的消息错误");
            return null;
        }
    }

    BageBaseMsg decodeMessage(byte[] bytes) {
        try {
            BageRead bageRead = new BageRead(bytes);
            int packetType = bageRead.readPacketType();
            bageRead.readRemainingLength();
            if (packetType == BageMsgType.CONNACK) {
                int hasServerVersion = BageTypeUtils.getInstance().getBit(bytes[0], 0);
                return deConnectAckMsg(bageRead, hasServerVersion);
            } else if (packetType == BageMsgType.SENDACK) {
                return deSendAckMsg(bageRead);
            } else if (packetType == BageMsgType.DISCONNECT) {
                return deDisconnectMsg(bageRead);
            } else if (packetType == BageMsgType.RECEIVED) {
                return deReceivedMsg(bageRead);
            } else if (packetType == BageMsgType.PONG) {
                return new BagePongMsg();
            } else {
                BageLoggerUtils.getInstance().e("解码未知消息包类型：" + packetType);
                return null;
            }
        } catch (IOException e) {
            BageLoggerUtils.getInstance().e("解码消息错误：" + e.getMessage());
            return null;
        }
    }

    JSONObject getSendPayload(BageMsg msg) {
        JSONObject jsonObject = null;
        if (msg.baseContentMsgModel != null) {
            jsonObject = msg.baseContentMsgModel.encodeMsg();
        } else {
            msg.baseContentMsgModel = new BageMessageContent();
        }
        try {
            if (jsonObject == null) jsonObject = new JSONObject();
            jsonObject.put(BageDBColumns.BageMessageColumns.type, msg.type);
            //判断@情况
            if (msg.baseContentMsgModel.mentionInfo != null
                    && msg.baseContentMsgModel.mentionInfo.uids != null
                    && !msg.baseContentMsgModel.mentionInfo.uids.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0, size = msg.baseContentMsgModel.mentionInfo.uids.size(); i < size; i++) {
                    jsonArray.put(msg.baseContentMsgModel.mentionInfo.uids.get(i));
                }
                if (!jsonObject.has("mention")) {
                    JSONObject mentionJson = new JSONObject();
                    mentionJson.put("all", msg.baseContentMsgModel.mentionAll);
                    mentionJson.put("uids", jsonArray);
                    jsonObject.put("mention", mentionJson);
                }

            } else {
                if (msg.baseContentMsgModel.mentionAll == 1) {
                    JSONObject mentionJson = new JSONObject();
                    mentionJson.put("all", msg.baseContentMsgModel.mentionAll);
                    jsonObject.put("mention", mentionJson);
                }
            }
            // 被回复消息
            if (msg.baseContentMsgModel.reply != null) {
                jsonObject.put("reply", msg.baseContentMsgModel.reply.encodeMsg());
            }
            // 机器人ID
            if (!TextUtils.isEmpty(msg.baseContentMsgModel.robotID)) {
                jsonObject.put("robot_id", msg.baseContentMsgModel.robotID);
            }
            if (!TextUtils.isEmpty(msg.robotID)) {
                jsonObject.put("robot_id", msg.robotID);
            }
            if (BageCommonUtils.isNotEmpty(msg.baseContentMsgModel.entities)) {
                JSONArray jsonArray = new JSONArray();
                for (BageMsgEntity entity : msg.baseContentMsgModel.entities) {
                    JSONObject jo = new JSONObject();
                    jo.put("offset", entity.offset);
                    jo.put("length", entity.length);
                    jo.put("type", entity.type);
                    jo.put("value", entity.value);
                    jsonArray.put(jo);
                }
                jsonObject.put("entities", jsonArray);
            }
            if (msg.flame != 0) {
                jsonObject.put("flame_second", msg.flameSecond);
                jsonObject.put("flame", msg.flame);
            }
//            if (msg.baseContentMsgModel.flame != 0) {
//                jsonObject.put("flame_second", msg.baseContentMsgModel.flameSecond);
//                jsonObject.put("flame", msg.baseContentMsgModel.flame);
//            }
        } catch (JSONException e) {
            BageLoggerUtils.getInstance().e(TAG, "获取消息体错误");
        }
        return jsonObject;
    }

    /**
     * 获取发送的消息
     *
     * @param msg 本地消息
     * @return 网络消息
     */
    BageSendMsg getSendBaseMsg(BageMsg msg) {
        if (msg == null || TextUtils.isEmpty(msg.clientMsgNO) || TextUtils.isEmpty(msg.channelID)) {
            BageLoggerUtils.getInstance().e(TAG, "getSendBaseMsg: msg is null or missing clientMsgNO/channelID");
            return null;
        }
        //发送消息
        JSONObject jsonObject = getSendPayload(msg);
        BageSendMsg sendMsg = new BageSendMsg();
        // 默认先设置clientSeq，因为有可能本条消息并不需要入库，UI上自己设置了clientSeq
        sendMsg.clientSeq = (int) msg.clientSeq;
        sendMsg.sync_once = msg.header.syncOnce;
        sendMsg.no_persist = msg.header.noPersist;
        sendMsg.red_dot = msg.header.redDot;
        sendMsg.clientMsgNo = msg.clientMsgNO;
        sendMsg.channelId = msg.channelID;
        sendMsg.channelType = msg.channelType;
        sendMsg.topicID = msg.topicID;
        sendMsg.setting = msg.setting;
        sendMsg.expire = msg.expireTime;
        if (BageMediaMessageContent.class.isAssignableFrom(msg.baseContentMsgModel.getClass())) {
            //多媒体数据
            if (jsonObject.has("localPath")) {
                jsonObject.remove("localPath");
            }
            //视频地址
            if (jsonObject.has("videoLocalPath")) {
                jsonObject.remove("videoLocalPath");
            }
        }
        sendMsg.payload = jsonObject.toString();
        return sendMsg;
    }

    BageMsg baseMsg2BageMsg(BageBaseMsg baseMsg) {
        BageReceivedMsg receivedMsg = (BageReceivedMsg) baseMsg;
        BageMsg msg = new BageMsg();
        msg.channelType = receivedMsg.channelType;
        msg.channelID = receivedMsg.channelID;
        msg.content = receivedMsg.payload;
        msg.messageID = receivedMsg.messageID;
        msg.messageSeq = receivedMsg.messageSeq;
        msg.timestamp = receivedMsg.messageTimestamp;
        msg.fromUID = receivedMsg.fromUID;
        msg.setting = receivedMsg.setting;
        msg.clientMsgNO = receivedMsg.clientMsgNo;
        msg.status = BageSendMsgResult.send_success;
        msg.topicID = receivedMsg.topicID;
        msg.expireTime = receivedMsg.expire;
        if (msg.expireTime > 0) {
            msg.expireTimestamp = msg.expireTime + msg.timestamp;
        }
        msg.orderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(msg.messageSeq, msg.channelID, msg.channelType);
        msg.isDeleted = isDelete(msg.content);
        return msg;
    }

    private int isDelete(String contentJson) {
        int isDelete = 0;
        if (!TextUtils.isEmpty(contentJson)) {
            try {
                JSONObject jsonObject = new JSONObject(contentJson);
                isDelete = BageIM.getInstance().getMsgManager().isDeletedMsg(jsonObject);
            } catch (JSONException e) {
                BageLoggerUtils.getInstance().e(TAG, "获取消息是否删除时发现消息体非json");
            }
        }
        return isDelete;
    }
}
