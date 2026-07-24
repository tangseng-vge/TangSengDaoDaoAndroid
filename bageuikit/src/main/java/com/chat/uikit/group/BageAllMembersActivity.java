package com.chat.uikit.group;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.entity.BageChannelCustomerExtras;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.chat.search.member.SearchWithMemberActivity;
import com.chat.uikit.databinding.ActAllMemberLayoutBinding;
import com.chat.uikit.enity.AllGroupMemberEntity;
import com.chat.uikit.enity.OnlineUser;
import com.chat.uikit.group.adapter.AllMembersAdapter;
import com.chat.uikit.user.UserDetailActivity;
import com.chat.uikit.user.service.UserModel;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-12-11 15:15
 * 所有成员
 */
public class BageAllMembersActivity extends BageBaseActivity<ActAllMemberLayoutBinding> {

    private AllMembersAdapter adapter;
    private int page = 1;
    String channelID;
    byte channelType;
    private String searchKey;
    private TextView titleTv;
    private int groupType = 0;
    private boolean searchMessage;

    @Override
    protected ActAllMemberLayoutBinding getViewBinding() {
        return ActAllMemberLayoutBinding.inflate(getLayoutInflater());
    }

    @SuppressLint("StringFormatMatches")
    @Override
    protected void setTitle(TextView titleTv) {
        this.titleTv = titleTv;
    }

    @Override
    protected void initView() {
        if (getIntent().hasExtra("searchMessage")) {
            searchMessage = getIntent().getBooleanExtra("searchMessage", false);
        }
        channelID = getIntent().getStringExtra("channelID");
        channelType = getIntent().getByteExtra("channelType", BageChannelType.GROUP);
        adapter = new AllMembersAdapter();
        initAdapter(bageVBinding.recyclerView, adapter);


    }

    @Override
    protected void initListener() {

        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                page++;
                getData();
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                page = 1;
                getData();
            }
        });
        bageVBinding.searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        bageVBinding.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                SoftKeyboardUtils.getInstance().hideSoftKeyboard(BageAllMembersActivity.this);
                return true;
            }

            return false;
        });
        bageVBinding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchKey = editable.toString();
                page = 1;
                adapter.setSearchKey(searchKey);
                getData();
            }
        });
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view2 -> {
            AllGroupMemberEntity entity = adapter.getItem(position);
            if (entity != null) {
                entity.getChannelMember();
                Intent intent;
                if (searchMessage) {
                    intent = new Intent(this, SearchWithMemberActivity.class);
                    intent.putExtra("channelID", channelID);
                    intent.putExtra("fromUID", entity.getChannelMember().memberUID);
                } else {
                    intent = new Intent(this, UserDetailActivity.class);
                    intent.putExtra("uid", entity.getChannelMember().memberUID);
                    intent.putExtra("groupID", entity.getChannelMember().channelID);
                }
                startActivity(intent);
            }
        }));
    }

    @Override
    protected void initData() {
        super.initData();
        int count = BageIM.getInstance().getChannelMembersManager().getMemberCount(channelID, channelType);
        if (searchMessage) {
            titleTv.setText(R.string.uikit_search_with_member);
        } else {
            titleTv.setText(String.format(getString(R.string.group_members), count + ""));
        }
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
        if (channel != null && channel.remoteExtraMap != null) {
            if (channel.remoteExtraMap.containsKey(BageChannelExtras.groupType)) {
                Object groupTypeObject = channel.remoteExtraMap.get(BageChannelExtras.groupType);
                if (groupTypeObject instanceof Integer) {
                    groupType = (int) groupTypeObject;
                }
            }
            Object memberCountObject = channel.remoteExtraMap.get(BageChannelCustomerExtras.memberCount);
            if (memberCountObject instanceof Integer) {
                count = (int) memberCountObject;
                if (searchMessage) {
                    titleTv.setText(R.string.uikit_search_with_member);
                } else {
                    titleTv.setText(String.format(getString(R.string.group_members), count + ""));
                }
            }
        }
        getData();
    }

    private void getData() {
        BageIM.getInstance().getChannelMembersManager().getWithPageOrSearch(channelID, channelType, searchKey, page, 50, (list, b) -> {
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
        if (BageReader.isNotEmpty(list)) {
            bageVBinding.refreshLayout.setEnableLoadMore(true);
            List<String> uidList = new ArrayList<>();
            for (BageChannelMember member : list) {
                uidList.add(member.memberUID);
            }
            UserModel.getInstance().getOnlineUsers(uidList, (code, msg, onlineUserList) -> {
                bageVBinding.refreshLayout.finishLoadMore();
                if (BageReader.isNotEmpty(list)) {

                    List<AllGroupMemberEntity> allList = new ArrayList<>();
                    for (BageChannelMember member : list) {
                        int online = 0;
                        String lastOfflineTime = "";
                        String lastOnlineTime = "";
                        for (OnlineUser onlineUser : onlineUserList) {
                            if (onlineUser.getUid().equals(member.memberUID)) {
                                online = onlineUser.getOnline();
                                lastOnlineTime =
                                        BageTimeUtils.getInstance().getOnlineTime(onlineUser.getLast_offline());
                                lastOfflineTime = BageTimeUtils.getInstance()
                                        .getShowDateAndMinute(onlineUser.getLast_offline() * 1000L);
                            }
                        }
                        AllGroupMemberEntity entity = new AllGroupMemberEntity(member, online, lastOfflineTime, lastOnlineTime);
                        allList.add(entity);
                    }
                    if (page == 1) {
                        adapter.setList(allList);
                    } else {
                        adapter.addData(allList);
                    }

                }
            });
        } else {
            if (page == 1) {
                adapter.setList(new ArrayList<>());
            } else {
                bageVBinding.refreshLayout.finishLoadMore();
                bageVBinding.refreshLayout.setEnableLoadMore(false);
            }
        }
    }

}
