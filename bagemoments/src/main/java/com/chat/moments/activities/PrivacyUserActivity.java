package com.chat.moments.activities;

import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.UserDetailMenu;
import com.chat.base.utils.BageReader;
import com.chat.moments.R;
import com.chat.moments.adapter.PrivacyUserAdapter;
import com.chat.moments.databinding.ActPrivacyUserLayoutBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-12-10 15:42
 * 部分可见和不可见成员列表
 */
public class PrivacyUserActivity extends BageBaseActivity<ActPrivacyUserLayoutBinding> {
    PrivacyUserAdapter adapter;
    private String privacyType;

    @Override
    protected ActPrivacyUserLayoutBinding getViewBinding() {
        return ActPrivacyUserLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        if (privacyType.equals("prohibit")) {
            titleTv.setText(R.string.moment_prohibit);
        } else if (privacyType.equals("internal")) {
            titleTv.setText(R.string.moment_internal);
        }
    }

    @Override
    protected void initPresenter() {
        privacyType = getIntent().getStringExtra("privacyType");
    }

    @Override
    protected void initView() {
        adapter = new PrivacyUserAdapter();
        initAdapter(bageVBinding.recyclerView, adapter);

        List<String> list = getIntent().getStringArrayListExtra("list");
        if (BageReader.isNotEmpty(list)) {
            List<BageChannel> channels = new ArrayList<>();
            for (String uid : list) {
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
                if (channel != null) {
                    channels.add(channel);
                }
            }
            adapter.setList(channels);
        }
    }

    @Override
    protected void initListener() {
        adapter.setOnItemClickListener((adapter1, view1, position) -> {
            BageChannel channel = (BageChannel) adapter1.getItem(position);
            if (channel != null) {
                EndpointManager.getInstance().invoke(EndpointSID.userDetailView, new UserDetailMenu(PrivacyUserActivity.this,channel.channelID));
            }
        });
    }
}
