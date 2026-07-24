package com.bage.im.interfaces;


import com.bage.im.entity.BageMsg;

/**
 * 2020-08-19 21:42
 * 删除消息监听
 */
public interface IDeleteMsgListener {
    void onDeleteMsg(BageMsg msg);
}
