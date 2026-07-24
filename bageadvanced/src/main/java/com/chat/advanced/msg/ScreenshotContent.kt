package com.chat.advanced.msg

import android.text.TextUtils
import com.chat.advanced.R
import com.chat.base.BageBaseApplication
import com.chat.base.config.BageConfig
import com.chat.base.msgitem.BageContentType
import com.chat.base.utils.BageLogUtils
import com.bage.im.BageIM
import com.bage.im.entity.BageChannelType
import com.bage.im.msgmodel.BageMessageContent
import org.json.JSONException
import org.json.JSONObject

class ScreenshotContent : BageMessageContent() {
    var fromname: String = ""
    var fromuid: String = ""

    init {
        type = BageContentType.screenshot
    }

    override fun encodeMsg(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("from_uid", fromuid)
            jsonObject.put("from_name", fromname)
        } catch (e: JSONException) {
            BageLogUtils.e("构建截屏消息错误")
        }
        return jsonObject
    }

    override fun decodeMsg(jsonObject: JSONObject): BageMessageContent {
        fromname = jsonObject.optString("from_name")
        fromuid = jsonObject.optString("from_uid")
        return this
    }

    override fun getDisplayContent(): String {

        return if (!TextUtils.isEmpty(this.fromuid)) {
            if (fromuid == BageConfig.getInstance().uid) {
                BageBaseApplication.getInstance().context.getString(R.string.screenshort_inchat_my)
            } else {
                var showName = fromname
                val channel = BageIM.getInstance().channelManager.getChannel(
                    fromuid,
                    BageChannelType.PERSONAL
                )
                if (channel != null) {
                    showName =
                        if (TextUtils.isEmpty(channel.channelRemark)) channel.channelName else channel.channelRemark
                }
                String.format(
                    BageBaseApplication.getInstance().context.getString(R.string.screenshot_inchat1),
                    showName
                )
            }
        } else ""
    }
}