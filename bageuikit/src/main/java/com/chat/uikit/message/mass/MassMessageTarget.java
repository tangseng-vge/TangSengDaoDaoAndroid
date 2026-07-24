package com.chat.uikit.message.mass;

import android.text.TextUtils;

import com.bage.im.entity.BageChannel;

public class MassMessageTarget {
    public final BageChannel channel;
    public boolean selected;

    public MassMessageTarget(BageChannel channel, boolean selected) {
        this.channel = channel;
        this.selected = selected;
    }

    public String key() {
        return channel.channelType + ":" + channel.channelID;
    }

    public String displayName() {
        if (!TextUtils.isEmpty(channel.channelRemark)) return channel.channelRemark;
        if (!TextUtils.isEmpty(channel.channelName)) return channel.channelName;
        return channel.channelID;
    }
}
