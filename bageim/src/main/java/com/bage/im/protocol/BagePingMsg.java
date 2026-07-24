package com.bage.im.protocol;


import com.bage.im.message.type.BageMsgType;

/**
 * 2019-11-11 10:49
 * 心跳消息
 */
public class BagePingMsg extends BageBaseMsg {
    public BagePingMsg() {
        packetType = BageMsgType.PING;
    }
}
