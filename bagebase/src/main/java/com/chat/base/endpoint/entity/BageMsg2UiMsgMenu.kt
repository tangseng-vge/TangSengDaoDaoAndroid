package com.chat.base.endpoint.entity

import com.chat.base.msg.IConversationContext
import com.bage.im.entity.BageMsg

class BageMsg2UiMsgMenu(
    val iConversationContext: IConversationContext,
    val bageMsg: BageMsg,
    val memberCount: Int,
    val showNickName: Boolean,
    val isChoose: Boolean
)