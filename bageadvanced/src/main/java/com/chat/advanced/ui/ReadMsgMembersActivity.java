package com.chat.advanced.ui;

import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.advanced.R;
import com.chat.advanced.databinding.ActReadMsgDetailLayoutBinding;
import com.chat.advanced.entity.MsgReadDetailEntity;
import com.chat.advanced.service.AdvancedModel;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.UserDetailMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.BageReader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;

/**
 * 4/9/21 10:07 AM
 * 消息已读未读详情
 */
public class ReadMsgMembersActivity extends BageBaseActivity<ActReadMsgDetailLayoutBinding> {

    private String messageID;
    private String group_no;
    MsgReadDetailAdapter adapter;
    private int page = 1;
    private String channelID;
    private byte channelType;

    @Override
    protected ActReadMsgDetailLayoutBinding getViewBinding() {
        return ActReadMsgDetailLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.msg_read_status);
    }

    @Override
    protected void initPresenter() {
        messageID = getIntent().getStringExtra("message_id");
        group_no = getIntent().getStringExtra("group_no");
    }

    @Override
    protected void initView() {
        adapter = new MsgReadDetailAdapter();
        initAdapter(bageVBinding.recyclerView, adapter);
        BageMsg msg = BageIM.getInstance().getMsgManager().getWithMessageID(messageID);
        if (msg != null) {
            channelID = msg.channelID;
            channelType = msg.channelType;
        }
    }

    @Override
    protected void initListener() {
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
        getData();
        adapter.setOnItemClickListener((adapter1, view, position) -> {
            MsgReadDetailEntity entity = (MsgReadDetailEntity) adapter1.getData().get(position);
            if (entity != null) {
                EndpointManager.getInstance().invoke(EndpointSID.userDetailView, new UserDetailMenu(ReadMsgMembersActivity.this,entity.uid, group_no));
            }
        });
    }

    private void getData() {
        AdvancedModel.Companion.getInstance().receipt(messageID, page, channelID, channelType, 1, (code, msg, list) -> {
            bageVBinding.refreshLayout.finishRefresh();
            if (code == HttpResponseCode.success) {
                if (page == 1) {
                    adapter.setList(list);
                } else {
                    if (BageReader.isEmpty(list)) {
                        bageVBinding.refreshLayout.finishLoadMoreWithNoMoreData();
                        return;
                    }
                    adapter.addData(list);
                    bageVBinding.refreshLayout.finishLoadMore();
                }
            }
        });
    }
}
