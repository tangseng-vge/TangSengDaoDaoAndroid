package com.chat.uikit.chat.provider

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.utils.AndroidUtilities
import com.chat.uikit.R

class BageSpanEmptyProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return null
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val contentLayout = parentView.findViewById<LinearLayout>(R.id.contentLayout)
        var height = AndroidUtilities.dp(50f)
        if (uiChatMsgItemEntity.bageMsg != null) {
            height = uiChatMsgItemEntity.bageMsg.messageSeq
        }
        contentLayout.layoutParams.height = height
    }

    override val itemViewType: Int
        get() = BageContentType.spanEmptyView

    override val layoutId: Int
        get() = R.layout.chat_item_span_empty_view
}