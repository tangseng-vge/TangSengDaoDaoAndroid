package com.chat.base.endpoint.entity;

import com.chat.base.msg.ChatAdapter;
import com.bage.im.entity.BageMsg;

/**
 * 4/16/21 5:08 PM
 * 消息回应
 */
public class MsgReactionMenu {
    public String emoji;
    public ChatAdapter chatAdapter;
    public int[] location;
    public BageMsg bageMsg;

    public MsgReactionMenu(BageMsg bageMsg, String emoji, ChatAdapter chatAdapter, int[] location) {
        this.emoji = emoji;
        this.bageMsg = bageMsg;
        this.chatAdapter = chatAdapter;
        this.location = location;
    }
}
