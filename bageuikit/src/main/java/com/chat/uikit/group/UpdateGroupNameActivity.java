package com.chat.uikit.group;

import android.text.TextUtils;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActUpdateGroupNameLayoutBinding;
import com.chat.uikit.group.service.GroupModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.Objects;

/**
 * 2020-01-29 10:24
 * 修改群名称
 */
public class UpdateGroupNameActivity extends BageBaseActivity<ActUpdateGroupNameLayoutBinding> {

    String groupNo;
    BageChannel channel;

    @Override
    protected ActUpdateGroupNameLayoutBinding getViewBinding() {
        return ActUpdateGroupNameLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_card);
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        String name = Objects.requireNonNull(bageVBinding.nameEt.getText()).toString();

        if (channel == null || TextUtils.isEmpty(channel.channelID) || TextUtils.isEmpty(name))
            return;

        if (TextUtils.equals(name, channel.channelName)) {
            finish();
        }
        channel.channelName = name;

        showTitleRightLoading();
        GroupModel.getInstance().updateGroupInfo(channel.channelID, "name", name, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                BageIM.getInstance().getChannelManager().updateName(channel.channelID, BageChannelType.GROUP, name);
                finish();
            } else {
                hideTitleRightLoading();
                showToast(msg);
            }
        });
    }

    @Override
    protected String getRightTvText(TextView textView) {
        return getString(R.string.save);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        groupNo = getIntent().getStringExtra("groupNo");
        if (groupNo != null) {
            channel = BageIM.getInstance().getChannelManager().getChannel(groupNo, BageChannelType.GROUP);
            bageVBinding.nameEt.setText(channel.channelName);
            bageVBinding.nameEt.setSelection(channel.channelName.length());
        }
        SoftKeyboardUtils.getInstance().showSoftKeyBoard(UpdateGroupNameActivity.this, bageVBinding.nameEt);
    }

    @Override
    protected void initListener() {

    }

    @Override
    public void finish() {
        super.finish();
        SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.nameEt);
    }
}
