package com.chat.uikit.enity;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.bage.im.entity.BageMsg;

/**
 * 2020-09-22 12:10
 * 合并转发
 */
public class ChatMultiForwardEntity implements MultiItemEntity {

    public int itemType;
    public String title;
    public BageMsg msg;

    @Override
    public int getItemType() {
        return itemType;
    }
}
