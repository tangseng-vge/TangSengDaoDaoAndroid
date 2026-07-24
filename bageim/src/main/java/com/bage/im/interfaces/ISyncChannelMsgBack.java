package com.bage.im.interfaces;


import com.bage.im.entity.BageSyncChannelMsg;

/**
 * 2020-10-10 15:17
 */
public interface ISyncChannelMsgBack {
    void onBack(BageSyncChannelMsg syncChannelMsg);
}
