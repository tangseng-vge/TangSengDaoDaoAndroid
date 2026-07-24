package com.chat.base.msgitem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chat.base.R
import com.chat.base.views.BubbleLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.ResourceObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class BageTypingProvider : BageChatBaseProvider() {
    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context).inflate(R.layout.chat_typing_layout, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        Observable.interval(0, 1, TimeUnit.SECONDS)
            .take((8 + 1).toLong())
            .map { takeValue: Long -> 8 - takeValue }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : ResourceObserver<Long>() {
                override fun onNext(t: Long) {
                }

                override fun onError(e: Throwable) {
                }

                override fun onComplete() {
                    for (i in getAdapter()!!.data.indices.reversed()) {
                        if (getAdapter()!!.data[i].bageMsg.type == BageContentType.typing) {
                            getAdapter()!!.removeAt(i)
                            if (getAdapter()!!.data.isNotEmpty()) {
                                val index = getAdapter()!!.data.size - 1
                                getAdapter()!!.data[index].nextMsg = null
                                getAdapter()!!.notifyItemChanged(index)
                            }
                            break
                        }
                    }

                }

            })
        val contentTvLayout = parentView.findViewById<BubbleLayout>(R.id.contentLayout)
        val bgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.bageMsg,
            uiChatMsgItemEntity.nextMsg
        )
        contentTvLayout.setAll(bgType, from, BageContentType.typing)
        val receivedTextNameTv = parentView.findViewById<TextView>(R.id.receivedTextNameTv)
        setFromName(uiChatMsgItemEntity,from,receivedTextNameTv)

    }

    override val itemViewType: Int
        get() = BageContentType.typing
}