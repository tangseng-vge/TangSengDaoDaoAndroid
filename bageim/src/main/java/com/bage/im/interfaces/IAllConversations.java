package com.bage.im.interfaces;

import com.bage.im.entity.BageUIConversationMsg;

import java.util.List;

public interface IAllConversations {
    void onResult(List<BageUIConversationMsg> list);
}
