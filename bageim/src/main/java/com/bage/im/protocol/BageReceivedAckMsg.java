package com.bage.im.protocol;


import com.bage.im.message.type.BageMsgType;

/**
 * 2019-11-11 10:46
 * 收到消息Ack消息
 */
public class BageReceivedAckMsg extends BageBaseMsg {
    //服务端的消息ID(全局唯一)
    public String messageID;
    //序列号
    public int messageSeq;
    public BageReceivedAckMsg() {
        packetType = BageMsgType.REVACK;
        remainingLength = 8;//序列号
    }
}
