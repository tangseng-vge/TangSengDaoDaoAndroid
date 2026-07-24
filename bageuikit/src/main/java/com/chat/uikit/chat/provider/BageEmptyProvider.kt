package com.chat.uikit.chat.provider

import android.view.View
import android.view.ViewGroup
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.uikit.R

class BageEmptyProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return null
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) { }

    override val itemViewType: Int
        get() = BageContentType.emptyView

    override val layoutId: Int
        get() = R.layout.chat_item_empty_view
}