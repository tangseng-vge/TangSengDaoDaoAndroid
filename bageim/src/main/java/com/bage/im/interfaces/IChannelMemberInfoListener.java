package com.bage.im.interfaces;


import com.bage.im.entity.BageChannelMember;

/**
 * 2019-12-01 15:54
 * 频道成员
 */
public interface IChannelMemberInfoListener {
    void onResult(BageChannelMember channelMember);
}
