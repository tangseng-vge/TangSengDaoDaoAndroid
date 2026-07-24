package com.bage.im.interfaces;


import com.bage.im.entity.BageSyncChat;

/**
 * 2020-10-09 14:43
 * 同步消息返回
 */
public interface ISyncConversationChatBack {
    void onBack(BageSyncChat syncChat);
}
