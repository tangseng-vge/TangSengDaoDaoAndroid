package com.chat.base.endpoint.entity

import android.widget.FrameLayout
import com.chat.base.ui.components.AvatarView
import com.bage.im.entity.BageChannel

class AvatarOtherViewMenu(
    val otherView: FrameLayout,
    val channel: BageChannel,
    val avatarView: AvatarView,
    val showUpdateDialog: Boolean
) {
}