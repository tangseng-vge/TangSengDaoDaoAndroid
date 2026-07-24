package com.bage.im.protocol;

import com.bage.im.message.type.BageMsgType;

/**
 * 2019-11-11 10:27
 * 连接talk service确认消息
 */
public class BageConnectAckMsg extends BageBaseMsg {
    //客户端时间与服务器的差值，单位毫秒。
    public long timeDiff;
    //连接原因码
    public short reasonCode;
    // 服务端公钥
    public String serverKey;
    // 安全码
    public String salt;
    // 节点
    public int nodeId;
    public int serviceProtoVersion;
    public BageConnectAckMsg() {
        packetType = BageMsgType.CONNACK;
    }
}
