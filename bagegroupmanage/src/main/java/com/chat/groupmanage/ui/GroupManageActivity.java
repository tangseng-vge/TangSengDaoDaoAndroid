package com.chat.groupmanage.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.msgitem.BageChannelMemberRole;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.groupmanage.Const;
import com.chat.groupmanage.R;
import com.chat.groupmanage.adapter.GroupManagerAdapter;
import com.chat.groupmanage.databinding.ActGroupManageLayoutBinding;
import com.chat.groupmanage.entity.ForbiddenTime;
import com.chat.groupmanage.entity.GroupMemberEntity;
import com.chat.groupmanage.service.GroupManageContract;
import com.chat.groupmanage.service.GroupManageModel;
import com.chat.groupmanage.service.GroupManagePresenter;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-04-06 21:01
 * 群管理
 */
public class GroupManageActivity extends BageBaseActivity<ActGroupManageLayoutBinding> implements GroupManageContract.GroupManageView {
    private String groupId;
    private GroupManagerAdapter adapter;
    private GroupManagePresenter presenter;

    @Override
    protected ActGroupManageLayoutBinding getViewBinding() {
        return ActGroupManageLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_manage);
    }

    @Override
    protected void initPresenter() {
        presenter = new GroupManagePresenter(this);
    }

    @Override
    protected void initView() {
        Theme.applyAccentSwitchStyle(this, bageVBinding.invitConfirmationSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.fullStaffBanSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.forbiddenAddFriendSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.allowNewMembersViewHistoryMsgSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.allowMemberPinnedMessageSwitch);
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        adapter = new GroupManagerAdapter(new ArrayList<>());
        initAdapter(bageVBinding.recyclerView, adapter);
        bageVBinding.recyclerView.setNestedScrollingEnabled(false);
        Object obj = EndpointManager.getInstance().invoke("is_register_pin_msg_module", null);
        if (obj instanceof Boolean) {
            bageVBinding.allowMemberPinnedMessageTV.setVisibility(View.VISIBLE);
            bageVBinding.allowMemberPinnedMessageLayout.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.allowMemberPinnedMessageTV.setVisibility(View.GONE);
            bageVBinding.allowMemberPinnedMessageLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void initListener() {
        adapter.addChildClickViewIds(R.id.removeIv);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> {
            GroupMemberEntity groupMemberEntity = (GroupMemberEntity) adapter1.getItem(position);
            if (groupMemberEntity != null) {
                if (groupMemberEntity.channelMember.role == BageChannelMemberRole.manager) {
                    List<String> uids = new ArrayList<>();
                    uids.add(groupMemberEntity.channelMember.memberUID);
                    presenter.removeGroupManager(groupId, uids);
                }
            }
        });
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view -> {
            GroupMemberEntity groupMemberEntity = (GroupMemberEntity) adapter1.getItem(position);
            if (groupMemberEntity != null && groupMemberEntity.getItemType() == 2) {
                Intent intent = new Intent(GroupManageActivity.this, ChooseNormalMembersActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            }
        }));
        bageVBinding.invitConfirmationSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "invite", b ? 1 : 0);
            }
        });
        bageVBinding.fullStaffBanSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "forbidden", b ? 1 : 0);
            }
        });
        bageVBinding.forbiddenAddFriendSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, "forbidden_add_friend", b ? 1 : 0);
            }
        });
        bageVBinding.allowNewMembersViewHistoryMsgSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, BageChannelExtras.allowViewHistoryMsg, b ? 1 : 0);
            }
        });
        bageVBinding.allowMemberPinnedMessageSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                presenter.updateGroupSetting(groupId, Const.allowMemberPinnedMessage, b ? 1 : 0);
            }
        });
        //监听频道改变通知
        BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo("groupManagerChannelRefresh", (channel, isEnd) -> {
            if (channel != null) {
                if (channel.channelID.equalsIgnoreCase(groupId) && channel.channelType == BageChannelType.GROUP) {
                    //同一个会话
                    getManagerList();
                }
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.groupOwnerTransferLayout, view1 -> {
            Intent intent = new Intent(this, ChooseNormalMembersActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("type", 2);
            chooseMemberResult.launch(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.blackListLayout, view1 -> {
            Intent intent = new Intent(this, GroupBlacklistActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.outUserLayout, view1 -> {
            Intent intent = new Intent(this, OutGroupMembersActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.disbandBtn, view1 -> BageDialogUtils.getInstance().showDialog(this, getString(R.string.group_disband), getString(R.string.disband_group_tip), true, getString(R.string.cancel), getString(R.string.disband), 0, ContextCompat.getColor(this, R.color.red), index -> {
            if (index == 1) {
                GroupManageModel.getInstance().disbandGroup(groupId, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        EndpointManager.getInstance().invokes(EndpointCategory.bageExitChat, new BageChannel(groupId, BageChannelType.GROUP));
                        finish();
                    } else {
                        showToast(msg);
                    }
                });
            }
        }));
        BageIM.getInstance().getChannelMembersManager().addOnRefreshChannelMemberInfo("group_manager_refresh_channel_member", (channelMember, isEnd) -> getManagerList());
        groupId = getIntent().getStringExtra("groupId");
        getManagerList();
    }

    //获取群主或管理员
    private void getManagerList() {
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(groupId, BageChannelType.GROUP);
        if (channel != null) {
            bageVBinding.invitConfirmationSwitch.setChecked(channel.invite == 1);
            bageVBinding.fullStaffBanSwitch.setChecked(channel.forbidden == 1);
            if (channel.remoteExtraMap != null) {
                Object object = channel.remoteExtraMap.get(BageChannelExtras.forbiddenAddFriend);
                if (object != null) {
                    int forbiddenAddFriend = (int) object;
                    bageVBinding.forbiddenAddFriendSwitch.setChecked(forbiddenAddFriend == 1);
                }
                Object viewHistoryMsgObject = channel.remoteExtraMap.get(BageChannelExtras.allowViewHistoryMsg);
                if (viewHistoryMsgObject != null) {
                    int viewHistoryMsg = (int) viewHistoryMsgObject;
                    bageVBinding.allowNewMembersViewHistoryMsgSwitch.setChecked(viewHistoryMsg == 1);
                }
                Object allowMemberPinnedMessageObject = channel.remoteExtraMap.get(Const.allowMemberPinnedMessage);
                if (allowMemberPinnedMessageObject != null) {
                    int allowMemberPinnedMessage = (int) allowMemberPinnedMessageObject;
                    bageVBinding.allowMemberPinnedMessageSwitch.setChecked(allowMemberPinnedMessage == 1);
                }
            }

        }
        List<BageChannelMember> adminList = BageIM.getInstance().getChannelMembersManager().getWithRole(groupId, BageChannelType.GROUP, BageChannelMemberRole.admin);
        List<BageChannelMember> managerList = BageIM.getInstance().getChannelMembersManager().getWithRole(groupId, BageChannelType.GROUP, BageChannelMemberRole.manager);
        List<BageChannelMember> list = new ArrayList<>();
        list.addAll(adminList);
        list.addAll(managerList);
//        List<BageChannelMember> list = BageIM.getInstance().getChannelMembersManager().getMembers(groupId, BageChannelType.GROUP);
        List<GroupMemberEntity> tempList = new ArrayList<>();
        int myRoleInGroup = 0;
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).role == BageChannelMemberRole.admin) {
                tempList.add(0, new GroupMemberEntity(list.get(i)));
            } else if (list.get(i).role == BageChannelMemberRole.manager) {
                tempList.add(new GroupMemberEntity(list.get(i)));
            }
            if (list.get(i).memberUID.equalsIgnoreCase(BageConfig.getInstance().getUid())) {
                myRoleInGroup = list.get(i).role;
            }
        }
        if (myRoleInGroup == BageChannelMemberRole.admin) {
            GroupMemberEntity entity = new GroupMemberEntity(null);
            entity.itemType = 2;
            tempList.add(entity);
            bageVBinding.disbandLayout.setVisibility(View.VISIBLE);
            bageVBinding.groupOwnerTransferLayout.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.disbandLayout.setVisibility(View.GONE);
            bageVBinding.groupOwnerTransferLayout.setVisibility(View.GONE);
        }
        adapter.setMyRoleInGroup(myRoleInGroup);
        adapter.setList(tempList);
    }

    @Override
    public void refreshData() {

    }

    @Override
    public void forbiddenTimeList(List<ForbiddenTime> list) {

    }

    @Override
    public void showError(String msg) {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BageIM.getInstance().getChannelManager().removeRefreshChannelInfo("groupManagerChannelRefresh");
        BageIM.getInstance().getChannelMembersManager().removeRefreshChannelMemberInfo("group_manager_refresh_channel_member");
    }

    ActivityResultLauncher<Intent> chooseMemberResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            EndpointManager.getInstance().invoke("chat_hide_group_manage_view", null);
            finish();
        }
    });
}
