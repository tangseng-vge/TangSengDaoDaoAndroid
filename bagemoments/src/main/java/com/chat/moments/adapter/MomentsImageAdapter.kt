package com.chat.moments.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.chat.base.config.BageApiConfig
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.ChatChooseContacts
import com.chat.base.endpoint.entity.ChooseChatMenu
import com.chat.base.entity.PopupMenuItem
import com.chat.base.glide.GlideUtils
import com.chat.base.ui.components.FilterImageView
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.ImageUtils
import com.chat.base.utils.BageDialogUtils
import com.chat.moments.R
import com.google.android.material.snackbar.Snackbar
import com.bage.im.BageIM
import com.bage.im.entity.BageChannel
import com.bage.im.msgmodel.BageImageContent

class MomentsImageAdapter(val iClickCollect: IClickCollect) :
    BaseQuickAdapter<String, BaseViewHolder>(R.layout.moments_item_img_layout) {
    override fun convert(holder: BaseViewHolder, item: String) {
        val imageView: FilterImageView = holder.getView(R.id.imageView)
        imageView.setAllCorners(0)
        imageView.strokeWidth = 0f
        val url = BageApiConfig.getShowUrl(item)
        GlideUtils.getInstance().showImg(
            context,
            url,
            holder.getView(R.id.imageView)
        )
        val w = AndroidUtilities.getScreenWidth() - AndroidUtilities.dp(80f)
        imageView.layoutParams.width = w / 3
        imageView.layoutParams.height = w / 3


        val list: MutableList<PopupMenuItem> = ArrayList()
        list.add(
            PopupMenuItem(context.getString(R.string.str_dynmaic_collect),
                R.mipmap.msg_fave,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        iClickCollect.onCollect(
                            holder.bindingAdapterPosition,
                            item
                        )
                    }
                })
        )
        list.add(
            PopupMenuItem(
                context.getString(R.string.moment_forward),
                R.mipmap.msg_forward,
                object : PopupMenuItem.IClick {
                    override fun onClick() {
                        val tempUrl: String = item.replace(".png".toRegex(), "")
                        val strings =
                            tempUrl.split("@").toTypedArray()[1].split("x").toTypedArray()
                        val wH = ImageUtils.getInstance()
                            .getImgWidthAndHeightToDynamic(
                                context,
                                strings[0],
                                strings[1]
                            )
                        val imageContent = BageImageContent()
                        imageContent.url = item
                        imageContent.width = wH[0]
                        imageContent.height = wH[1]
                        EndpointManager.getInstance()
                            .invoke(
                                EndpointSID.showChooseChatView, ChooseChatMenu(
                                    ChatChooseContacts { list1: List<BageChannel>? ->
                                        if (!list1.isNullOrEmpty()) {
                                            for (channel in list1) {
                                                BageIM.getInstance().msgManager.send(
                                                    imageContent,
                                                    channel
                                                )
                                            }
                                            val viewGroup =
                                                (context as Activity).findViewById<View>(
                                                    android.R.id.content
                                                )
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
                                    }, imageContent
                                )
                            )
                    }
                })
        )
        BageDialogUtils.getInstance().setViewLongClickPopup(holder.getView(R.id.imageView), list)
    }

    interface IClickCollect {
        fun onCollect(index: Int, url: String?)
    }
}