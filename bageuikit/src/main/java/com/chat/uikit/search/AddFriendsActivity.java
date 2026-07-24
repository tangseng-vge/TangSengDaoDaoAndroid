package com.chat.uikit.search;

import android.content.Intent;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.ui.Theme;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActAddFriendsLayoutBinding;
import com.chat.uikit.user.UserQrActivity;

/**
 * 2020-07-06 10:14
 * 添加好友
 */
public class AddFriendsActivity extends BageBaseActivity<ActAddFriendsLayoutBinding> {
    @Override
    protected ActAddFriendsLayoutBinding getViewBinding() {
        return ActAddFriendsLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.add_friends);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        Theme.setPressedBackground(bageVBinding.qrIv);
        bageVBinding.searchTitleTv.setText(String.format(getString(R.string.my_app_id), getString(R.string.app_name)));
        bageVBinding.identityTv.setText(BageConfig.getInstance().getUserInfo().short_no);
    }

    @Override
    protected void initListener() {
        SingleClickUtil.onSingleClick(bageVBinding.qrIv, v -> startActivity(new Intent(this, UserQrActivity.class)));
        SingleClickUtil.onSingleClick(bageVBinding.searchLayout, v -> startActivity(new Intent(this, SearchUserActivity.class)));
        SingleClickUtil.onSingleClick(bageVBinding.scanLayout, v -> EndpointManager.getInstance().invoke("bage_scan_show", null));
        SingleClickUtil.onSingleClick(bageVBinding.mailListLayout, v -> startActivity(new Intent(this, MailListActivity.class)));

        SingleClickUtil.onSingleClick(bageVBinding.rlSearch, v -> startActivity(new Intent(this, SearchUserActivity.class)));

    }

}
