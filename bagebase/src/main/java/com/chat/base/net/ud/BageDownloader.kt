package com.chat.base.net.ud

import java.io.File

class BageDownloader private constructor() {

    companion object {
        val instance: BageDownloader by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BageDownloader()
        }
    }

    fun pauseDownload(url: String) {
        Downloader.instance.pauseDownload(url)
    }

    fun download(url: String, savePath: String, iProgress: BageProgressManager.IProgress?) {
        Downloader.instance.download(url, savePath, object : OnDownload {
            override fun invoke(url: String, progress: Int) = if (iProgress != null) {
                iProgress.run { onProgress(url, progress) }
            } else {
                BageProgressManager.instance.seekProgress(url, progress)
            }

        }, object : OnComplete {
            override fun invoke(url: String, file: File) = if (iProgress != null) {
                iProgress.run {
                    onSuccess(url, file.absolutePath)
                }
            } else {
                BageProgressManager.instance.onSuccess(url, file.absolutePath)
            }

        }, object : OnFail {
            override fun invoke(url: String, reason: String) = if (iProgress != null) {
                iProgress.run { onFail(url, reason) }
            } else {
                BageProgressManager.instance.onFail(url, reason)
            }
        })
    }
}