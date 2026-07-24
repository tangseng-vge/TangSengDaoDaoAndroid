package com.chat.login.ui;

import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.login.R;
import com.chat.login.databinding.ActWebLoginAuthLayoutBinding;
import com.chat.login.service.LoginModel;

/**
 * 2020-04-19 19:00
 * web登录确认
 */
public class BageWebLoginConfirmActivity extends BageBaseActivity<ActWebLoginAuthLayoutBinding> {
    private String auth_code;

    @Override
    protected ActWebLoginAuthLayoutBinding getViewBinding() {
        return ActWebLoginAuthLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        Theme.setColorFilter(this, bageVBinding.closeIv,R.color.popupTextColor);
    }

    @Override
    protected void initView() {
        bageVBinding.loginBtn.getBackground().setTint(Theme.colorAccount);
        auth_code = getIntent().getStringExtra("auth_code");
    }

    @Override
    protected void initListener() {
        bageVBinding.webLoginDescTv.setText(String.format(getString(R.string.web_login_desc), getString(R.string.app_name)));
        bageVBinding.closeIv.setOnClickListener(v -> finish());
        bageVBinding.closeTv.setOnClickListener(v -> finish());
        bageVBinding.loginBtn.setOnClickListener(v -> LoginModel.getInstance().webLogin(auth_code, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                finish();
            } else showToast(msg);
        }));
    }

}
