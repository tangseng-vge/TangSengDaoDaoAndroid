package com.chat.base.common;

import com.chat.base.entity.AppModule;
import com.chat.base.entity.ChannelInfoEntity;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.entity.AppVersion;
import com.chat.base.entity.BageChannelState;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 4/21/21 6:25 PM
 */
interface BageCommonService {
    @GET("common/appversion/android/{version}")
    Observable<AppVersion> getAppNewVersion(@Path("version") String version);

    @GET("common/appconfig")
    Observable<BageAPPConfig> getAppConfig();

    @GET("channel/state")
    Observable<BageChannelState> getChannelState(@Query("channel_id") String channelID, @Query("channel_type") byte channelType);

    @GET("channels/{channelID}/{channelType}")
    Observable<ChannelInfoEntity> getChannel(@Path("channelID") String channelID, @Path("channelType") byte channelType);

    @GET("common/appmodule")
    Observable<List<AppModule>> getAppModule();
}
