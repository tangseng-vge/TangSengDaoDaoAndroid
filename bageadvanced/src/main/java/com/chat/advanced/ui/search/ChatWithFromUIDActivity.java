package com.chat.advanced.ui.search;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.advanced.R;
import com.chat.advanced.databinding.ActChatWithFromuidLayoutBinding;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.utils.BageReader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;


public class ChatWithFromUIDActivity extends BageBaseActivity<ActChatWithFromuidLayoutBinding> {
    ChatWithFromUIDAdapter adapter;
    private String channelID;
    private String fromUID;
    private long orderSeq = 0;


    @Override
    protected ActChatWithFromuidLayoutBinding getViewBinding() {
        return ActChatWithFromuidLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.search_with_member);
    }

    @Override
    protected void initPresenter() {
        channelID = getIntent().getStringExtra("channelID");
        fromUID = getIntent().getStringExtra("fromUID");
    }

    @Override
    protected void initView() {
        String name = "";
        String avatarKey = "";
        BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelID, BageChannelType.GROUP, fromUID);
        if (member != null) {
            name = TextUtils.isEmpty(member.memberRemark) ? member.memberName : member.memberRemark;
        }
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(fromUID, BageChannelType.PERSONAL);
        if (channel != null) {
            if (!TextUtils.isEmpty(channel.channelRemark))
                name = channel.channelRemark;
            avatarKey = channel.avatarCacheKey;
        }
        adapter = new ChatWithFromUIDAdapter(name, avatarKey);
        initAdapter(bageVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                getData();
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {

            }
        });
        adapter.setOnItemClickListener((adapter, view, position) -> {
            BageMsg msg = (BageMsg) adapter.getData().get(position);
            if (msg != null) {
                EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(ChatWithFromUIDActivity.this, channelID, BageChannelType.GROUP, msg.orderSeq, false));
            }
        });
        getData();
    }

    private void getData() {
        if (BageReader.isNotEmpty(adapter.getData()))
            orderSeq = adapter.getData().get(adapter.getData().size() - 1).orderSeq;
        List<BageMsg> list = BageIM.getInstance().getMsgManager().getWithFromUID(channelID, BageChannelType.GROUP, fromUID, orderSeq, 20);
        List<BageMsg> resultList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).baseContentMsgModel != null) {
                resultList.add(list.get(i));
            }
        }
        if (orderSeq != 0)
            adapter.addData(resultList);
        else {
            adapter.setList(resultList);
            if (BageReader.isEmpty(resultList)) {
                bageVBinding.noDataTv.setVisibility(View.VISIBLE);
                bageVBinding.refreshLayout.setVisibility(View.GONE);
            }
        }
        if (BageReader.isEmpty(resultList)) {
            bageVBinding.refreshLayout.setEnableLoadMore(false);
        } else
            bageVBinding.refreshLayout.finishLoadMore();
    }
}
