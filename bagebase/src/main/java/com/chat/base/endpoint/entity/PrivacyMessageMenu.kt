package com.chat.base.endpoint.entity

import com.bage.im.entity.BageMsg

class PrivacyMessageMenu(val iClick: IClick) {

    interface IClick {
        fun onDelete(mMsg: BageMsg)
        fun clearChannelMsg(channelID: String, channelType: Byte)
    }
}