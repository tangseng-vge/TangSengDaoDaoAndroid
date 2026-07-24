package com.chat.sticker.msg

import android.annotation.SuppressLint
import android.content.Intent
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chat.base.config.BageApiConfig
import com.chat.base.entity.PopupMenuItem
import com.chat.base.glide.GlideUtils
import com.chat.base.msg.ChatAdapter
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.net.HttpResponseCode
import com.chat.base.ui.Theme
import com.chat.base.ui.components.BottomSheet
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.LayoutHelper
import com.chat.base.utils.BageDialogUtils
import com.chat.base.utils.singleclick.SingleClickUtil
import com.chat.sticker.R
import com.chat.sticker.BageStickerApplication
import com.chat.sticker.entity.StickerDetail
import com.chat.sticker.service.StickerModel
import com.chat.sticker.ui.StickerStoreDetailActivity
import com.chat.sticker.ui.components.StickerView
import com.google.android.material.button.MaterialButton
import com.bage.im.entity.BageMsg

class StickerProvider : BageChatBaseProvider() {

    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context)
            .inflate(R.layout.item_chat_vector_sticker_layout, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val vectorStickerContent =
            uiChatMsgItemEntity.bageMsg.baseContentMsgModel as StickerContent
        val stickerView = parentView.findViewById<StickerView>(R.id.stickerView)
        stickerView.showSticker(
            vectorStickerContent.url,
            vectorStickerContent.placeholder,
            AndroidUtilities.dp(180f),
            true
        )
        showStickerData(parentView, uiChatMsgItemEntity, from)
    }

    private fun showStickerData(
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {

        val contentLayout = parentView.findViewById<LinearLayout>(R.id.contentLayout)
        val stickerLayout = parentView.findViewById<FrameLayout>(R.id.stickerLayout)
        if (from == BageChatIteMsgFromType.RECEIVED) {
            contentLayout.gravity = Gravity.START
        } else {
            contentLayout.gravity = Gravity.END
        }
        val vectorStickerContent =
            uiChatMsgItemEntity.bageMsg.baseContentMsgModel as StickerContent
        SingleClickUtil.onSingleClick(stickerLayout) {
//            val intent = Intent(context, StickerDetailActivity::class.java)
//            intent.putExtra("path", vectorStickerContent.url)
//            intent.putExtra("category", vectorStickerContent.category)
//            context.startActivity(intent)
            showStickerDetailDialog(
                uiChatMsgItemEntity.bageMsg,
                vectorStickerContent.url,
                vectorStickerContent.placeholder,
                vectorStickerContent.category
            )
        }
    }

    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        val stickerLayout = parentView.findViewById<FrameLayout>(R.id.stickerLayout)
        addLongClick(stickerLayout, uiChatMsgItemEntity)
    }

    override val itemViewType: Int
        get() = BageContentType.Bage_VECTOR_STICKER

    @SuppressLint("ClickableViewAccessibility")
    private fun showStickerDetailDialog(
        bageMsg: BageMsg,
        path: String,
        placeholder: String,
        category: String
    ) {
        val chatAdapter = getAdapter() as ChatAdapter
        chatAdapter.hideSoftKeyboard()
        val builder = BottomSheet.Builder(context, false)
        builder.setApplyBottomPadding(false)
        val linearLayout = LinearLayout(context)
        linearLayout.gravity = Gravity.CENTER
        linearLayout.orientation = LinearLayout.VERTICAL
        val stickerView = StickerView(context)
        linearLayout.addView(
            stickerView,
            LayoutHelper.createLinear(
                180,
                180,
                0f, 20f, 0f, 20f
            )
        )
        if (TextUtils.isEmpty(category)) {
            GlideUtils.getInstance().showGif(
                context,
                BageApiConfig.getShowUrl(path),
                stickerView.imageView,
                null
            )
        } else {
            stickerView.showSticker(
                path,
                placeholder,
                AndroidUtilities.dp(180f),
                true
            )
        }
        val bottomLayout = LinearLayout(context)
        bottomLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.addView(
            bottomLayout, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                0f, 20f, 0f, 20f
            )
        )
        val categoryLayout = LinearLayout(context)
        categoryLayout.setOnClickListener {
            builder.dismissRunnable.run()
            val intent =
                Intent(context, StickerStoreDetailActivity::class.java)
            intent.putExtra("category", category)
            context.startActivity(intent)
        }
        categoryLayout.orientation = LinearLayout.HORIZONTAL
        categoryLayout.gravity = Gravity.CENTER_VERTICAL
        bottomLayout.addView(
            categoryLayout,
            LayoutHelper.createLinear(
                0,
                LayoutHelper.WRAP_CONTENT,
                1f,
                Gravity.CENTER,
                15,
                0,
                10,
                0
            )
        )

        val categoryIV = StickerView(context)
        val categoryNameTv = TextView(context)
        categoryNameTv.setTextColor(ContextCompat.getColor(context, R.color.colorDark))
        categoryNameTv.textSize = 16f
        categoryLayout.addView(categoryIV, LayoutHelper.createLinear(35, 35, Gravity.CENTER))
        categoryLayout.addView(
            categoryNameTv,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                10f,
                0f,
                10f,
                0f
            )
        )
        val button = MaterialButton(context)
        button.cornerRadius = AndroidUtilities.dp(30f)
        button.setTextColor(ContextCompat.getColor(context, R.color.white))
        button.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
        bottomLayout.addView(
            button,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                10f,
                0f,
                10f,
                0f
            )
        )
        bottomLayout.visibility = View.INVISIBLE
        if (!TextUtils.isEmpty(category)) {
            StickerModel().fetchStickerWithCategory(category,
                object : StickerModel.IStickerDetailListener {
                    override fun onResult(code: Int, msg: String, stickerDetail: StickerDetail?) {
                        if (code == HttpResponseCode.success.toInt()) {
                            bottomLayout.visibility = View.VISIBLE
                            categoryIV.showSticker(
                                stickerDetail!!.cover_lim,
                                "",
                                AndroidUtilities.dp(35f),
                                true
                            )
                            categoryNameTv.text = stickerDetail.title
                            if (stickerDetail.added) {
                                button.text = context.getString(R.string.str_sticker_added)
                                button.isEnabled = false
                                button.alpha = 0.2f
                            } else {
                                button.text = context.getString(R.string.str_sticker_add)
                                button.isEnabled = true
                                button.alpha = 1f
                            }
                        }
                    }

                })
        }
        val parentLayout = LinearLayout(context)
        val moreIV = ImageView(context)
        moreIV.setImageResource(R.mipmap.ic_ab_more)
        moreIV.rotation = 90f
        Theme.setColorFilter(moreIV, ContextCompat.getColor(context, R.color.popupTextColor))
        parentLayout.addView(
            moreIV,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.END, 0, 15, 15, 20
            )
        )
        moreIV.background = Theme.createSelectorDrawable(Theme.getPressedColor())
        parentLayout.orientation = LinearLayout.VERTICAL
        parentLayout.addView(linearLayout)
        builder.customView = parentLayout
        val bottomSheet = builder.show()
        bottomSheet.itemViews
        bottomSheet.setBackgroundColor(ContextCompat.getColor(context, R.color.screen_bg))
        moreIV.setOnClickListener {
            val list = ArrayList<PopupMenuItem>()
            list.add(
                PopupMenuItem(
                    context.getString(R.string.base_forward),
                    R.mipmap.msg_forward,
                    object : PopupMenuItem.IClick {
                        override fun onClick() {
                            builder.dismissRunnable.run()
                            BageStickerApplication.instance.forwardMessage(context, bageMsg)
                        }
                    })
            )
            if (!TextUtils.isEmpty(category)) {
                list.add(
                    PopupMenuItem(
                        context.getString(R.string.show_collections),
                        R.mipmap.msg_sticker,
                        object : PopupMenuItem.IClick {
                            override fun onClick() {
                                val intent =
                                    Intent(context, StickerStoreDetailActivity::class.java)
                                intent.putExtra("category", category)
                                context.startActivity(intent)
                                builder.dismissRunnable.run()
                            }
                        })
                )
            }
            BageDialogUtils.getInstance().showScreenPopup(moreIV, list)

        }
    }

}