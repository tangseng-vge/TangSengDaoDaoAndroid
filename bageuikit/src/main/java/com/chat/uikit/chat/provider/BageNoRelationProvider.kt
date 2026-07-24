package com.chat.uikit.chat.provider

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.msg.ChatAdapter
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.net.HttpResponseCode
import com.chat.base.ui.Theme
import com.chat.base.utils.BageDialogUtils
import com.chat.base.utils.BageToastUtils
import com.chat.base.views.WordToSpan
import com.chat.uikit.R
import com.chat.uikit.contacts.service.FriendModel
import com.bage.im.BageIM
import java.util.*

class BageNoRelationProvider : BageChatBaseProvider() {
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

    override val itemViewType: Int
        get() = BageContentType.noRelation

    override val layoutId: Int
        get() = R.layout.chat_item_no_relation_layout

    override fun convert(
        helper: BaseViewHolder,
        item: BageUIChatMsgItemEntity
    ) {
        super.convert(helper, item)
        var showName = ""
        val mChannel = BageIM.getInstance().channelManager.getChannel(
            item.bageMsg.channelID,
            item.bageMsg.channelType
        )
        if (mChannel != null) {
            showName =
                if (TextUtils.isEmpty(mChannel.channelRemark)) mChannel.channelName else mChannel.channelRemark
        }
        val content = String.format(context.getString(R.string.no_relation_request), showName)
        helper.setText(R.id.contentTv, content)
        val link = WordToSpan()
        link.setColorCUSTOM(Theme.colorAccount)
            .setUnderlineURL(true).setRegexCUSTOM(context.getString(R.string.send_request))
            .setLink(content)
            .into(helper.getView(R.id.contentTv))
            .setClickListener { _: String?, _: String? ->
                (Objects.requireNonNull(
                    getAdapter()
                ) as ChatAdapter).conversationContext.hideSoftKeyboard()
                BageDialogUtils.getInstance().showInputDialog(
                    context,
                    context.getString(R.string.apply),
                    context.getString(R.string.input_remark),
                    "",
                    "",
                    20
                ) { text ->
                    FriendModel.getInstance()
                        .applyAddFriend(
                            item.bageMsg.channelID, "", text
                        ) { code: Int, msg: String? ->
                            if (code == HttpResponseCode.success.toInt()) {
                                BageToastUtils.getInstance()
                                    .showToastNormal(context.getString(R.string.applyed))
                            } else {
                                BageToastUtils.getInstance().showToastNormal(msg)
                            }
                        }
                }
            }
    }

}