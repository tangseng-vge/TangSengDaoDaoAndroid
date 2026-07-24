package com.chat.uikit.chat.provider

import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.ui.components.RadialProgressView
import com.chat.uikit.R

class LoadingProvider : BageChatBaseProvider() {
    override val layoutId: Int
        get() = R.layout.chat_item_loading
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return null
    }

    override fun convert(helper: BaseViewHolder, item: BageUIChatMsgItemEntity) {
        super.convert(helper, item)
        helper.getView<RadialProgressView>(R.id.progress).setSize(50)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
    }

    override val itemViewType: Int
        get() = BageContentType.loading

}