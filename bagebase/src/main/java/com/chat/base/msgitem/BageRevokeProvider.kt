package com.chat.base.msgitem

import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.BageBaseApplication
import com.chat.base.R
import com.chat.base.config.BageConfig
import com.chat.base.msg.ChatAdapter
import com.chat.base.ui.Theme
import com.chat.base.ui.components.NormalClickableContent
import com.chat.base.ui.components.NormalClickableSpan
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.BageTimeUtils
import com.bage.im.BageIM
import com.bage.im.entity.BageChannelType
import com.bage.im.entity.BageMsg

class BageRevokeProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return null
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
    }

    override val layoutId: Int
        get() = R.layout.chat_system_layout
    override val itemViewType: Int
        get() = BageContentType.revoke

    override fun convert(helper: BaseViewHolder, item: BageUIChatMsgItemEntity) {
        super.convert(helper, item)
        val isReEdit: Boolean
        var content: String = showRevokeMsg(item.bageMsg)
        if (!TextUtils.isEmpty(item.bageMsg.fromUID) && !TextUtils.isEmpty(
                item.bageMsg.remoteExtra.revoker
            ) && item.bageMsg.fromUID == BageConfig.getInstance().uid && item.bageMsg.remoteExtra.revoker == BageConfig.getInstance().uid
        ) {
            if (item.bageMsg.type == BageContentType.Bage_TEXT && BageTimeUtils.getInstance().currentSeconds - item.bageMsg.timestamp < 300) {
                isReEdit = true
                content = String.format("%s %s", content, context.getString(R.string.re_edit))
            } else {
                content = String.format("%s", content)
                isReEdit = false
            }
        } else {
            isReEdit = false
            content = String.format("%s", content)
        }

        val textView: TextView = helper.getView(R.id.contentTv)
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.setShadowLayer(AndroidUtilities.dp(5f).toFloat(), 0f, 0f, 0)
        val str = SpannableString(content)
        str.setSpan(
            SystemMsgBackgroundColorSpan(
                ContextCompat.getColor(
                    context,
                    R.color.colorSystemBg
                ), AndroidUtilities.dp(5f), AndroidUtilities.dp((2 * 5).toFloat())
            ), 0, content.length, 0
        )
        if (isReEdit) {
            val index = content.indexOf(context.getString(R.string.re_edit))
            str.setSpan(
                NormalClickableSpan(false,
                    Theme.colorAccount,
                    NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""),
                    object : NormalClickableSpan.IClick {
                        override fun onClick(view: View) {
                            val chatAdapter = getAdapter() as ChatAdapter
                            if (item.bageMsg.baseContentMsgModel.reply != null && !TextUtils.isEmpty(
                                    item.bageMsg.baseContentMsgModel.reply.message_id
                                )
                            ) {
                                val mMsg =
                                    BageIM.getInstance().msgManager.getWithMessageID(item.bageMsg.baseContentMsgModel.reply.message_id)
                                if (mMsg != null) {
                                    chatAdapter.replyMsg(mMsg)
                                }
                            }
                            chatAdapter.setEditContent(item.bageMsg.baseContentMsgModel.displayContent)
                        }
                    }),
                index,
                index + context.getString(R.string.re_edit).length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        textView.text = str
    }


    companion object {

        fun showRevokeMsg(mMsg: BageMsg?): String {
            val content: String
            if (mMsg == null) return ""
            if (!TextUtils.isEmpty(mMsg.remoteExtra.revoker) && !TextUtils.isEmpty(mMsg.fromUID)) {
                if (mMsg.remoteExtra.revoker == mMsg.fromUID) {
                    if (mMsg.fromUID == BageConfig.getInstance().uid) {
                        content =
                            BageBaseApplication.getInstance().context.getString(R.string.my_revoke_msg)
                    } else {
                        val mChannel = mMsg.from
                        var showName: String? = ""
                        if (mChannel != null) {
                            showName = mChannel.channelRemark
                            if (TextUtils.isEmpty(showName)) showName = mChannel.channelName
                        } else {
                            val member = mMsg.memberOfFrom
                            if (member != null) showName =
                                if (TextUtils.isEmpty(member.memberRemark)) member.memberName else member.memberRemark
                        }
                        if (TextUtils.isEmpty(showName)){
                            BageIM.getInstance().channelManager.fetchChannelInfo(mMsg.fromUID,BageChannelType.PERSONAL)
                        }
                        content = String.format(
                            BageBaseApplication.getInstance().context.getString(R.string.user_revoke_msg),
                            showName
                        )
                    }
                } else {
                    var showName: String? = ""
                    if (mMsg.remoteExtra.revoker == BageConfig.getInstance().uid) {
                        // 你撤回了一条成员''的消息
                        if (mMsg.memberOfFrom != null) {
                            showName =
                                if (TextUtils.isEmpty(mMsg.memberOfFrom.memberRemark)) mMsg.memberOfFrom.memberName else mMsg.memberOfFrom.memberRemark
                        }
                        if (TextUtils.isEmpty(showName)) {
                            val mChannel = BageIM.getInstance().channelManager.getChannel(
                                mMsg.fromUID,
                                BageChannelType.PERSONAL
                            )
                            if (mChannel != null) {
                                showName =
                                    if (TextUtils.isEmpty(mChannel.channelRemark)) mChannel.channelName else mChannel.channelRemark
                            }
                        }
                        content = String.format(
                            BageBaseApplication.getInstance().context.getString(R.string.manager_revoke_user_msg),
                            showName
                        )
                    } else {
                        // ''撤回了一条成员消息
                        val member = BageIM.getInstance().channelMembersManager.getMember(
                            mMsg.channelID,
                            mMsg.channelType,
                            mMsg.remoteExtra.revoker
                        )
                        if (member != null) showName =
                            if (TextUtils.isEmpty(member.memberRemark)) member.memberName else member.memberRemark
                        if (TextUtils.isEmpty(showName)) {
                            val mChannel = BageIM.getInstance().channelManager.getChannel(
                                mMsg.remoteExtra.revoker,
                                BageChannelType.PERSONAL
                            )
                            if (mChannel != null) {
                                showName =
                                    if (TextUtils.isEmpty(mChannel.channelRemark)) mChannel.channelName else mChannel.channelRemark
                            }
                        }
                        content = String.format(
                            BageBaseApplication.getInstance().context.getString(R.string.manager_revoke_user_msg1),
                            showName
                        )
                    }
                }
            } else {
                if (mMsg.fromUID != BageConfig.getInstance().uid) {
                    val mChannel = mMsg.from
                    var showName: String? = ""
                    if (mChannel != null) {
                        showName = mChannel.channelRemark
                        if (TextUtils.isEmpty(showName)) showName = mChannel.channelName
                    } else {
                        val member = mMsg.memberOfFrom
                        if (member != null) showName =
                            if (TextUtils.isEmpty(member.memberRemark)) member.memberName else member.memberRemark
                    }
                    content = String.format(
                        BageBaseApplication.getInstance().context.getString(R.string.user_revoke_msg),
                        showName
                    )
                } else {
                    content =
                        BageBaseApplication.getInstance().context.getString(R.string.my_revoke_msg)
                }
            }
            return content
        }
    }

}