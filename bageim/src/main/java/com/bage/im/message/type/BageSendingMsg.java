package com.bage.im.message.type;

import com.bage.im.protocol.BageSendMsg;
import com.bage.im.utils.DateUtils;

/**
 * 2020-05-28 17:45
 * 正在发送的消息
 */
public class BageSendingMsg {
    // 消息
    public BageSendMsg bageSendMsg;
    // 发送次数
    public int sendCount;
    // 发送时间
    public long sendTime;
    // 是否可重发本条消息
    public boolean isCanResend;

    public BageSendingMsg(int sendCount, BageSendMsg bageSendMsg, boolean isCanResend) {
        this.sendCount = sendCount;
        this.bageSendMsg = bageSendMsg;
        this.isCanResend = isCanResend;
        this.sendTime = DateUtils.getInstance().getCurrentSeconds();
    }
}
