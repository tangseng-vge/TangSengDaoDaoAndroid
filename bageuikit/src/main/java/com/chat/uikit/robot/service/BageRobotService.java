package com.chat.uikit.robot.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.uikit.robot.entity.BageRobotInlineQueryResult;
import com.chat.uikit.robot.entity.BageSyncRobotEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BageRobotService {
    @POST("robot/sync")
    Observable<List<BageSyncRobotEntity>> syncRobot(@Body JSONArray jsonArray);

    @POST("robot/inline_query")
    Observable<BageRobotInlineQueryResult> inlineQuery(@Body JSONObject jsonObject);
}
