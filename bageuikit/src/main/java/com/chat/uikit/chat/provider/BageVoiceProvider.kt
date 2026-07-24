package com.chat.uikit.chat.provider

import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chat.base.config.BageApiConfig
import com.chat.base.config.BageConfig
import com.chat.base.config.BageConstants
import com.chat.base.msgitem.BageChatBaseProvider
import com.chat.base.msgitem.BageChatIteMsgFromType
import com.chat.base.msgitem.BageContentType
import com.chat.base.msgitem.BageUIChatMsgItemEntity
import com.chat.base.net.ud.BageProgressManager
import com.chat.base.net.ud.BageDownloader
import com.chat.base.ui.Theme
import com.chat.base.ui.components.SecretDeleteTimer
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.BageCommonUtils
import com.chat.base.utils.BageFileUtils
import com.chat.base.utils.BageTimeUtils
import com.chat.base.utils.BageToastUtils
import com.chat.base.views.BubbleLayout
import com.chat.uikit.R
import com.chat.uikit.message.MsgModel
import com.chat.uikit.view.CircleProgress
import com.chat.uikit.view.BagePlayVoiceUtils
import com.chat.uikit.view.BagePlayVoiceUtils.IPlayListener
import com.chat.uikit.view.WaveformView
import com.bage.im.BageIM
import com.bage.im.message.type.BageMsgContentType
import com.bage.im.msgmodel.BageVoiceContent
import java.io.File
import kotlin.math.max

class BageVoiceProvider : BageChatBaseProvider() {
    private var lastClientMsgNo: String? = null

    override fun getChatViewItem(parentView: ViewGroup, from: BageChatIteMsgFromType): View? {
        return LayoutInflater.from(context).inflate(R.layout.chat_item_voice, parentView, false)
    }

    override fun setData(
        adapterPosition: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        val contentLayout = parentView.findViewById<LinearLayout>(R.id.contentLayout)

        val voiceTimeTv = parentView.findViewById<TextView>(R.id.voiceTimeTv)
        val voiceWaveform = parentView.findViewById<WaveformView>(R.id.voiceWaveform)
        val playBtn = parentView.findViewById<CircleProgress>(R.id.playBtn)
        playBtn.setProgressColor(Theme.colorAccount)

        resetCellBackground(parentView, uiChatMsgItemEntity, from)
        if (from == BageChatIteMsgFromType.SEND) {
            contentLayout.gravity = Gravity.END
            voiceTimeTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            playBtn.setShadowColor(ContextCompat.getColor(context, R.color.white))
        } else {
            contentLayout.gravity = Gravity.START
            voiceTimeTv.setTextColor(ContextCompat.getColor(context, R.color.color999))
            playBtn.setShadowColor(ContextCompat.getColor(context, R.color.homeColor))
        }

        playBtn.setBindId(uiChatMsgItemEntity.bageMsg.clientMsgNO)
        voiceWaveform.setBind(uiChatMsgItemEntity.bageMsg.clientMsgNO)
        val isSender = from == BageChatIteMsgFromType.SEND
        voiceWaveform.setSenderWaveStyle(isSender)
        if (BagePlayVoiceUtils.getInstance().playKey != uiChatMsgItemEntity.bageMsg.clientMsgNO) {
            if (isSender) {
                voiceWaveform.isFresh = false
            } else voiceWaveform.isFresh = uiChatMsgItemEntity.bageMsg.voiceStatus == 0
        }
        val isCurrentPlaying = uiChatMsgItemEntity.isPlaying
                || (BagePlayVoiceUtils.getInstance().playKey == uiChatMsgItemEntity.bageMsg.clientMsgNO
                && BagePlayVoiceUtils.getInstance().isPlaying)
        if (isSender) {
            voiceWaveform.applySenderPlayingState(isCurrentPlaying)
        }
        val voiceContent = uiChatMsgItemEntity.bageMsg.baseContentMsgModel as BageVoiceContent
        voiceWaveform.layoutParams.width =
            getVoiceWidth(voiceContent.timeTrad, uiChatMsgItemEntity.bageMsg.flame)
        if (!TextUtils.isEmpty(voiceContent.waveform)) {
            val bytes = BageCommonUtils.getInstance().base64Decode(voiceContent.waveform)
            voiceWaveform.setWaveform(bytes)
        }
        if (TextUtils.isEmpty(voiceContent.localPath)) {
            playBtn.enableDownload()
        } else {
            if (!uiChatMsgItemEntity.isPlaying) {
                playBtn.setPlay()
            } else playBtn.setPause()
        }
        val showTime: String
        var mStr: String
        var sStr: String
        if (voiceContent.timeTrad >= 60) {
            val m = voiceContent.timeTrad / 60
            val s = voiceContent.timeTrad % 60
            mStr = m.toString()
            sStr = s.toString()
            if (m < 10) {
                mStr = "0$m"
            }
            if (s < 10) {
                sStr = "0$s"
            }
        } else {
            mStr = "00"
            sStr = if (voiceContent.timeTrad < 10) {
                "0" + voiceContent.timeTrad
            } else voiceContent.timeTrad.toString()
        }
        showTime = String.format("%s:%s", mStr, sStr)
        voiceTimeTv.text = showTime
        playBtn.setOnClickListener {
            lastClientMsgNo = uiChatMsgItemEntity.bageMsg.clientMsgNO
            if (TextUtils.isEmpty(voiceContent.localPath)) {
                stopPlay()
                val fileDir =
                    BageConstants.voiceDir + uiChatMsgItemEntity.bageMsg.channelType + "/" + uiChatMsgItemEntity.bageMsg.channelID + "/"
                BageFileUtils.getInstance().createFileDir(fileDir)
                val filePath = fileDir + uiChatMsgItemEntity.bageMsg.clientMsgNO + ".amr"
                val file = File(filePath)
                if (file.exists()) {
                    playBtn.setPlay()
                    BagePlayVoiceUtils.getInstance()
                        .playVoice(filePath, uiChatMsgItemEntity.bageMsg.clientMsgNO)
                    updateViewed(uiChatMsgItemEntity, parentView, from)
                } else {
                    playBtn.enableLoading(1)
                    BageDownloader.instance.download(
                        BageApiConfig.getShowUrl(voiceContent.url),
                        filePath,
                        object : BageProgressManager.IProgress {
                            override fun onProgress(tag: Any?, progress: Int) {
                                playBtn.enableLoading(progress)
                            }

                            override fun onSuccess(tag: Any?, path: String?) {
                                if (!TextUtils.isEmpty(filePath)) {
                                    voiceContent.localPath = filePath
                                    uiChatMsgItemEntity.bageMsg.voiceStatus = 1
                                    uiChatMsgItemEntity.bageMsg.baseContentMsgModel = voiceContent
                                    BageIM.getInstance().msgManager.updateContentAndRefresh(
                                        uiChatMsgItemEntity.bageMsg.clientMsgNO,
                                        voiceContent,
                                        false
                                    )
                                    BageIM.getInstance().msgManager.updateVoiceReadStatus(
                                        uiChatMsgItemEntity.bageMsg.clientMsgNO,
                                        1,
                                        false
                                    )
                                    updateViewed(uiChatMsgItemEntity, parentView, from)
                                    MsgModel.getInstance().updateVoiceStatus(
                                        uiChatMsgItemEntity.bageMsg.messageID,
                                        uiChatMsgItemEntity.bageMsg.channelID,
                                        uiChatMsgItemEntity.bageMsg.channelType,
                                        uiChatMsgItemEntity.bageMsg.messageSeq
                                    )
                                    if (!TextUtils.isEmpty(lastClientMsgNo) && lastClientMsgNo == uiChatMsgItemEntity.bageMsg.clientMsgNO) {
                                        playBtn.setPlay()
                                        //  voiceWaveform.setFresh(uiChatMsgItemEntity.bageMsg.voiceStatus == 0);
                                        BagePlayVoiceUtils.getInstance()
                                            .playVoice(
                                                filePath,
                                                uiChatMsgItemEntity.bageMsg.clientMsgNO
                                            )
                                    }
                                }

                            }

                            override fun onFail(tag: Any?, msg: String?) {
                                BageToastUtils.getInstance()
                                    .showToastNormal(context.getString(R.string.voice_download_fail))
                            }

                        })
                }
            } else {
                if (BagePlayVoiceUtils.getInstance().isPlaying) {
                    if (BagePlayVoiceUtils.getInstance()
                            .oldPlayKey == uiChatMsgItemEntity.bageMsg.clientMsgNO
                    ) {
                        BagePlayVoiceUtils.getInstance().onPause()
                        playBtn.setPlay()
                    } else {

                        stopPlay()
                        updateViewed(uiChatMsgItemEntity, parentView, from)
                        BagePlayVoiceUtils.getInstance()
                            .playVoice(
                                voiceContent.localPath,
                                uiChatMsgItemEntity.bageMsg.clientMsgNO
                            )
                    }
                } else {
                    val file = File(voiceContent.localPath)
                    if (file.exists()) {
                        updateViewed(uiChatMsgItemEntity, parentView, from)
                        BagePlayVoiceUtils.getInstance()
                            .playVoice(
                                voiceContent.localPath,
                                uiChatMsgItemEntity.bageMsg.clientMsgNO
                            )
                    } else {
                        stopPlay()
                    }
                }
            }
        }
        BagePlayVoiceUtils.getInstance().setPlayListener(object : IPlayListener {
            override fun onCompletion(key: String) {
                if (key == uiChatMsgItemEntity.bageMsg.clientMsgNO) {
                    voiceWaveform.setProgress(0f)
                    playBtn.setPlay()
                    uiChatMsgItemEntity.isPlaying = false
                    voiceWaveform.isFresh = false
                    if (isSender) voiceWaveform.applySenderPlayingState(false)
                    playNext(key)
                }
            }

            override fun onProgress(key: String, pg: Float) {
                if (key == uiChatMsgItemEntity.bageMsg.clientMsgNO) {
                    voiceWaveform.setProgress(pg)
                    playBtn.setPause()
                    uiChatMsgItemEntity.isPlaying = true
                    if (isSender) voiceWaveform.applySenderPlayingState(true)
                }
            }

            override fun onStop(key: String) {
                if (key == uiChatMsgItemEntity.bageMsg.clientMsgNO) {
                    voiceWaveform.setProgress(0f)
                    playBtn.setPlay()
                    uiChatMsgItemEntity.isPlaying = false
                    voiceWaveform.isFresh = false
                    if (isSender) voiceWaveform.applySenderPlayingState(false)
                }
            }
        })
    }

    override val itemViewType: Int
        get() = BageMsgContentType.Bage_VOICE


    private fun stopPlay() {
        BagePlayVoiceUtils.getInstance().stopPlay()
        var i = 0
        val size = getAdapter()!!.data.size
        while (i < size) {
            if (getAdapter()!!.data[i].bageMsg != null
                && getAdapter()!!.data[i].bageMsg.clientMsgNO == BagePlayVoiceUtils.getInstance().oldPlayKey
            ) {
                getAdapter()!!.data[i].isPlaying = false
                val waveformView =
                    getAdapter()!!.getViewByPosition(i, R.id.voiceWaveform) as WaveformView?
                val tempPlayBtn =
                    getAdapter()!!.getViewByPosition(i, R.id.playBtn) as CircleProgress?
                waveformView?.setProgress(0f)
                waveformView?.applySenderPlayingState(false)
                tempPlayBtn?.setPlay()
                break
            }
            i++
        }
    }

    private fun playNext(clientMsgNO: String) {
        val list: List<BageUIChatMsgItemEntity> = getAdapter()!!.data
        if (list.isNotEmpty()) {
            for (i in list.indices) {
                val mMsg = list[i].bageMsg
                if (mMsg != null && mMsg.type == BageContentType.Bage_VOICE && mMsg.clientMsgNO != clientMsgNO && mMsg.voiceStatus == 0 && !TextUtils.isEmpty(
                        mMsg.fromUID
                    )
                    && mMsg.fromUID != BageConfig.getInstance().uid
                ) {
                    val tempPlayBtn =
                        getAdapter()!!.getViewByPosition(i, R.id.playBtn) as CircleProgress?
                    tempPlayBtn?.performClick()
                    break
                }
            }
        }
    }

    private fun getVoiceWidth(timeTrad: Int, flame: Int): Int {
        var showWidth = 0
        val minWidth = AndroidUtilities.dp(150f)
        if (timeTrad <= 10) {
            showWidth = minWidth
        } else if (timeTrad <= 20) {
            showWidth = (minWidth * 1.1).toInt()
        } else if (timeTrad <= 30) {
            showWidth = (minWidth * 1.2).toInt()
        } else if (timeTrad <= 40) {
            showWidth = (minWidth * 1.3).toInt()
        } else if (timeTrad <= 50) {
            showWidth = (minWidth * 1.4).toInt()
        } else if (timeTrad <= 60) {
            showWidth = (minWidth * 1.5).toInt()
        }
        if (flame == 1) {
            showWidth -= AndroidUtilities.dp(45f)
        }
        return showWidth
    }

    fun updateViewed(
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        parentView: View,
        from: BageChatIteMsgFromType
    ) {
        if (uiChatMsgItemEntity.bageMsg.flame == 1 && uiChatMsgItemEntity.bageMsg.viewed == 0) {
            uiChatMsgItemEntity.bageMsg.viewed = 1
            uiChatMsgItemEntity.bageMsg.viewedAt =
                BageTimeUtils.getInstance().currentMills
            BageIM.getInstance().msgManager.updateViewedAt(
                1,
                uiChatMsgItemEntity.bageMsg.viewedAt,
                uiChatMsgItemEntity.bageMsg.clientMsgNO
            )
            val parentLayout = parentView as LinearLayout
            var deleteTimer: SecretDeleteTimer? = null
            if (uiChatMsgItemEntity.bageMsg.flameSecond > 0 && parentLayout.childCount > 1) {
                if (from == BageChatIteMsgFromType.RECEIVED) {
                    deleteTimer =
                        parentLayout.getChildAt(1) as SecretDeleteTimer
                } else if (from == BageChatIteMsgFromType.SEND) {
                    deleteTimer =
                        parentLayout.getChildAt(0) as SecretDeleteTimer
                }
                if (deleteTimer != null) {
                    deleteTimer.visibility = View.VISIBLE
                    val flameSecond: Int =
                        if (uiChatMsgItemEntity.bageMsg.type == BageContentType.Bage_VOICE) {
                            val voiceContent =
                                uiChatMsgItemEntity.bageMsg.baseContentMsgModel as BageVoiceContent
                            max(voiceContent.timeTrad, uiChatMsgItemEntity.bageMsg.flameSecond)
                        } else {
                            uiChatMsgItemEntity.bageMsg.flameSecond
                        }
                    deleteTimer.setDestroyTime(
                        uiChatMsgItemEntity.bageMsg.clientMsgNO,
                        flameSecond,
                        uiChatMsgItemEntity.bageMsg.viewedAt,
                        false
                    )
                }
            }
        }
    }

    override fun resetCellBackground(
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellBackground(parentView, uiChatMsgItemEntity, from)
        val bgType = getMsgBgType(
            uiChatMsgItemEntity.previousMsg,
            uiChatMsgItemEntity.bageMsg,
            uiChatMsgItemEntity.nextMsg
        )
        val voiceLayout = parentView.findViewById<BubbleLayout>(R.id.voiceLayout)
        voiceLayout.setAll(bgType, from, BageContentType.Bage_VOICE)

    }

    override fun resetCellListener(
        position: Int,
        parentView: View,
        uiChatMsgItemEntity: BageUIChatMsgItemEntity,
        from: BageChatIteMsgFromType
    ) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from)
        val voiceLayout = parentView.findViewById<BubbleLayout>(R.id.voiceLayout)
        val playBtn = parentView.findViewById<CircleProgress>(R.id.playBtn)
        addLongClick(voiceLayout, uiChatMsgItemEntity)
        addLongClick(playBtn, uiChatMsgItemEntity)
    }
}