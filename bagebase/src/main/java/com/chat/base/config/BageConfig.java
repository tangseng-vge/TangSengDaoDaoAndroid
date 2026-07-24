package com.chat.base.config;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.entity.UserInfoSetting;

/**
 * 2019-11-13 10:27
 * 配置文件
 */
public class BageConfig {
    private BageConfig() {
    }

    private static class ConfigBinder {
        private static final BageConfig Bage_CONFIG = new BageConfig();
    }

    public static BageConfig getInstance() {
        return ConfigBinder.Bage_CONFIG;
    }

    public void setUid(String uid) {
        BageSharedPreferencesUtil.getInstance().putSP("bage_uid", uid);
    }

    public String getUid() {
        return BageSharedPreferencesUtil.getInstance().getSP("bage_uid");
    }

    public void setToken(String token) {
        BageSharedPreferencesUtil.getInstance().putSP("bage_token", token);
    }

    public String getToken() {
        return BageSharedPreferencesUtil.getInstance().getSP("bage_token");
    }

    public void setImToken(String imToken) {
        BageSharedPreferencesUtil.getInstance().putSP("bage_im_token", imToken);
    }

    public String getImToken() {
        return BageSharedPreferencesUtil.getInstance().getSP("bage_im_token");
    }

    public void setUserName(String name) {
        BageSharedPreferencesUtil.getInstance().putSP("bage_name", name);
    }

    public String getUserName() {
        return BageSharedPreferencesUtil.getInstance().getSP("bage_name");
    }

    public void clearInfo() {
        setUid("");
        setToken("");
        setImToken("");
        UserInfoEntity userInfoEntity = BageConfig.getInstance().getUserInfo();
        userInfoEntity.token = "";
        userInfoEntity.im_token = "";
        BageConfig.getInstance().saveUserInfo(userInfoEntity);
    }

    public void saveAppConfig(BageAPPConfig BageAPPConfig) {
        String json = new Gson().toJson(BageAPPConfig);
        BageSharedPreferencesUtil.getInstance().putSP("app_config", json);
    }

    public BageAPPConfig getAppConfig() {
        String json = BageSharedPreferencesUtil.getInstance().getSP("app_config");
        BageAPPConfig BageAPPConfig = null;
        if (!TextUtils.isEmpty(json)) {
            BageAPPConfig = new Gson().fromJson(json, BageAPPConfig.class);
        }
        if (BageAPPConfig == null) {
            BageAPPConfig = new BageAPPConfig();
        }
        return BageAPPConfig;
    }

    public void saveUserInfo(UserInfoEntity userInfoEntity) {
        String json = new Gson().toJson(userInfoEntity);
        BageSharedPreferencesUtil.getInstance().putSP("user_info", json);
    }

    public UserInfoEntity getUserInfo() {
        String json = BageSharedPreferencesUtil.getInstance().getSP("user_info");
        UserInfoEntity userInfoEntity = null;
        if (!TextUtils.isEmpty(json)) {
            userInfoEntity = new Gson().fromJson(json, UserInfoEntity.class);
        }
        if (userInfoEntity == null) {
            userInfoEntity = new UserInfoEntity();
        }
        if (userInfoEntity.setting == null)
            userInfoEntity.setting = new UserInfoSetting();
        return userInfoEntity;
    }
}
