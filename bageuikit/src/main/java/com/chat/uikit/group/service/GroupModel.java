package com.chat.uikit.group.service;

import android.text.TextUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.BageBaseModel;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageConfig;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.ICommonListener;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageReader;
import com.chat.uikit.group.GroupEntity;
import com.chat.uikit.group.service.entity.GroupMember;
import com.chat.uikit.group.service.entity.GroupQr;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelMemberExtras;
import com.bage.im.entity.BageChannelType;
import com.bage.im.interfaces.IChannelMemberListResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 2019-11-30 10:25
 * 群相关处理
 */
public class GroupModel extends BageBaseModel {

    private GroupModel() {
    }

    private static class GroupModelBinder {
        private final static GroupModel groupModel = new GroupModel();
    }

    public static GroupModel getInstance() {
        return GroupModelBinder.groupModel;
    }

    /**
     * 创建群组
     *
     * @param name 群名
     * @param ids  成员
     */
    public void createGroup(String name, List<String> ids, List<String> names, final IGroupInfo iGroupInfo) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(ids);
        jsonObject.put("members", jsonArray);
        JSONArray jsonArray1 = new JSONArray();
        jsonArray1.addAll(names);
        jsonObject.put("member_names", jsonArray1);
        jsonObject.put("msg_auto_delete", BageConfig.getInstance().getUserInfo().msg_expire_second);
        request(createService(GroupService.class).createGroup(jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(GroupEntity groupEntity) {
                BageChannel channel = new BageChannel();
                channel.channelID = groupEntity.group_no;
                channel.channelType = BageChannelType.GROUP;
                channel.channelName = groupEntity.name;
                BageIM.getInstance().getChannelManager().saveOrUpdateChannel(channel);
                iGroupInfo.onResult(HttpResponseCode.success, "", groupEntity);
            }

            @Override
            public void onFail(int code, String msg) {
                iGroupInfo.onResult(code, msg, null);
            }
        });
    }

    public interface IGroupInfo {
        void onResult(int code, String msg, GroupEntity groupEntity);
    }

    /**
     * 添加群成员
     *
     * @param groupNo 群号
     * @param ids     成员
     */
    public void addGroupMembers(String groupNo, List<String> ids, List<String> names, final ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(ids);
        jsonObject.put("members", jsonArray);
        JSONArray nameArr = new JSONArray();
        nameArr.addAll(names);
        jsonObject.put("names", nameArr);
        request(createService(GroupService.class).addGroupMembers(groupNo, jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 邀请加入群聊
     *
     * @param groupNo         群编号
     * @param ids             用户id
     * @param iCommonListener 返回
     */
    public void inviteGroupMembers(String groupNo, List<String> ids, final ICommonListener iCommonListener) {
        JSONObject jsonObject1 = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(ids);
        jsonObject1.put("uids", jsonArray);
        jsonObject1.put("remark", "");
        request(createService(GroupService.class).inviteGroupMembers(groupNo, jsonObject1), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }


    /**
     * 获取群详情
     *
     * @param groupNo     群编号
     * @param iGetChannel 返回
     */
    public void getGroupInfo(String groupNo, final BageCommonModel.IGetChannel iGetChannel) {
        BageCommonModel.getInstance().getChannel(groupNo, BageChannelType.GROUP, (code, msg, entity) -> {
            if (iGetChannel != null) {
                iGetChannel.onResult(code, msg, entity);
            }
        });
    }


    public void getChannelMembers(String groupNO, String keyword, int page, int limit, IChannelMemberListResult iChannelMemberListResult) {
        request(createService(GroupService.class).groupMembers(groupNO, keyword, page, limit), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<GroupMember> result) {
                List<BageChannelMember> list = serialize(result);
                iChannelMemberListResult.onResult(list);
            }

            @Override
            public void onFail(int code, String msg) {

            }
        });
    }

    /**
     * 同步群成员
     *
     * @param groupNo 群编号
     */
    public synchronized void groupMembersSync(String groupNo, final ICommonListener iCommonListener) {
        long version = BageIM.getInstance().getChannelMembersManager().getMaxVersion(groupNo, BageChannelType.GROUP);
        request(createService(GroupService.class).syncGroupMembers(groupNo, 1000, version), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<GroupMember> list) {
                if (BageReader.isNotEmpty(list)) {
                    List<BageChannelMember> members = serialize(list);
                    BageIM.getInstance().getChannelMembersManager().save(members);
                    AndroidUtilities.runOnUIThread(() -> groupMembersSync(groupNo, iCommonListener), 500);
                } else {
                    if (iCommonListener != null)
                        iCommonListener.onResult(HttpResponseCode.success, "");
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (iCommonListener != null) iCommonListener.onResult(code, msg);
            }
        });

    }

    private List<BageChannelMember> serialize(List<GroupMember> list) {
        List<BageChannelMember> members = new ArrayList<>();
        if (BageReader.isEmpty(list)) {
            return members;
        }
        for (int i = 0, size = list.size(); i < size; i++) {
            BageChannelMember member = new BageChannelMember();
            member.memberUID = list.get(i).uid;
            member.memberRemark = list.get(i).remark;
            member.memberName = list.get(i).name;
            member.channelID = list.get(i).group_no;
            member.channelType = BageChannelType.GROUP;
            member.isDeleted = list.get(i).is_deleted;
            member.version = list.get(i).version;
            member.role = list.get(i).role;
            member.status = list.get(i).status;
            member.memberInviteUID = list.get(i).invite_uid;
            member.robot = list.get(i).robot;
            member.forbiddenExpirationTime = list.get(i).forbidden_expir_time;
            if (member.robot == 1 && !TextUtils.isEmpty(list.get(i).username)) {
                member.memberName = list.get(i).username;
            }
            member.updatedAt = list.get(i).updated_at;
            member.createdAt = list.get(i).created_at;
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put(BageChannelMemberExtras.BageCode, list.get(i).vercode);
            member.extraMap = hashMap;
            members.add(member);
        }
        return members;
    }

    /**
     * 修改群设置
     *
     * @param groupNo         群编号
     * @param key             修改字段
     * @param value           修改值
     * @param iCommonListener 返回
     */
    public void updateGroupSetting(String groupNo, String key, int value, final ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, value);
        request(createService(GroupService.class).updateGroupSetting(groupNo, jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    public void updateGroupSetting(String groupNo, String key, String value, final ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, value);
        request(createService(GroupService.class).updateGroupSetting(groupNo, jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 修改群信息
     *
     * @param groupNo         群编号
     * @param key             修改字段
     * @param value           修改值
     * @param iCommonListener 返回
     */
    public void updateGroupInfo(String groupNo, String key, String value, final ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, value);
        request(createService(GroupService.class).updateGroupInfo(groupNo, jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 删除群成员
     *
     * @param groupNo         群编号
     * @param uidList         用户ID
     * @param iCommonListener 返回
     */
    public void deleteGroupMembers(String groupNo, List<String> uidList, List<String> names, final ICommonListener iCommonListener) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(uidList);
        jsonObject.put("members", jsonArray);
        JSONArray nameArr = new JSONArray();
        nameArr.addAll(names);
        jsonObject.put("names", nameArr);
        request(createService(GroupService.class).deleteGroupMembers(groupNo, jsonObject), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                List<BageChannelMember> list = new ArrayList<>();
                for (int i = 0, size = uidList.size(); i < size; i++) {
                    BageChannelMember member = new BageChannelMember();
                    member.isDeleted = 1;
                    member.channelID = groupNo;
                    member.channelType = BageChannelType.GROUP;
                    member.memberUID = uidList.get(i);
                    list.add(member);
                }
                BageIM.getInstance().getChannelMembersManager().delete(list);
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 修改群成员信息
     *
     * @param groupNo         群号
     * @param uid             用户ID
     * @param key             主键
     * @param value           修改值
     * @param iCommonListener 返回
     */
    public void updateGroupMemberInfo(String groupNo, String uid, String key, String value, final ICommonListener iCommonListener) {
        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put(key, value);
        request(createService(GroupService.class).updateGroupMemberInfo(groupNo, uid, jsonObject1), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                if (key.equalsIgnoreCase("remark")) {
                    //sdk层数据库修改
                    BageIM.getInstance().getChannelMembersManager().updateRemarkName(groupNo, BageChannelType.GROUP, uid, value);
                }
                iCommonListener.onResult(result.status, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

    /**
     * 群二维码
     *
     * @param groupID  群号
     * @param iGroupQr 返回
     */
    void getGroupQr(String groupID, final IGroupQr iGroupQr) {
        request(createService(GroupService.class).getGroupQr(groupID), new IRequestResultListener<>() {
            @Override
            public void onSuccess(GroupQr result) {
                iGroupQr.onResult(HttpResponseCode.success, "", result.day, result.qrcode, result.expire);
            }

            @Override
            public void onFail(int code, String msg) {
                iGroupQr.onResult(code, msg, 0, "", "");
            }
        });

    }

    public interface IGroupQr {
        void onResult(int code, String msg, int day, String qrCode, String expire);
    }

    /**
     * 我保存的群聊
     *
     * @param iGetMyGroups 返回
     */
    public void getMyGroups(final IGetMyGroups iGetMyGroups) {
        request(createService(GroupService.class).getMyGroups(), new IRequestResultListener<>() {
            @Override
            public void onSuccess(List<GroupEntity> result) {
                iGetMyGroups.onResult(HttpResponseCode.success, "", result);
            }

            @Override
            public void onFail(int code, String msg) {
                iGetMyGroups.onResult(code, msg, null);
            }
        });
    }

    public interface IGetMyGroups {
        void onResult(int code, String msg, List<GroupEntity> list);
    }

    public void exitGroup(String groupNo, final ICommonListener iCommonListener) {
        request(createService(GroupService.class).exitGroup(groupNo), new IRequestResultListener<>() {
            @Override
            public void onSuccess(CommonResponse result) {
                iCommonListener.onResult(HttpResponseCode.success, result.msg);
            }

            @Override
            public void onFail(int code, String msg) {
                iCommonListener.onResult(code, msg);
            }
        });
    }

}
