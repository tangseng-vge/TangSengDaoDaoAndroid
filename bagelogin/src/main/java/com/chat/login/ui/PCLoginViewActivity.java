package com.chat.login.ui;

import static android.view.View.VISIBLE;

import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageToastUtils;
import com.chat.login.R;
import com.chat.login.databinding.PcLoginViewLayoutBinding;
import com.chat.login.service.LoginModel;
import com.bage.im.entity.BageChannelType;

/**
 * 4/14/21 4:57 PM
 * pc登录
 */
public class PCLoginViewActivity extends BageBaseActivity<PcLoginViewLayoutBinding> {
    @Override
    protected PcLoginViewLayoutBinding getViewBinding() {
        overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
        return PcLoginViewLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        Theme.setColorFilter(this, bageVBinding.closeIv, R.color.popupTextColor);
        bageVBinding.closeIv.setOnClickListener(v -> finish());
        bageVBinding.exitBtn.setTextColor(Theme.colorAccount);
        bageVBinding.exitBtn.setText(String.format(getString(R.string.exit_pc_login), getString(R.string.app_name)));
        bageVBinding.pcLoginTv.setText(String.format(getString(R.string.pc_login), getString(R.string.app_name)));
        bageVBinding.phoneMuteBtn.setOnClickListener(v -> {
            int muteForApp = BageSharedPreferencesUtil.getInstance().getInt(BageConfig.getInstance().getUid() + "_mute_of_app");
            LoginModel.getInstance().updateUserSetting("mute_of_app", muteForApp == 1 ? 0 : 1, (code, msg) -> {
                if (code == HttpResponseCode.success) {
                    BageSharedPreferencesUtil.getInstance().putInt(BageConfig.getInstance().getUid() + "_mute_of_app", muteForApp == 1 ? 0 : 1);
                    updateMuteStatus(muteForApp == 1 ? 0 : 1);
                } else BageToastUtils.getInstance().showToastNormal(msg);
            });

        });

        int muteForApp = BageSharedPreferencesUtil.getInstance().getInt(BageConfig.getInstance().getUid() + "_mute_of_app");
        updateMuteStatus(muteForApp);
        findViewById(R.id.exitBtn).setOnClickListener(v -> LoginModel.getInstance().quitPc((code, msg) -> {
            if (code == HttpResponseCode.success) {
                finish();
            } else BageToastUtils.getInstance().showToastNormal(msg);
        }));
        findViewById(R.id.fileLayout).setOnClickListener(v -> {
            finish();
            EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(PCLoginViewActivity.this, BageSystemAccount.system_file_helper, BageChannelType.PERSONAL, 0, false));
        });
        bageVBinding.lockLayout.setOnClickListener(v -> {
            //锁定
            bageVBinding.lockLayout.setBackground(Theme.getBackground(Theme.colorAccount,55));
            bageVBinding.lockIv.setImageResource(R.mipmap.icon_lock_white);
            bageVBinding.topLockIv.setVisibility(VISIBLE);
        });
    }

    @Override
    protected void initListener() {

    }


    private void updateMuteStatus(int muteForApp) {
        if (muteForApp == 1) {
            bageVBinding.noticeTv.setText(R.string.phone_notice_close);
            bageVBinding.phoneMuteBtn.setBackground(Theme.getBackground(Theme.colorAccount, 55, 55, 55));
            bageVBinding.pcLoginIV.setImageResource(R.mipmap.device_status_pc_online_silence);
            bageVBinding.muteIv.setImageResource(R.mipmap.icon_mute_white);
        } else {
            bageVBinding.noticeTv.setText(R.string.phone_notice_open);
            bageVBinding.phoneMuteBtn.setBackgroundResource(R.drawable.pc_login_btn_bg);
            bageVBinding.pcLoginIV.setImageResource(R.mipmap.device_status_pc_online_normal);
            bageVBinding.muteIv.setImageResource(R.mipmap.icon_mute);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.bottom_silent, R.anim.bottom_out);
    }
}
