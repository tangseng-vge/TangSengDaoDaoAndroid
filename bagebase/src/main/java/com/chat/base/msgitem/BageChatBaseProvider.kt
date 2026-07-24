package com.chat.base.msgitem

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.MeasureSpec
import android.view.View.OnTouchListener
import android.view.View.TRANSLATION_X
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.R
import com.chat.base.BageBaseApplication
import com.chat.base.config.BageConfig
import com.chat.base.config.BageConstants
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.CanReactionMenu
import com.chat.base.endpoint.entity.ChatChooseContacts
import com.chat.base.endpoint.entity.ChatItemPopupMenu
import com.chat.base.endpoint.entity.ChooseChatMenu
import com.chat.base.endpoint.entity.MsgConfig
import com.chat.base.endpoint.entity.MsgReactionMenu
import com.chat.base.endpoint.entity.PrivacyMessageMenu
import com.chat.base.endpoint.entity.ReadMsgDetailMenu
import com.chat.base.endpoint.entity.ShowMsgReactionMenu
import com.chat.base.endpoint.entity.WithdrawMsgMenu
import com.chat.base.entity.PopupMenuItem
import com.chat.base.msg.ChatAdapter
import com.chat.base.ui.Theme
import com.chat.base.ui.components.ActionBarMenuSubItem
import com.chat.base.ui.components.ActionBarPopupWindow
import com.chat.base.ui.components.ActionBarPopupWindow.ActionBarPopupWindowLayout
import com.chat.base.ui.components.AvatarView
import com.chat.base.ui.components.ChatScrimPopupContainerLayout
import com.chat.base.ui.components.CheckBox
import com.chat.base.ui.components.PopupSwipeBackLayout
import com.chat.base.ui.components.ReactionsContainerLayout
import com.chat.base.ui.components.ReactionsContainerLayout.ReactionsContainerDelegate
import com.chat.base.ui.components.SecretDeleteTimer
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.LayoutHelper
import com.chat.base.utils.StringUtils
import com.chat.base.utils.BageDialogUtils
import com.chat.base.utils.BageTimeUtils
import com.chat.base.utils.BageToastUtils
import com.chat.base.views.ChatItemView
import com.google.android.material.snackbar.Snackbar
import com.bage.im.BageIM
import com.bage.im.entity.BageChannel
import com.bage.im.entity.BageChannelType
import com.bage.im.entity.BageMsg
import com.bage.im.entity.BageSendOptions
import com.bage.im.message.type.BageSendMsgResult
import com.bage.im.msgmodel.BageVoiceContent
import org.telegram.ui.Components.RLottieDrawable
import org.telegram.ui.Components.RLottieImageView
import java.util.Objects
import kotlin.math.abs
import kotlin.math.max
import androidx.core.view.isVisible


abstract class BageChatBaseProvider : BaseItemProvider<BageUIChatMsgItemEntity>() {

    override val layoutId: Int
        get() = R.layout.chat_item_base_layout

    override fun convert(helper: BaseViewHolder, item: BageUIChatMsgItemEntity, payloads: List<Any>) {
        super.convert(helper, item, payloads)
        val msgItemEntity = payloads[0] as BageUIChatMsgItemEntity
        val from = getMsgFromType(msgItemEntity.bageMsg)

        if (msgItemEntity.isRefreshReaction && helper.getViewOrNull<AvatarView>(R.id.avatarView) != null) {
            msgItemEntity.isRefreshReaction = false
            val avatarView = helper.getView<AvatarView>(R.id.avatarView)
            setAvatarLayoutParams(msgItemEntity, from, avatarView)
            EndpointManager.getInstance().invoke(
                "show_msg_reaction", ShowMsgReactionMenu(
                    helper.getView(R.id.reactionsView),
                    from,
                    (Objects.requireNonNull(getAdapter()) as ChatAdapter),
                    msgItemEntity.bageMsg.reactionList
                )
            )
        }
        if (msgItemEntity.isRefreshAvatarAndName && helper.getViewOrNull<AvatarView>(R.id.avatarView) != null) {
            val avatarView = helper.getView<AvatarView>(R.id.avatarView)
            setAvatar(msgItemEntity, avatarView)
            if (helper.getViewOrNull<View>(R.id.receivedNameTv) != null && msgItemEntity.bageMsg.type != BageContentType.Bage_TEXT && msgItemEntity.bageMsg.type != BageContentType.typing && msgItemEntity.bageMsg.type != BageContentType.richText) {
                setFromName(msgItemEntity, from, helper.getView(R.id.receivedNameTv))
            } else {
                if (helper.getViewOrNull<View>(R.id.bageBaseContentLayout) != null) {
                    val baseView = helper.getView<LinearLayout>(R.id.bageBaseContentLayout)
                    resetFromName(helper.bindingAdapterPosition, baseView, msgItemEntity, from)

                }
            }
            msgItemEntity.isRefreshReaction = false
        }
        if (helper.getViewOrNull<CheckBox>(R.id.checkBox) != null) {
            setCheckBox(
                msgItemEntity,
                from,
                helper.getView(R.id.checkBox),
                helper.getView(R.id.viewContentLayout)
            )
            if (helper.getViewOrNull<View>(R.id.viewGroupLayout) != null) {
                val viewGroupLayout = helper.getView<ChatItemView>(R.id.viewGroupLayout)
                viewGroupLayout.setTouchData(
                    !msgItemEntity.isChoose
                ) { setChoose(helper, msgItemEntity) }
            }
        }
    }

    override fun convert(helper: BaseViewHolder, item: BageUIChatMsgItemEntity) {
        showData(helper, item)
    }

    open fun refreshReply(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {

    }

    protected abstract fun getChatViewItem(
        parentView: ViewGroup,
        from: BageChatIteMsgFromType
    ): View?

    protected abstract fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    )

    fun refreshData(
        adapterPosition: Int,
        parentView: View,
        content: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        setData(adapterPosition, parentView, content, from)
    }

    open fun resetCellBackground(
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {

    }

    open fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
    }

    open fun resetFromName(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
    }

    open fun getMsgFromType(bageMsg: BageMsg?): BageChatIteMsgFromType {
        val from: BageChatIteMsgFromType = if (bageMsg != null) {
            if (!TextUtils.isEmpty(bageMsg.fromUID)
                && bageMsg.fromUID == BageConfig.getInstance().uid
            ) {
                BageChatIteMsgFromType.SEND //自己发送的
            } else {
                BageChatIteMsgFromType.RECEIVED //他人发的
            }
        } else {
            BageChatIteMsgFromType.SYSTEM //系统
        }
        return from
    }

    private fun showData(
        baseViewHolder: BaseViewHolder,
        msgItemEntity: BageUIChatMsgItemEntity
    ) {
        if (baseViewHolder.getViewOrNull<View>(R.id.viewGroupLayout) != null) {
            val viewGroupLayout = baseViewHolder.getView<ChatItemView>(R.id.viewGroupLayout)
            // 提示本条消息
            if (msgItemEntity.isShowTips) {
                val colorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    ContextCompat.getColor(context, R.color.tip_message_cell_bg),
                    ContextCompat.getColor(context, R.color.transparent)
                )
                colorAnimation.setDuration(2500)
                colorAnimation.addUpdateListener { animator ->
                    viewGroupLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                msgItemEntity.isShowTips = false
            }
            setItemPadding(baseViewHolder.bindingAdapterPosition, viewGroupLayout)
            viewGroupLayout.setOnClickListener {
                setChoose(
                    baseViewHolder,
                    msgItemEntity
                )
            }
            viewGroupLayout.setTouchData(
                !msgItemEntity.isChoose
            ) { setChoose(baseViewHolder, msgItemEntity) }
        }
        if (baseViewHolder.getViewOrNull<View>(R.id.bageBaseContentLayout) != null) {
            val fullContentLayout = baseViewHolder.getView<LinearLayout>(R.id.fullContentLayout)
            val baseView = baseViewHolder.getView<LinearLayout>(R.id.bageBaseContentLayout)
            val avatarView = baseViewHolder.getView<AvatarView>(R.id.avatarView)
            val from = getMsgFromType(msgItemEntity.bageMsg)

            // deleteTimer.invalidate()
//            val deleteTimerLP = deleteTimer.layoutParams as RelativeLayout.LayoutParams
            baseView.removeAllViews()
            baseView.addView(getChatViewItem(baseView, from))
            baseViewHolder.getView<View>(R.id.viewContentLayout).setOnClickListener {
                val chatAdapter = getAdapter() as ChatAdapter
                chatAdapter.conversationContext.hideSoftKeyboard()
            }
            setFullLayoutParams(msgItemEntity, from, fullContentLayout)
            setAvatarLayoutParams(msgItemEntity, from, avatarView)
            resetCellBackground(baseView, msgItemEntity, from)
            resetCellListener(
                baseViewHolder.bindingAdapterPosition,
                baseView,
                msgItemEntity,
                from
            )

            if (baseViewHolder.getViewOrNull<CheckBox>(R.id.checkBox) != null) {
                setCheckBox(
                    msgItemEntity,
                    from,
                    baseViewHolder.getView(R.id.checkBox),
                    baseViewHolder.getView(R.id.viewContentLayout)
                )
            }

            if (isAddFlameView(msgItemEntity)) {
                val deleteTimer = SecretDeleteTimer(context)
                deleteTimer.setSize(25)
                val flameSecond: Int =
                    if (msgItemEntity.bageMsg.type == BageContentType.Bage_VOICE) {
                        val voiceContent =
                            msgItemEntity.bageMsg.baseContentMsgModel as BageVoiceContent
                        max(voiceContent.timeTrad, msgItemEntity.bageMsg.flameSecond)
                    } else {
                        msgItemEntity.bageMsg.flameSecond
                    }

                deleteTimer.setDestroyTime(
                    msgItemEntity.bageMsg.clientMsgNO,
                    flameSecond,
                    msgItemEntity.bageMsg.viewedAt,
                    false
                )
                if (from == BageChatIteMsgFromType.RECEIVED) {
                    baseView.addView(
                        deleteTimer,
                        LayoutHelper.createLinear(
                            25,
                            25,
                            Gravity.CENTER or Gravity.BOTTOM,
                            5,
                            0,
                            0,
                            0
                        )
                    )
                } else {
                    baseView.addView(
                        deleteTimer,
                        0,
                        LayoutHelper.createLinear(
                            25,
                            25,
                            Gravity.CENTER or Gravity.BOTTOM,
                            0,
                            0,
                            5,
                            0
                        )
                    )
                }
                if (msgItemEntity.bageMsg.viewed == 0) {
                    deleteTimer.visibility = INVISIBLE
                } else deleteTimer.visibility = VISIBLE
            }

            if (msgItemEntity.isShowPinnedMessage) {
                val openMessageFrameLayout = FrameLayout(context)
                openMessageFrameLayout.background =
                    ContextCompat.getDrawable(context, R.drawable.shape_corner_rectangle)
                val openMessageImageView = AppCompatImageView(context)
                openMessageImageView.setImageResource(R.mipmap.filled_open_message)
                openMessageFrameLayout.addView(
                    openMessageImageView,
                    LayoutHelper.createFrame(25, 25, Gravity.CENTER)
                )
                if (from == BageChatIteMsgFromType.RECEIVED) {
                    baseView.addView(
                        openMessageFrameLayout,
                        LayoutHelper.createLinear(
                            30,
                            30,
                            Gravity.CENTER or Gravity.BOTTOM,
                            5,
                            0,
                            0,
                            0
                        )
                    )
                } else {
                    baseView.addView(
                        openMessageFrameLayout,
                        0,
                        LayoutHelper.createLinear(
                            30,
                            30,
                            Gravity.CENTER or Gravity.BOTTOM,
                            0,
                            0,
                            5,
                            0
                        )
                    )

                }
                openMessageFrameLayout.setOnClickListener {
                    EndpointManager.getInstance()
                        .invoke("tip_msg_in_chat", msgItemEntity.bageMsg.clientMsgNO)
                    val chatAdapter = getAdapter() as ChatAdapter
                    chatAdapter.conversationContext.closeActivity()
                }
            }


            setData(baseViewHolder.bindingAdapterPosition, baseView, msgItemEntity, from)
            if (baseViewHolder.getViewOrNull<View>(R.id.receivedNameTv) != null && msgItemEntity.bageMsg.type != BageContentType.Bage_TEXT && msgItemEntity.bageMsg.type != BageContentType.typing && msgItemEntity.bageMsg.type != BageContentType.richText) {
                setFromName(msgItemEntity, from, baseViewHolder.getView(R.id.receivedNameTv))
            }
            setMsgTimeAndStatus(
                msgItemEntity,
                baseView,
                from
            )
            EndpointManager.getInstance().invoke(
                "show_msg_reaction", ShowMsgReactionMenu(
                    baseViewHolder.getView(R.id.reactionsView),
                    from,
                    (Objects.requireNonNull(getAdapter()) as ChatAdapter),
                    msgItemEntity.bageMsg.reactionList
                )
            )
            msgItemEntity.isUpdateStatus = false
        }
    }

    // 获取消息显示背景类型
    protected open fun getMsgBgType(
        previousMsg: BageMsg?,
        nowMsg: BageMsg,
        nextMsg: BageMsg?
    ): BageMsgBgType {
        val bgType: BageMsgBgType
        var previousBubble = false
        var nextBubble = false
        val previousIsSystem = previousMsg != null && BageContentType.isSystemMsg(previousMsg.type)
        val nextIsSystem = nextMsg != null && BageContentType.isSystemMsg(nextMsg.type)
        if (previousMsg != null && previousMsg.remoteExtra.revoke == 0 && previousMsg.isDeleted == 0 && !previousIsSystem
            && !TextUtils.isEmpty(previousMsg.fromUID)
            && previousMsg.fromUID == nowMsg.fromUID
        ) {
            previousBubble = true
        }
        if (nextMsg != null && nextMsg.remoteExtra.revoke == 0 && nextMsg.isDeleted == 0 && !nextIsSystem
            && !TextUtils.isEmpty(nextMsg.fromUID)
            && nextMsg.fromUID == nowMsg.fromUID
        ) {
            nextBubble = true
        }
        bgType = if (previousBubble) {
            if (nextBubble) {
                BageMsgBgType.center
            } else {
                BageMsgBgType.bottom
            }
        } else {
            if (nextBubble) {
                BageMsgBgType.top
            } else BageMsgBgType.single
        }
        return bgType
    }

    protected open fun isShowAvatar(nowMsg: BageMsg?, nextMsg: BageMsg?): Boolean {
        return shouldShowMessageAvatar(nowMsg)
    }

    protected open fun shouldShowMessageAvatar(msg: BageMsg?): Boolean {
        if (msg == null) return false
        if (msg.remoteExtra != null && msg.remoteExtra.revoke == 1) return false
        if (BageContentType.isSystemMsg(msg.type)) return false
        when (msg.type) {
            BageContentType.loading,
            BageContentType.emptyView,
            BageContentType.spanEmptyView,
            BageContentType.msgPromptTime,
            BageContentType.msgPromptNewMsg,
            BageContentType.noRelation,
            BageContentType.sensitiveWordsTips,
            BageContentType.revoke -> return false
        }
        return true
    }

    private fun setChoose(
        baseViewHolder: BaseViewHolder,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity
    ) {
        if (uiChatMsgItemEntity.bageMsg.flame == 1) {
            uiChatMsgItemEntity.isChoose = false
        }
        if (uiChatMsgItemEntity.isChoose) {
            var count = 0
            var i = 0
            val size = getAdapter()!!.itemCount
            while (i < size) {
                if (getAdapter()!!.data[i].isChecked) {
                    count++
                }
                i++
            }
            if (count == 100) {
                BageToastUtils.getInstance()
                    .showToastNormal(context.getString(R.string.max_choose_msg_count))
                return
            }
            uiChatMsgItemEntity.isChecked = !uiChatMsgItemEntity.isChecked
            val checkBox = baseViewHolder.getView<CheckBox>(R.id.checkBox)
            checkBox.setChecked(uiChatMsgItemEntity.isChecked, true)
            if (uiChatMsgItemEntity.isChecked) {
                count++
            } else count--
            (getAdapter() as ChatAdapter?)!!.showTitleRightText(count.toString())
        }
    }

    protected fun setFromName(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType, receivedNameTv: TextView
    ) {
        val bgType: BageMsgBgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.bageMsg,
            uiChatMsgItemEntity.nextMsg
        )
        if (uiChatMsgItemEntity.bageMsg.channelType == BageChannelType.GROUP) {
            var showName: String? = ""
            receivedNameTv.tag = uiChatMsgItemEntity.bageMsg.fromUID
            if (uiChatMsgItemEntity.bageMsg.from != null && !TextUtils.isEmpty(uiChatMsgItemEntity.bageMsg.from.channelRemark)) {
                showName = uiChatMsgItemEntity.bageMsg.from.channelRemark
            }
            if (TextUtils.isEmpty(showName)) {
                if (uiChatMsgItemEntity.bageMsg.memberOfFrom != null) {
                    showName = uiChatMsgItemEntity.bageMsg.memberOfFrom.remark
                    if (TextUtils.isEmpty(showName)) {
                        showName = uiChatMsgItemEntity.bageMsg.memberOfFrom.memberRemark
                    }
                }
                if (TextUtils.isEmpty(showName) && uiChatMsgItemEntity.bageMsg.from != null) {
                    showName = uiChatMsgItemEntity.bageMsg.from.channelName
                }
                if (TextUtils.isEmpty(showName) && uiChatMsgItemEntity.bageMsg.memberOfFrom != null) {
                    showName = uiChatMsgItemEntity.bageMsg.memberOfFrom.memberName
                }
            }
            val os = getMsgOS(uiChatMsgItemEntity.bageMsg.clientMsgNO)
            if (receivedNameTv.tag is String && receivedNameTv.tag == uiChatMsgItemEntity.bageMsg.fromUID) {
                if (uiChatMsgItemEntity.bageMsg.type == BageContentType.typing) {
                    receivedNameTv.text = showName
                } else {
                    receivedNameTv.text = String.format("%s/%s", showName, os)
                }
            }


            if (!TextUtils.isEmpty(uiChatMsgItemEntity.bageMsg.fromUID)) {
                val colors =
                    BageBaseApplication.getInstance().context.resources.getIntArray(R.array.name_colors)
                val index =
                    abs(uiChatMsgItemEntity.bageMsg.fromUID.hashCode()) % colors.size
                receivedNameTv.setTextColor(colors[index])
            }
            if (from == BageChatIteMsgFromType.RECEIVED) {
                val showNickName = uiChatMsgItemEntity.showNickName
                if (showNickName && (bgType == BageMsgBgType.single || bgType == BageMsgBgType.top)) {
                    receivedNameTv.visibility = VISIBLE
                } else receivedNameTv.visibility = GONE
            } else {
                receivedNameTv.visibility = GONE
            }
        } else receivedNameTv.visibility = GONE

    }

    private fun setCheckBox(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType,
        checkBox: CheckBox,
        viewContentLayout: View
    ) {
        if (uiChatMsgItemEntity.isChoose) {
            val cbAnimator: Animator =
                ObjectAnimator.ofFloat(checkBox, TRANSLATION_X, 120f)
            val animator: Animator = ObjectAnimator.ofFloat(
                viewContentLayout,
                TRANSLATION_X,
                if (from == BageChatIteMsgFromType.RECEIVED) 120f else 0f
            )
            val animatorSet = AnimatorSet()
            animatorSet.play(animator).with(cbAnimator)
            animatorSet.duration = 200
            animatorSet.interpolator = DecelerateInterpolator()
            animatorSet.start()
        } else {
            if (checkBox.isVisible) {
                val cbAnimator: Animator =
                    ObjectAnimator.ofFloat(checkBox, TRANSLATION_X, 0f)
                val animator: Animator =
                    ObjectAnimator.ofFloat(viewContentLayout, TRANSLATION_X, 0f)
                val animatorSet = AnimatorSet()
                animatorSet.duration = 250
                animatorSet.play(animator).with(cbAnimator)
                animatorSet.interpolator = DecelerateInterpolator()
                animatorSet.start()
            }
        }
        checkBox.setResId(context, R.mipmap.round_check2)
        checkBox.setDrawBackground(true)
        checkBox.setHasBorder(true)
        checkBox.setBorderColor(ContextCompat.getColor(context, R.color.white))
        checkBox.setSize(24)
        checkBox.setStrokeWidth(AndroidUtilities.dp(2f))
        //            checkBox.setCheckOffset(AndroidUtilities.dp(2));
        checkBox.setColor(
            Theme.colorAccount,
            ContextCompat.getColor(context, R.color.white)
        )
        if (uiChatMsgItemEntity.bageMsg.flame == 1) checkBox.visibility = INVISIBLE
        else
            checkBox.visibility = VISIBLE
        checkBox.setChecked(uiChatMsgItemEntity.isChecked, true)
    }

    fun setAvatarLayoutParams(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType, avatarView: AvatarView
    ) {
        avatarView.setSize(40f)
        val layoutParams = avatarView.layoutParams as FrameLayout.LayoutParams
        if (uiChatMsgItemEntity.bageMsg.reactionList != null && uiChatMsgItemEntity.bageMsg.reactionList.isNotEmpty()) {
            // 向下的距离是回应数据的高度+阴影高度-回应向上的距离
            layoutParams.bottomMargin = AndroidUtilities.dp(24f)
        } else layoutParams.bottomMargin = 0

        layoutParams.gravity =
            if (from == BageChatIteMsgFromType.RECEIVED) Gravity.START or Gravity.BOTTOM else Gravity.END or Gravity.BOTTOM
//        if (from == BageChatIteMsgFromType.RECEIVED) {
//            layoutParams.leftMargin = AndroidUtilities.dp(10f)
//            layoutParams.rightMargin = AndroidUtilities.dp(10f)
//        }
        avatarView.layoutParams = layoutParams
        avatarView.setOnClickListener {
            val adapter = getAdapter() as ChatAdapter
            adapter.conversationContext.onChatAvatarClick(uiChatMsgItemEntity.bageMsg.fromUID, false)
        }
        avatarView.setOnLongClickListener {
            val adapter = getAdapter() as ChatAdapter
            adapter.conversationContext.onChatAvatarClick(uiChatMsgItemEntity.bageMsg.fromUID, true)
            true
        }
        if (shouldShowMessageAvatar(uiChatMsgItemEntity.bageMsg)) {
            avatarView.visibility = VISIBLE
            setAvatar(uiChatMsgItemEntity, avatarView)
        } else {
            avatarView.visibility = GONE
        }
    }

    fun setFullLayoutParams(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType,
        fullContentLayout: LinearLayout
    ) {
        val fullContentLayoutParams = fullContentLayout.layoutParams as FrameLayout.LayoutParams
        var isBubble = false
        val list: List<Boolean>? = EndpointManager.getInstance()
            .invokes(EndpointCategory.chatShowBubble, uiChatMsgItemEntity.bageMsg.type)
        if (!list.isNullOrEmpty()) {
            for (b in list) {
                if (b) {
                    isBubble = true
                    break
                }
            }
        }
        if (uiChatMsgItemEntity.bageMsg.type == BageContentType.Bage_TEXT
            || uiChatMsgItemEntity.bageMsg.type == BageContentType.Bage_CARD
            || uiChatMsgItemEntity.bageMsg.type == BageContentType.Bage_VOICE
            || uiChatMsgItemEntity.bageMsg.type == BageContentType.Bage_MULTIPLE_FORWARD
            || uiChatMsgItemEntity.bageMsg.type == BageContentType.unknown_msg
            || uiChatMsgItemEntity.bageMsg.type == BageContentType.Bage_CONTENT_FORMAT_ERROR
            || uiChatMsgItemEntity.bageMsg.type == BageContentType.typing
        ) {
            isBubble = true
        }
        val itemProvider = if (!uiChatMsgItemEntity.isShowPinnedMessage)
            BageMsgItemViewManager.getInstance()
                .getItemProvider(uiChatMsgItemEntity.bageMsg.type) else BageMsgItemViewManager.getInstance()
            .getPinnedItemProvider(uiChatMsgItemEntity.bageMsg.type)
        if (itemProvider == null) {
            isBubble = true
        }
        var margin = 10f
        if (isBubble) margin = 0f
        val showAvatar = shouldShowMessageAvatar(uiChatMsgItemEntity.bageMsg)
        val avatarSpace = if (showAvatar) 50f else 10f
        if (from == BageChatIteMsgFromType.SEND) {
            fullContentLayoutParams.gravity = Gravity.END
            fullContentLayoutParams.rightMargin = AndroidUtilities.dp(avatarSpace + margin)
            fullContentLayoutParams.leftMargin = AndroidUtilities.dp(55f)
        } else {
            fullContentLayoutParams.gravity = Gravity.START
            fullContentLayoutParams.leftMargin = AndroidUtilities.dp(avatarSpace + margin)
            fullContentLayoutParams.rightMargin = AndroidUtilities.dp(55f)
        }
        fullContentLayout.layoutParams = fullContentLayoutParams
    }

    open fun setAvatar(uiChatMsgItemEntity: BageUIChatMsgItemEntity, avatarView: AvatarView) {

        if (uiChatMsgItemEntity.bageMsg.from != null) {
            avatarView.showAvatar(uiChatMsgItemEntity.bageMsg.from)
        } else {
            BageIM.getInstance().channelManager.fetchChannelInfo(
                uiChatMsgItemEntity.bageMsg.fromUID,
                BageChannelType.PERSONAL
            )
            avatarView.showAvatar(
                uiChatMsgItemEntity.bageMsg.fromUID,
                BageChannelType.PERSONAL,
                false
            )
        }

    }

    open fun setMsgTimeAndStatus(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        parentView: View,
        fromType: BageChatIteMsgFromType,
    ) {
        val mMsg = uiChatMsgItemEntity.bageMsg
        val isPlayAnimation = uiChatMsgItemEntity.isUpdateStatus
        val msgTimeTv = parentView.findViewById<TextView>(R.id.msgTimeTv)
        val editedTv = parentView.findViewById<TextView>(R.id.editedTv)
        val statusIV = parentView.findViewById<RLottieImageView>(R.id.statusIV)
        val pinIV = parentView.findViewById<AppCompatImageView>(R.id.pinIV)
        if (msgTimeTv == null || mMsg == null) return
        var msgTime = mMsg.timestamp
        if (mMsg.remoteExtra != null) {
            if (mMsg.remoteExtra.editedAt != 0L) {
                msgTime = mMsg.remoteExtra.editedAt
                editedTv.visibility = VISIBLE
            } else {
                editedTv.visibility = GONE
            }
        } else {
            editedTv.visibility = GONE
        }
        pinIV.visibility = if (uiChatMsgItemEntity.isPinned == 1) VISIBLE else GONE
        val timeSpace = BageTimeUtils.getInstance().getTimeSpace(msgTime * 1000)
        val time = BageTimeUtils.getInstance().time2HourStr(msgTime * 1000)
        if (!BageTimeUtils.getInstance().is24Hour) msgTimeTv.text =
            String.format("%s %s", timeSpace, time) else msgTimeTv.text =
            String.format("%s", time)
        msgTimeTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
        editedTv?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
        val isShowNormalColor: Boolean
        val drawable: RLottieDrawable
        var autoRepeat = false
        val whiteColor = ContextCompat.getColor(context, R.color.white)
        val normalColor = ContextCompat.getColor(context, R.color.color999)
        if (fromType == BageChatIteMsgFromType.SEND) {
            isShowNormalColor = false
            msgTimeTv.setTextColor(whiteColor)
            editedTv?.setTextColor(whiteColor)
        } else if (mMsg.type == BageContentType.Bage_IMAGE || mMsg.type == BageContentType.Bage_GIF || mMsg.type == BageContentType.Bage_VIDEO || mMsg.type == BageContentType.Bage_VECTOR_STICKER || mMsg.type == BageContentType.Bage_EMOJI_STICKER || mMsg.type == BageContentType.Bage_LOCATION) {
            isShowNormalColor = false
            msgTimeTv.setTextColor(whiteColor)
            editedTv?.setTextColor(whiteColor)
        } else {
            isShowNormalColor = true
            msgTimeTv.setTextColor(normalColor)
            editedTv?.setTextColor(normalColor)
        }
        pinIV.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(
                    context,
                    if (isShowNormalColor) R.color.color999 else R.color.white
                ), PorterDuff.Mode.MULTIPLY
            )
        if (mMsg.remoteExtra.needUpload == 1) mMsg.status = BageSendMsgResult.send_loading
        if (fromType == BageChatIteMsgFromType.SEND) {
            if (mMsg.setting.receipt == 1 && mMsg.remoteExtra.readedCount > 0) {
                drawable = RLottieDrawable(
                    context,
                    R.raw.ticks_double,
                    "ticks_double",
                    AndroidUtilities.dp(18f),
                    AndroidUtilities.dp(18f)
                )
            } else {
                when (mMsg.status) {
                    BageSendMsgResult.send_success -> {
                        drawable = RLottieDrawable(
                            context,
                            R.raw.ticks_single,
                            "ticks_single",
                            AndroidUtilities.dp(18f),
                            AndroidUtilities.dp(18f)
                        )
                    }

                    BageSendMsgResult.send_loading -> {
                        autoRepeat = true
                        drawable = RLottieDrawable(
                            context,
                            R.raw.msg_sending,
                            "msg_sending",
                            AndroidUtilities.dp(18f),
                            AndroidUtilities.dp(18f)
                        )
                    }

                    else -> {
                        drawable = RLottieDrawable(
                            context,
                            R.raw.error,
                            "error",
                            AndroidUtilities.dp(18f),
                            AndroidUtilities.dp(18f)
                        )
                        statusIV.setOnClickListener {

                            if (mMsg.status == BageSendMsgResult.send_success) return@setOnClickListener
                            if (!canResendMsg(mMsg.channelID, mMsg.channelType)) {
                                BageToastUtils.getInstance()
                                    .showToastNormal(context.getString(R.string.forbidden_can_not_resend))
                                return@setOnClickListener
                            }
                            var content = context.getString(R.string.str_resend_msg_tips)
                            when (mMsg.status) {
                                BageSendMsgResult.send_fail -> {
                                    content = context.getString(R.string.str_resend_msg_tips)
                                }

                                BageSendMsgResult.no_relation -> {
                                    content = context.getString(R.string.no_relation_group)
                                }

                                BageSendMsgResult.black_list -> {
                                    content =
                                        context.getString(if (mMsg.channelType == BageChannelType.GROUP) R.string.blacklist_group else R.string.blacklist_user)
                                }

                                BageSendMsgResult.not_on_white_list -> {
                                    content = context.getString(R.string.no_relation_user)
                                }
                            }
                            BageDialogUtils.getInstance().showDialog(
                                context,
                                context.getString(R.string.msg_send_fail),
                                content,
                                true,
                                "",
                                context.getString(R.string.msg_send_fail_resend),
                                0,
                                Theme.colorAccount,
                            ) { index: Int ->
                                if (index == 1) {
                                    val mMsg1 =
                                        BageMsg()
                                    mMsg1.channelID = mMsg.channelID
                                    mMsg1.channelType = mMsg.channelType
                                    mMsg1.setting = mMsg.setting
                                    mMsg1.header = mMsg.header
                                    mMsg1.type = mMsg.type
                                    mMsg1.content = mMsg.content
                                    mMsg1.baseContentMsgModel = mMsg.baseContentMsgModel
                                    mMsg1.fromUID = BageConfig.getInstance().uid
                                    BageIM.getInstance().msgManager.sendMessage(mMsg1)
                                    BageIM.getInstance().msgManager
                                        .deleteWithClientMsgNO(mMsg.clientMsgNO)
                                }
                            }
                        }
                    }
                }
            }
            if (mMsg.status <= BageSendMsgResult.send_success) {
                statusIV.colorFilter =
                    PorterDuffColorFilter(
                        ContextCompat.getColor(
                            context,
                            if (isShowNormalColor) R.color.color999 else R.color.white
                        ), PorterDuff.Mode.MULTIPLY
                    )
            } else {
                statusIV.colorFilter =
                    PorterDuffColorFilter(
                        ContextCompat.getColor(
                            context, R.color.white
                        ), PorterDuff.Mode.MULTIPLY
                    )
            }
            statusIV.setAutoRepeat(autoRepeat)
            statusIV.setAnimation(drawable)
            if (autoRepeat || isPlayAnimation) {
                statusIV.playAnimation()
            } else drawable.currentFrame = drawable.framesCount - 1
        } else {
            statusIV.visibility = GONE
        }
        uiChatMsgItemEntity.isUpdateStatus = false
    }

    /**
     * 添加view的长按事件
     *
     * @param clickView 需要长按的控件
     */
    @SuppressLint("ClickableViewAccessibility")
    protected open fun addLongClick(clickView: View, uiChatMsgItemEntity: BageUIChatMsgItemEntity) {
        if (uiChatMsgItemEntity.isShowPinnedMessage) return
        val mMsgConfig: MsgConfig = getMsgConfig(uiChatMsgItemEntity.bageMsg.type)
        var isShowReaction = false
        val `object` = EndpointManager.getInstance()
            .invoke("is_show_reaction", CanReactionMenu(uiChatMsgItemEntity.bageMsg, mMsgConfig))
        if (`object` != null) {
            isShowReaction = `object` as Boolean
        }
        if (uiChatMsgItemEntity.bageMsg.flame == 1) isShowReaction = false
        val finalIsShowReaction = isShowReaction
        val location = arrayOf(FloatArray(2))
        clickView.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                location[0] = floatArrayOf(event.rawX, event.rawY)
            }
            false
        }
        clickView.setOnLongClickListener {
            EndpointManager.getInstance().invoke("stop_reaction_animation", null)
            showChatPopup(
                uiChatMsgItemEntity.bageMsg,
                clickView,
                location[0],
                finalIsShowReaction,
                getPopupList(uiChatMsgItemEntity.bageMsg)
            )
            true
        }

    }

    /**
     * 是否能撤回
     * 发送成功且在2分钟内的消息
     *
     * @param mMsg 消息
     * @return boolean
     */
    private fun canWithdraw(mMsg: BageMsg): Boolean {
        var isManager = false
        if (mMsg.channelType == BageChannelType.GROUP) {
            val member = BageIM.getInstance().channelMembersManager.getMember(
                mMsg.channelID,
                mMsg.channelType,
                BageConfig.getInstance().uid
            )
            if (member != null && member.role != BageChannelMemberRole.normal) {
                isManager = true
            }
        }
        var revokeSecond = BageConfig.getInstance().appConfig.revoke_second
        if (revokeSecond == -1 && (mMsg.fromUID == BageConfig.getInstance().uid || isManager)) {
            return true
        }
        if (revokeSecond == 0) revokeSecond = 120
        return (BageTimeUtils.getInstance().currentSeconds - mMsg.timestamp < revokeSecond
                && mMsg.fromUID == BageConfig.getInstance().uid && mMsg.status == BageSendMsgResult.send_success) || (isManager && mMsg.status == BageSendMsgResult.send_success)
    }

    open fun getMsgConfig(msgType: Int): MsgConfig {
        val mMsgConfig: MsgConfig = if (EndpointManager.getInstance()
                .invoke(EndpointCategory.msgConfig + msgType, null) != null
        ) {
            EndpointManager.getInstance()
                .invoke(EndpointCategory.msgConfig + msgType, null) as MsgConfig
        } else {
            MsgConfig(false)
        }
        return mMsgConfig
    }

    var scrimPopupWindow: ActionBarPopupWindow? = null

    protected fun getPopupList(mMsg: BageMsg): List<PopupMenuItem> {
        var isRegisterMsgPrivacyModule = false
        val obj = EndpointManager.getInstance().invoke("is_register_msg_privacy_module", null)
        if (obj != null && obj is PrivacyMessageMenu) {
            isRegisterMsgPrivacyModule = true
        }
        //防止重复添加
        val list: MutableList<PopupMenuItem> = ArrayList()
        var isAddDelete = true
        val mMsgConfig = getMsgConfig(mMsg.type)
        if (mMsgConfig.isCanWithdraw && canWithdraw(mMsg) && !isRegisterMsgPrivacyModule) {
            isAddDelete = false
            list.add(
                0,
                PopupMenuItem(context.getString(R.string.base_withdraw), R.mipmap.msg_withdraw,
                    object : PopupMenuItem.IClick {
                        override fun onClick() {
                            var msgId = mMsg.messageID
                            if (TextUtils.isEmpty(msgId) || msgId == "0") {
                                msgId = mMsg.clientMsgNO
                            }
                            //撤回消息
                            if (!TextUtils.isEmpty(msgId)) {
                                EndpointManager.getInstance().invoke(
                                    "chat_withdraw_msg",
                                    WithdrawMsgMenu(
                                        msgId,
                                        mMsg.channelID,
                                        mMsg.clientMsgNO,
                                        mMsg.channelType
                                    )
                                )
                            }

                        }
                    })
            )
        }
        if (mMsgConfig.isCanForward && mMsg.flame == 0) {
            var index = 0
            if (list.isNotEmpty()) {
                index = 1
            }
            list.add(
                index,
                PopupMenuItem(context.getString(R.string.base_forward), R.mipmap.msg_forward,
                    object : PopupMenuItem.IClick {
                        override fun onClick() {

                            var mMessageContent =
                                mMsg.baseContentMsgModel
                            if (mMsg.remoteExtra != null && mMsg.remoteExtra.contentEditMsgModel != null) {
                                mMessageContent = mMsg.remoteExtra.contentEditMsgModel
                            }
                            val chooseChatMenu =
                                ChooseChatMenu(
                                    ChatChooseContacts { channelList: List<BageChannel>? ->
                                        if (!channelList.isNullOrEmpty()) {
                                            for (mChannel in channelList) {
                                                var msgContent =
                                                    mMsg.baseContentMsgModel
                                                if (mMsg.remoteExtra != null && mMsg.remoteExtra.contentEditMsgModel != null) {
                                                    msgContent =
                                                        mMsg.remoteExtra.contentEditMsgModel
                                                }
                                                msgContent.mentionAll = 0
                                                msgContent.mentionInfo = null
                                                val option = BageSendOptions()
                                                option.setting.receipt = mChannel.receipt
                                                BageIM.getInstance().msgManager.sendWithOptions(
                                                    msgContent,
                                                    mChannel, option
                                                )
                                            }
                                            val viewGroup =
                                                (context as Activity).findViewById<View>(android.R.id.content)
                                                    .rootView as ViewGroup
                                            Snackbar.make(
                                                viewGroup,
                                                context.getString(R.string.str_forward),
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
                            EndpointManager.getInstance()
                                .invoke(EndpointSID.showChooseChatView, chooseChatMenu)
                        }
                    })
            )
        }
        val menus = EndpointManager.getInstance()
            .invokes<ChatItemPopupMenu>(EndpointCategory.bageChatPopupItem, mMsg)

        if (menus != null && menus.isNotEmpty() && mMsg.flame == 0) {
            for (menu in menus) {
                val popupMenu =
                    PopupMenuItem(menu.text, menu.imageResource,
                        object : PopupMenuItem.IClick {
                            override fun onClick() {
                                menu.iPopupItemClick.onClick(
                                    mMsg,
                                    (Objects.requireNonNull(
                                        getAdapter()
                                    ) as ChatAdapter).conversationContext
                                )
                            }
                        })
                popupMenu.subText = menu.subText
                popupMenu.tag = menu.tag
                if (menu != null) list.add(
                    popupMenu
                )
            }
        }
        var addIndex = list.size
        val result = EndpointManager.getInstance().invoke("auto_delete", mMsg)
        if (result != null) {
            addIndex = list.size - 1
        }
        if (mMsgConfig.isCanMultipleChoice && mMsg.flame == 0) {
            list.add(
                addIndex,
                PopupMenuItem(
                    context.getString(R.string.multiple_choice),
                    R.mipmap.msg_select,
                    object : PopupMenuItem.IClick {
                        override fun onClick() {
                            var i = 0
                            val size = getAdapter()!!.data.size
                            while (i < size) {
                                getAdapter()!!.data[i].isChoose = true
                                if (getAdapter()!!.data[i].bageMsg.clientMsgNO == mMsg.clientMsgNO) {
                                    getAdapter()!!.data[i].isChecked = true
                                }
                                getAdapter()!!.notifyItemChanged(
                                    i,
                                    getAdapter()!!.data[i]
                                )
                                i++
                            }

                            //    getAdapter()!!.notifyItemRangeChanged(0, getAdapter()!!.data.size)
                            (Objects.requireNonNull(
                                getAdapter()
                            ) as ChatAdapter).showTitleRightText("1")
                            (getAdapter() as ChatAdapter?)!!.showMultipleChoice()

                        }
                    })
            )
            addIndex++
        }
        //发送成功的消息才能回复
        if (mMsgConfig.isCanReply && mMsg.status == BageSendMsgResult.send_success && mMsg.flame == 0) {
            list.add(
                addIndex,
                PopupMenuItem(context.getString(R.string.msg_reply), R.mipmap.msg_reply,
                    object : PopupMenuItem.IClick {
                        override fun onClick() {
                            (Objects.requireNonNull(
                                getAdapter()
                            ) as ChatAdapter).replyMsg(mMsg)
                        }
                    })
            )
            addIndex++
        }
        //撤回和删除不能同时存在
        if (isAddDelete && mMsg.flame == 0 && result == null) {
            list.add(
                addIndex,
                PopupMenuItem(
                    context.getString(R.string.base_delete),
                    R.mipmap.msg_delete, object : PopupMenuItem.IClick {
                        override fun onClick() {
                            var singleDelete = false
                            if (mMsg.status != BageSendMsgResult.send_success) {
                                singleDelete = true
                            } else {
                                if (mMsg.channelType == BageChannelType.GROUP) {
                                    val loginUID = BageConfig.getInstance().uid
                                    val member = BageIM.getInstance().channelMembersManager.getMember(
                                        mMsg.channelID,
                                        mMsg.channelType,
                                        loginUID
                                    )
                                    if (member == null || (member.role == BageChannelMemberRole.normal && (!TextUtils.isEmpty(
                                            mMsg.fromUID
                                        ) && mMsg.fromUID != loginUID)
                                                )
                                    ) {
                                        singleDelete = true
                                    }
                                }
                            }
                            if (obj != null && obj is PrivacyMessageMenu && !singleDelete) {
                                val checkBoxText: String
                                if (mMsg.channelType == BageChannelType.GROUP) {
                                    checkBoxText =
                                        context.getString(R.string.str_delete_message_for_all)
                                } else {
                                    var showName = ""
                                    val channel = BageIM.getInstance().channelManager.getChannel(
                                        mMsg.channelID,
                                        mMsg.channelType
                                    )
                                    if (channel != null) {
                                        showName =
                                            if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                                    }
                                    checkBoxText = String.format(
                                        context.getString(R.string.str_delete_message_also_to),
                                        showName
                                    )
                                }
                                BageDialogUtils.getInstance().showCheckBoxDialog(
                                    context,
                                    context.getString(R.string.str_delete_message),
                                    context.getString(R.string.str_delete_message_tip),
                                    checkBoxText,
                                    true,
                                    "",
                                    context.getString(R.string.base_delete),
                                    0,
                                    ContextCompat.getColor(context, R.color.red)
                                ) { index, isChecked ->
                                    if (index == 1) {
                                        if (isChecked) {
                                            obj.iClick.onDelete(mMsg)
                                        } else {
                                            EndpointManager.getInstance()
                                                .invoke("str_delete_msg", mMsg)
                                            BageIM.getInstance().msgManager.deleteWithClientMsgNO(
                                                mMsg.clientMsgNO
                                            )
                                        }
                                    }
                                }
                            } else {
                                BageDialogUtils.getInstance().showDialog(
                                    context,
                                    context.getString(R.string.str_delete_message),
                                    context.getString(R.string.str_delete_message_tip),
                                    true,
                                    "",
                                    context.getString(R.string.base_delete),
                                    0,
                                    ContextCompat.getColor(context, R.color.red)
                                ) { index ->
                                    if (index == 1) {
                                        EndpointManager.getInstance().invoke("str_delete_msg", mMsg)
                                        BageIM.getInstance().msgManager.deleteWithClientMsgNO(mMsg.clientMsgNO)
                                    }
                                }

                            }
                        }
                    })
            )
        }
        return list
    }

    private val rect = RectF()

    @SuppressLint("ClickableViewAccessibility")
    protected fun showChatPopup(
        mMsg: BageMsg,
        v: View,
        local: FloatArray,
        isShowReaction: Boolean,
        list: List<PopupMenuItem>
    ) {
        val mMsgConfig: MsgConfig = getMsgConfig(mMsg.type)
        if (mMsg.flame == 1 && (!mMsgConfig.isCanWithdraw || !canWithdraw(mMsg))) {
            return
        }

        val scrimPopupContainerLayout: ChatScrimPopupContainerLayout =
            object : ChatScrimPopupContainerLayout(context) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0 && scrimPopupWindow != null) {
                        scrimPopupWindow!!.dismiss(true)
                    }
                    return super.dispatchKeyEvent(event)
                }

                override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                    val b = super.dispatchTouchEvent(ev)
                    if (ev.action == MotionEvent.ACTION_DOWN && !b && scrimPopupWindow != null) {
                        scrimPopupWindow!!.dismiss(true)
                    }
                    return b
                }
            }
        scrimPopupContainerLayout.setOnTouchListener(object : OnTouchListener {
            private val pos = IntArray(2)
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    if (scrimPopupWindow != null && scrimPopupWindow!!.isShowing) {
                        val contentView = scrimPopupWindow!!.contentView
                        contentView.getLocationInWindow(pos)
                        rect.set(
                            pos[0].toFloat(),
                            pos[1].toFloat(),
                            (pos[0] + contentView.measuredWidth).toFloat(),
                            (pos[1] + contentView.measuredHeight).toFloat()
                        )
                        if (!rect.contains(event.x.toInt().toFloat(), event.y.toInt().toFloat())) {
                            scrimPopupWindow!!.dismiss(true)
                        }
                    }
                } else if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    scrimPopupWindow!!.dismiss(true)
                }
                return false
            }
        })
        val popupLayout = ActionBarPopupWindowLayout(
            context,
            R.mipmap.popup_fixed_alert,
            ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK
        )
        val `object` = EndpointManager.getInstance().invoke("show_receipt", mMsg)
        if (`object` != null) {
            val isShowReceipt = `object` as Boolean
            if (isShowReceipt) {
                val str = String.format(
                    context.getString(R.string.msg_read_count),
                    mMsg.remoteExtra.readedCount
                )
                val subItem1 = ActionBarMenuSubItem(context, false, false, false)
                subItem1.setTextAndIcon(str, R.mipmap.msg_seen)
                subItem1.setTag(R.id.width_tag, 240)
                subItem1.setMultiline()
                subItem1.setRightIcon(R.mipmap.msg_arrowright)
                popupLayout.addView(subItem1)

                subItem1.setOnClickListener {
                    scrimPopupWindow!!.dismiss()
                    EndpointManager.getInstance().invoke("chat_activity_touch", null)
                    EndpointManager.getInstance().invoke(
                        "show_msg_read_detail",
                        ReadMsgDetailMenu(
                            mMsg.messageID,
                            (Objects.requireNonNull(
                                getAdapter()
                            ) as ChatAdapter).conversationContext
                        )
                    )
                }
                val subItem2 = ActionBarMenuSubItem(context, false, false, false)
                subItem2.setItemHeight(10)
                subItem2.setBackgroundColor(ContextCompat.getColor(context, R.color.homeColor))
                popupLayout.addView(
                    subItem2,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 10)
                )
            }
        }
        var i = 0
        val size = list.size
        while (i < size) {
            val item = list[i]
            val subItem = ActionBarMenuSubItem(context, false, false, i == list.size - 1)
            subItem.setTextAndIcon(item.text, item.iconResourceID)
            subItem.setTag(R.id.width_tag, 240)
            subItem.setMultiline()
            if (!TextUtils.isEmpty(item.subText)) {
                subItem.setSubtext(item.subText)
            }
            if (!TextUtils.isEmpty(item.tag) && item.tag == "auto_delete") {
                EndpointManager.getInstance().invoke("chat_popup_item", subItem)
            }
            subItem.setOnClickListener {
                scrimPopupWindow?.dismiss()
                item.iClick.onClick()
                EndpointManager.getInstance().invoke("chat_activity_touch", null)
            }
            popupLayout.addView(subItem)
            i++
        }
        popupLayout.backgroundColor = ContextCompat.getColor(context, R.color.screen_bg)
        popupLayout.minimumWidth = AndroidUtilities.dp(200f)
        var reactionsLayout: ReactionsContainerLayout? = null
        val pad = 22
        val sPad = 24
        if (isShowReaction) {
            reactionsLayout = ReactionsContainerLayout(context)
            reactionsLayout.setPadding(
                AndroidUtilities.dp(4f) + if (AndroidUtilities.isRTL) 0 else sPad,
                AndroidUtilities.dp(4f),
                AndroidUtilities.dp(4f) + if (AndroidUtilities.isRTL) sPad else 0,
                AndroidUtilities.dp(pad.toFloat())
            )
            reactionsLayout.setDelegate(ReactionsContainerDelegate { _: View?, reaction: String?, _: Boolean, location: IntArray? ->
                scrimPopupWindow?.dismiss(true)
                EndpointManager.getInstance().invoke(
                    "bage_msg_reaction",
                    MsgReactionMenu(mMsg, reaction, getAdapter() as ChatAdapter?, location)
                )
            })
        }

//        Rect backgroundPaddings = new Rect();
//        Drawable shadowDrawable2 = ContextCompat.getDrawable(getContext(), R.mipmap.popup_fixed_alert).mutate();
//        shadowDrawable2.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.layoutColor), PorterDuff.Mode.MULTIPLY));
//        shadowDrawable2.getPadding(backgroundPaddings);
//        scrimPopupContainerLayout.setBackground(shadowDrawable2);
        if (isShowReaction) {
            val params = LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                52 + pad,
                Gravity.START,
                0,
                0,
                0,
                0
            )
            scrimPopupContainerLayout.addView(reactionsLayout, params)
            scrimPopupContainerLayout.setReactionsLayout(reactionsLayout)
            reactionsLayout?.setTransitionProgress(0f)
        }
        scrimPopupContainerLayout.clipChildren = false
        val fl = FrameLayout(context)
        //        fl.setBackground(shadowDrawable2);
        fl.addView(
            popupLayout,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat())
        )
        scrimPopupContainerLayout.addView(
            fl,
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.START,
                16,
                if (isShowReaction) -18 else 0,
                36,
                0
            )
        )
        scrimPopupContainerLayout.applyViewBottom(fl)
        scrimPopupContainerLayout.setPopupWindowLayout(popupLayout)
        if (popupLayout.swipeBack != null) {
            val finalReactionsLayout = reactionsLayout
            if (isShowReaction) {
                popupLayout.swipeBack!!
                    .addOnSwipeBackProgressListener { _: PopupSwipeBackLayout?, toProgress: Float, progress: Float ->
                        if (toProgress == 0f) {
                            finalReactionsLayout?.startEnterAnimation()
                        } else if (toProgress == 1f) finalReactionsLayout!!.alpha = 1f - progress
                    }
            }
        }
        scrimPopupWindow = object : ActionBarPopupWindow(
            scrimPopupContainerLayout,
            LayoutHelper.WRAP_CONTENT,
            LayoutHelper.WRAP_CONTENT
        ) {
            override fun dismiss() {
                super.dismiss()
                if (scrimPopupWindow !== this) {
                    return
                }
                scrimPopupWindow = null
            }
        }
        scrimPopupWindow!!.setPauseNotifications(true)
        scrimPopupWindow!!.setDismissAnimationDuration(220)
        scrimPopupWindow!!.isOutsideTouchable = true
        scrimPopupWindow!!.isClippingEnabled = true
        scrimPopupWindow!!.animationStyle = R.style.PopupContextAnimation
        scrimPopupWindow!!.isFocusable = true
        scrimPopupContainerLayout.measure(
            MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000f), MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(
                AndroidUtilities.dp(1000f), MeasureSpec.AT_MOST
            )
        )
        scrimPopupWindow!!.inputMethodMode = ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED
        scrimPopupWindow!!.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        scrimPopupWindow!!.contentView.isFocusableInTouchMode = true
        popupLayout.setFitItems(false)
        val x = local[0]
        val y = local[1]
        val adapter = getAdapter() as ChatAdapter
        val recyclerViewLayout = adapter.conversationContext.recyclerViewLayout
        var popupX =
            v.left + x.toInt() - scrimPopupContainerLayout.measuredWidth + -AndroidUtilities.dp(28f)
        if (popupX < AndroidUtilities.dp(6f)) {
            popupX = AndroidUtilities.dp(6f)
        } else if (popupX > recyclerViewLayout.measuredWidth - AndroidUtilities.dp(
                6f
            ) - scrimPopupContainerLayout.measuredWidth
        ) {
            popupX =
                recyclerViewLayout.measuredWidth - AndroidUtilities.dp(6f) - scrimPopupContainerLayout.measuredWidth
        }
        var totalHeight = AndroidUtilities.getScreenHeight()
        val height = scrimPopupContainerLayout.measuredHeight + AndroidUtilities.dp(48f)
        val keyboardHeight = BageConstants.getKeyboardHeight()
        if (keyboardHeight > AndroidUtilities.dp(20f)) {
            totalHeight += keyboardHeight
        }
        var popupY: Int
        if (height < totalHeight) {
            popupY = (recyclerViewLayout.y + v.top + y).toInt()
            if (height > AndroidUtilities.dp(240f)) {
                popupY += AndroidUtilities.dp(240f) - height / 10 * 7
            }
            if (popupY < recyclerViewLayout.y + AndroidUtilities.dp(24f)) {
                popupY = (recyclerViewLayout.y + AndroidUtilities.dp(24f)).toInt()
            } else if (popupY > totalHeight - height - AndroidUtilities.dp(8f)) {
                popupY = totalHeight - height - AndroidUtilities.dp(8f)
            }
        } else {
            popupY = 0
        }
        val finalPopupX = popupX
        val finalPopupY = popupY
        val finalReactionsLayout1 = reactionsLayout
        val showMenu = Runnable {
            if (scrimPopupWindow == null) {
                return@Runnable
            }
            scrimPopupWindow!!.showAtLocation(
                recyclerViewLayout, Gravity.START or Gravity.TOP, finalPopupX, finalPopupY
            )
            if (isShowReaction) finalReactionsLayout1!!.startEnterAnimation()
        }
        showMenu.run()
    }


    // 消息item显示最大宽度
    protected open fun getViewWidth(
        fromType: BageChatIteMsgFromType,
        msgItemEntity: BageUIChatMsgItemEntity
    ): Int {
        val maxWidth =
            if (AndroidUtilities.isPORTRAIT) AndroidUtilities.getScreenWidth() else AndroidUtilities.getScreenHeight()
        val width: Int
        val checkBoxMargin = 30
        var flameWidth = 0
        var pinnedWidth = 0
        if ((msgItemEntity.bageMsg.flame == 1 && msgItemEntity.bageMsg.flameSecond > 0) && msgItemEntity.bageMsg.type != BageContentType.Bage_IMAGE
            && msgItemEntity.bageMsg.type != BageContentType.Bage_VIDEO
        ) {
            flameWidth = 30
        }
        if (msgItemEntity.isShowPinnedMessage) {
            pinnedWidth = 35
        }
        val showAvatar = shouldShowMessageAvatar(msgItemEntity.bageMsg)
        val avatarReserve = if (showAvatar) 72 else 0
        val horizontalReserve = 52 + checkBoxMargin + avatarReserve + flameWidth + pinnedWidth
        width =
            if (fromType == BageChatIteMsgFromType.SEND || msgItemEntity.bageMsg.channelType == BageChannelType.PERSONAL) {
                maxWidth - AndroidUtilities.dp(horizontalReserve.toFloat())
            } else {
                maxWidth - AndroidUtilities.dp((horizontalReserve + 36).toFloat())
            }
        return width.coerceAtLeast(AndroidUtilities.dp(140f))
    }

    protected open fun getShowContent(contentJson: String): String? {
        return StringUtils.getShowContent(context, contentJson)
    }

    private fun isAddFlameView(msgItemEntity: BageUIChatMsgItemEntity): Boolean {
        return !(msgItemEntity.bageMsg.flame == 0 || BageContentType.isSystemMsg(msgItemEntity.bageMsg.type) || BageContentType.isLocalMsg(
            msgItemEntity.bageMsg.type
        ) || (msgItemEntity.bageMsg.flame == 1 && msgItemEntity.bageMsg.flameSecond == 0)
                || msgItemEntity.bageMsg.type == BageContentType.Bage_IMAGE
                || msgItemEntity.bageMsg.type == BageContentType.Bage_VIDEO)
    }

    private fun canResendMsg(channelID: String, channelType: Byte): Boolean {
        if (channelType == BageChannelType.PERSONAL) return true
        val mChannel =
            BageIM.getInstance().channelManager.getChannel(channelID, channelType)
        val member = BageIM.getInstance().channelMembersManager.getMember(
            channelID,
            channelType,
            BageConfig.getInstance().uid
        )
        if (member != null) {
            if (mChannel != null && mChannel.forbidden == 1) {
                if (member.role == BageChannelMemberRole.admin) {
                    return true
                }
                if (member.role == BageChannelMemberRole.manager) {
                    return member.forbiddenExpirationTime <= 0L
                }
                return false
            }
            if (member.forbiddenExpirationTime > 0L) {
                return false
            }
        }
        return true
    }

    fun setItemPadding(position: Int, viewGroupLayout: ChatItemView) {
        var top: Int
        var bottom: Int
        val currentFromUID: String? = getAdapter()!!.data[position].bageMsg.fromUID
        var nextFromUID: String? = ""
        var previousFromUID: String? = ""
        if (position + 1 <= getAdapter()!!.data.size - 1) {
            nextFromUID = getAdapter()!!.data[position + 1].bageMsg.fromUID
        }
        if (position - 1 > 0) {
            previousFromUID = getAdapter()!!.data[position - 1].bageMsg.fromUID
        }
        if (TextUtils.isEmpty(currentFromUID)) {
            top = AndroidUtilities.dp(8f)
            bottom = AndroidUtilities.dp(8f)
        } else {
            top = if (!TextUtils.isEmpty(previousFromUID) && previousFromUID == currentFromUID) {
                AndroidUtilities.dp(4f)
            } else {
                AndroidUtilities.dp(8f)
            }
            bottom = if (!TextUtils.isEmpty(nextFromUID) && nextFromUID == currentFromUID) {
                AndroidUtilities.dp(4f)
            } else {
                AndroidUtilities.dp(8f)
            }
        }
        if (position == getAdapter()!!.data.size - 1) {
            bottom = AndroidUtilities.dp(14f)
        }
        if (position == 0) {
            top = AndroidUtilities.dp(14f)
        }
        viewGroupLayout.setPadding(0, top, 0, bottom)
    }

    private fun getMsgOS(clientMsgNo: String): String {
        return if (clientMsgNo.endsWith("1")) {
            "Android"
        } else if (clientMsgNo.endsWith("2")) {
            "iOS"
        } else if (clientMsgNo.endsWith("3")) {
            "Web"
        } else if (clientMsgNo.endsWith("5")) {
            "Flutter"
        } else {
            "PC"
        }
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        val chatAdapter = getAdapter() as ChatAdapter
        chatAdapter.conversationContext.onMsgViewed(
            chatAdapter.data[holder.bindingAdapterPosition - chatAdapter.headerLayoutCount].bageMsg,
            holder.bindingAdapterPosition - chatAdapter.headerLayoutCount
        )
    }

}