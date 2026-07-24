package com.chat.uikit.crypto;

import com.alibaba.fastjson.JSONObject;
import com.chat.uikit.enity.BageSignalData;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BageCryptoService {
    @POST("user/signal/getkey")
    Observable<BageSignalData> getUserSignalData(@Body JSONObject jsonObject);
}
