package com.bage.im.interfaces;


import com.bage.im.entity.BageChannelMember;

/**
 * 2019-12-01 15:52
 * 获取频道成员
 */
public interface IGetChannelMemberInfo {
    BageChannelMember onResult(String channelId, byte channelType, String uid, IChannelMemberInfoListener iChannelMemberInfoListener);
}
