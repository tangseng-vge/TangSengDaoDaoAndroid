package com.bage.im.interfaces;


import com.bage.im.entity.BageChannelMember;

/**
 * 2020-02-01 15:19
 * 刷新频道成员信息
 */
public interface IRefreshChannelMember {
    void onRefresh(BageChannelMember channelMember, boolean isEnd);
}
