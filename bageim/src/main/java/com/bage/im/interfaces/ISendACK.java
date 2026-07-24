package com.bage.im.interfaces;

import com.bage.im.entity.BageMsg;

/**
 * 5/12/21 2:02 PM
 * 发送消息ack监听
 */
public interface ISendACK {
    void msgACK(BageMsg msg);
}
