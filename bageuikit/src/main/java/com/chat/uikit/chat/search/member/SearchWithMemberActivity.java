package com.chat.uikit.chat.search.member;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.entity.GlobalMessage;
import com.chat.base.entity.GlobalSearchReq;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.search.GlobalSearchModel;
import com.chat.base.utils.BageReader;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActCommonRefreshListLayoutBinding;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;


public class SearchWithMemberActivity extends BageBaseActivity<ActCommonRefreshListLayoutBinding> {
    SearchWithMemberAdapter adapter;
    private String channelID;
    private String fromUID;
    private int page = 1;

    @Override
    protected ActCommonRefreshListLayoutBinding getViewBinding() {
        return ActCommonRefreshListLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.uikit_search_with_member);
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
        adapter = new SearchWithMemberAdapter(name, avatarKey);
        initAdapter(bageVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                page ++;
                getData();
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {

            }
        });
        adapter.setOnItemClickListener((adapter, view, position) -> {
            GlobalMessage msg = (GlobalMessage) adapter.getData().get(position);
            if (msg != null) {
                long orderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(
                        msg.getMessage_seq(),
                        msg.getChannel().getChannel_id(),
                        msg.getChannel().getChannel_type()
                );
                EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(SearchWithMemberActivity.this, channelID, BageChannelType.GROUP, orderSeq, false));
            }
        });
        getData();
    }

    private void getData() {
        ArrayList<Integer> contentType = new ArrayList<>();
        contentType.add(BageContentType.Bage_TEXT);
        contentType.add(BageContentType.Bage_FILE);
        GlobalSearchReq req = new GlobalSearchReq(1, "", channelID, BageChannelType.GROUP, fromUID, "", contentType, page, 20, 0, 0);
        GlobalSearchModel.INSTANCE.search(req, (code, s, globalSearch) -> {
            bageVBinding.refreshLayout.finishLoadMore();
            bageVBinding.refreshLayout.finishRefresh();
            if (code != HttpResponseCode.success) {
                showToast(s);
                return null;
            }
            if (globalSearch == null || BageReader.isEmpty(globalSearch.messages)) {
                if (page != 1) {
                    bageVBinding.refreshLayout.setEnableLoadMore(false);
                }
                return null;
            }
            if (page == 1) {
                adapter.setList(globalSearch.messages);
            } else {
                adapter.addData(globalSearch.messages);
            }
            return null;
        });
    }
}
