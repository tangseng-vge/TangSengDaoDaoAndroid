package com.chat.base.entity

import com.bage.im.entity.BageChannelExtras

class BageChannelCustomerExtras : BageChannelExtras() {
    companion object {
        const val joinGroupRemind = "join_group_remind"
        const val memberCount = "member_count"
        const val onlineCount = "online_count"
        const val role = "role"
    }

}