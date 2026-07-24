package com.chat.uikit.chat.provider

import android.app.Activity
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.alibaba.fastjson.JSONObject
import com.chat.base.config.BageApiConfig
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.ChatChooseContacts
import com.chat.base.endpoint.entity.ChooseChatMenu
import com.chat.base.glide.GlideUtils
import com.chat.base.msg.ChatAdapter
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageMsgBgType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.net.ud.BageProgressManager
import com.chat.base.ui.Theme
import com.chat.base.ui.components.FilterImageView
import com.chat.base.ui.components.SecretDeleteTimer
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.ImageUtils
import com.chat.base.utils.LayoutHelper
import com.chat.base.utils.BageDialogUtils
import com.chat.base.utils.BageDialogUtils.IImagePopupListener
import com.chat.base.utils.BageTimeUtils
import com.chat.base.utils.BageToastUtils
import com.chat.base.views.CircularProgressView
import com.chat.base.views.CustomImageViewerPopup.IImgPopupMenu
import com.chat.base.views.CustomImageViewerPopup
import com.chat.base.msgmodel.BageChatImageContent
import com.chat.base.views.blurview.ShapeBlurView
import com.chat.uikit.R
import com.google.android.material.snackbar.Snackbar
import com.bage.im.BageIM
import com.bage.im.entity.BageCMD
import com.bage.im.entity.BageCMDKeys
import com.bage.im.entity.BageChannel
import com.bage.im.entity.BageMsg
import com.bage.im.message.type.BageMsgContentType
import com.bage.im.msgmodel.BageImageContent
import java.io.File
import java.util.Objects

class BageImageProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context).inflate(R.layout.chat_item_img, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val contentLayout = parentView.findViewById<LinearLayout>(R.id.contentLayout)
        val imgMsgModel = uiChatMsgItemEntity.bageMsg.baseContentMsgModel as BageImageContent
        val imageView = parentView.findViewById<FilterImageView>(R.id.imageView)
        val blurView = parentView.findViewById<ShapeBlurView>(R.id.blurView)
        setCorners(from, uiChatMsgItemEntity, imageView, blurView)
        val progressTv = parentView.findViewById<TextView>(R.id.progressTv)
        val progressView = parentView.findViewById<CircularProgressView>(R.id.progressView)
        progressView.setProgColor(Theme.colorAccount)
        val imageLayout = parentView.findViewById<View>(R.id.imageLayout)
        val otherLayout = parentView.findViewById<FrameLayout>(R.id.otherLayout)
        val deleteTimer = SecretDeleteTimer(context)

        otherLayout.removeAllViews()
        otherLayout.addView(deleteTimer, LayoutHelper.createFrame(35, 35, Gravity.CENTER))
        contentLayout.gravity =
            if (from == BageChatIteMsgFromType.RECEIVED) Gravity.START else Gravity.END
        val layoutParams = imageView.layoutParams as FrameLayout.LayoutParams
        val blurViewLayoutParams = blurView.layoutParams as FrameLayout.LayoutParams
        val ints = ImageUtils.getInstance()
            .getImageWidthAndHeightToTalk(imgMsgModel.width, imgMsgModel.height)

        blurView.visibility = if (uiChatMsgItemEntity.bageMsg.flame == 1)
            View.VISIBLE
        else View.GONE
        if (uiChatMsgItemEntity.bageMsg.flame == 1) {
            otherLayout.visibility = View.VISIBLE
            deleteTimer.setSize(35)
            if (uiChatMsgItemEntity.bageMsg.viewedAt > 0 && uiChatMsgItemEntity.bageMsg.flameSecond > 0) {
                deleteTimer.setDestroyTime(
                    uiChatMsgItemEntity.bageMsg.clientMsgNO,
                    uiChatMsgItemEntity.bageMsg.flameSecond,
                    uiChatMsgItemEntity.bageMsg.viewedAt,
                    false
                )
            }
        } else {
            otherLayout.visibility = View.GONE
        }
        val showUrl = getShowURL(uiChatMsgItemEntity)
        GlideUtils.getInstance().showImg(context, showUrl, ints[0], ints[1], imageView)

        val layoutParams1 = imageLayout.layoutParams as LinearLayout.LayoutParams
        if (uiChatMsgItemEntity.bageMsg.flame == 1) {
            layoutParams.height = AndroidUtilities.dp(150f)
            layoutParams.width = AndroidUtilities.dp(150f)
            blurViewLayoutParams.height = AndroidUtilities.dp(150f)
            blurViewLayoutParams.width = AndroidUtilities.dp(150f)
            layoutParams1.height = AndroidUtilities.dp(150f)
            layoutParams1.width = AndroidUtilities.dp(150f)
        } else {
            layoutParams.height = ints[1]
            layoutParams.width = ints[0]
            blurViewLayoutParams.height = ints[1]
            blurViewLayoutParams.width = ints[0]
            layoutParams1.height = ints[1]
            layoutParams1.width = ints[0]
        }
        imageView.layoutParams = layoutParams
        blurView.layoutParams = blurViewLayoutParams
//        if (uiChatMsgItemEntity.bageMsg.channelType != BageChannelType.PERSONAL && from != BageChatIteMsgFromType.SEND) {
//            layoutParams1.leftMargin = AndroidUtilities.dp(10f)
//            layoutParams1.rightMargin = AndroidUtilities.dp(10f)
//        }
        imageLayout.layoutParams = layoutParams1

        //设置上传进度
        if (TextUtils.isEmpty(imgMsgModel.url)) {
            BageProgressManager.instance.registerProgress(uiChatMsgItemEntity.bageMsg.clientSeq,
                object : BageProgressManager.IProgress {
                    override fun onProgress(tag: Any?, progress: Int) {

                        if (tag is Long) {
                            if (tag == uiChatMsgItemEntity.bageMsg.clientSeq) {
                                progressView.progress = progress
                                progressTv.text =
                                    String.format("%s%%", progress)
                                if (progress >= 100) {
                                    progressTv.visibility = View.GONE
                                    progressView.visibility = View.GONE
                                    deleteTimer.visibility = View.VISIBLE
                                } else {
                                    progressView.visibility = View.VISIBLE
                                    progressTv.visibility = View.VISIBLE
                                    deleteTimer.visibility = View.GONE
                                }
                            }
                        }

                    }

                    override fun onSuccess(tag: Any?, path: String?) {
                        progressTv.visibility = View.GONE
                        progressView.visibility = View.GONE
                        deleteTimer.visibility = View.VISIBLE
                        if (tag != null) {
                            BageProgressManager.instance.unregisterProgress(tag)
                        }
                    }

                    override fun onFail(tag: Any?, msg: String?) {
                    }

                })
        }
        addLongClick(imageView, uiChatMsgItemEntity)
        imageView.setOnClickListener {
            onImageClick(
                uiChatMsgItemEntity,
                adapterPosition,
                imageView,
                getPreviewURL(uiChatMsgItemEntity.bageMsg)
            )
        }
    }

    override val itemViewType: Int
        get() = BageMsgContentType.Bage_IMAGE


    //查看大图
    private fun showImages(mMsg: BageMsg, uri: String, imageView: ImageView) {
        val flame = mMsg.flame
        val list: List<BageUIChatMsgItemEntity> = getAdapter()!!.data
        val imgList: MutableList<ImageView?> = ArrayList()
        val showImgList: MutableList<BageMsg> = ArrayList()
        val tempImgList: MutableList<Any?> = ArrayList()
        val originalImgList: MutableList<Any?> = ArrayList()
        if (flame == 1) {
            tempImgList.add(uri)
            originalImgList.add(uri)
            imgList.add(imageView)
        } else
            run {
                var i = 0
                val size = list.size
                while (i < size) {
                    if (list[i].bageMsg != null && list[i].bageMsg.type == BageContentType.Bage_IMAGE && list[i].bageMsg.remoteExtra.revoke == 0 && list[i].bageMsg.isDeleted == 0 && list[i].bageMsg.flame == 0
                    ) {
                        val showUrl: String = getPreviewURL(list[i].bageMsg)
                        showImgList.add(list[i].bageMsg)
                        val itemView =
                            getAdapter()!!.recyclerView.layoutManager!!.findViewByPosition(i)
                        if (itemView != null) {
                            val imageView1 =
                                itemView.findViewById<ImageView>(R.id.imageView)
                            imgList.add(imageView1)
                        } else imgList.add(null)
                        if (!TextUtils.isEmpty(showUrl)) {
                            tempImgList.add(showUrl)
                            originalImgList.add(getOriginalURL(list[i].bageMsg))
                        }
                    }
                    i++
                }
            }

        if (tempImgList.isEmpty()) return
        var index = 0
        for (i in tempImgList.indices) {
            if (!TextUtils.isEmpty(uri) && tempImgList[i] != null && tempImgList[i] == uri) {
                index = i
                break
            }
        }
        imageView.tag = flame
        val popupView = BageDialogUtils.getInstance().showImagePopup(
            context,
            mMsg,
            tempImgList,
            imgList,
            imageView,
            index,
            null,
            object : IImgPopupMenu {
                override fun onForward(position: Int) {
                    val mMessageContent = showImgList[position].baseContentMsgModel
                    EndpointManager.getInstance().invoke(
                        EndpointSID.showChooseChatView,
                        ChooseChatMenu(
                            ChatChooseContacts { list1: List<BageChannel>? ->
                                if (!list1.isNullOrEmpty()) {
                                    for (mChannel in list1) {
                                        BageIM.getInstance().msgManager.send(
                                            mMessageContent,
                                            mChannel
                                        )
                                    }
                                    val viewGroup =
                                        (context as Activity).findViewById<View>(android.R.id.content)
                                            .rootView as ViewGroup
                                    Snackbar.make(
                                        viewGroup,
                                        context.getString(R.string.is_forward),
                                        1000
                                    )
                                        .setAction(
                                            ""
                                        ) { }
                                        .show()
                                }
                            },
                            mMessageContent
                        )
                    )
                }

                override fun onFavorite(position: Int) {
                    collect(showImgList[position])
                }

                override fun onShowInChat(position: Int) {
                    (Objects.requireNonNull(getAdapter()) as ChatAdapter).showTipsMsg(
                        showImgList[position].clientMsgNO
                    )
                }
            },
            object : IImagePopupListener {
                override fun onShow() {
                    val adapter = getAdapter() as ChatAdapter
                    adapter.conversationContext.onViewPicture(true)
                }

                override fun onDismiss() {
                    val adapter = getAdapter() as ChatAdapter
                    adapter.conversationContext.onViewPicture(false)
                    BageIM.getInstance().msgManager.removeRefreshMsgListener("show_chat_img")
                    BageIM.getInstance().cmdManager.removeCmdListener("show_chat_img")
                }
            })
        if (popupView is CustomImageViewerPopup) {
            popupView.setOriginalUrls(originalImgList)
        }
        BageIM.getInstance().cmdManager.addCmdListener(
            "show_chat_img"
        ) { cmd: BageCMD ->
            if (!TextUtils.isEmpty(cmd.cmdKey)) {
                if (cmd.cmdKey == BageCMDKeys.bage_messageRevoke) {
                    if (cmd.paramJsonObject != null && cmd.paramJsonObject.has("message_id")) {
                        val msgID = cmd.paramJsonObject.optString("message_id")
                        val mMsg1 =
                            BageIM.getInstance().msgManager.getWithMessageID(msgID)
                        if (mMsg1 != null) {
                            for (msg in showImgList) {
                                if (msg.clientMsgNO == mMsg1.clientMsgNO && popupView != null && popupView.isShow) {
                                    BageToastUtils.getInstance()
                                        .showToast(context.getString(R.string.msg_revoked))
                                    popupView.dismiss()
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun collect(mMsg: BageMsg) {
        val jsonObject = JSONObject()
        val mImageContent = mMsg.baseContentMsgModel as BageImageContent
        jsonObject["content"] = BageApiConfig.getShowUrl(mImageContent.url)
        jsonObject["width"] = mImageContent.width
        jsonObject["height"] = mImageContent.height
        val hashMap = HashMap<String, Any>()
        hashMap["type"] = mMsg.type
        var uniqueKey = mMsg.messageID
        if (TextUtils.isEmpty(uniqueKey)) uniqueKey = mMsg.clientMsgNO
        hashMap["unique_key"] = uniqueKey
        if (mMsg.from != null) {
            hashMap["author_uid"] = mMsg.from.channelID
            hashMap["author_name"] = mMsg.from.channelName
        }
        hashMap["payload"] = jsonObject
        hashMap["activity"] = context
        EndpointManager.getInstance().invoke("favorite_add", hashMap)
    }

    private fun onImageClick(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        adapterPosition: Int,
        imageView: ImageView,
        tempShowImgUrl: String
    ) {
        if (uiChatMsgItemEntity.bageMsg.flame == 1 && uiChatMsgItemEntity.bageMsg.viewed == 0) {
            for (i in 0 until getAdapter()!!.data.size) {
                if (getAdapter()!!.data[i].bageMsg.clientMsgNO.equals(uiChatMsgItemEntity.bageMsg.clientMsgNO)) {
                    getAdapter()!!.data[i].bageMsg.viewed = 1
                    getAdapter()!!.data[i].bageMsg.viewedAt =
                        BageTimeUtils.getInstance().currentMills
                    getAdapter()!!.notifyItemChanged(adapterPosition)
                    uiChatMsgItemEntity.bageMsg.viewedAt = getAdapter()!!.data[i].bageMsg.viewedAt
                    BageIM.getInstance().msgManager.updateViewedAt(
                        1,
                        getAdapter()!!.data[i].bageMsg.viewedAt,
                        getAdapter()!!.data[i].bageMsg.clientMsgNO
                    )
                    break
                }
            }

        }
        showImages(
            uiChatMsgItemEntity.bageMsg,
            tempShowImgUrl,
            imageView
        )

    }

    private fun getShowURL(uiChatMsgItemEntity: BageUIChatMsgItemEntity): String {
        val imgMsgModel = uiChatMsgItemEntity.bageMsg.baseContentMsgModel as BageImageContent
        if (!TextUtils.isEmpty(imgMsgModel.url)) {
            return BageApiConfig.getShowUrl(imgMsgModel.url)
        }
        if (!TextUtils.isEmpty(imgMsgModel.localPath)) {
            val file = File(imgMsgModel.localPath)
            if (file.exists() && file.length() > 0L) {
                return file.absolutePath
            }
        }
        return ""
    }

    private fun getPreviewURL(msg: BageMsg): String {
        val content = msg.baseContentMsgModel as BageImageContent
        if (content is BageChatImageContent && !TextUtils.isEmpty(content.previewUrl)) {
            return BageApiConfig.getShowUrl(content.previewUrl)
        }
        if (!TextUtils.isEmpty(content.localPath)) {
            val file = File(content.localPath)
            if (file.exists() && file.length() > 0L) return file.absolutePath
        }
        return if (!TextUtils.isEmpty(content.url)) BageApiConfig.getShowUrl(content.url) else ""
    }

    private fun getOriginalURL(msg: BageMsg): String {
        val content = msg.baseContentMsgModel as BageImageContent
        if (content is BageChatImageContent && !TextUtils.isEmpty(content.originalUrl)) {
            return BageApiConfig.getShowUrl(content.originalUrl)
        }
        return getPreviewURL(msg)
    }

    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        val imageView = parentView.findViewById<FilterImageView>(R.id.imageView)
        addLongClick(imageView, uiChatMsgItemEntity)
    }

    override fun resetCellBackground(
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellBackground(parentView, uiChatMsgItemEntity, from)
        val imageView = parentView.findViewById<FilterImageView>(R.id.imageView)
        val blurView = parentView.findViewById<ShapeBlurView>(R.id.blurView)
        if (imageView != null && blurView != null) {
            setCorners(from, uiChatMsgItemEntity, imageView, blurView)
        }
    }

    private fun setCorners(
        from: BageChatIteMsgFromType,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        imageView: FilterImageView,
        blurView: ShapeBlurView
    ) {
        imageView.strokeWidth = 0f
        val bgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.bageMsg,
            uiChatMsgItemEntity.nextMsg
        )
        if (bgType == BageMsgBgType.center) {
            if (from == BageChatIteMsgFromType.SEND) {
                imageView.setCorners(10, 5, 10, 5)
                blurView.setCornerRadius(
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(5f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(5f).toFloat()
                )
            } else {
                imageView.setCorners(5, 10, 5, 10)
                blurView.setCornerRadius(
                    AndroidUtilities.dp(5f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(5f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat()
                )
            }
        } else if (bgType == BageMsgBgType.top) {
            if (from == BageChatIteMsgFromType.SEND) {
                imageView.setCorners(10, 10, 10, 5)
                blurView.setCornerRadius(
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(5f).toFloat()
                )
            } else {
                imageView.setCorners(10, 10, 5, 10)
                blurView.setCornerRadius(
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(5f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat()
                )
            }
        } else if (bgType == BageMsgBgType.bottom) {
            if (from == BageChatIteMsgFromType.SEND) {
                imageView.setCorners(10, 5, 10, 10)
                blurView.setCornerRadius(
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(5f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat()
                )
            } else {
                imageView.setCorners(5, 10, 10, 10)
                blurView.setCornerRadius(
                    AndroidUtilities.dp(5f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat(),
                    AndroidUtilities.dp(10f).toFloat()
                )
            }
        } else {
            imageView.setAllCorners(10)
            blurView.setCornerRadius(
                AndroidUtilities.dp(10f).toFloat(),
                AndroidUtilities.dp(10f).toFloat(),
                AndroidUtilities.dp(10f).toFloat(),
                AndroidUtilities.dp(10f).toFloat()
            )
        }
    }
}
