package com.chat.uikit.setting;

import android.view.View;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.systembar.BageOSUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActMsgNoticesSetLayoutBinding;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.entity.BageChannelType;

/**
 * 2020-06-30 13:31
 * 新消息通知设置
 */
public class MsgNoticesSettingActivity extends BageBaseActivity<ActMsgNoticesSetLayoutBinding> {
    UserInfoEntity userInfoEntity;

    @Override
    protected ActMsgNoticesSetLayoutBinding getViewBinding() {
        return ActMsgNoticesSetLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.new_msg_notice);
    }

    @Override
    protected void initPresenter() {
        userInfoEntity = BageConfig.getInstance().getUserInfo();
    }

    @Override
    protected void initView() {
        bageVBinding.voiceShockDescTv.setText(String.format(getString(R.string.voice_shock_desc), getString(R.string.app_name)));
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        Theme.applyAccentSwitchStyle(this, bageVBinding.newMsgNoticeSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.newMsgNoticeDetailSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.voiceSwitch);
        Theme.applyAccentSwitchStyle(this, bageVBinding.shockSwitch);
        bageVBinding.newMsgNoticeSwitch.setChecked(userInfoEntity.setting.new_msg_notice == 1);
        bageVBinding.voiceSwitch.setChecked(userInfoEntity.setting.voice_on == 1);
        bageVBinding.shockSwitch.setChecked(userInfoEntity.setting.shock_on == 1);
        bageVBinding.newMsgNoticeDetailSwitch.setChecked(userInfoEntity.setting.msg_show_detail == 1);
        View keepAliveView = (View) EndpointManager.getInstance().invoke("show_keep_alive_item", this);
        if (keepAliveView != null) {
            bageVBinding.keepAliveLayout.addView(keepAliveView);
        }
    }

    @Override
    protected void initListener() {
        bageVBinding.newMsgNoticeSwitch.setOnCheckedChangeListener((view, b) -> {
            if (view.isPressed()) {
                userInfoEntity.setting.new_msg_notice = b ? 1 : 0;
                UserModel.getInstance().updateUserSetting("new_msg_notice", userInfoEntity.setting.new_msg_notice, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        BageConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        bageVBinding.voiceSwitch.setOnCheckedChangeListener((view, b) -> {
            if (view.isPressed()) {
                userInfoEntity.setting.voice_on = b ? 1 : 0;
                UserModel.getInstance().updateUserSetting("voice_on", userInfoEntity.setting.voice_on, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        BageConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        bageVBinding.shockSwitch.setOnCheckedChangeListener((view, b) -> {
            if (view.isPressed()) {
                userInfoEntity.setting.shock_on = b ? 1 : 0;
                UserModel.getInstance().updateUserSetting("shock_on", userInfoEntity.setting.shock_on, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        BageConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        bageVBinding.newMsgNoticeDetailSwitch.setOnCheckedChangeListener((view, b) -> {
            if (view.isPressed()) {
                userInfoEntity.setting.msg_show_detail = b ? 1 : 0;
                UserModel.getInstance().updateUserSetting("msg_show_detail", userInfoEntity.setting.msg_show_detail, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        BageConfig.getInstance().saveUserInfo(userInfoEntity);
                    } else showToast(msg);
                });
            }
        });
        bageVBinding.openNoticeLayout.setOnClickListener(v -> {
            BageOSUtils.openChannelSetting(this, BageConstants.newMsgChannelID);
        });
        bageVBinding.openRTCNoticeLayout.setOnClickListener(v -> {
            BageOSUtils.openChannelSetting(this, BageConstants.newRTCChannelID);
        });
    }

}
