package com.bage.im.entity;

import java.util.List;

/**
 * 2020-10-09 14:49
 * 同步会话
 */
public class BageSyncChat {
    public long cmd_version;
    public List<BageSyncCmd> cmds;
    public String uid;
    public List<BageSyncConvMsg> conversations;
    public List<BageChannelState> channel_status;
}
