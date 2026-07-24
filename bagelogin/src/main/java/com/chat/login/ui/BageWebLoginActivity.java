package com.chat.login.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageToastUtils;
import com.chat.login.R;
import com.chat.login.databinding.ActWebLoginLayoutBinding;

//import org.telegram.ui.Components.RLottieDrawable;

public class BageWebLoginActivity extends BageBaseActivity<ActWebLoginLayoutBinding> {
    @Override
    protected ActWebLoginLayoutBinding getViewBinding() {
        return ActWebLoginLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.web_login);
    }


    @Override
    protected void initView() {
        bageVBinding.urlTv.setText(BageConfig.getInstance().getAppConfig().web_url);
        bageVBinding.webLoginDescTv.setText(String.format(getString(R.string.web_scan_login_desc), BageConfig.getInstance().getAppConfig().web_url));
        bageVBinding.nameTv.setText(String.format(getString(R.string.web_side), getString(R.string.app_name)));
        Theme.setPressedBackground(bageVBinding.copyIv);

//        RLottieDrawable drawable = new RLottieDrawable(this, R.raw.qrcode_web, "", AndroidUtilities.dp(180), AndroidUtilities.dp(180), false, null);
//        bageVBinding.imageView.setAutoRepeat(false);
//        bageVBinding.imageView.setAnimation(drawable);
//        bageVBinding.imageView.playAnimation();
    }

    @Override
    protected void initListener() {
        bageVBinding.copyIv.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", BageConfig.getInstance().getAppConfig().web_url);
            assert cm != null;
            cm.setPrimaryClip(mClipData);
            BageToastUtils.getInstance().showToastNormal(getString(R.string.copied));
        });
        bageVBinding.scanLayout.setOnClickListener(v -> {
            EndpointManager.getInstance().invoke("bage_scan_show", null);
        });
    }
}
