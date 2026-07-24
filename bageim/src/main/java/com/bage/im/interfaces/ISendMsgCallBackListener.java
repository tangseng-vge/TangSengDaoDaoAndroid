package com.bage.im.interfaces;


import com.bage.im.entity.BageMsg;

/**
 * 2020-08-02 00:21
 * 发送消息监听
 */
public interface ISendMsgCallBackListener {
    void onInsertMsg(BageMsg msg);
}
