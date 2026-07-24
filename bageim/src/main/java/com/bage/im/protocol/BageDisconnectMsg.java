package com.bage.im.protocol;


import com.bage.im.message.type.BageMsgType;

/**
 * 2020-01-30 17:34
 * 断开连接消息
 */
public class BageDisconnectMsg extends BageBaseMsg {
    public byte reasonCode;
    public String reason;

    public BageDisconnectMsg() {
        packetType = BageMsgType.DISCONNECT;
    }
}
