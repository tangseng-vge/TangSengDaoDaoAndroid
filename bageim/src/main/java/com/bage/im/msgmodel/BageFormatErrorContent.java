package com.bage.im.msgmodel;

import com.bage.im.message.type.BageMsgContentType;

public class BageFormatErrorContent extends BageMessageContent {
    public BageFormatErrorContent() {
        this.type = BageMsgContentType.Bage_CONTENT_FORMAT_ERROR;
    }

    @Override
    public String getDisplayContent() {
        return "[消息格式错误]";
    }
}
