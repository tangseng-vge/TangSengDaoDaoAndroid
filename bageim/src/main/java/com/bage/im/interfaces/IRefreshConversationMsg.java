package com.bage.im.interfaces;


import com.bage.im.entity.BageUIConversationMsg;

/**
 * 2020-02-21 11:11
 * 刷新最近会话
 */
public interface IRefreshConversationMsg {
    void onRefreshConversationMsg(BageUIConversationMsg bageuiConversationMsg, boolean isEnd);
}
