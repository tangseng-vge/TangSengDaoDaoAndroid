package com.chat.uikit.chat.manager;

import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageMsgSetting;
import com.bage.im.entity.BageSendOptions;
import com.bage.im.msgmodel.BageMessageContent;

public class SendMsgEntity {
    public BageMessageContent messageContent;
    public BageChannel bageChannel;
    public BageSendOptions options;

    public SendMsgEntity(BageMessageContent messageContent, BageChannel channel, BageSendOptions options) {
        this.bageChannel = channel;
        this.messageContent = messageContent;
        this.options = options;
    }
}
