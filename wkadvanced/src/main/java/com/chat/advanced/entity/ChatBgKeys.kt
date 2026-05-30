package com.chat.advanced.entity

import android.text.TextUtils
import com.chat.base.config.WKConstants

class ChatBgKeys {
    companion object {
        const val chatBgGradientAngle = "chat_bg_gradient_angle"
        const val chatBgUrl = "chat_bg_url"
        const val chatBgIsSvg = "chat_bg_is_svg"
        const val chatBgIsBlurred = "chat_bg_is_blurred"
        const val chatBgColorLight = "chat_bg_color_light"
        const val chatBgColorDark = "chat_bg_color_dark"
        const val chatBgShowPattern = "chat_bg_show_pattern"
        const val chatBgIsDeleted = "chat_bg_is_deleted"

        fun cacheFilePath(url: String): String {
            if (TextUtils.isEmpty(url)) return ""
            val safeName = url.replace("/", "_").replace(":", "_").replace("\\", "_")
            return WKConstants.chatBgCacheDir + safeName
        }

        fun blurredCacheFilePath(url: String): String {
            val original = cacheFilePath(url)
            if (TextUtils.isEmpty(original)) return ""
            return blurredCachePath(original)
        }

        fun blurredCachePath(originalCachePath: String): String {
            return "$originalCachePath.blur"
        }
    }
}