package com.chat.uikit.chat.provider

import android.text.SpannableString
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan
import com.chat.base.utils.AndroidUtilities
import com.chat.uikit.R
import org.json.JSONException
import org.json.JSONObject

class BageSensitiveWordsProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return null
    }

    override val layoutId: Int
        get() = R.layout.chat_item_sensitive_words_layout

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
        if (!TextUtils.isEmpty(item.bageMsg.content)) {
            try {
                val jsonObject = JSONObject(item.bageMsg.content)
                val content = jsonObject.optString("content")
                //                baseViewHolder.setText(R. qid.contentTv, content);
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
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    override val itemViewType: Int
        get() = BageContentType.sensitiveWordsTips
}