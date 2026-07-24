package com.chat.advanced

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.chat.advanced.entity.ChatBgKeys
import com.chat.advanced.utils.ChatBgBlurHelper
import com.chat.advanced.msg.ScreenshotContent
import com.chat.advanced.msg.ScreenshotProvider
import com.chat.advanced.service.AdvancedModel
import com.chat.advanced.ui.ChatBgListActivity
import com.chat.advanced.ui.MsgRemindActivity
import com.chat.advanced.ui.ReadMsgMembersActivity
import com.chat.advanced.ui.search.RecordActivity
import com.chat.advanced.utils.ReactionAnimation
import com.chat.advanced.utils.ReactionStickerUtils
import com.chat.advanced.utils.ScreenShotListenManager
import com.chat.advanced.utils.checkEditTime
import com.chat.base.BageBaseApplication
import com.chat.base.config.BageApiConfig
import com.chat.base.config.BageConfig
import com.chat.base.config.BageConstants
import com.chat.base.config.BageSharedPreferencesUtil
import com.chat.base.net.ud.BageDownloader
import com.chat.base.net.ud.BageProgressManager
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointHandler
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.CanReactionMenu
import com.chat.base.endpoint.entity.ChatBgItemMenu
import com.chat.base.endpoint.entity.ChatItemPopupMenu
import com.chat.base.endpoint.entity.ChatSettingCellMenu
import com.chat.base.endpoint.entity.EditImgMenu
import com.chat.base.endpoint.entity.EditMsgMenu
import com.chat.base.endpoint.entity.MsgReactionMenu
import com.chat.base.endpoint.entity.OtherLoginViewMenu
import com.chat.base.endpoint.entity.ReadMsgDetailMenu
import com.chat.base.endpoint.entity.ReadMsgMenu
import com.chat.base.endpoint.entity.SetChatBgMenu
import com.chat.base.endpoint.entity.ShowMsgReactionMenu
import com.chat.base.endpoint.entity.BageSendMsgMenu
import com.chat.base.glide.GlideUtils
import com.chat.base.msg.IConversationContext
import com.chat.base.msgitem.ReactionSticker
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageMsgItemViewManager
import com.chat.base.net.HttpResponseCode
import com.chat.base.ui.Theme
import com.chat.base.ui.components.SwitchView
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.ImageUtils
import com.chat.base.utils.SvgHelper
import com.chat.base.utils.UserUtils
import com.chat.base.utils.BageFileUtils
import com.chat.base.utils.BageTimeUtils
import com.chat.base.utils.BageToastUtils
import com.chat.base.utils.singleclick.SingleClickUtil
import com.bage.im.BageIM
import com.bage.im.entity.BageChannel
import com.bage.im.entity.BageChannelExtras
import com.bage.im.entity.BageChannelType
import com.bage.im.entity.BageMsg
import com.bage.im.message.type.BageSendMsgResult
import com.bage.im.msgmodel.BageImageContent
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs


class BageAdvancedApplication private constructor() {
    //截屏监听
    private var screenShotListenManager: ScreenShotListenManager? = null

    private object SingletonInstance {
        val INSTANCE = BageAdvancedApplication()
    }

    companion object {
        val instance: BageAdvancedApplication
            get() = SingletonInstance.INSTANCE
    }

    fun init() {
        val appModule = BageBaseApplication.getInstance().getAppModuleWithSid("advanced")
        if (!BageBaseApplication.getInstance().appModuleIsInjection(appModule)) {
            return
        }
        initReactionSticker()
//        BageImageEditorApplication.getInstance().init()
        BageIM.getInstance().msgManager.registerContentMsg(ScreenshotContent::class.java)
        BageMsgItemViewManager.getInstance()
            .addChatItemViewProvider(BageContentType.screenshot, ScreenshotProvider())
        BageIM.getInstance().cmdManager.addCmdListener("advanced_application") { cmd ->
            if (cmd != null && cmd.cmdKey == "syncMessageReaction") {
                if (cmd.paramJsonObject.has("channel_id") && cmd.paramJsonObject.has("channel_type")) {
                    val channelId: String = cmd.paramJsonObject.optString("channel_id")
                    val channelType: Byte = cmd.paramJsonObject.optInt("channel_type").toByte()
                    AdvancedModel.instance.syncReaction(channelId, channelType)
                }
            }
        }
        EndpointManager.getInstance()
            .setMethod("advancedModule", EndpointSID.sendMessage) { `object` ->
                if (`object` is BageSendMsgMenu) {
                    `object`.option.setting.receipt = `object`.channel.receipt
                }
            }

        EndpointManager.getInstance().setMethod(EndpointSID.openChatPage) { `object` ->
            if (`object` is BageChannel && !TextUtils.isEmpty(`object`.channelID)) {
                AdvancedModel.instance.syncReaction(`object`.channelID, `object`.channelType)
            }
            null
        }
        EndpointManager.getInstance().setMethod("stop_screen_shot") {
            stopScreenShotListen()
        }
        EndpointManager.getInstance().setMethod(
            "start_screen_shot"
        ) { `object` ->
            val iConversationContext = `object` as IConversationContext
            startScreenShotListen(iConversationContext)
            null
        }
        EndpointManager.getInstance().setMethod(
            "msg_remind_view"
        ) { `object` ->
            val chatSettingCellMenu = `object` as ChatSettingCellMenu
            getMsgItemRemindView(chatSettingCellMenu)
        }
        EndpointManager.getInstance().setMethod(
            "find_msg_view"
        ) { `object` ->
            val chatSettingCellMenu = `object` as ChatSettingCellMenu
            getFindMsgView(chatSettingCellMenu)
        }

        EndpointManager.getInstance().setMethod(
            "msg_receipt_view"
        ) { `object` ->
            val chatSettingCellMenu = `object` as ChatSettingCellMenu
            getMsgReceiptView(chatSettingCellMenu)
        }


        // 搜索聊天图片
//        EndpointManager.getInstance().setMethod(
//            "str_search_chat_img", EndpointCategory.bageSearchChatContent, 96
//        ) {
//            SearchChatContentMenu(
//                BageBaseApplication.getInstance().context.getString(R.string.image)
//            ) { channelID: String?, channelType: Byte ->
//                val intent = Intent(
//                    BageBaseApplication.getInstance().context,
//                    ChatImgActivity::class.java
//                )
//                intent.putExtra("channel_id", channelID)
//                intent.putExtra("channel_type", channelType)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                BageBaseApplication.getInstance().context.startActivity(intent)
//            }
//        }
//        // 按日期查找聊天内容
//        EndpointManager.getInstance().setMethod(
//            "str_search_chat_for_date", EndpointCategory.bageSearchChatContent, 100
//        ) {
//            SearchChatContentMenu(
//                BageBaseApplication.getInstance().context.getString(R.string.str_search_for_date)
//            ) { channelID: String?, channelType: Byte ->
//                val intent = Intent(
//                    BageBaseApplication.getInstance().context,
//                    ChatWithDateActivity::class.java
//                )
//                intent.putExtra("channel_id", channelID)
//                intent.putExtra("channel_type", channelType)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                BageBaseApplication.getInstance().context.startActivity(intent)
//            }
//        }
//        // 搜索群成员
//        EndpointManager.getInstance().setMethod(
//            "str_search_group_member", EndpointCategory.bageSearchChatContent, 101
//        ) { `object`: Any ->
//            val mChannel = `object` as BageChannel
//            if (mChannel.channelType == BageChannelType.GROUP) {
//                return@setMethod SearchChatContentMenu(
//                    BageBaseApplication.getInstance().context
//                        .getString(R.string.str_find_group_member)
//                ) { channelID: String?, channelType: Byte ->
//                    val intent = Intent(
//                        BageBaseApplication.getInstance().context,
//                        SearchAllMembersActivity::class.java
//                    )
//                    intent.putExtra("channelID", channelID)
//                    intent.putExtra("channelType", channelType)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    BageBaseApplication.getInstance().context.startActivity(intent)
//                }
//            } else {
//                return@setMethod null
//            }
//        }

        EndpointManager.getInstance()
            .setMethod(
                "message_edit",
                EndpointCategory.bageChatPopupItem,
                1,
                object : EndpointHandler {
                    override fun invoke(`object`: Any?): Any? {
                        val mMsg = `object` as BageMsg
                        //han
                        if(checkEditTime(mMsg.createdAt)){//if sent time is greater than 60s
                            return null
                        }//han
                        if (mMsg.type == BageContentType.Bage_TEXT) {
                            if (!TextUtils.isEmpty(mMsg.fromUID) && mMsg.fromUID.equals(
                                    BageConfig.getInstance().uid
                                ) && mMsg.status == BageSendMsgResult.send_success && BageTimeUtils.getInstance().currentSeconds - mMsg.timestamp < 60 * 60 * 24
                            ) {
                                val popupMenu = ChatItemPopupMenu(
                                    R.mipmap.msg_edit,
                                    BageBaseApplication.getInstance().context.getString(R.string.str_edit),
                                    object : ChatItemPopupMenu.IPopupItemClick {
                                        override fun onClick(
                                            mMsg: BageMsg,
                                            iConversationContext: IConversationContext
                                        ) {
                                            iConversationContext.showEdit(mMsg)
                                        }

                                    })
                                popupMenu.tag = "text_message_edit"
                                return popupMenu
                            }
                        } else if (mMsg.type == BageContentType.Bage_IMAGE) {
                            val popupMenu = ChatItemPopupMenu(
                                R.mipmap.msg_edit,
                                BageBaseApplication.getInstance().context.getString(R.string.str_edit),
                                object : ChatItemPopupMenu.IPopupItemClick {
                                    override fun onClick(
                                        mMsg: BageMsg,
                                        iConversationContext: IConversationContext
                                    ) {
                                        val imgMsgModel = mMsg.baseContentMsgModel as
                                                BageImageContent
                                        var showUrl: String
                                        if (!TextUtils.isEmpty(imgMsgModel.localPath)) {
                                            showUrl = imgMsgModel.localPath
                                            val file = File(showUrl)
                                            if (!file.exists() || file.length() == 0L) {
                                                //如果本地文件被删除就显示网络图片
                                                showUrl = BageApiConfig.getShowUrl(imgMsgModel.url)
                                            }
                                        } else {
                                            showUrl = BageApiConfig.getShowUrl(imgMsgModel.url)
                                        }
                                        editImg(showUrl, iConversationContext.chatActivity)
                                    }
                                })
                            popupMenu.tag = "image_message_edit"
                            return popupMenu
                        }
                        return null
                    }

                })

        EndpointManager.getInstance().setMethod(
            "editMsg"
        ) { `object` ->
            val menu = `object` as
                    EditMsgMenu
            editImg(menu.url, menu.context)
            null
        }

        EndpointManager.getInstance().setMethod(
            "show_receipt"
        ) { `object` ->
            val mMsg = `object` as BageMsg
            mMsg.setting.receipt == 1 && mMsg.remoteExtra.readedCount > 0 && mMsg.channelType == BageChannelType.GROUP && !TextUtils.isEmpty(
                mMsg.fromUID
            ) && mMsg.fromUID == BageConfig.getInstance().uid
        }

        EndpointManager.getInstance().setMethod(
            "is_show_reaction"
        ) { `object` ->
            val menu = `object` as CanReactionMenu
            val mMsg = menu.mMsg
            val config = menu.config
            var isShowReaction = true
            if (mMsg.status != BageSendMsgResult.send_success || TextUtils.isEmpty(mMsg.messageID) || !config.isCanShowReaction) {
                isShowReaction = false
            }
            if (mMsg.channelType == BageChannelType.PERSONAL) {
                if (UserUtils.getInstance()
                        .checkFriendRelation(mMsg.channelID) || UserUtils.getInstance()
                        .checkBlacklist(mMsg.channelID)
                ) {
                    isShowReaction = false
                }
            }
            if (mMsg.channelType == BageChannelType.GROUP) {
                if (!UserUtils.getInstance().checkInGroupOk(
                        mMsg.channelID,
                        BageConfig.getInstance().uid
                    ) || UserUtils.getInstance()
                        .checkGroupBlacklist(mMsg.channelID, BageConfig.getInstance().uid)
                ) {
                    isShowReaction = false
                }
            }
            isShowReaction
        }

        // 查看消息已读未读详情
        EndpointManager.getInstance().setMethod("show_msg_read_detail") { `object`: Any? ->
            if (`object` is ReadMsgDetailMenu) {
                val intent = Intent(
                    BageBaseApplication.getInstance().context,
                    ReadMsgMembersActivity::class.java
                )
                intent.putExtra("message_id", `object`.messageID)
                intent.putExtra(
                    "group_no",
                    `object`.iConversationContext.chatChannelInfo.channelID
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                BageBaseApplication.getInstance().context.startActivity(intent)
            }
            null
        }


        EndpointManager.getInstance().setMethod(
            "other_login_view"
        ) { `object` ->
            val otherLoginView = `object` as OtherLoginViewMenu
            getOtherLoginView(otherLoginView.context, otherLoginView.parentViewGroup)
        }

        EndpointManager.getInstance().setMethod("get_wx_token") { `object` ->
            val code = `object` as String
            AdvancedModel.instance.wxLogin(code)
            null
        }

        EndpointManager.getInstance().setMethod(
            "set_chat_bg_view"
        ) { `object` ->
            val menu = `object` as ChatBgItemMenu
            getChatBgView(menu)
            null
        }

        EndpointManager.getInstance().setMethod("set_chat_bg")
        { `object` ->
            val menu = `object` as SetChatBgMenu
            setChatBG(menu)
            null
        }


        EndpointManager.getInstance().setMethod(
            "reaction_sticker"
        ) {
            reactionStickers
        }
        EndpointManager.getInstance().setMethod("stop_reaction_animation") {
            ReactionAnimation.stop()
        }
        //消息回应监听
        EndpointManager.getInstance().setMethod("bage_msg_reaction") { `object`: Any? ->
            if (`object` is MsgReactionMenu) {
//                ReactionAnimation.show(
//                    `object`.chatAdapter.context,
//                    `object`.chatAdapter,
//                    `object`.emoji,
//                    `object`.location,
//                    `object`.bageMsg
//                )
                var isAdded = false
                if (`object`.bageMsg.reactionList != null && `object`.bageMsg.reactionList.isNotEmpty()) {
                    for (reaction in `object`.bageMsg.reactionList) {
                        if (reaction.emoji == `object`.emoji && reaction.uid == BageConfig.getInstance().uid) {
                            isAdded = true
                            break
                        }
                    }
                }
                if (!isAdded) {
                    ReactionStickerUtils.showAnimation = `object`.emoji
                }

                AdvancedModel.instance.reactionsMsg(
                    `object`.bageMsg.channelID,
                    `object`.bageMsg.channelType,
                    `object`.bageMsg.messageID,
                    `object`.emoji
                )
            }
            null
        }
        EndpointManager.getInstance().setMethod("refresh_msg_reaction") { `object` ->
            val menu = `object` as ShowMsgReactionMenu
            ReactionStickerUtils.refreshMsgReactionsData(
                menu.parentView,
                menu.chatAdapter,
                menu.from,
                menu.list
            )
            null
        }
        EndpointManager.getInstance().setMethod(
            "show_msg_reaction"
        ) { `object` ->
            val menu = `object` as ShowMsgReactionMenu
            ReactionStickerUtils.setMsgReactionsData(
                menu.parentView,
                menu.chatAdapter,
                menu.from,
                menu.list
            )
            null
        }

        EndpointManager.getInstance().setMethod(
            "read_msg"
        ) { `object` ->
            val menu = `object` as ReadMsgMenu
            AdvancedModel.instance.readMsg(
                menu.channelID, menu.channelType, menu.msgIds
            )
            null
        }

    }

    private fun setChatBG(menu: SetChatBgMenu) {
        val channel =
            BageIM.getInstance().channelManager.getChannel(menu.channelID, menu.channelType)
                ?: return
        var url = ""
        channel.localExtra?.let { localExtra ->
            val urlObject = localExtra[ChatBgKeys.chatBgUrl]
            if (urlObject is String) {
                url = urlObject
            }
        }
        val themePref = Theme.getTheme()
        val isDark = if (themePref != Theme.DARK_MODE && themePref != Theme.LIGHT_MODE) {
            Theme.isSystemDarkMode(menu.backGroundIV.context)
        } else {
            themePref == Theme.DARK_MODE
        }

        if (TextUtils.isEmpty(url)) {
//            val userInfoEntity = BageConfig.getInstance().userInfo
            val overallURL =
                BageSharedPreferencesUtil.getInstance().getSPWithUID(ChatBgKeys.chatBgUrl)
            val overallIsDeleted =
                BageSharedPreferencesUtil.getInstance().getIntWithUID(ChatBgKeys.chatBgIsDeleted)
            if (overallIsDeleted == 1) return

            if (!TextUtils.isEmpty(overallURL)) {
                val isSvg =
                    BageSharedPreferencesUtil.getInstance().getIntWithUID(ChatBgKeys.chatBgIsSvg)
                val colorStr = BageSharedPreferencesUtil.getInstance()
                    .getSPWithUID(if (isDark) ChatBgKeys.chatBgColorDark else ChatBgKeys.chatBgColorLight)
                var colors = intArrayOf()
                if (!TextUtils.isEmpty(colorStr)) {
                    val colorArr = colorStr.split(",")
                    colors = intArrayOf(
                        Color.parseColor("#" + colorArr[0]),
                        Color.parseColor("#" + colorArr[1]),
                        Color.parseColor("#" + colorArr[2]),
                        Color.parseColor("#" + colorArr[3])
                    )
//                    colors[0] = Color.parseColor("#" + colorArr[0])
//                    colors[1] = Color.parseColor("#" + colorArr[1])
//                    colors[2] = Color.parseColor("#" + colorArr[2])
//                    colors[3] = Color.parseColor("#" + colorArr[3])
                }
//                if (isDark){
//                    colors = intArrayOf(
//                        Color.parseColor("#252D3A"),
//                        Color.parseColor("#252D3A"),
//                        Color.parseColor("#252D3A"),
//                        Color.parseColor("#252D3A")
//                    )
//                }
//                val colorIndex = BageSharedPreferencesUtil.getInstance()
//                    .getInt(userInfoEntity.uid + "_" + BageChannelCustomerExtras.chatBgColorIndex)
                val gradientAngle = BageSharedPreferencesUtil.getInstance().getIntWithUID(
                    ChatBgKeys.chatBgGradientAngle
                )
                val showPattern = BageSharedPreferencesUtil.getInstance()
                    .getIntWithUID(ChatBgKeys.chatBgShowPattern)
                //userInfoEntity.chat_bg_show_pattern
                val path = ChatBgKeys.cacheFilePath(overallURL)
                val file = File(path)
                if (file.exists()) {
                    if (isSvg == 1)
                        setSvgBG(
                            colors,
                            path,
                            showPattern,
                            gradientAngle,
                            menu.rootLayout,
                            menu.backGroundIV,
                            menu.blurView,
                            isDark
                        )
                    else {
                        applyImageChatBG(menu, path, isBlurred = BageSharedPreferencesUtil.getInstance()
                            .getIntWithUID(ChatBgKeys.chatBgIsBlurred))
                    }
                } else {
                    // 下载文件
                    downloadBG(menu.backGroundIV.context, overallURL, path) {
                        if (isSvg == 1)
                            setSvgBG(
                                colors,
                                path,
                                showPattern,
                                gradientAngle,
                                menu.rootLayout,
                                menu.backGroundIV,
                                menu.blurView,
                                isDark
                            )
                        else {
                            applyImageChatBG(menu, path, isBlurred = BageSharedPreferencesUtil.getInstance()
                                .getIntWithUID(ChatBgKeys.chatBgIsBlurred))
                        }
                    }
                }
            } else {
                // 设置系统默认背景
                if (isDark) {
                    menu.backGroundIV.setBackgroundResource(R.mipmap.ic_chat_bg_dark)
                } else {
                    menu.backGroundIV.setBackgroundResource(R.mipmap.ic_chat_bg)
                }
            }
//            else {
//                val drawable = GradientDrawable(
//                    Theme.getGradientOrientation(0),
//                    Theme.defaultColorsDark[0]
//                )
//                menu.rootLayout.background = drawable
//                val patternColor = AndroidUtilities.getPatternColor(
//                    Theme.defaultColorsDark[0][0],
//                    Theme.defaultColorsDark[0][1],
//                    Theme.defaultColorsDark[0][2],
//                    Theme.defaultColorsDark[0][3]
//                )
//                val svgBitmap = SvgHelper.getBitmap(
//                    R.raw.def,
//                    AndroidUtilities.getScreenWidth(),
//                    AndroidUtilities.getScreenHeight(),
//                    patternColor
//                )
//                menu.backGroundIV.setImageBitmap(svgBitmap)
//            }
        } else {
            var colors = intArrayOf()
            var isSvg = 0
//            var colorIndex = 0
            var gradientAngle = 0
            var showPattern = 0
            var isBlurred = 0
            var isDeleted = 0
            val isDeletedObject = channel.localExtra[ChatBgKeys.chatBgIsDeleted]
            val isSvgObject = channel.localExtra[ChatBgKeys.chatBgIsSvg]
//            val colorIndexObject = channel.localExtra[BageChannelCustomerExtras.chatBgColorIndex]
            val gradientAngleObject = channel.localExtra[ChatBgKeys.chatBgGradientAngle]
            val showPatternObject = channel.localExtra[ChatBgKeys.chatBgShowPattern]
            val isBlurredObject = channel.localExtra[ChatBgKeys.chatBgIsBlurred]
            val colorObject =
                channel.localExtra[if (isDark) ChatBgKeys.chatBgColorDark else ChatBgKeys.chatBgColorLight]
            if (colorObject != null) {
                val colorsStr = colorObject as String
                if (!TextUtils.isEmpty(colorsStr)) {
                    val colorArr = colorsStr.split(",")
                    colors = intArrayOf(
                        Color.parseColor("#" + colorArr[0]),
                        Color.parseColor("#" + colorArr[1]),
                        Color.parseColor("#" + colorArr[2]),
                        Color.parseColor("#" + colorArr[3])
                    )
                }
            }
            if (isDeletedObject != null) isDeleted = isDeletedObject as Int
            if (isDeleted == 1) return
            if (isSvgObject != null) isSvg = isSvgObject as Int
//            if (colorIndexObject != null) colorIndex = colorIndexObject as Int
            if (gradientAngleObject != null) gradientAngle = parseChatBgInt(gradientAngleObject)
            if (showPatternObject != null) showPattern = parseChatBgInt(showPatternObject)
            if (isBlurredObject != null) isBlurred = parseChatBgInt(isBlurredObject)
            val path = ChatBgKeys.cacheFilePath(url)
            val file = File(path)
            if (file.exists()) {
                if (isSvg == 1)
                    setSvgBG(
                        colors,
                        path,
                        showPattern,
                        gradientAngle,
                        menu.rootLayout,
                        menu.backGroundIV,
                        menu.blurView,
                        isDark
                    )
                else {
                    applyImageChatBG(menu, path, isBlurred)
                }
            } else {
                // 下载文件
                downloadBG(menu.backGroundIV.context, url, path) {
                    if (isSvg == 1)
                        setSvgBG(
                            colors,
                            path,
                            showPattern,
                            gradientAngle,
                            menu.rootLayout,
                            menu.backGroundIV,
                            menu.blurView,
                            isDark
                        )
                    else {
                        applyImageChatBG(menu, path, isBlurred)
                    }
                }
            }
        }
    }

    private fun downloadBG(
        context: Context,
        url: String,
        savePath: String,
        result: () -> Unit
    ) {
        val downloadUrl = BageApiConfig.getShowUrl(url)
        File(savePath).parentFile?.mkdirs()
        BageDownloader.instance.download(downloadUrl, savePath, object : BageProgressManager.IProgress {
            override fun onProgress(tag: Any?, progress: Int) {}

            override fun onSuccess(tag: Any?, path: String?) {
                AndroidUtilities.runOnUIThread {
                    if (File(savePath).exists()) {
                        result()
                    }
                }
            }

            override fun onFail(tag: Any?, msg: String?) {}
        })
    }

    private fun applyImageChatBG(menu: SetChatBgMenu, path: String, isBlurred: Int) {
        menu.rootLayout.background = null
        menu.blurView.visibility = View.GONE
        menu.backGroundIV.setImageDrawable(null)
        val context = menu.backGroundIV.context
        val displayPath = if (isBlurred == 1) {
            ChatBgBlurHelper.displayPath(context, path)
        } else {
            path
        }
        GlideUtils.getInstance().showImg(context, displayPath, menu.backGroundIV)
    }

    private fun parseChatBgInt(value: Any?): Int {
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun setSvgBG(
        colors: IntArray,
        path: String,
        showPattern: Int,
        gradientAngle: Int,
        rootLayout: View,
        imageView: ImageView,
        blurView: View,
        isDark: Boolean
    ) {
        blurView.visibility = View.GONE
        val gradientColors = if (colors.isNotEmpty()) {
            colors
        } else if (isDark) {
            Theme.defaultColorsDark[abs(path.hashCode()) % Theme.defaultColorsDark.size]
        } else {
            intArrayOf()
        }
        if (gradientColors.isNotEmpty()) {
            rootLayout.background = GradientDrawable(
                Theme.getGradientOrientation(gradientAngle),
                gradientColors
            )
        }
//        val colors1 = intArrayOf(
//            Color.parseColor("#5f4167"),
//            Color.parseColor("#5f4167"),
//            Color.parseColor("#252D3A"),
//            Color.parseColor("#252D3A")
//        )
////        if (colors.isNotEmpty()) {
//            val drawable = GradientDrawable(
//                Theme.getGradientOrientation(gradientAngle),
//                colors1
//            )
//            rootLayout.background = drawable
////        }

        if (showPattern == 1 && colors.isNotEmpty() && colors.size == 4) {
            val patternColor = AndroidUtilities.getPatternColor(
                colors[0],
                colors[1],
                colors[2],
                colors[3],
            )
            val svgBitmap = SvgHelper.getBitmap(
                File(path),
                AndroidUtilities.getScreenWidth(),
                AndroidUtilities.getScreenHeight(),
                patternColor
            )

            imageView.setImageBitmap(svgBitmap)
        } else {
            imageView.setImageBitmap(null)
        }

    }


    /**
     * 监听
     */
    private fun startScreenShotListen(iConversationContext: IConversationContext) {
        screenShotListenManager =
            ScreenShotListenManager.newInstance(iConversationContext.chatActivity)
        if (screenShotListenManager != null) {
            screenShotListenManager!!.setListener {
                if (!iConversationContext.isShowChatActivity)
                    return@setListener
                val channel = iConversationContext.chatChannelInfo
                var screenshot = 0
                if (channel?.remoteExtraMap != null && channel.remoteExtraMap.containsKey(
                        BageChannelExtras.screenshot
                    )
                ) {
                    val `object` =
                        channel.remoteExtraMap[BageChannelExtras.screenshot]
                    if (`object` != null) screenshot = `object` as Int
                }
                if (screenshot == 1) {
                    val screenshotContent = ScreenshotContent()
                    screenshotContent.fromuid = BageConfig.getInstance().uid
                    screenshotContent.fromname = BageConfig.getInstance().userName
                    iConversationContext.sendMessage(screenshotContent)
                }
            }
            screenShotListenManager!!.startListen()
        }

    }


    private fun stopScreenShotListen() {
        if (screenShotListenManager != null) {
            screenShotListenManager!!.stopListen()
            screenShotListenManager = null
        }
    }

    private fun getMsgItemRemindView(chatSettingCellMenu: ChatSettingCellMenu): View {
        val context = chatSettingCellMenu.parentLayout.context
        val channelID = chatSettingCellMenu.channelID
        val channelType = chatSettingCellMenu.channelType
        val view = LayoutInflater.from(context)
            .inflate(R.layout.msg_item_remind_layout, chatSettingCellMenu.parentLayout, false)
        val remindLayout = view.findViewById<View>(R.id.remindLayout)
        SingleClickUtil.onSingleClick(remindLayout) {
            val intent = Intent(context, MsgRemindActivity::class.java)
            intent.putExtra("channelID", channelID)
            intent.putExtra("channelType", channelType)
            context.startActivity(intent)
        }
        val chatBgLayout = view.findViewById<View>(R.id.chatBgLayout)
        chatBgLayout.visibility = View.VISIBLE
        SingleClickUtil.onSingleClick(chatBgLayout) {
            val intent = Intent(context, ChatBgListActivity::class.java)
            intent.putExtra("channelID", channelID)
            intent.putExtra("channelType", channelType)
            context.startActivity(intent)
        }
        return view
    }

    private fun getFindMsgView(chatSettingCellMenu: ChatSettingCellMenu): View {
        val context = chatSettingCellMenu.parentLayout.context
        val channelID = chatSettingCellMenu.channelID
        val channelType = chatSettingCellMenu.channelType
        val view = LayoutInflater.from(context)
            .inflate(R.layout.msg_item_remind_layout, chatSettingCellMenu.parentLayout, false)
        val textView = view.findViewById<TextView>(R.id.titleCenterTv)
        textView.setText(R.string.str_find_chat_content)
        SingleClickUtil.onSingleClick(view) {
            val intent = Intent(context, RecordActivity::class.java)
            intent.putExtra("channel_id", channelID)
            intent.putExtra("channel_type", channelType)
            context.startActivity(intent)
        }
        return view
    }

    private fun getMsgReceiptView(chatSettingCellMenu: ChatSettingCellMenu): View {
        val context = chatSettingCellMenu.parentLayout.context
        val channelID = chatSettingCellMenu.channelID
        val channelType = chatSettingCellMenu.channelType
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_msg_receipt_layout, chatSettingCellMenu.parentLayout, false)
        val switchView = view.findViewById<SwitchView>(R.id.receiptSwitchView)
        Theme.applyAccentSwitchStyle(context, switchView)
//        var seekBarView: SeekBarView? = null

        val channel = BageIM.getInstance().channelManager.getChannel(
            channelID,
            channelType
        )
        if (channel != null) {
            switchView.isChecked = channel.receipt == 1
        }
        switchView.setOnCheckedChangeListener { buttonView, isChecked ->
            run {
                if (buttonView.isPressed) {
                    if (channelType == BageChannelType.PERSONAL) {
                        AdvancedModel.instance
                            .updateUserSetting(
                                channelID, "receipt", if (isChecked) 1 else 0
                            ) { code: Int, msg: String? ->
                                if (code != HttpResponseCode.success.toInt()) {
                                    switchView.isChecked = !isChecked
                                    BageToastUtils.getInstance().showToast(msg)
                                }
                            }
                    }
                } else {
                    AdvancedModel.instance.updateGroupSetting(
                        channelID,
                        "receipt",
                        if (isChecked) 1 else 0
                    ) { code: Int, msg: String? ->
                        if (code != HttpResponseCode.success.toInt()) {
                            switchView.isChecked = !isChecked
                            BageToastUtils.getInstance().showToast(msg)
                        }
                    }
                }
            }
        }
        return view
    }

    fun editImg(path: String, context: Context) {
        if (path.startsWith("http") || path.startsWith("HTTP")) {
            Glide.with(context)
                .asBitmap()
                .load(path)
                .into(object : CustomTarget<Bitmap?>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        ImageUtils.getInstance().saveBitmap(
                            context, resource, true
                        ) { path -> gotEdit(context, path) }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        } else {
            gotEdit(context, path)
        }
    }

    private fun gotEdit(context: Context, path: String) {
        EndpointManager.getInstance().invoke("edit_img", EditImgMenu(
            context, true, path, null, -1
        ) { _: Bitmap?, _: String? -> })
    }

    private val reactionStickers: ArrayList<ReactionSticker> = ArrayList()
    private fun initReactionSticker() {
        reactionStickers.add(ReactionSticker("like", R.raw.like_small))
        reactionStickers.add(ReactionSticker("bad", R.raw.bad_small))
        reactionStickers.add(ReactionSticker("love", R.raw.love_small))
        reactionStickers.add(ReactionSticker("fire", R.raw.fire_small))
        reactionStickers.add(ReactionSticker("celebrate", R.raw.celebrate_small))
        reactionStickers.add(ReactionSticker("happy", R.raw.happy_small))
        reactionStickers.add(ReactionSticker("haha", R.raw.haha_small))
        reactionStickers.add(ReactionSticker("terrified", R.raw.terrified_small))
        reactionStickers.add(ReactionSticker("depressed", R.raw.depressed_small))
        reactionStickers.add(ReactionSticker("shit", R.raw.shit_small))
        reactionStickers.add(ReactionSticker("vomit", R.raw.vomit_small))
    }

    private fun getChatBgView(
        menu: ChatBgItemMenu
    ) {
        val view =
            LayoutInflater.from(menu.activity)
                .inflate(R.layout.chat_bg_layout, menu.parentView, false)
        val chatBgLayout = view.findViewById<View>(R.id.chatBgLayout)
        SingleClickUtil.onSingleClick(chatBgLayout) {
            val intent = Intent(menu.activity, ChatBgListActivity::class.java)
            intent.putExtra("channelID", menu.channelID)
            intent.putExtra("channelType", menu.channelType)
            menu.activity.startActivity(intent)
        }
        menu.parentView.removeAllViews()
        menu.parentView.addView(view)
    }

    private fun getOtherLoginView(context: Context, parentView: ViewGroup): View {
        val view =
            LayoutInflater.from(context)
                .inflate(R.layout.other_login_type_layout, parentView, false)
        val wxLoginLayout =
            view.findViewById<View>(R.id.wxLoginLayout)
        SingleClickUtil.onSingleClick(wxLoginLayout) {
        }
        view.findViewById<View>(R.id.phoneLoginLayout).setOnClickListener {
        }
        parentView.removeAllViews()
        parentView.addView(view)
        return view
    }

}