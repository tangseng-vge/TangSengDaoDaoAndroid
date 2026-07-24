package com.chat.base.endpoint.entity;



import com.bage.im.msgmodel.BageMessageContent;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-09-25 18:34
 * 选择会话
 */
public class ChooseChatMenu {
    public ChatChooseContacts mChatChooseContacts;
    public List<BageMessageContent> list;

    public ChooseChatMenu(ChatChooseContacts mChatChooseContacts, BageMessageContent messageContent) {
        this.mChatChooseContacts = mChatChooseContacts;
        list = new ArrayList<>();
        list.add(messageContent);
    }

    public ChooseChatMenu(ChatChooseContacts mChatChooseContacts, List<BageMessageContent> list) {
        this.mChatChooseContacts = mChatChooseContacts;
        this.list = list;
    }
}
