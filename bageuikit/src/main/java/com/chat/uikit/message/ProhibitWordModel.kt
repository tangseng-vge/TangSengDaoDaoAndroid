package com.chat.uikit.message

import com.chat.base.base.BageBaseModel
import com.chat.base.config.BageConstants
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.net.IRequestResultListener
import com.chat.base.utils.BageReader
import com.chat.uikit.db.ProhibitWordDB
import com.chat.uikit.enity.ProhibitWord

class ProhibitWordModel private constructor() : BageBaseModel() {
    companion object {
        val instance = SingletonHolder.holder
    }

    private object SingletonHolder {
        val holder = ProhibitWordModel()
    }

    private var words: ArrayList<ProhibitWord> = ArrayList()
    fun getAll(): List<ProhibitWord> {
        if (words.isEmpty()) {
            words = ProhibitWordDB.instance.getAll()
        }
        return words
    }

    fun sync() {
        if (!BageConstants.isLogin()) return
        val version = ProhibitWordDB.instance.getMaxVersion()
        request(createService(MsgService::class.java).syncProhibitWord(version),
            object : IRequestResultListener<List<ProhibitWord>> {
                override fun onSuccess(result: List<ProhibitWord>) {
                    if (BageReader.isNotEmpty(result)) {
                        ProhibitWordDB.instance.save(result)
                        words.clear()
                        getAll()
                        val list: List<Any>? = EndpointManager.getInstance()
                            .invokes(EndpointCategory.refreshProhibitWord, 1)
                    }
                }

                override fun onFail(code: Int, msg: String?) {
                }
            })
    }
}