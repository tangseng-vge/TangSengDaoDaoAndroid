package com.chat.video.search.remote

import com.chad.library.adapter.base.entity.MultiItemEntity
import com.chat.base.entity.GlobalMessage
import com.bage.im.msgmodel.BageVideoContent

class SearchEntity(
    val type: Int,
    ) :
    MultiItemEntity {
    var date: String = ""
    var second: String = ""
    lateinit var videoModel: BageVideoContent
    lateinit var message: GlobalMessage
    override val itemType: Int
        get() = type
}