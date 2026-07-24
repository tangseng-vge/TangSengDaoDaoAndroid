package com.bage.im.interfaces;


import com.bage.im.entity.BageChannelMember;

import java.util.List;

public interface IChannelMemberListResult {
    void onResult(List<BageChannelMember> list);
}
