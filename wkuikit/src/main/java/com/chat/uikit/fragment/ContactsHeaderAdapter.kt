package com.chat.uikit.fragment

import android.text.TextUtils
import android.view.Gravity
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.endpoint.entity.ContactsMenu
import com.chat.base.ui.components.AvatarView
import com.chat.base.ui.components.CounterView
import com.chat.uikit.R
import com.xinbida.wukongim.entity.WKChannelType

class ContactsHeaderAdapter @JvmOverloads constructor(
    private val discoverStyle: Boolean = false
) : BaseQuickAdapter<ContactsMenu, BaseViewHolder>(
    if (discoverStyle) R.layout.item_discover_header_layout else R.layout.item_contacts_menu_layout
) {
    override fun convert(holder: BaseViewHolder, item: ContactsMenu) {
        holder.setImageResource(R.id.imageView, item.imgResourceID)
        holder.setText(R.id.nameTv, item.text)
        val msgCountTv: CounterView = holder.getView(R.id.msgCountTv)
        msgCountTv.setColors(R.color.white, R.color.reminderColor)
        msgCountTv.setGravity(Gravity.RIGHT)

        if (discoverStyle) {
            val showBadge = item.badgeNum > 0
            holder.setVisible(R.id.userLayout, showBadge)
            msgCountTv.setCount(item.badgeNum, true)
            msgCountTv.visibility = if (showBadge) View.VISIBLE else View.GONE
        } else {
            msgCountTv.setCount(item.badgeNum, true)
            msgCountTv.visibility = if (item.badgeNum > 0) View.VISIBLE else View.GONE
            val avatarView: AvatarView = holder.getView(R.id.avatarView)
            avatarView.setSize(32f)
            if (!TextUtils.isEmpty(item.uid)) {
                holder.setVisible(R.id.userLayout, true)
                holder.setVisible(R.id.dotIv, true)
                avatarView.showAvatar(item.uid, WKChannelType.PERSONAL)
            } else {
                holder.setVisible(R.id.userLayout, false)
            }
        }
    }
}