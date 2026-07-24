package com.chat.base.common;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.chat.base.R;
import com.chat.base.BageBaseApplication;
import com.chat.base.base.BageBaseModel;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.AppModule;
import com.chat.base.entity.AppVersion;
import com.chat.base.entity.ChannelInfoEntity;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.entity.BageChannelState;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.DispatchQueuePool;
import com.chat.base.utils.BageDeviceUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageToastUtils;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 4/21/21 6:23 PM
 */
public class BageCommonModel extends BageBaseModel {
    private final DispatchQueuePool dispatchQueuePool = new DispatchQueuePool(3);

    private BageCommonModel() {
    }

    private static class CommonModelBinder {
        final static BageCommonModel model = new BageCommonModel();
    }

    public static BageCommonModel getInstance() {
        return CommonModelBinder.model;
    }

    public void getAppNewVersion(boolean isShowToast, final IAppNewVersion iAppNewVersion) {
        String v = BageDeviceUtils.getInstance().getVersionName(BageBaseApplication.getInstance().getContext());
        request(createService(BageCommonService.class).getAppNewVersion(v), new IRequestResultListener<AppVersion>() {
            @Override
            public void onSuccess(AppVersion result) {
                if ((result == null || TextUtils.isEmpty(result.download_url)) && isShowToast) {
                    BageToastUtils.getInstance().showToastNormal(BageBaseApplication.getInstance().getContext().getString(R.string.is_new_version));
                } else {
                    iAppNewVersion.onNewVersion(result);
                }
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    public interface IAppNewVersion {
        void onNewVersion(AppVersion version);
    }

    public interface IAppConfig {
        void onResult(int code, String msg, BageAPPConfig bageappConfig);
    }

    public void getAppConfig(IAppConfig iAppConfig) {
        request(createService(BageCommonService.class).getAppConfig(), new IRequestResultListener<>() {
            @Override
            public void onSuccess(BageAPPConfig result) {
                BageConfig.getInstance().saveAppConfig(result);
                if (iAppConfig != null) {
                    iAppConfig.onResult(HttpResponseCode.success, "", result);
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (iAppConfig != null) {
                    iAppConfig.onResult(code, msg, null);
                }
            }
        });
    }

    public void getChannelState(String channelID, byte channelType, final IChannelState iChannelState) {
        request(createService(BageCommonService.class).getChannelState(channelID, channelType), new IRequestResultListener<BageChannelState>() {
            @Override
            public void onSuccess(BageChannelState result) {
                iChannelState.onResult(result);
            }

            @Override
            public void onFail(int code, String msg) {
                iChannelState.onResult(null);
            }
        });
    }

    public interface IChannelState {
        void onResult(BageChannelState channelState);
    }


    public void getChannel(String channelID, byte channelType, IGetChannel iGetChannel) {
        dispatchQueuePool.execute(() -> request(createService(BageCommonService.class).getChannel(channelID, channelType), new IRequestResultListener<ChannelInfoEntity>() {
            @Override
            public void onSuccess(ChannelInfoEntity result) {
                saveChannel(result);
                if (iGetChannel != null) {
                    AndroidUtilities.runOnUIThread(() -> iGetChannel.onResult(HttpResponseCode.success, "", result));
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (iGetChannel != null) {
                    AndroidUtilities.runOnUIThread(() -> iGetChannel.onResult(code, msg, null));

                }
            }
        }));

    }

    private void saveChannel(ChannelInfoEntity entity) {
        HashMap<String, Object> hashMap = null;
        BageChannel bageChannel = new BageChannel(entity.channel.channel_id, entity.channel.channel_type);
        BageChannel localChannel = BageIM.getInstance().getChannelManager().getChannel(entity.channel.channel_id, entity.channel.channel_type);
        boolean isRefreshContacts = false;
        if (localChannel != null && !TextUtils.isEmpty(localChannel.channelID)) {
            bageChannel.avatarCacheKey = localChannel.avatarCacheKey;
            hashMap = localChannel.localExtra;

            if (bageChannel.follow != entity.follow || bageChannel.status != entity.status)
                isRefreshContacts = true;
        }
        if (hashMap == null)
            hashMap = new HashMap<>();

        bageChannel.channelName = entity.name;
        bageChannel.avatar = entity.logo;
        bageChannel.channelRemark = entity.remark;
        bageChannel.status = entity.status;
        bageChannel.online = entity.online;
        bageChannel.lastOffline = entity.last_offline;
        bageChannel.receipt = entity.receipt;
        bageChannel.robot = entity.robot;
        bageChannel.category = entity.category;
        bageChannel.top = entity.stick;
        bageChannel.mute = entity.mute;
        bageChannel.showNick = entity.show_nick;
        bageChannel.follow = entity.follow;
        bageChannel.save = entity.save;
        bageChannel.forbidden = entity.forbidden;
        bageChannel.invite = entity.invite;
        bageChannel.flame = entity.flame;
        bageChannel.flameSecond = entity.flame_second;
        bageChannel.deviceFlag = entity.device_flag;
        if (entity.parent_channel != null) {
            bageChannel.parentChannelID = entity.parent_channel.channel_id;
            bageChannel.parentChannelType = entity.parent_channel.channel_type;
        }
        bageChannel.remoteExtraMap = (HashMap) entity.extra;
        hashMap.put(BageChannelExtras.beDeleted, entity.be_deleted);
        hashMap.put(BageChannelExtras.beBlacklist, entity.be_blacklist);
        hashMap.put(BageChannelExtras.notice, entity.notice);
//        hashMap.put(BageChannelCustomerExtras.chatBgUrl, entity.chat_bg_url);
//        hashMap.put(BageChannelCustomerExtras.chatBgIsSvg, entity.chat_bg_is_svg);
//        hashMap.put(BageChannelCustomerExtras.chatBgIsBlurred, entity.chat_bg_is_blurred);
//        hashMap.put(BageChannelCustomerExtras.chatBgIsDeleted, entity.chat_bg_is_deleted);
//        hashMap.put(BageChannelCustomerExtras.chatBgShowPattern, entity.chat_bg_show_pattern);
        bageChannel.localExtra = hashMap;
//
//        if (entity.extra != null) {
//            Set<String> set = entity.extra.keySet();
//            for (String key : set) {
//                bageChannel.remoteExtraMap.put(key, entity.extra.get(key));
//            }
//        }
        BageIM.getInstance().getChannelManager().saveOrUpdateChannel(bageChannel);
        if (isRefreshContacts) {
            EndpointManager.getInstance().invoke(BageConstants.refreshContacts, null);
        }
    }

    public interface IGetChannel {
        void onResult(int code, String msg, ChannelInfoEntity entity);
    }

    public void getAppModule(@NotNull final IAppModule iAppModule) {
        request(createService(BageCommonService.class).getAppModule(), new IRequestResultListener<List<AppModule>>() {
            @Override
            public void onSuccess(List<AppModule> result) {
                String text = BageSharedPreferencesUtil.getInstance().getSPWithUID("app_module");
                List<AppModule> localSavedAppModule = new ArrayList<>();
                if (!TextUtils.isEmpty(text)) {
                    localSavedAppModule = JSON.parseArray(text, AppModule.class);
                }
                List<AppModule> tempList = new ArrayList<>();
                if (BageReader.isNotEmpty(result)) {
                    for (AppModule item : result) {
                        AppModule m = new AppModule();
                        m.setName(item.getName());
                        m.setDesc(item.getDesc());
                        m.setSid(item.getSid());
                        m.setStatus(item.getStatus());
                        if (item.getStatus() == 2) {
                            m.setChecked(true);
                        } else if (item.getStatus() == 0) {
                            m.setChecked(false);
                        } else {
                            if (BageReader.isNotEmpty(localSavedAppModule)) {
                                for (AppModule temp : localSavedAppModule) {
                                    if (temp.getSid().equals(item.getSid())) {
                                        m.setChecked(temp.getChecked());
                                    }
                                }
                            } else {
                                m.setChecked(false);
                            }
                        }
                        tempList.add(m);
                    }
                }
                String json = JSON.toJSONString(tempList);
                BageSharedPreferencesUtil.getInstance().putSPWithUID("app_module", json);

                iAppModule.onResult(HttpResponseCode.success, "", tempList);
            }

            @Override
            public void onFail(int code, String msg) {
                iAppModule.onResult(code, msg, null);
            }
        });
    }

    public interface IAppModule {
        void onResult(int code, String msg, List<AppModule> list);
    }
}
