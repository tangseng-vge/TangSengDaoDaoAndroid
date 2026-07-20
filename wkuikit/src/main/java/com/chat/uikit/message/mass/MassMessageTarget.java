package com.chat.uikit.message.mass;

import android.text.TextUtils;

import com.xinbida.wukongim.entity.WKChannel;

public class MassMessageTarget {
    public final WKChannel channel;
    public boolean selected;

    public MassMessageTarget(WKChannel channel, boolean selected) {
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
