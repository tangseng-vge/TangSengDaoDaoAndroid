package com.chat.advanced.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chat.advanced.R
import com.chat.base.R as BaseR
import com.chat.advanced.databinding.ActPreviewChatBgLayoutBinding
import com.chat.advanced.entity.ChatBgKeys
import com.chat.advanced.utils.ChatBgBlurHelper
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConfig
import com.chat.base.config.WKConstants
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.glide.GlideUtils
import com.chat.base.msgitem.WKChatIteMsgFromType
import com.chat.base.msgitem.WKContentType
import com.chat.base.msgitem.WKMsgBgType
import com.chat.base.ui.Theme
import com.chat.base.ui.components.CheckBox
import com.chat.base.ui.components.CubicBezierInterpolator
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.WKFileUtils
import com.chat.base.utils.WKTimeUtils
import com.chat.base.utils.SvgHelper
import com.chat.base.net.ud.WKDownloader
import com.chat.base.net.ud.WKProgressManager
import com.chat.base.utils.singleclick.SingleClickUtil
import com.xinbida.wukongim.WKIM
import java.io.File


class PreviewChatBgActivity : WKBaseActivity<ActPreviewChatBgLayoutBinding>() {
    private lateinit var channelID: String
    private var channelType: Byte = 0

    //    private var colorIndex: Int = 0
    private lateinit var url: String
    private var cover: String = ""
    private var lightColors: String = ""
    private var darkColors: String = ""
    private var isSvg: Int = 0
    private var isLocal: Int = 0
    private var cachePath: String = ""
    private var downloadTag: String = ""
    private var previewReady: Boolean = false

    override fun getViewBinding(): ActPreviewChatBgLayoutBinding {
        return ActPreviewChatBgLayoutBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv!!.setText(R.string.chat_bg_preview)
    }

    override fun initPresenter() {
        url = intent.getStringExtra("url") ?: ""
        cover = intent.getStringExtra("cover") ?: ""
        channelID = intent.getStringExtra("channelID") ?: ""
        channelType = intent.getByteExtra("channelType", 0)
        isSvg = intent.getIntExtra("isSvg", 0)
        if (isSvg == 1) {
            lightColors = intent.getStringExtra("lightColors") ?: ""
            darkColors = intent.getStringExtra("darkColors") ?: ""
        }
        if (intent.hasExtra("isLocal")) isLocal = intent.getIntExtra("isLocal", 0)
        cachePath = if (isLocal == 0) ChatBgKeys.cacheFilePath(url) else url
    }

    private var gradientAngle = 45
    private fun initCheckBox(checkBox: CheckBox) {
        checkBox.setResId(this, R.mipmap.round_check2)
        checkBox.setDrawBackground(true)
        checkBox.setHasBorder(true)
        checkBox.setStrokeWidth(AndroidUtilities.dp(2f))
        checkBox.setBorderColor(ContextCompat.getColor(this, R.color.white))
        checkBox.setSize(24)
        checkBox.setColor(
            ContextCompat.getColor(this, R.color.transparent),
            ContextCompat.getColor(this, R.color.white)
        )
        checkBox.visibility = View.VISIBLE
        checkBox.isEnabled = true
        checkBox.setChecked(true, true)
    }

    override fun initView() {
        wkVBinding.saveTV.setTextColor(Theme.colorAccount)
        if (Theme.isDark()) {
            wkVBinding.textSizeTv.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            wkVBinding.textSizeTv.setTextColor(ContextCompat.getColor(this, R.color.black))
        }
        initCheckBox(wkVBinding.patternCB)
        initCheckBox(wkVBinding.blurredCB)
        wkVBinding.blurredCB.setChecked(false, true)
        wkVBinding.patternLayout.visibility = if (isSvg == 1) View.VISIBLE else View.GONE
        wkVBinding.rotateView.visibility = if (isSvg == 1) View.VISIBLE else View.GONE
        wkVBinding.blurredLayout.visibility = if (isSvg == 1) View.GONE else View.VISIBLE
        if (TextUtils.isEmpty(url)) wkVBinding.blurredLayout.visibility = View.GONE
        else wkVBinding.blurredLayout.visibility = View.VISIBLE

        if (TextUtils.isEmpty(channelID)) {
//            val userInfoEntity = WKConfig.getInstance().userInfo
            val chatBgUrl =
                WKSharedPreferencesUtil.getInstance().getSPWithUID(ChatBgKeys.chatBgUrl)
            if (!TextUtils.isEmpty(chatBgUrl) && url == chatBgUrl) {
                gradientAngle = WKSharedPreferencesUtil.getInstance()
                    .getIntWithUID(ChatBgKeys.chatBgGradientAngle)
//                colorIndex = WKSharedPreferencesUtil.getInstance()
//                    .getInt(userInfoEntity.uid + "_" + WKChannelCustomerExtras.chatBgColorIndex)
                lightColors = WKSharedPreferencesUtil.getInstance()
                    .getSPWithUID(ChatBgKeys.chatBgColorLight)
                darkColors = WKSharedPreferencesUtil.getInstance()
                    .getSPWithUID(ChatBgKeys.chatBgColorDark)
                val showPattern = WKSharedPreferencesUtil.getInstance()
                    .getIntWithUID(ChatBgKeys.chatBgShowPattern)
                val isBlurred =
                    WKSharedPreferencesUtil.getInstance().getIntWithUID(ChatBgKeys.chatBgIsBlurred)
                wkVBinding.patternCB.setChecked(showPattern == 1, true)
                if (isBlurred == 1) {
                    wkVBinding.blurView.visibility = View.VISIBLE
                }
            }
        } else {
            val channel =
                WKIM.getInstance().channelManager.getChannel(channelID, channelType)
            if (channel?.localExtra != null) {
                val urlObject = channel.localExtra[ChatBgKeys.chatBgUrl]
                if (urlObject != null && urlObject == url) {
                    val isSvgObject = channel.localExtra[ChatBgKeys.chatBgIsSvg]
                    val isBlurredObject =
                        channel.localExtra[ChatBgKeys.chatBgIsBlurred]
                    if (isSvgObject != null) {
                        val colorLightObject =
                            channel.localExtra[ChatBgKeys.chatBgColorLight]
                        if (colorLightObject != null) {
                            lightColors = colorLightObject as String
                        }
                        val colorDarkObject =
                            channel.localExtra[ChatBgKeys.chatBgColorDark]
                        if (colorDarkObject != null) {
                            darkColors = colorDarkObject as String
                        }
                        val gradientAngleObject =
                            channel.localExtra[ChatBgKeys.chatBgGradientAngle]
                        if (gradientAngleObject != null) {
                            gradientAngle = gradientAngleObject as Int
                        }
                        val showPatternObject =
                            channel.localExtra[ChatBgKeys.chatBgShowPattern]
                        if (showPatternObject != null) {
                            val showPattern = showPatternObject as Int
                            wkVBinding.patternCB.setChecked(showPattern == 1, true)
                        }
                    }
                    if (isBlurredObject != null) {
                        val isBlurred = isBlurredObject as Int
                        wkVBinding.blurredCB.setChecked(isBlurred == 1, true)
                        if (isBlurred == 1)
                            wkVBinding.blurView.visibility = View.VISIBLE
                    }

                }

            }
        }
        val showTime = WKTimeUtils.getInstance().getNewChatTime(System.currentTimeMillis())
        wkVBinding.msgTimeTv.text = showTime
        wkVBinding.msgTimeTv1.text = showTime
        Theme.setColorFilter(this, wkVBinding.statusIV, R.color.color999)
        wkVBinding.statusIV.setImageDrawable(Theme.getTicksDoubleDrawable())
        wkVBinding.sendLayout.setAll(
            WKMsgBgType.single,
            WKChatIteMsgFromType.SEND,
            WKContentType.WK_TEXT
        )
        wkVBinding.recvLayout.setAll(
            WKMsgBgType.single,
            WKChatIteMsgFromType.RECEIVED,
            WKContentType.WK_TEXT
        )

        wkVBinding.progress.setSize(50)
        updateSaveButtonState()
        loadBackgroundPreview()
    }

    private fun updateSaveButtonState() {
        val canSave = when {
            TextUtils.isEmpty(url) -> true
            isLocal == 1 -> File(url).exists()
            else -> previewReady
        }
        wkVBinding.saveTV.isEnabled = canSave
        wkVBinding.saveTV.alpha = if (canSave) 1f else 0.5f
    }

    private fun showPreviewLoading(show: Boolean) {
        wkVBinding.loading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun loadBackgroundPreview() {
        if (TextUtils.isEmpty(url)) {
            showPreviewLoading(false)
            return
        }
        val file = File(cachePath)
        if (file.exists() && file.length() > 0) {
            previewReady = true
            showPreviewLoading(false)
            updateSaveButtonState()
            displayBackgroundFromFile(file)
            return
        }
        if (isLocal == 1) {
            previewReady = File(url).exists()
            showPreviewLoading(false)
            if (!previewReady) showToast(BaseR.string.download_err)
            updateSaveButtonState()
            if (previewReady) displayBackgroundFromFile(File(url))
            return
        }
        downloadBackground()
    }

    private fun previewImageUrl(): String {
        val path = if (!TextUtils.isEmpty(cover)) cover else url
        return WKApiConfig.getShowUrl(path)
    }

    private fun showImmediatePreview() {
        if (isSvg == 1) {
            displaySvgGradientOnly()
        } else if (!TextUtils.isEmpty(url) || !TextUtils.isEmpty(cover)) {
            GlideUtils.getInstance().showImg(this, previewImageUrl(), wkVBinding.imageView)
        }
    }

    private fun displaySvgGradientOnly() {
        val colors = gradientColors() ?: return
        val orientation = Theme.getGradientOrientation(gradientAngle)
        wkVBinding.parentView.background = GradientDrawable(orientation, colors)
        wkVBinding.imageView.visibility =
            if (wkVBinding.patternCB.isChecked) View.VISIBLE else View.GONE
    }

    private fun downloadBackground() {
        val downloadUrl = WKApiConfig.getShowUrl(url)
        if (TextUtils.isEmpty(downloadUrl)) {
            onBackgroundDownloadFailed()
            return
        }
        previewReady = false
        updateSaveButtonState()
        showPreviewLoading(true)
        showImmediatePreview()
        File(cachePath).parentFile?.mkdirs()
        downloadTag = downloadUrl
        WKDownloader.instance.download(
            downloadUrl,
            cachePath,
            object : WKProgressManager.IProgress {
                override fun onProgress(tag: Any?, progress: Int) {}

                override fun onSuccess(tag: Any?, path: String?) {
                    if (isFinishing || isDestroyed) return
                    val file = File(cachePath)
                    if (file.exists() && file.length() > 0) {
                        onBackgroundDownloadSuccess()
                    } else {
                        onBackgroundDownloadFailed()
                    }
                }

                override fun onFail(tag: Any?, msg: String?) {
                    onBackgroundDownloadFailed()
                }
            }
        )
    }

    private fun onBackgroundDownloadSuccess() {
        if (isFinishing || isDestroyed) return
        previewReady = true
        showPreviewLoading(false)
        updateSaveButtonState()
        if (isSvg == 1) {
            displayBackgroundFromFile(File(cachePath))
        }
    }

    private fun onBackgroundDownloadFailed() {
        if (isFinishing || isDestroyed) return
        previewReady = false
        showPreviewLoading(false)
        updateSaveButtonState()
        showToast(BaseR.string.download_err)
    }

    private fun copyToCache(sourcePath: String, destPath: String): Boolean {
        val destFile = File(destPath)
        destFile.parentFile?.mkdirs()
        WKFileUtils.getInstance().fileCopy(sourcePath, destPath)
        return destFile.exists() && destFile.length() > 0
    }

    private fun displayBackgroundFromFile(file: File) {
        if (isSvg == 1) {
            val colors = gradientColors() ?: return
            displaySvgGradientOnly()
            val (color1, color2, color3, color4) = colors
            val pco = AndroidUtilities.getPatternColor(color1, color2, color3, color4)
            val svgDrawable = SvgHelper.getBitmap(
                file,
                AndroidUtilities.getScreenWidth(),
                AndroidUtilities.getScreenHeight(),
                pco
            )
            wkVBinding.imageView.setImageBitmap(svgDrawable)
        } else {
            GlideUtils.getInstance().showImg(this, file.absolutePath, wkVBinding.imageView)
        }
    }

    private fun gradientColors(): IntArray? {
        val colorStr = if (Theme.isDark()) darkColors else lightColors
        if (TextUtils.isEmpty(colorStr)) return null
        val parts = colorStr.split(",")
        if (parts.size < 4) return null
        return try {
            intArrayOf(
                Color.parseColor("#" + parts[0]),
                Color.parseColor("#" + parts[1]),
                Color.parseColor("#" + parts[2]),
                Color.parseColor("#" + parts[3])
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        if (!TextUtils.isEmpty(downloadTag)) {
            WKDownloader.instance.pauseDownload(downloadTag)
        }
        super.onDestroy()
    }

    var rotation = 0f

    override fun initListener() {
        wkVBinding.sendLayout.setOnLongClickListener { true }
        wkVBinding.recvLayout.setOnLongClickListener { true }
        wkVBinding.patternLayout.setOnClickListener {
            wkVBinding.patternCB.setChecked(!wkVBinding.patternCB.isChecked, true)
            wkVBinding.imageView.visibility =
                if (wkVBinding.patternCB.isChecked) View.VISIBLE else View.GONE
        }
        wkVBinding.rotateView.setOnClickListener {
            wkVBinding.rotateIV.rotation = rotation
            rotation -= 45
            changeColor()
            wkVBinding.rotateIV.animate().rotationBy(-45f).setDuration(300)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT).start()
        }
        wkVBinding.blurredLayout.setOnClickListener {
            wkVBinding.blurredCB.setChecked(!wkVBinding.blurredCB.isChecked, true)
            wkVBinding.blurView.visibility =
                if (wkVBinding.blurredCB.isChecked) View.VISIBLE else View.GONE
        }

        SingleClickUtil.onSingleClick(wkVBinding.saveTV) {
            if (!wkVBinding.saveTV.isEnabled) {
                showToast(BaseR.string.download_err)
                return@onSingleClick
            }
            var savePath = url
            if (isLocal == 1) {
                val uid = WKConfig.getInstance().uid
                savePath = if (!TextUtils.isEmpty(channelID)) {
                    uid + "_" + channelType + "_" + channelID + ".jpg"
                } else {
                    "$uid.jpg"
                }
                val localPath = WKConstants.chatBgCacheDir + savePath
                val file = File(localPath)
                if (file.exists()) file.delete()
                if (!copyToCache(url, localPath)) {
                    showToast(BaseR.string.download_err)
                    return@onSingleClick
                }
            }
            val isBlurred = if (wkVBinding.blurredCB.isChecked) 1 else 0
            if (isBlurred == 1 && isSvg == 0) {
                wkVBinding.blurView.post {
                    persistBlurredWallpaper(savePath, isBlurred)
                }
            } else {
                if (isSvg == 0) {
                    ChatBgBlurHelper.deleteBlurredCache(savePath)
                }
                saveChatBG(savePath, isSvg, isBlurred)
            }
        }
    }

    private fun originalCachePath(savePath: String): String {
        return if (isLocal == 1) {
            WKConstants.chatBgCacheDir + savePath
        } else {
            ChatBgKeys.cacheFilePath(savePath)
        }
    }

    private fun persistBlurredWallpaper(savePath: String, isBlurred: Int) {
        val blurredPath = ChatBgKeys.blurredCacheFilePath(savePath)
        val captured = ChatBgBlurHelper.captureWallpaperBitmap(
            wkVBinding.imageView,
            wkVBinding.blurView
        )
        val saved = if (captured != null) {
            val result = ChatBgBlurHelper.saveBitmap(blurredPath, captured)
            captured.recycle()
            result
        } else {
            ChatBgBlurHelper.createBlurredCacheFromFile(
                this,
                originalCachePath(savePath),
                blurredPath
            )
        }
        if (!saved) {
            showToast(BaseR.string.download_err)
            return
        }
        saveChatBG(savePath, isSvg, isBlurred)
    }

    private fun changeColor() {
        gradientAngle += 45
        while (gradientAngle >= 360) {
            gradientAngle -= 360
        }
        val colors = gradientColors() ?: return
        val orientation = Theme.getGradientOrientation(gradientAngle)
        wkVBinding.parentView.background = GradientDrawable(
            orientation,
            colors
        )
    }

    private fun saveChatBG(url: String, isSvg: Int, isBlurred: Int) {
        if (TextUtils.isEmpty(channelID)) {
//            val userInfoEntity = WKConfig.getInstance().userInfo
            WKSharedPreferencesUtil.getInstance().putSPWithUID(ChatBgKeys.chatBgUrl, url)
            WKSharedPreferencesUtil.getInstance()
                .putIntWithUID(ChatBgKeys.chatBgIsBlurred, isBlurred)
            WKSharedPreferencesUtil.getInstance().putIntWithUID(ChatBgKeys.chatBgIsSvg, isSvg)
            WKSharedPreferencesUtil.getInstance().putIntWithUID(
                ChatBgKeys.chatBgShowPattern,
                if (wkVBinding.patternCB.isChecked) 1 else 0
            )
            WKSharedPreferencesUtil.getInstance()
                .putIntWithUID(ChatBgKeys.chatBgIsDeleted, if (TextUtils.isEmpty(url)) 1 else 0)
//            userInfoEntity.chat_bg_url = url
//            userInfoEntity.chat_bg_is_blurred = isBlurred
//            userInfoEntity.chat_bg_is_svg = isSvg
//            userInfoEntity.chat_bg_show_pattern =
//                if (wkVBinding.patternCB.isChecked) 1 else 0
//            userInfoEntity.chat_bg_is_deleted = if (TextUtils.isEmpty(url)) 1 else 0
//            WKConfig.getInstance().saveUserInfo(userInfoEntity)
            WKSharedPreferencesUtil.getInstance()
                .putIntWithUID(ChatBgKeys.chatBgGradientAngle, gradientAngle)
//                    WKSharedPreferencesUtil.getInstance()
//                        .putInt(
//                            userInfoEntity.uid + "_" + WKChannelCustomerExtras.chatBgColorIndex,
//                            colorIndex
//                        )
//
            WKSharedPreferencesUtil.getInstance().putSPWithUID(
                ChatBgKeys.chatBgColorLight,
                lightColors
            )
            WKSharedPreferencesUtil.getInstance().putSPWithUID(
                ChatBgKeys.chatBgColorDark,
                darkColors
            )
            setResult(RESULT_OK)
        } else {
            saveChannelChatBG(url, isSvg, isBlurred, if (TextUtils.isEmpty(url)) 1 else 0)
            setResult(RESULT_OK)
        }
        showToast(R.string.save_success)
        finish()
    }

    private fun saveChannelChatBG(url: String, isSvg: Int, isBlurred: Int, isDeleted: Int) {
        val channel = WKIM.getInstance().channelManager.getChannel(
            channelID,
            channelType
        )
        if (channel != null) {
            val showPattern = if (wkVBinding.patternCB.isChecked) 1 else 0
            if (channel.localExtra == null) channel.localExtra = HashMap<String, Any>()
            channel.localExtra[ChatBgKeys.chatBgUrl] = url
            channel.localExtra[ChatBgKeys.chatBgIsSvg] = isSvg
            channel.localExtra[ChatBgKeys.chatBgIsBlurred] = isBlurred
            channel.localExtra[ChatBgKeys.chatBgGradientAngle] = gradientAngle
//            channel.localExtra[WKChannelCustomerExtras.chatBgColorIndex] = colorIndex
            channel.localExtra[ChatBgKeys.chatBgShowPattern] = showPattern
            channel.localExtra[ChatBgKeys.chatBgIsDeleted] = isDeleted
            channel.localExtra[ChatBgKeys.chatBgColorLight] = lightColors
            channel.localExtra[ChatBgKeys.chatBgColorDark] = darkColors
            WKIM.getInstance().channelManager.saveOrUpdateChannel(channel)
        }
    }

}