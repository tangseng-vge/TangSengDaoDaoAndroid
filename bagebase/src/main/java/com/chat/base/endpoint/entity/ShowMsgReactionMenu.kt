package com.chat.base.endpoint.entity

import android.widget.FrameLayout
import com.chat.base.msg.ChatAdapter
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.bage.im.entity.BageMsgReaction

class ShowMsgReactionMenu(
    val parentView: FrameLayout,
    val from: BageChatIteMsgFromType,
    val chatAdapter: ChatAdapter,
    val list: List<BageMsgReaction>?
)