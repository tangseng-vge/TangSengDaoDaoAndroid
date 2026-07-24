package com.chat.advanced.ui;

import android.widget.TextView;

import com.chat.advanced.R;
import com.chat.advanced.databinding.ActMsgRemindLayoutBinding;
import com.chat.advanced.service.AdvancedModel;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelType;

/**
 * 2020-10-20 11:15
 * 消息提醒设置
 */
public class MsgRemindActivity extends BageBaseActivity<ActMsgRemindLayoutBinding> {

    private String channelID;
    private byte channelType;
    BageChannel channel;

    @Override
    protected ActMsgRemindLayoutBinding getViewBinding() {
        return ActMsgRemindLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.msg_remind_setting);
    }

    @Override
    protected void initPresenter() {
        channelType = getIntent().getByteExtra("channelType", channelType);
        channelID = getIntent().getStringExtra("channelID");
    }

    @Override
    protected void initView() {
        Theme.applyAccentSwitchStyle(this, bageVBinding.screenshotSwitchView);
        Theme.applyAccentSwitchStyle(this, bageVBinding.revokeSwitchView);
        Theme.applyAccentSwitchStyle(this, bageVBinding.joinGroupSwitchView);
        resetData();
    }

    @Override
    protected void initListener() {

        bageVBinding.screenshotSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (channelType == BageChannelType.GROUP)
                    AdvancedModel.Companion.getInstance().updateGroupSetting(channelID, "screenshot", b ? 1 : 0, (code, msg) -> {
                        if (code != HttpResponseCode.success) {
                            bageVBinding.screenshotSwitchView.setChecked(!b);
                            showToast(msg);
                        }
                    });
                else {
                    AdvancedModel.Companion.getInstance().updateUserSetting(channelID, "screenshot", b ? 1 : 0, (code, msg) -> {
                        if (code != HttpResponseCode.success) {
                            bageVBinding.screenshotSwitchView.setChecked(!b);
                            showToast(msg);
                        }
                    });
                }
            }
        });

        bageVBinding.joinGroupSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (channelType == BageChannelType.GROUP) {
                    AdvancedModel.Companion.getInstance().updateGroupSetting(channelID, "join_group_remind", b ? 1 : 0, (code, msg) -> {
                        if (code != HttpResponseCode.success) {
                            bageVBinding.joinGroupSwitchView.setChecked(!b);
                            showToast(msg);
                        }
                    });
                }
            }
        });
        bageVBinding.revokeSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (channelType == BageChannelType.GROUP) {
                    AdvancedModel.Companion.getInstance().updateGroupSetting(channelID, "revoke_remind", b ? 1 : 0, (code, msg) -> {
                        if (code != HttpResponseCode.success) {
                            bageVBinding.revokeSwitchView.setChecked(!b);
                            showToast(msg);
                        }
                    });
                } else {
                    AdvancedModel.Companion.getInstance().updateUserSetting(channelID, "revoke_remind", b ? 1 : 0, (code, msg) -> {
                        if (code != HttpResponseCode.success) {
                            bageVBinding.revokeSwitchView.setChecked(!b);
                            showToast(msg);
                        }
                    });
                }
            }
        });
    }


    private void resetData() {
        channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
        if (channel == null) return;

        if (channel.remoteExtraMap != null) {
            if (channel.remoteExtraMap.containsKey(BageChannelExtras.screenshot)) {
                Object object = channel.remoteExtraMap.get(BageChannelExtras.screenshot);
                if (object != null) {
                    int screenshot = (int) object;
                    bageVBinding.screenshotSwitchView.setChecked(screenshot == 1);
                }
            }
            if (channel.remoteExtraMap.containsKey(BageChannelExtras.revokeRemind)) {
                Object object = channel.remoteExtraMap.get(BageChannelExtras.revokeRemind);
                if (object != null) {
                    int revokeRemind = (int) object;
                    bageVBinding.revokeSwitchView.setChecked(revokeRemind == 1);
                }
            }
            if (channelType == BageChannelType.GROUP && channel.remoteExtraMap.containsKey(BageChannelExtras.joinGroupRemind)) {
                Object object = channel.remoteExtraMap.get(BageChannelExtras.joinGroupRemind);
                if (object != null) {
                    int joinGroupRemind = (int) object;
                    bageVBinding.joinGroupSwitchView.setChecked(joinGroupRemind == 1);
                }
            }
        }
    }
}
