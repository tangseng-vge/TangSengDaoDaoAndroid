package com.bage.im.interfaces;


import com.bage.im.protocol.BageConnectAckMsg;
import com.bage.im.protocol.BageDisconnectMsg;
import com.bage.im.protocol.BagePongMsg;
import com.bage.im.protocol.BageSendAckMsg;

/**
 * 2019-11-10 17:03
 * 接受通讯协议消息
 */
public interface IReceivedMsgListener {
    /**
     * 登录状态消息
     *
     * @param connectAckMsg 状态
     */
    void loginStatusMsg(BageConnectAckMsg connectAckMsg);

    /**
     * 心跳消息
     */
    void pongMsg(BagePongMsg pongMsg);

    /**
     * 被踢消息
     */
    void kickMsg(BageDisconnectMsg disconnectMsg);

    /**
     * 发送消息状态消息
     *
     * @param sendAckMsg ack
     */
    void sendAckMsg(BageSendAckMsg sendAckMsg);

    /**
     * 重连
     */
    void reconnect();
}
