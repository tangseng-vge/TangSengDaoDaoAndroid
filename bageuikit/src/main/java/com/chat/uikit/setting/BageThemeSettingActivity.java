package com.chat.uikit.setting;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.ui.Theme;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActDarkSettingLayoutBinding;

/**
 * 2020-12-02 11:57
 * 深色模式
 */
public class BageThemeSettingActivity extends BageBaseActivity<ActDarkSettingLayoutBinding> {

    private int type = 0;

    @Override
    protected ActDarkSettingLayoutBinding getViewBinding() {
        return ActDarkSettingLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.dark_night);
    }

    @Override
    protected void rightButtonClick() {
        super.rightButtonClick();
        showDialog(String.format(getString(R.string.dark_save_tips), getString(R.string.app_name)), index -> {
            if (index == 1) {
                saveType();
            }
        });
    }

    @Override
    protected String getRightBtnText(Button titleRightBtn) {
        return getString(R.string.sure);
    }

    @Override
    protected void initView() {
        String sp = Theme.getTheme();
        if (sp.equals(Theme.DARK_MODE)) {
            bageVBinding.followSystemSwitch.setChecked(false);
            bageVBinding.nightIv.setVisibility(View.VISIBLE);
            bageVBinding.bottomView.setVisibility(View.VISIBLE);
            bageVBinding.normalIv.setVisibility(View.INVISIBLE);
        } else if (sp.equals(Theme.LIGHT_MODE)) {
            bageVBinding.followSystemSwitch.setChecked(false);
            bageVBinding.nightIv.setVisibility(View.INVISIBLE);
            bageVBinding.normalIv.setVisibility(View.VISIBLE);
            bageVBinding.bottomView.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.followSystemSwitch.setChecked(true);
            bageVBinding.nightIv.setVisibility(View.INVISIBLE);
            bageVBinding.normalIv.setVisibility(View.VISIBLE);
            bageVBinding.bottomView.setVisibility(View.GONE);
        }

    }

    @Override
    protected void initListener() {
        bageVBinding.followSystemSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                bageVBinding.bottomView.setVisibility(View.GONE);
                type = 0;
            } else {
                type = 1;
                bageVBinding.bottomView.setVisibility(View.VISIBLE);
            }
        });
        bageVBinding.darkLayout.setOnClickListener(v -> {
            type = 2;
            bageVBinding.nightIv.setVisibility(View.VISIBLE);
            bageVBinding.normalIv.setVisibility(View.INVISIBLE);
        });
        bageVBinding.normalLayout.setOnClickListener(v -> {
            type = 1;
            bageVBinding.nightIv.setVisibility(View.INVISIBLE);
            bageVBinding.normalIv.setVisibility(View.VISIBLE);
        });
    }

    private void saveType() {
        String s = Theme.DEFAULT_MODE;
        if (type == 0) {
            s = Theme.DEFAULT_MODE;
        } else if (type == 1) {
            s = Theme.LIGHT_MODE;
        } else if (type == 2){
            s = Theme.DARK_MODE;
        }
        Theme.setTheme(s);
        finish();
    }

    @Override
    protected void resetTheme(boolean isDark) {
        super.resetTheme(isDark);
        bageVBinding.contentLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.homeColor));
        bageVBinding.topLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.layoutColor));
        bageVBinding.normalLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.layoutColor));
        bageVBinding.darkLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.layoutColor));
        bageVBinding.normalTv.setTextColor(ContextCompat.getColor(this, R.color.colorDark));
        bageVBinding.darkTv.setTextColor(ContextCompat.getColor(this, R.color.colorDark));
        bageVBinding.systemTv.setTextColor(ContextCompat.getColor(this, R.color.colorDark));
        bageVBinding.followSystemSwitch.invalidate();
    }
}
