package com.chat.sticker.service

import com.chat.base.base.BageBasePresenter
import com.chat.base.base.BageBaseView
import com.chat.sticker.entity.Sticker
import com.chat.sticker.entity.StoreEntity

/**
 * 12/30/20 3:37 PM
 *
 */
interface StickerContact {
    interface StickerPresenter : BageBasePresenter {
        fun storeList(pageIndex: Int)
        fun getStickerByCategory(category: String)
        fun getUserCustomSticker()
    }

    interface StickerView : BageBaseView {
        fun setStoreList(list: List<StoreEntity>)
        fun setCategorySticker(list: List<Sticker>)
        fun setUserCustomSticker(list: List<Sticker>)
    }
}