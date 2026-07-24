package com.bage.im.interfaces;


import com.bage.im.entity.BageChannelMember;

import java.util.List;

/**
 * 2020-02-01 16:43
 * 移除频道成员
 */
public interface IRemoveChannelMember {
    void onRemoveMembers(List<BageChannelMember> list);
}
