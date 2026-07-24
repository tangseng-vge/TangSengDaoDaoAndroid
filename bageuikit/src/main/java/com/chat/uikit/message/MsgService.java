package com.chat.uikit.message;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.net.entity.CommonResponse;
import com.chat.uikit.enity.BageSyncReminder;
import com.chat.uikit.enity.ProhibitWord;
import com.chat.uikit.enity.SensitiveWords;
import com.bage.im.entity.BageSyncChannelMsg;
import com.bage.im.entity.BageSyncChat;
import com.bage.im.entity.BageSyncConvMsgExtra;
import com.bage.im.entity.BageSyncExtraMsg;
import com.bage.im.entity.BageSyncMsgReaction;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 2020-07-20 23:18
 * 消息
 */
public interface MsgService {

    @POST("message/revoke")
    Observable<CommonResponse> revokeMsg(@Query("message_id") String message_id, @Query("channel_id") String channel_id, @Query("channel_type") byte channel_type, @Query("client_msg_no") String client_msg_no);

    @PUT("coversation/clearUnread")
    Observable<CommonResponse> clearUnread(@Body JSONObject jsonObject);

    @PUT("message/voicereaded")
    Observable<CommonResponse> updateVoiceStatus(@Body JSONObject jsonObject);

    @GET("users/{uid}/im")
    Observable<Ipentity> getImIp(@Path("uid") String uid);

    @HTTP(method = "DELETE", path = "message", hasBody = true)
    Observable<CommonResponse> deleteMsg(@Body JSONArray jsonArray);

    @POST("message/syncack/{last_message_seq}")
    Observable<CommonResponse> ackMsg(@Path("last_message_seq") int last_message_seq);

    @POST("message/sync")
    Observable<List<SyncMsg>> syncMsg(@Body JSONObject jsonObject);

    @POST("message/typing")
    Observable<CommonResponse> typing(@Body JSONObject jsonObject);

    @POST("message/favorite")
    Observable<CommonResponse> favoriteMessage(@Body JSONObject jsonObject);

    @HTTP(method = "DELETE", path = "message/favorite", hasBody = true)
    Observable<CommonResponse> unfavoriteMessage(@Body JSONObject jsonObject);

    @POST("message/favorites")
    Observable<JSONObject> favoriteMessages(@Body JSONObject jsonObject);

    @POST("conversation/sync")
    Observable<BageSyncChat> syncChat(@Body JSONObject jsonObject);

    @POST("message/channel/sync")
    Observable<BageSyncChannelMsg> syncChannelMsg(@Body JSONObject jsonObject);

    @POST("message/extra/sync")
    Observable<List<BageSyncExtraMsg>> syncExtraMsg(@Body JSONObject jsonObject);


    @POST("message/offset")
    Observable<CommonResponse> offsetMsg(@Body JSONObject jsonObject);

    @GET("message/sync/sensitivewords")
    Observable<SensitiveWords> syncSensitiveWords(@Query("version") long version);

    @POST("conversation/syncack")
    Observable<CommonResponse> ackCoverMsg(@Body JSONObject jsonObject);

    @POST("message/edit")
    Observable<CommonResponse> editMsg(@Body JSONObject jsonObject);

    @POST("message/reminder/sync")
    Observable<List<BageSyncReminder>> syncReminder(@Body JSONObject jsonObject);

    @POST("message/reminder/done")
    Observable<CommonResponse> doneReminder(@Body List<Long> ids);

    @POST("conversations/{channel_id}/{channel_type}/extra")
    Observable<CommonResponse> updateCoverExtra(@Path("channel_id") String channelID, @Path("channel_type") byte channelType, @Body JSONObject jsonObject);

    @POST("conversation/extra/sync")
    Observable<List<BageSyncConvMsgExtra>> syncCoverExtra(@Body JSONObject jsonObject);

    @GET("message/prohibit_words/sync")
    Observable<List<ProhibitWord>> syncProhibitWord(@Query("version") long version);
}
