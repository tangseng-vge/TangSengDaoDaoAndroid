package com.chat.advanced.ui.search;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.bage.im.msgmodel.BageMessageContent;

/**
 * 3/23/21 10:31 AM
 * 搜索聊天图片
 */
class ChatImgEntity implements MultiItemEntity {
    public int itemType;
    public String url;
    public String date;
    public String clientMsgNo;
    public long oldestOrderSeq;
    public BageMessageContent messageContent;

    @Override
    public int getItemType() {
        return itemType;
    }
}
