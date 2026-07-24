package com.chat.base.endpoint.entity;

import android.app.Activity;

import com.bage.im.entity.BageChannel;

import java.util.List;

/**
 * 5/7/21 6:39 PM
 */
public class CreateVideoCallMenu {
    public String channelID;
    public byte channelType;
    public List<BageChannel> BageChannels;
    public Activity activity;

    public CreateVideoCallMenu(Activity activity, String channelID, byte channelType, List<BageChannel> BageChannels) {
        this.BageChannels = BageChannels;
        this.activity = activity;
        this.channelID = channelID;
        this.channelType = channelType;
    }
}
