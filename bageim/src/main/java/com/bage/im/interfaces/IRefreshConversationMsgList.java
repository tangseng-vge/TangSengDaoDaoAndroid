package com.bage.im.interfaces;

import com.bage.im.entity.BageUIConversationMsg;

import java.util.List;

public interface IRefreshConversationMsgList {
    void onRefresh(List<BageUIConversationMsg> list);
}
