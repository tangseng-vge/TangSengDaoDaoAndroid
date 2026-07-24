package com.chat.sticker.msg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.utils.AndroidUtilities
import com.chat.sticker.R
import com.chat.sticker.ui.components.StickerView
import com.bage.im.message.type.BageSendMsgResult

class EmojiProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.chat_item_emoji_sticker_layout, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val emojiStickerContent =
            uiChatMsgItemEntity.bageMsg.baseContentMsgModel as EmojiContent
        val stickerView = parentView.findViewById<StickerView>(R.id.stickerView)
        stickerView.showSticker(
            emojiStickerContent.url,
            emojiStickerContent.placeholder,
            AndroidUtilities.dp(120f),
            false,
            uiChatMsgItemEntity.bageMsg.status != BageSendMsgResult.send_success
        )
        addLongClick(stickerView, uiChatMsgItemEntity)
        stickerView.setOnClickListener {
            stickerView.restart()

        }
    }

    override val itemViewType: Int
        get() = BageContentType.Bage_EMOJI_STICKER


    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        val stickerView = parentView.findViewById<StickerView>(R.id.stickerView)
        addLongClick(stickerView, uiChatMsgItemEntity)
    }

}