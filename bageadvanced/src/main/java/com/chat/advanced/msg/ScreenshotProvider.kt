package com.chat.advanced.msg

import android.text.SpannableString
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.advanced.R
import com.chat.base.config.BageConfig
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan
import com.chat.base.utils.AndroidUtilities
import com.bage.im.BageIM
import com.bage.im.entity.BageChannel
import com.bage.im.entity.BageChannelType

class ScreenshotProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return null
    }

    override val layoutId: Int
        get() = R.layout.chat_system_layout

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {

    }

    override fun convert(
        helper: BaseViewHolder,
        item: BageUIChatMsgItemEntity
    ) {
        super.convert(helper, item)
        val content: String
        if (item.bageMsg.fromUID == BageConfig.getInstance().uid) {
            helper.setText(R.id.contentTv, R.string.screenshort_inchat_my)
            content = context.getString(R.string.screenshort_inchat_my)
        } else {
            var mChannel = BageIM.getInstance().channelManager.getChannel(
                item.bageMsg.fromUID,
                BageChannelType.PERSONAL
            )
            if (mChannel == null) {
                mChannel = BageChannel()
                val mChannelMember = BageIM.getInstance().channelMembersManager.getMember(
                    item.bageMsg.channelID,
                    item.bageMsg.channelType,
                    item.bageMsg.fromUID
                )
                if (mChannelMember != null) {
                    mChannel.channelName = mChannelMember.memberName
                    mChannel.channelRemark = mChannelMember.memberRemark
                } else {
                    mChannel.channelName = ""
                    mChannel.channelRemark = ""
                }
            }
            val format = context.getString(R.string.screenshot_inchat1)
            val showName =
                if (TextUtils.isEmpty(mChannel.channelRemark)) mChannel.channelName else mChannel.channelRemark
            content = String.format(format, showName)
            //            baseViewHolder.setText(R.id.contentTv, content);
        }
        val textView = helper.getView<TextView>(R.id.contentTv)
        textView.setShadowLayer(AndroidUtilities.dp(10f).toFloat(), 0f, 0f, 0)
        val str = SpannableString(content)
        str.setSpan(
            SystemMsgBackgroundColorSpan(
                ContextCompat.getColor(
                    context,
                    R.color.colorSystemBg
                ), AndroidUtilities.dp(5f), AndroidUtilities.dp((2 * 5).toFloat())
            ), 0, content.length, 0
        )
        textView.text = str
    }

    override val itemViewType: Int
        get() = BageContentType.screenshot
}