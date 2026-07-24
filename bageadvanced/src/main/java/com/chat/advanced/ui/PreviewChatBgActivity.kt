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
import com.chat.base.base.BageBaseActivity
import com.chat.base.config.BageApiConfig
import com.chat.base.config.BageConfig
import com.chat.base.config.BageConstants
import com.chat.base.config.BageSharedPreferencesUtil
import com.chat.base.glide.GlideUtils
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageMsgBgType
import com.chat.base.ui.Theme
import com.chat.base.ui.components.CheckBox
import com.chat.base.ui.components.CubicBezierInterpolator
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.BageFileUtils
import com.chat.base.utils.BageTimeUtils
import com.chat.base.utils.SvgHelper
import com.chat.base.net.ud.BageDownloader
import com.chat.base.net.ud.BageProgressManager
import com.chat.base.utils.singleclick.SingleClickUtil
import com.bage.im.BageIM
import java.io.File


class PreviewChatBgActivity : BageBaseActivity<ActPreviewChatBgLayoutBinding>() {
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
        bageVBinding.saveTV.setTextColor(Theme.colorAccount)
        if (Theme.isDark()) {
            bageVBinding.textSizeTv.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            bageVBinding.textSizeTv.setTextColor(ContextCompat.getColor(this, R.color.black))
        }
        initCheckBox(bageVBinding.patternCB)
        initCheckBox(bageVBinding.blurredCB)
        bageVBinding.blurredCB.setChecked(false, true)
        bageVBinding.patternLayout.visibility = if (isSvg == 1) View.VISIBLE else View.GONE
        bageVBinding.rotateView.visibility = if (isSvg == 1) View.VISIBLE else View.GONE
        bageVBinding.blurredLayout.visibility = if (isSvg == 1) View.GONE else View.VISIBLE
        if (TextUtils.isEmpty(url)) bageVBinding.blurredLayout.visibility = View.GONE
        else bageVBinding.blurredLayout.visibility = View.VISIBLE

        if (TextUtils.isEmpty(channelID)) {
//            val userInfoEntity = BageConfig.getInstance().userInfo
            val chatBgUrl =
                BageSharedPreferencesUtil.getInstance().getSPWithUID(ChatBgKeys.chatBgUrl)
            if (!TextUtils.isEmpty(chatBgUrl) && url == chatBgUrl) {
                gradientAngle = BageSharedPreferencesUtil.getInstance()
                    .getIntWithUID(ChatBgKeys.chatBgGradientAngle)
//                colorIndex = BageSharedPreferencesUtil.getInstance()
//                    .getInt(userInfoEntity.uid + "_" + BageChannelCustomerExtras.chatBgColorIndex)
                lightColors = BageSharedPreferencesUtil.getInstance()
                    .getSPWithUID(ChatBgKeys.chatBgColorLight)
                darkColors = BageSharedPreferencesUtil.getInstance()
                    .getSPWithUID(ChatBgKeys.chatBgColorDark)
                val showPattern = BageSharedPreferencesUtil.getInstance()
                    .getIntWithUID(ChatBgKeys.chatBgShowPattern)
                val isBlurred =
                    BageSharedPreferencesUtil.getInstance().getIntWithUID(ChatBgKeys.chatBgIsBlurred)
                bageVBinding.patternCB.setChecked(showPattern == 1, true)
                if (isBlurred == 1) {
                    bageVBinding.blurView.visibility = View.VISIBLE
                }
            }
        } else {
            val channel =
                BageIM.getInstance().channelManager.getChannel(channelID, channelType)
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
                            bageVBinding.patternCB.setChecked(showPattern == 1, true)
                        }
                    }
                    if (isBlurredObject != null) {
                        val isBlurred = isBlurredObject as Int
                        bageVBinding.blurredCB.setChecked(isBlurred == 1, true)
                        if (isBlurred == 1)
                            bageVBinding.blurView.visibility = View.VISIBLE
                    }

                }

            }
        }
        val showTime = BageTimeUtils.getInstance().getNewChatTime(System.currentTimeMillis())
        bageVBinding.msgTimeTv.text = showTime
        bageVBinding.msgTimeTv1.text = showTime
        Theme.setColorFilter(this, bageVBinding.statusIV, R.color.color999)
        bageVBinding.statusIV.setImageDrawable(Theme.getTicksDoubleDrawable())
        bageVBinding.sendLayout.setAll(
            BageMsgBgType.single,
            BageChatIteMsgFromType.SEND,
            BageContentType.Bage_TEXT
        )
        bageVBinding.recvLayout.setAll(
            BageMsgBgType.single,
            BageChatIteMsgFromType.RECEIVED,
            BageContentType.Bage_TEXT
        )

        bageVBinding.progress.setSize(50)
        updateSaveButtonState()
        loadBackgroundPreview()
    }

    private fun updateSaveButtonState() {
        val canSave = when {
            TextUtils.isEmpty(url) -> true
            isLocal == 1 -> File(url).exists()
            else -> previewReady
        }
        bageVBinding.saveTV.isEnabled = canSave
        bageVBinding.saveTV.alpha = if (canSave) 1f else 0.5f
    }

    private fun showPreviewLoading(show: Boolean) {
        bageVBinding.loading.visibility = if (show) View.VISIBLE else View.GONE
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
        return BageApiConfig.getShowUrl(path)
    }

    private fun showImmediatePreview() {
        if (isSvg == 1) {
            displaySvgGradientOnly()
        } else if (!TextUtils.isEmpty(url) || !TextUtils.isEmpty(cover)) {
            GlideUtils.getInstance().showImg(this, previewImageUrl(), bageVBinding.imageView)
        }
    }

    private fun displaySvgGradientOnly() {
        val colors = gradientColors() ?: return
        val orientation = Theme.getGradientOrientation(gradientAngle)
        bageVBinding.parentView.background = GradientDrawable(orientation, colors)
        bageVBinding.imageView.visibility =
            if (bageVBinding.patternCB.isChecked) View.VISIBLE else View.GONE
    }

    private fun downloadBackground() {
        val downloadUrl = BageApiConfig.getShowUrl(url)
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
        BageDownloader.instance.download(
            downloadUrl,
            cachePath,
            object : BageProgressManager.IProgress {
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
        BageFileUtils.getInstance().fileCopy(sourcePath, destPath)
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
            bageVBinding.imageView.setImageBitmap(svgDrawable)
        } else {
            GlideUtils.getInstance().showImg(this, file.absolutePath, bageVBinding.imageView)
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
            BageDownloader.instance.pauseDownload(downloadTag)
        }
        super.onDestroy()
    }

    var rotation = 0f

    override fun initListener() {
        bageVBinding.sendLayout.setOnLongClickListener { true }
        bageVBinding.recvLayout.setOnLongClickListener { true }
        bageVBinding.patternLayout.setOnClickListener {
            bageVBinding.patternCB.setChecked(!bageVBinding.patternCB.isChecked, true)
            bageVBinding.imageView.visibility =
                if (bageVBinding.patternCB.isChecked) View.VISIBLE else View.GONE
        }
        bageVBinding.rotateView.setOnClickListener {
            bageVBinding.rotateIV.rotation = rotation
            rotation -= 45
            changeColor()
            bageVBinding.rotateIV.animate().rotationBy(-45f).setDuration(300)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT).start()
        }
        bageVBinding.blurredLayout.setOnClickListener {
            bageVBinding.blurredCB.setChecked(!bageVBinding.blurredCB.isChecked, true)
            bageVBinding.blurView.visibility =
                if (bageVBinding.blurredCB.isChecked) View.VISIBLE else View.GONE
        }

        SingleClickUtil.onSingleClick(bageVBinding.saveTV) {
            if (!bageVBinding.saveTV.isEnabled) {
                showToast(BaseR.string.download_err)
                return@onSingleClick
            }
            var savePath = url
            if (isLocal == 1) {
                val uid = BageConfig.getInstance().uid
                savePath = if (!TextUtils.isEmpty(channelID)) {
                    uid + "_" + channelType + "_" + channelID + ".jpg"
                } else {
                    "$uid.jpg"
                }
                val localPath = BageConstants.chatBgCacheDir + savePath
                val file = File(localPath)
                if (file.exists()) file.delete()
                if (!copyToCache(url, localPath)) {
                    showToast(BaseR.string.download_err)
                    return@onSingleClick
                }
            }
            val isBlurred = if (bageVBinding.blurredCB.isChecked) 1 else 0
            if (isBlurred == 1 && isSvg == 0) {
                bageVBinding.blurView.post {
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
            BageConstants.chatBgCacheDir + savePath
        } else {
            ChatBgKeys.cacheFilePath(savePath)
        }
    }

    private fun persistBlurredWallpaper(savePath: String, isBlurred: Int) {
        val blurredPath = ChatBgKeys.blurredCacheFilePath(savePath)
        val captured = ChatBgBlurHelper.captureWallpaperBitmap(
            bageVBinding.imageView,
            bageVBinding.blurView
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
        bageVBinding.parentView.background = GradientDrawable(
            orientation,
            colors
        )
    }

    private fun saveChatBG(url: String, isSvg: Int, isBlurred: Int) {
        if (TextUtils.isEmpty(channelID)) {
//            val userInfoEntity = BageConfig.getInstance().userInfo
            BageSharedPreferencesUtil.getInstance().putSPWithUID(ChatBgKeys.chatBgUrl, url)
            BageSharedPreferencesUtil.getInstance()
                .putIntWithUID(ChatBgKeys.chatBgIsBlurred, isBlurred)
            BageSharedPreferencesUtil.getInstance().putIntWithUID(ChatBgKeys.chatBgIsSvg, isSvg)
            BageSharedPreferencesUtil.getInstance().putIntWithUID(
                ChatBgKeys.chatBgShowPattern,
                if (bageVBinding.patternCB.isChecked) 1 else 0
            )
            BageSharedPreferencesUtil.getInstance()
                .putIntWithUID(ChatBgKeys.chatBgIsDeleted, if (TextUtils.isEmpty(url)) 1 else 0)
//            userInfoEntity.chat_bg_url = url
//            userInfoEntity.chat_bg_is_blurred = isBlurred
//            userInfoEntity.chat_bg_is_svg = isSvg
//            userInfoEntity.chat_bg_show_pattern =
//                if (bageVBinding.patternCB.isChecked) 1 else 0
//            userInfoEntity.chat_bg_is_deleted = if (TextUtils.isEmpty(url)) 1 else 0
//            BageConfig.getInstance().saveUserInfo(userInfoEntity)
            BageSharedPreferencesUtil.getInstance()
                .putIntWithUID(ChatBgKeys.chatBgGradientAngle, gradientAngle)
//                    BageSharedPreferencesUtil.getInstance()
//                        .putInt(
//                            userInfoEntity.uid + "_" + BageChannelCustomerExtras.chatBgColorIndex,
//                            colorIndex
//                        )
//
            BageSharedPreferencesUtil.getInstance().putSPWithUID(
                ChatBgKeys.chatBgColorLight,
                lightColors
            )
            BageSharedPreferencesUtil.getInstance().putSPWithUID(
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
        val channel = BageIM.getInstance().channelManager.getChannel(
            channelID,
            channelType
        )
        if (channel != null) {
            val showPattern = if (bageVBinding.patternCB.isChecked) 1 else 0
            if (channel.localExtra == null) channel.localExtra = HashMap<String, Any>()
            channel.localExtra[ChatBgKeys.chatBgUrl] = url
            channel.localExtra[ChatBgKeys.chatBgIsSvg] = isSvg
            channel.localExtra[ChatBgKeys.chatBgIsBlurred] = isBlurred
            channel.localExtra[ChatBgKeys.chatBgGradientAngle] = gradientAngle
//            channel.localExtra[BageChannelCustomerExtras.chatBgColorIndex] = colorIndex
            channel.localExtra[ChatBgKeys.chatBgShowPattern] = showPattern
            channel.localExtra[ChatBgKeys.chatBgIsDeleted] = isDeleted
            channel.localExtra[ChatBgKeys.chatBgColorLight] = lightColors
            channel.localExtra[ChatBgKeys.chatBgColorDark] = darkColors
            BageIM.getInstance().channelManager.saveOrUpdateChannel(channel)
        }
    }

}