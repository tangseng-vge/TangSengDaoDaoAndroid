package com.chat.uikit.group;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.chat.base.act.BageWebViewActivity;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatSettingCellMenu;
import com.chat.base.endpoint.entity.PrivacyMessageMenu;
import com.chat.base.entity.ChannelInfoEntity;
import com.chat.base.entity.BageChannelCustomerExtras;
import com.chat.base.entity.BageGroupType;
import com.chat.base.msgitem.BageChannelMemberRole;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageToastUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.base.views.FullyGridLayoutManager;
import com.chat.uikit.Const;
import com.chat.uikit.R;
import com.chat.uikit.chat.search.MessageRecordActivity;
import com.chat.uikit.contacts.ChooseContactsActivity;
import com.chat.uikit.databinding.ActGroupDetailLayoutBinding;
import com.chat.uikit.group.adapter.GroupMemberAdapter;
import com.chat.uikit.group.service.GroupContract;
import com.chat.uikit.group.service.GroupModel;
import com.chat.uikit.group.service.GroupPresenter;
import com.chat.uikit.message.MsgModel;
import com.chat.uikit.user.UserDetailActivity;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 2019-11-30 10:24
 * 群组详情
 */
public class GroupDetailActivity extends BageBaseActivity<ActGroupDetailLayoutBinding> implements GroupContract.GroupView {
    private String groupNo;
    private GroupMemberAdapter groupMemberAdapter;
    private GroupPresenter groupPresenter;
    private int memberRole;
    private BageChannel groupChannel;
    private int groupType = 0;
    private TextView titleTv;
    private boolean isResetMembers = false;

    @Override
    protected ActGroupDetailLayoutBinding getViewBinding() {
        return ActGroupDetailLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        this.titleTv = titleTv;
        titleTv.setText(R.string.chat_info);
    }

    @Override
    protected void initPresenter() {
        groupNo = getIntent().getStringExtra("channelId");
        groupPresenter = new GroupPresenter(this);
    }

    @Override
    protected void initView() {
        Theme.applyAccentSwitchStyle(this, bageVBinding.muteSwitchView);
        Theme.applyAccentSwitchStyle(this, bageVBinding.stickSwitchView);
        Theme.applyAccentSwitchStyle(this, bageVBinding.saveSwitchView);
        Theme.applyAccentSwitchStyle(this, bageVBinding.showNickSwitchView);
        FullyGridLayoutManager layoutManager = new FullyGridLayoutManager(this, 5);
        bageVBinding.userRecyclerView.setLayoutManager(layoutManager);
        groupMemberAdapter = new GroupMemberAdapter(new ArrayList<>());
        bageVBinding.userRecyclerView.setAdapter(groupMemberAdapter);
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        View view = (View) EndpointManager.getInstance().invoke("msg_remind_view", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.msgRemindLayout));
        if (view != null) {
            bageVBinding.msgRemindLayout.removeAllViews();
            bageVBinding.msgRemindLayout.addView(view);
        }
//
//        View findMsgView = (View) EndpointManager.getInstance().invoke("find_msg_view", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.findContentLayout));
//        if (findMsgView != null) {
//            bageVBinding.findContentView.setVisibility(View.VISIBLE);
//            bageVBinding.findContentLayout.removeAllViews();
//            bageVBinding.findContentLayout.addView(findMsgView);
//        }

        View msgReceiptView = (View) EndpointManager.getInstance().invoke("msg_receipt_view", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.msgSettingLayout));
        if (msgReceiptView != null) {
            bageVBinding.msgSettingLayout.removeAllViews();
            bageVBinding.msgSettingLayout.addView(msgReceiptView);
        }

        View msgPrivacyLayout = (View) EndpointManager.getInstance().invoke("chat_setting_msg_privacy", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.msgSettingLayout));
        if (msgPrivacyLayout != null) {
            bageVBinding.msgSettingLayout.addView(msgPrivacyLayout);
        }

        View groupAvatarLayout = (View) EndpointManager.getInstance().invoke("group_avatar_view", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.groupAvatarLayout));
        if (groupAvatarLayout != null) {
            bageVBinding.groupAvatarLayout.addView(groupAvatarLayout);
        }

        View groupManagerLayout = (View) EndpointManager.getInstance().invoke("group_manager_view", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.groupManageLayout));
        if (groupManagerLayout != null) {
            bageVBinding.groupManageLayout.addView(groupManagerLayout);
        }
        View chatPwdView = (View) EndpointManager.getInstance().invoke("chat_pwd_view", new ChatSettingCellMenu(groupNo, BageChannelType.GROUP, bageVBinding.chatPwdView));
        if (chatPwdView != null) {
            bageVBinding.chatPwdView.addView(chatPwdView);
        }
    }

    @Override
    protected void initListener() {
        EndpointManager.getInstance().setMethod("group_detail", EndpointCategory.bageExitChat, object -> {
            if (object != null) {
                BageChannel channel = (BageChannel) object;
                if (groupNo.equals(channel.channelID) && channel.channelType == BageChannelType.GROUP) {
                    finish();
                }
            }
            return null;
        });
        SingleClickUtil.onSingleClick(bageVBinding.findContentLayout, view1 -> {
            if (groupIsEnable()) {
                Intent intent = new Intent(this, MessageRecordActivity.class);
                intent.putExtra("channel_id", groupNo);
                intent.putExtra("channel_type", BageChannelType.GROUP);
                startActivity(intent);
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.remarkLayout, view1 -> {
            if (groupIsEnable()) {
                Intent intent = new Intent(GroupDetailActivity.this, BageSetGroupRemarkActivity.class);
                intent.putExtra("groupNo", groupNo);
                startActivity(intent);
            }
        });
        bageVBinding.showNickSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed() && groupIsEnable()) {
                groupPresenter.updateGroupSetting(groupNo, "show_nick", b ? 1 : 0);
            }
        });
        bageVBinding.saveSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed() && groupIsEnable()) {
                groupPresenter.updateGroupSetting(groupNo, "save", b ? 1 : 0);
            }
        });

        bageVBinding.muteSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed() && groupIsEnable()) {
                groupPresenter.updateGroupSetting(groupNo, "mute", b ? 1 : 0);
            }
        });
        bageVBinding.stickSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed() && groupIsEnable()) {
                groupPresenter.updateGroupSetting(groupNo, "top", b ? 1 : 0);
            }
        });
        groupMemberAdapter.addChildClickViewIds(R.id.handlerIv, R.id.userLayout);
        groupMemberAdapter.setOnItemChildClickListener((adapter, view1, position) -> {
            if (!groupIsEnable()) {
                return;
            }
            BageChannelMember groupMemberEntity = groupMemberAdapter.getItem(position);
            if (groupMemberEntity != null) {
                if (view1.getId() == R.id.handlerIv) {
                    //添加或删除
                    if (groupMemberEntity.memberUID.equalsIgnoreCase("-1")) {
                        //添加
                        String unSelectUidList = "";
                        List<BageChannelMember> list = BageIM.getInstance().getChannelMembersManager().getMembers(groupNo, BageChannelType.GROUP);
                        for (int i = 0, size = list.size(); i < size; i++) {
                            if (TextUtils.isEmpty(unSelectUidList)) {
                                unSelectUidList = list.get(i).memberUID;
                            } else unSelectUidList = unSelectUidList + "," + list.get(i).memberUID;
                        }

                        Intent intent = new Intent(GroupDetailActivity.this, ChooseContactsActivity.class);
                        intent.putExtra("unSelectUids", unSelectUidList);
                        intent.putExtra("isIncludeUids", false);
                        intent.putExtra("groupId", groupNo);
                        intent.putExtra("type", 1);
                        startActivity(intent);
                    } else {
                        //删除
                        Intent intent = new Intent(GroupDetailActivity.this, DeleteGroupMemberActivity.class);
                        intent.putExtra("groupId", groupNo);
                        startActivity(intent);
                    }
                } else if (view1.getId() == R.id.userLayout) {
                    Intent intent = new Intent(GroupDetailActivity.this, UserDetailActivity.class);
                    intent.putExtra("uid", groupMemberEntity.memberUID);
                    intent.putExtra("groupID", groupNo);
                    startActivity(intent);

                }
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.showAllMembersTv, view1 -> {
            if (groupIsEnable()) {
                Intent intent = new Intent(this, BageAllMembersActivity.class);
                intent.putExtra("channelID", groupNo);
                intent.putExtra("channelType", BageChannelType.GROUP);
                startActivity(intent);
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.reportLayout, view1 -> {
            if (groupIsEnable()) {
                Intent intent = new Intent(this, BageWebViewActivity.class);
                intent.putExtra("channelType", BageChannelType.GROUP);
                intent.putExtra("channelID", groupNo);
                intent.putExtra("url", BageApiConfig.baseWebUrl + "report.html");
                startActivity(intent);
            }
        });

        bageVBinding.exitBtn.setOnClickListener(v -> BageDialogUtils.getInstance().showDialog(this, getString(R.string.delete_group), getString(R.string.exit_group_tips), true, "", getString(R.string.delete_group), 0, ContextCompat.getColor(this, R.color.red), index -> {
            if (index == 1) {
                GroupModel.getInstance().exitGroup(groupNo, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        BageIM.getInstance().getMsgManager().clearWithChannel(groupNo, BageChannelType.GROUP);
                        MsgModel.getInstance().offsetMsg(groupNo, BageChannelType.GROUP, null);
                        BageIM.getInstance().getConversationManager().deleteWitchChannel(groupNo, BageChannelType.GROUP);
                        EndpointManager.getInstance().invokes(EndpointCategory.bageExitChat, new BageChannel(groupNo, BageChannelType.GROUP));
                        finish();
                    } else showToast(msg);
                });
            }
        }));

        SingleClickUtil.onSingleClick(bageVBinding.groupQrLayout, view1 -> {
            if (groupIsEnable()) {
                Intent intent = new Intent(this, GroupQrActivity.class);
                intent.putExtra("groupId", groupNo);
                startActivity(intent);
            }
        });
        bageVBinding.clearChatMsgLayout.setOnClickListener(v -> {
            String showName = "";
            if (groupChannel != null) {
                if (TextUtils.isEmpty(groupChannel.channelRemark)) {
                    showName = groupChannel.channelName;
                } else {
                    showName = groupChannel.channelRemark;
                }
            }
            Object object = EndpointManager.getInstance().invoke("is_register_msg_privacy_module", null);
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(groupNo, BageChannelType.GROUP, BageConfig.getInstance().getUid());
            if (member != null && member.role != BageChannelMemberRole.normal) {
                if (object instanceof PrivacyMessageMenu) {
                    String checkBoxText = getString(R.string.str_delete_message_for_all);
                    BageDialogUtils.getInstance().showCheckBoxDialog(this, getString(R.string.clear_history), String.format(getString(R.string.clear_history_tip), showName), checkBoxText, true, "", getString(R.string.delete), 0, ContextCompat.getColor(this, R.color.red), new BageDialogUtils.ICheckBoxDialog() {
                        @Override
                        public void onClick(int index, boolean isChecked) {
                            if (index == 1) {
                                if (isChecked) {
                                    ((PrivacyMessageMenu) object).getIClick().clearChannelMsg(groupNo, BageChannelType.GROUP);
                                } else {
                                    MsgModel.getInstance().offsetMsg(groupNo, BageChannelType.GROUP, null);
                                    BageIM.getInstance().getMsgManager().clearWithChannel(groupNo, BageChannelType.GROUP);
                                    showToast(getString(R.string.cleared));
                                }
                            }
                        }
                    });
                    return;
                }
            }
            BageDialogUtils.getInstance().showDialog(this, getString(R.string.clear_history), String.format(getString(R.string.clear_history_tip), showName), true, "", getString(R.string.delete), 0, ContextCompat.getColor(this, R.color.red), index -> {
                if (index == 1) {
                    MsgModel.getInstance().offsetMsg(groupNo, BageChannelType.GROUP, null);
                    BageIM.getInstance().getMsgManager().clearWithChannel(groupNo, BageChannelType.GROUP);
                    showToast(getString(R.string.cleared));
                }
            });
        });
        bageVBinding.inGroupNameLayout.setOnClickListener(v -> updateNameInGroupDialog());
        SingleClickUtil.onSingleClick(bageVBinding.noticeLayout, view1 -> {
            if (!groupIsEnable()) return;
            String notice = "";
            if (groupChannel.localExtra != null && groupChannel.localExtra.containsKey(BageChannelExtras.notice)) {
                notice = (String) groupChannel.localExtra.get(BageChannelExtras.notice);
            }
            if (TextUtils.isEmpty(notice) && memberRole == BageChannelMemberRole.normal) {
                showSingleBtnDialog(getString(R.string.edit_group_notice));
                return;
            }
            Intent intent = new Intent(this, GroupNoticeActivity.class);
            intent.putExtra("groupNo", groupNo);
            intent.putExtra("oldNotice", notice);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.groupNameLayout, view1 -> {
            if (!groupIsEnable()) return;
            if (memberRole != BageChannelMemberRole.normal) {
                Intent intent = new Intent(this, UpdateGroupNameActivity.class);
                intent.putExtra("groupNo", groupNo);
                startActivity(intent);
            } else showSingleBtnDialog(getString(R.string.edit_group_notice));
        });
        //监听频道改变通知
        BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo("group_detail_refresh_channel", (channel, isEnd) -> {
            if (channel != null) {
                if (channel.channelID.equalsIgnoreCase(groupNo) && channel.channelType == BageChannelType.GROUP) {
                    //同一个会话
                    groupChannel = channel;
                    setData();
                    setNotice();
                }
            }
        });
        //监听频道成员信息改变通知
        BageIM.getInstance().getChannelMembersManager().addOnRefreshChannelMemberInfo("group_detail_refresh_channel_member", (channelMember, isEnd) -> {
            if (channelMember != null) {
                if (channelMember.channelID.equals(groupNo) && channelMember.channelType == BageChannelType.GROUP) {
                    boolean isUpdate = false;
                    //本群内某个成员
                    for (int i = 0, size = groupMemberAdapter.getData().size(); i < size; i++) {
                        if (groupMemberAdapter.getData().get(i).memberUID.equalsIgnoreCase(channelMember.memberUID)) {
                            isUpdate = true;
                            if (groupMemberAdapter.getData().get(i).role != channelMember.role || groupMemberAdapter.getData().get(i).status != channelMember.status) {
                                isResetMembers = true;
                            } else {
                                groupMemberAdapter.getData().get(i).memberName = channelMember.memberName;
                                groupMemberAdapter.getData().get(i).memberRemark = channelMember.memberRemark;
                                groupMemberAdapter.notifyItemChanged(i);
                            }
                            break;
                        }
                    }
                    if (!isUpdate) {
                        isResetMembers = true;
                    }
                }
            }
            if (isEnd && isResetMembers) {
                //如果有角色更改就重新获取成员
                getMembers();
            }
        });
        //移除群成员监听
        BageIM.getInstance().getChannelMembersManager().addOnRemoveChannelMemberListener("group_detail_remove_channel_member", list -> {
            if (BageReader.isNotEmpty(list)) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    for (int j = 0, len = groupMemberAdapter.getData().size(); j < len; j++) {
                        if (list.get(i).memberUID.equalsIgnoreCase(groupMemberAdapter.getData().get(j).memberUID)
                                && list.get(i).channelID.equals(groupMemberAdapter.getData().get(j).channelID)
                                && list.get(i).channelType == BageChannelType.GROUP) {
                            groupMemberAdapter.removeAt(j);
                            break;
                        }
                    }
                }
            }
            if (groupType == BageGroupType.normalGroup) {
                int count = BageIM.getInstance().getChannelMembersManager().getMemberCount(groupNo, BageChannelType.GROUP);
                titleTv.setText(String.format("%s(%s)", getString(R.string.chat_info), count));
            }
        });
        //添加群成员监听
        BageIM.getInstance().getChannelMembersManager().addOnAddChannelMemberListener("group_detail_add_channel_member", list -> {
            //这里这是演示sdk数据转成UI层数据。
            // 当然UI层也可以直接使用sdk的数据库
            List<BageChannelMember> tempList = new ArrayList<>();
            if (BageReader.isNotEmpty(list)) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    if (list.get(i).channelID.equalsIgnoreCase(groupNo)
                            && list.get(i).channelType == BageChannelType.GROUP) {
                        tempList.add(list.get(i));
                    }
                }
                if (groupType == BageGroupType.normalGroup) {
                    int count = BageIM.getInstance().getChannelMembersManager().getMemberCount(groupNo, BageChannelType.GROUP);
                    titleTv.setText(String.format("%s(%s)", getString(R.string.chat_info), count));

                    if (memberRole != BageChannelMemberRole.normal) {
                        groupMemberAdapter.addData(groupMemberAdapter.getData().size() - 2, tempList);
                    } else
                        groupMemberAdapter.addData(groupMemberAdapter.getData().size() - 1, tempList);

                }
            }
        });
        //监听隐藏群管理入口
        EndpointManager.getInstance().setMethod("chat_hide_group_manage_view", object -> {
            bageVBinding.groupManageLayout.setVisibility(View.GONE);
            return null;
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initData() {
        super.initData();

        int count = BageIM.getInstance().getChannelMembersManager().getMemberCount(groupNo, BageChannelType.GROUP);
        titleTv.setText(getString(R.string.chat_info) + "(" + count + ")");
        groupChannel = BageIM.getInstance().getChannelManager().getChannel(groupNo, BageChannelType.GROUP);
        if (groupChannel != null) {
            if (groupChannel.remoteExtraMap != null) {
                if (groupChannel.remoteExtraMap.containsKey(BageChannelExtras.groupType)) {
                    Object groupTypeObject = groupChannel.remoteExtraMap.get(BageChannelExtras.groupType);
                    if (groupTypeObject instanceof Integer) {
                        groupType = (int) groupTypeObject;
                    }
                }
                if (groupType == BageGroupType.superGroup && groupChannel.remoteExtraMap.containsKey(BageChannelCustomerExtras.memberCount)) {
                    Object memberCountObject = groupChannel.remoteExtraMap.get(BageChannelCustomerExtras.memberCount);
                    if (memberCountObject instanceof Integer) {
                        int memberCount = (int) memberCountObject;
                        titleTv.setText(getString(R.string.chat_info) + "(" + memberCount + ")");
                    }
                }
            }

            setData();
            setNotice();
        }
        groupPresenter.getGroupInfo(groupNo);
        getMembers();
    }

    private void getMembers() {
        isResetMembers = false;
        BageIM.getInstance().getChannelMembersManager().getWithPageOrSearch(groupNo, BageChannelType.GROUP, "", 1, 20, (list, b) -> {
            if (groupType == 0)
                resortData(list);
            else {
                if (b) {
                    resortData(list);
                }
            }
        });
    }

    private void resortData(List<BageChannelMember> list) {
        BageChannelMember channelMember = BageIM.getInstance().getChannelMembersManager().getMember(groupNo, BageChannelType.GROUP, BageConfig.getInstance().getUid());
        if (channelMember != null) {
            if (channelMember.memberUID.equals(BageConfig.getInstance().getUid())) {
                String name = channelMember.memberRemark;
                memberRole = channelMember.role;
                if (TextUtils.isEmpty(name))
                    name = channelMember.memberName;
                bageVBinding.inGroupNameTv.setText(name);
            }
        }
        int maxCount;
        if (memberRole != BageChannelMemberRole.normal) {
            maxCount = 18;
        } else {
            maxCount = 19;
        }
        if (list != null) {
            List<BageChannelMember> temp = new ArrayList<>();
            for (int i = 0, size = Math.min(list.size(), maxCount); i < size; i++) {
                if (list.get(i).role == BageChannelMemberRole.admin) {
                    //群主或管理员
                    temp.add(0, list.get(i));
                } else temp.add(list.get(i));
            }
            //添加按钮
            BageChannelMember addUser = new BageChannelMember();
            addUser.memberUID = "-1";
            temp.add(addUser);
            if (memberRole != BageChannelMemberRole.normal) {
                //删除按钮
                BageChannelMember deleteUser = new BageChannelMember();
                deleteUser.memberUID = "-2";
                temp.add(deleteUser);
                bageVBinding.groupManageLayout.setVisibility(View.VISIBLE);
            }
            groupMemberAdapter.setList(temp);
            if (list.size() >= 18) {
                bageVBinding.showAllMembersTv.setVisibility(View.VISIBLE);
            } else bageVBinding.showAllMembersTv.setVisibility(View.GONE);
        }
    }

    @Override
    public void onGroupInfo(ChannelInfoEntity groupEntity) {
        setData();
    }

    @Override
    public void onRefreshGroupSetting(String key, int value) {

    }

    @Override
    public void setQrData(int day, String qrCode, String expire) {

    }

    @Override
    public void setMyGroups(List<GroupEntity> list) {

    }

    @Override
    public void showError(String msg) {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            String content = data.getStringExtra("content");
            bageVBinding.groupNoticeTv.setText(content);
        }
    }

    private void setNotice() {
        HashMap hashMap = groupChannel.localExtra;
        String notice = "";
        if (hashMap != null) {
            if (hashMap.containsKey(BageChannelExtras.notice)) {
                notice = (String) hashMap.get(BageChannelExtras.notice);
            }
        }
        if (!TextUtils.isEmpty(notice)) {
            bageVBinding.unsetNoticeLayout.setVisibility(View.GONE);
            bageVBinding.groupNoticeTv.setVisibility(View.VISIBLE);
            bageVBinding.groupNoticeTv.setText(notice);
        } else {
            bageVBinding.unsetNoticeLayout.setVisibility(View.VISIBLE);
            bageVBinding.groupNoticeTv.setVisibility(View.GONE);
        }

    }

    private void setData() {
        bageVBinding.nameTv.setText(groupChannel.channelName);
        bageVBinding.remarkTv.setText(groupChannel.channelRemark);
        bageVBinding.muteSwitchView.setChecked(groupChannel.mute == 1);
        bageVBinding.stickSwitchView.setChecked(groupChannel.top == 1);
        bageVBinding.saveSwitchView.setChecked(groupChannel.save == 1);
        bageVBinding.showNickSwitchView.setChecked(groupChannel.showNick == 1);


        if (groupType == BageGroupType.superGroup && groupChannel.remoteExtraMap != null && groupChannel.remoteExtraMap.containsKey(BageChannelCustomerExtras.memberCount)) {
            Object memberCountObject = groupChannel.remoteExtraMap.get(BageChannelCustomerExtras.memberCount);
            if (memberCountObject instanceof Integer) {
                int memberCount = (int) memberCountObject;
                String content = String.format("%s(%s)", getString(R.string.chat_info), memberCount);
                titleTv.setText(content);
            }
        }
    }

    private void updateNameInGroupDialog() {
        if (!groupIsEnable()) return;
        String showName = "";
        BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(groupNo, BageChannelType.GROUP, BageConfig.getInstance().getUid());
        if (member != null) {
            String name = member.memberRemark;
            if (TextUtils.isEmpty(name))
                name = member.memberName;
            if (!TextUtils.isEmpty(name)) {
                showName = name;
            }
        }
        BageDialogUtils.getInstance().showInputDialog(this, getString(R.string.my_in_group_name), getString(R.string.update_in_gorup_name), showName, "", 10, text -> {
            if (!TextUtils.isEmpty(text)) {
                GroupModel.getInstance().updateGroupMemberInfo(groupNo, BageConfig.getInstance().getUid(), "remark", text, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        bageVBinding.inGroupNameTv.setText(text);
                    } else BageToastUtils.getInstance().showToastNormal(msg);
                });

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EndpointManager.getInstance().remove("chat_hide_group_manage_view");
        BageIM.getInstance().getChannelManager().removeRefreshChannelInfo("group_detail_refresh_channel");
        BageIM.getInstance().getChannelMembersManager().removeRefreshChannelMemberInfo("group_detail_refresh_channel_member");
        BageIM.getInstance().getChannelMembersManager().removeRemoveChannelMemberListener("group_detail_remove_channel_member");
        BageIM.getInstance().getChannelMembersManager().removeAddChannelMemberListener("group_detail_add_channel_member");
    }

    private boolean groupIsEnable() {
        return groupChannel != null && groupChannel.status != Const.GroupStatusDisband;
    }
}
