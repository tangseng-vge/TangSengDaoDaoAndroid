package com.bage.im.protocol;


import com.bage.im.message.type.BageMsgType;

/**
 * 2019-11-11 10:49
 * 对ping请求的响应
 */
public class BagePongMsg extends BageBaseMsg {
    public BagePongMsg() {
        packetType = BageMsgType.PONG;
    }
}
