package com.chat.sticker.msg

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.chat.base.config.BageApiConfig
import com.chat.base.entity.PopupMenuItem
import com.chat.base.glide.GlideUtils
import com.chat.base.msg.ChatAdapter
import com.chat.base.msg.model.BageGifContent
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.ui.Theme
import com.chat.base.ui.components.BottomSheet
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.LayoutHelper
import com.chat.base.utils.BageDialogUtils
import com.chat.base.utils.singleclick.SingleClickUtil
import com.chat.sticker.R
import com.chat.sticker.BageStickerApplication
import com.chat.sticker.service.StickerModel
import com.bage.im.entity.BageMsg
import java.io.File
import kotlin.math.max

class GifProvider : BageChatBaseProvider() {

    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context).inflate(R.layout.chat_item_gif, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val contentLayout = parentView.findViewById<LinearLayout>(R.id.contentLayout)
        val gifLayout = parentView.findViewById<FrameLayout>(R.id.gifLayout)
        val imageView = parentView.findViewById<ImageView>(R.id.imageView)
        if (from == BageChatIteMsgFromType.RECEIVED) {
            contentLayout.gravity = Gravity.START
        } else {
            contentLayout.gravity = Gravity.END
        }

        val gifContent = uiChatMsgItemEntity.bageMsg.baseContentMsgModel as BageGifContent
        if (gifContent.width == 0) gifContent.width = 100
        if (gifContent.height == 0) gifContent.height = 100
        val wH: IntArray = getWH(gifContent.width, gifContent.height)
        imageView.layoutParams.width = wH[0]
        imageView.layoutParams.height = wH[1]
//        val gifLayoutParams = gifLayout.layoutParams as LinearLayout.LayoutParams
//        gifLayoutParams.leftMargin = AndroidUtilities.dp(10f)
//        gifLayoutParams.rightMargin = AndroidUtilities.dp(10f)
        val showURL: String
        if (!TextUtils.isEmpty(gifContent.localPath)) {
            val file = File(gifContent.localPath)
            if (file.exists()) showURL = file.absolutePath else {
                showURL = BageApiConfig.getShowUrl(gifContent.url)
                StickerModel().download(showURL, file.absolutePath)
            }
        } else {
            val file = File(StickerModel().getLocalPath(gifContent.url))
            if (file.exists()) showURL = file.absolutePath else {
                showURL = BageApiConfig.getShowUrl(gifContent.url)
                StickerModel().download(showURL, file.absolutePath)
            }
            //showURL = BageApiConfig.getShowUrl(gifContent.url)
        }
        GlideUtils.getInstance().showImg(context, showURL, imageView)
        addLongClick(gifLayout, uiChatMsgItemEntity)
        SingleClickUtil.onSingleClick(gifLayout) {
//            val intent = Intent(context, StickerDetailActivity::class.java)
//            intent.putExtra("path", gifContent.url)
//            intent.putExtra("category", gifContent.category)
//            intent.putExtra("width", wH[0])
//            intent.putExtra("height", wH[1])
//            context.startActivity(intent)
            showStickerDetailDialog(uiChatMsgItemEntity.bageMsg, showURL)
        }
    }

    override val itemViewType: Int
        get() = BageContentType.Bage_GIF


    //获取GIF显示的高宽
    private fun getWH(width: Int, height: Int): IntArray {
        val intArr = IntArray(2)
        var showHeight: Int
        val showWidth: Int = if (width > AndroidUtilities.getScreenWidth() / 3) {
            AndroidUtilities.getScreenWidth() / 3
        } else {
            max(width, AndroidUtilities.getScreenWidth() / 3)
        }
        showHeight = showWidth / width * height
        if (showHeight < AndroidUtilities.getScreenHeight() / 6) {
            showHeight = AndroidUtilities.getScreenHeight() / 6
        }
        intArr[0] = showWidth
        intArr[1] = showHeight
        return intArr
    }


    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        val gifLayout = parentView.findViewById<FrameLayout>(R.id.gifLayout)
        addLongClick(gifLayout, uiChatMsgItemEntity)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun showStickerDetailDialog(
        bageMsg: BageMsg,
        path: String,
    ) {
        val chatAdapter = getAdapter() as ChatAdapter
        chatAdapter.hideSoftKeyboard()
        val builder = BottomSheet.Builder(context, false)
        builder.setApplyBottomPadding(false)
        val linearLayout = LinearLayout(context)
        linearLayout.gravity = Gravity.CENTER
        linearLayout.orientation = LinearLayout.VERTICAL
        val imageView = ImageView(context)
        linearLayout.addView(
            imageView,
            LayoutHelper.createLinear(
                180,
                180,
                0f, 20f, 0f, 50f
            )
        )
        GlideUtils.getInstance().showImg(context, path, imageView)

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
            BageDialogUtils.getInstance().showScreenPopup(moreIV, list)

        }
    }
}