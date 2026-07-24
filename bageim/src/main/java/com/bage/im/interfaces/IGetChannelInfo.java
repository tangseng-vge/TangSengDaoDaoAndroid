package com.bage.im.interfaces;


import com.bage.im.entity.BageChannel;

/**
 * 2019-12-01 15:40
 * 获取频道信息
 */
public interface IGetChannelInfo {
    BageChannel onGetChannelInfo(String channelId, byte channelType, IChannelInfoListener iChannelInfoListener);
}
