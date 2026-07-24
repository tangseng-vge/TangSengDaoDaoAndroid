package com.bage.im.interfaces;


import com.bage.im.entity.BageChannel;

/**
 * 2020-02-01 14:38
 * 刷新频道
 */
public interface IRefreshChannel {
    void onRefreshChannel(BageChannel channel, boolean isEnd);
}
