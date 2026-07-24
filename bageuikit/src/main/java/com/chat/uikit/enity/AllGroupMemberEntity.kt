package com.chat.uikit.enity

import com.bage.im.entity.BageChannelMember

class AllGroupMemberEntity(
    val channelMember: BageChannelMember,
    val onLine: Int,
    val lastOfflineTime: String,
    val lastOnlineTime: String,
) {
}