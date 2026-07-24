package com.chat.uikit.robot.service;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.BageBaseModel;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.utils.BageReader;
import com.chat.uikit.robot.entity.BageRobotEntity;
import com.chat.uikit.robot.entity.BageRobotInlineQueryResult;
import com.chat.uikit.robot.entity.BageRobotMenuEntity;
import com.chat.uikit.robot.entity.BageSyncRobotEntity;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageRobot;
import com.bage.im.entity.BageRobotMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BageRobotModel extends BageBaseModel {
    private BageRobotModel() {
    }

    private static class BageRobotModelBinder {
        final static BageRobotModel model = new BageRobotModel();
    }

    public static BageRobotModel getInstance() {
        return BageRobotModelBinder.model;
    }

    public void syncRobotData(BageChannel channel) {
        new Thread(() -> sync(channel)).start();
    }

    private void sync(BageChannel channel) {

        boolean isSync = false;
        List<BageRobotEntity> list = new ArrayList<>();
        if (channel.robot == 1) {
            isSync = true;
            BageRobotEntity entity = new BageRobotEntity();
            entity.robot_id = channel.channelID;
            BageRobot robot = BageIM.getInstance().getRobotManager().getWithRobotID(channel.channelID);
            if (robot != null) {
                entity.version = robot.version;
            } else {
                entity.version = 0;
            }
            list.add(entity);
        }
        if (channel.channelType == BageChannelType.GROUP) {
            List<BageChannelMember> memberList = BageIM.getInstance().getChannelMembersManager().getRobotMembers(channel.channelID, channel.channelType);
            if (BageReader.isNotEmpty(memberList)) {
                List<String> robotIds = new ArrayList<>();
                for (BageChannelMember member : memberList) {
                    robotIds.add(member.memberUID);
                }
                List<BageRobot> robotList = BageIM.getInstance().getRobotManager().getWithRobotIds(robotIds);
                if (BageReader.isNotEmpty(robotList)) {
                    for (String robotID : robotIds) {
                        long version = 0;
                        for (BageRobot robot : robotList) {
                            if (robotID.equals(robot.robotID)) {
                                version = robot.version;
                                break;
                            }
                        }
                        list.add(new BageRobotEntity(robotID, version));
                    }
                } else {
                    for (String robotID : robotIds) {
                        list.add(new BageRobotEntity(robotID, 0));
                    }
                }
                isSync = true;
            }
        }
        if (isSync && BageReader.isNotEmpty(list)) {
            BageRobotModel.getInstance().syncRobot(1, list);
        }
    }

    public void syncRobot(int syncType, List<BageRobotEntity> list) {
        JSONArray jsonArray = new JSONArray();
        for (BageRobotEntity entity : list) {
            JSONObject jsonObject = new JSONObject();
            if (syncType == 1) {
                jsonObject.put("robot_id", entity.robot_id);
            } else
                jsonObject.put("username", entity.username);
            jsonObject.put("version", entity.version);
            jsonArray.add(jsonObject);
        }
        request(createService(BageRobotService.class).syncRobot(jsonArray), new IRequestResultListener<List<BageSyncRobotEntity>>() {
            @Override
            public void onSuccess(List<BageSyncRobotEntity> result) {
                List<BageRobot> robotList = new ArrayList<>();
                List<BageRobotMenu> menuList = new ArrayList<>();
                if (BageReader.isNotEmpty(result)) {
                    for (BageSyncRobotEntity entity : result) {
                        BageRobot robot = new BageRobot();
                        robot.username = entity.username;
                        robot.placeholder = entity.placeholder;
                        robot.inlineOn = entity.inline_on;
                        robot.robotID = entity.robot_id;
                        robot.status = entity.status;
                        robot.version = entity.version;
                        robot.updatedAT = entity.updated_at;
                        robot.createdAT = entity.created_at;
                        robotList.add(robot);

                        if (BageReader.isNotEmpty(entity.menus)) {
                            for (BageRobotMenuEntity mRobotMenuEntity : entity.menus) {
                                BageRobotMenu menu = new BageRobotMenu();
                                menu.cmd = mRobotMenuEntity.cmd;
                                menu.type = mRobotMenuEntity.type;
                                menu.remark = mRobotMenuEntity.remark;
                                menu.robotID = mRobotMenuEntity.robot_id;
                                menu.createdAT = mRobotMenuEntity.created_at;
                                menu.updatedAT = mRobotMenuEntity.updated_at;
                                menuList.add(menu);
                            }
                        }
                    }
                }
                // 无数据也调用sdk保存是为了让页面刷新出robot menus
                BageIM.getInstance().getRobotManager().saveOrUpdateRobotMenus(menuList);
                BageIM.getInstance().getRobotManager().saveOrUpdateRobots(robotList);
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    public List<BageRobotMenuEntity> getRobotMenus(String channelID, byte channelType) {
        List<BageRobotMenuEntity> list = new ArrayList<>();
        if (channelType == BageChannelType.PERSONAL) {
            BageRobot robot = BageIM.getInstance().getRobotManager().getWithRobotID(channelID);
            if (robot != null && !TextUtils.isEmpty(robot.robotID) && robot.status == 1) {
                List<BageRobotMenu> menus = BageIM.getInstance().getRobotManager().getRobotMenus(robot.robotID);
                for (BageRobotMenu menu : menus) {
                    BageRobotMenuEntity entity = new BageRobotMenuEntity();
                    entity.robot_id = menu.robotID;
                    entity.cmd = menu.cmd;
                    entity.remark = menu.remark;
                    entity.type = menu.type;
                    list.add(entity);
                }
            }
        } else {
            List<BageChannelMember> memberList = BageIM.getInstance().getChannelMembersManager().getRobotMembers(channelID, channelType);
            if (BageReader.isNotEmpty(memberList)) {
                List<String> robotIds = new ArrayList<>();
                for (BageChannelMember member : memberList) {
                    if (!TextUtils.isEmpty(member.memberUID) && member.robot == 1) {
                        robotIds.add(member.memberUID);
                    }
                }
                if (BageReader.isNotEmpty(robotIds)) {
                    HashMap<String, List<BageRobotMenuEntity>> hashMap = new HashMap<>();
                    List<BageRobot> robotList = BageIM.getInstance().getRobotManager().getWithRobotIds(robotIds);
                    List<BageRobotMenu> menuList = BageIM.getInstance().getRobotManager().getRobotMenus(robotIds);
                    for (BageRobotMenu menu : menuList) {
                        boolean isAddMenu = true;
                        if (BageReader.isNotEmpty(robotList)) {
                            for (BageRobot robot : robotList) {
                                if (menu.robotID.equals(robot.robotID)) {
                                    if (robot.status == 0) {
                                        isAddMenu = false;
                                    }
                                    break;
                                }
                            }
                        }
                        if (!isAddMenu) continue;
                        BageRobotMenuEntity entity = new BageRobotMenuEntity();
                        entity.cmd = menu.cmd;
                        entity.robot_id = menu.robotID;
                        entity.remark = menu.remark;
                        entity.type = menu.type;
                        List<BageRobotMenuEntity> tempList;
                        if (hashMap.containsKey(menu.robotID)) {
                            tempList = hashMap.get(menu.robotID);
                        } else {
                            tempList = new ArrayList<>();
                        }
                        tempList.add(entity);
                        hashMap.put(menu.robotID, tempList);
                    }

                    for (String key : hashMap.keySet()) {
                        list.addAll(hashMap.get(key));
                    }
                }
            }
        }
        return list;
    }

    public void inlineQuery(String offset, String username, String searchContent, String channelID, byte channelType, final InlineQueryListener inlineQueryListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("query", searchContent);
        jsonObject.put("username", username);
        jsonObject.put("channel_id", channelID);
        jsonObject.put("offset", offset);
        jsonObject.put("channel_type", channelType);
        request(createService(BageRobotService.class).inlineQuery(jsonObject), new IRequestResultListener<BageRobotInlineQueryResult>() {
            @Override
            public void onSuccess(BageRobotInlineQueryResult result) {
                inlineQueryListener.onResult(HttpResponseCode.success, "", result);
            }

            @Override
            public void onFail(int code, String msg) {
                inlineQueryListener.onResult(code, msg, null);
            }
        });
    }

    public interface InlineQueryListener {
        void onResult(int code, String msg, BageRobotInlineQueryResult result);
    }
}
