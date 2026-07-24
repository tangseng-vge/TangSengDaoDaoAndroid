package com.bage.im.interfaces;


import com.bage.im.entity.BageMsg;

/**
 * 2020-08-27 21:18
 * 消息修改监听
 */
public interface IRefreshMsg {
    void onRefresh(BageMsg msg, boolean left);
}
