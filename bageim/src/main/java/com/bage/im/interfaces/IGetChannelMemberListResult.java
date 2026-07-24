package com.bage.im.interfaces;

import com.bage.im.entity.BageChannelMember;

import java.util.List;

public interface IGetChannelMemberListResult {
    public void onResult(List<BageChannelMember> list, boolean isRemote);
}
