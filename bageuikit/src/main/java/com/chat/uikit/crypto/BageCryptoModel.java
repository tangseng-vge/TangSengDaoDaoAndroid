package com.chat.uikit.crypto;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.BageBaseModel;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.IRequestResultListener;
import com.chat.uikit.enity.BageSignalData;
import com.bage.im.entity.BageChannelType;

public class BageCryptoModel extends BageBaseModel {
    private BageCryptoModel() {
    }

    private static class CryptoModelBinder {
        final static BageCryptoModel model = new BageCryptoModel();
    }

    public static BageCryptoModel getInstance() {
        return CryptoModelBinder.model;
    }

    public void getUserKey(String uid, final @NonNull ISignalData iSignalData) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channel_id", uid);
        jsonObject.put("channel_type", BageChannelType.PERSONAL);
        request(createService(BageCryptoService.class).getUserSignalData(jsonObject), new IRequestResultListener<BageSignalData>() {
            @Override
            public void onSuccess(BageSignalData result) {
                iSignalData.onResult(HttpResponseCode.success, "", result);
            }

            @Override
            public void onFail(int code, String msg) {
                iSignalData.onResult(code, msg, null);
            }
        });
    }

   public interface ISignalData {
        void onResult(int code, String msg, BageSignalData data);
    }
}
