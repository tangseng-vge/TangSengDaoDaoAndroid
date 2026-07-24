package com.chat.groupmanage.entity;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.bage.im.entity.BageChannelMember;

/**
 * 2020-04-11 23:06
 * 群成员
 */
public class GroupMemberEntity implements MultiItemEntity {
    public boolean isChecked;
    public BageChannelMember channelMember;
    public String pying;
    public int itemType = 1;
    public boolean isSetDelete;
    public GroupMemberEntity(BageChannelMember channelMember) {
        this.channelMember = channelMember;
    }

    @Override
    public int getItemType() {
        return itemType;
    }
}
