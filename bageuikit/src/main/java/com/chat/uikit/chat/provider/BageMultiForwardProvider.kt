package com.chat.uikit.chat.provider

import android.content.Intent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.chat.base.emoji.MoonUtil
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.views.BubbleLayout
import com.chat.uikit.R
import com.chat.uikit.chat.ChatMultiForwardDetailActivity
import com.chat.uikit.chat.msgmodel.BageMultiForwardContent
import com.bage.im.BageIM
import com.bage.im.entity.BageChannelType
import kotlin.math.min

class BageMultiForwardProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.chat_item_multi_forward, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val multiView = parentView.findViewById<LinearLayout>(R.id.multiView)
        multiView.layoutParams.width = getViewWidth(from, uiChatMsgItemEntity)
        val contentLayout = parentView.findViewById<BubbleLayout>(R.id.contentLayout)
        val titleTv = parentView.findViewById<TextView>(R.id.titleTv)
        val contentTv = parentView.findViewById<TextView>(R.id.contentTv)
        resetCellBackground(parentView, uiChatMsgItemEntity, from)
        val multiForwardContent =
            uiChatMsgItemEntity.bageMsg.baseContentMsgModel as BageMultiForwardContent
        val title: String = if (multiForwardContent.channelType.toInt() == 1) {
            if (multiForwardContent.userList.size > 1) {
                val sBuilder = StringBuilder()
                for (i in multiForwardContent.userList.indices) {
                    if (!TextUtils.isEmpty(sBuilder)) sBuilder.append("、")
                    sBuilder.append(multiForwardContent.userList[i].channelName)
                }
                sBuilder.toString()
            } else multiForwardContent.userList[0].channelName
        } else {
            context.getString(R.string.group_chat)
        }
        titleTv.text = String.format(context.getString(R.string.chat_title_records), title)
        //设置内容
        val sBuilder = StringBuilder()
        if (multiForwardContent.msgList != null && multiForwardContent.msgList.isNotEmpty()) {
            val size = min(multiForwardContent.msgList.size, 3)
            for (i in 0 until size) {
                var name = ""
                var content = ""
                val fromUID = multiForwardContent.msgList[i].fromUID
                val messageContent = multiForwardContent.msgList[i].baseContentMsgModel
                if (messageContent != null) {
                    content = if (!TextUtils.isEmpty(messageContent.displayContent)) {
                        messageContent.displayContent
                    } else {
                        context.getString(R.string.base_unknow_msg)
                    }
                    // 如果文字太长滑动会卡顿
                    if (content.length > 100) {
                        content = content.substring(0, 80)
                    }
                }
                if (!TextUtils.isEmpty(fromUID)) {
                    val mChannel = BageIM.getInstance().channelManager.getChannel(
                        fromUID,
                        BageChannelType.PERSONAL
                    )
                    if (mChannel != null) {
                        name = mChannel.channelName
                    } else {
                        BageIM.getInstance().channelManager.fetchChannelInfo(
                            fromUID,
                            BageChannelType.PERSONAL
                        )
                    }
                }
                if (!TextUtils.isEmpty(sBuilder)) sBuilder.append("\n")
                sBuilder.append(name).append(":").append(content)
            }
        }
        // 显示表情
        MoonUtil.identifyFaceExpression(context, contentTv, sBuilder.toString(), MoonUtil.DEF_SCALE)
        addLongClick(contentLayout, uiChatMsgItemEntity)
        contentLayout.setOnClickListener {
            val intent = Intent(context, ChatMultiForwardDetailActivity::class.java)
            intent.putExtra("client_msg_no", uiChatMsgItemEntity.bageMsg.clientMsgNO)
            context.startActivity(intent)
        }
    }

    override val itemViewType: Int
        get() = BageContentType.Bage_MULTIPLE_FORWARD

    override fun resetCellBackground(
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellBackground(parentView, uiChatMsgItemEntity, from)
        val bgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.bageMsg,
            uiChatMsgItemEntity.nextMsg
        )
        val contentLayout = parentView.findViewById<BubbleLayout>(R.id.contentLayout)
        contentLayout.setAll(bgType, from, BageContentType.Bage_MULTIPLE_FORWARD)
    }

    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        val contentLayout = parentView.findViewById<BubbleLayout>(R.id.contentLayout)
        addLongClick(contentLayout, uiChatMsgItemEntity)
    }
}