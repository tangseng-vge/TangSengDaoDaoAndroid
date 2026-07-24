package com.chat.uikit.setting;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.utils.BageDeviceUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActAboutLayoutBinding;
import com.bage.im.entity.BageChannelType;

/**
 * 5/26/21 3:03 PM
 * 关于
 */
public class BageAboutActivity extends BageBaseActivity<ActAboutLayoutBinding> {

    @Override
    protected ActAboutLayoutBinding getViewBinding() {
        return ActAboutLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(String.format("%s%s", getString(R.string.about), getString(R.string.app_name)));
    }

    @Override
    protected void initView() {

        SingleClickUtil.onSingleClick(bageVBinding.icpTV, view1 -> {
            // 隐私政策
            showWebView("https://beian.miit.gov.cn/#/home");
        });
        SingleClickUtil.onSingleClick(bageVBinding.privacyPolicyLayout, view1 -> {
            // 隐私政策
            showWebView(BageApiConfig.baseWebUrl + "privacy_policy.html");
        });
        SingleClickUtil.onSingleClick(bageVBinding.userAgreementLayout, view1 -> {
            // 用户协议
            showWebView(BageApiConfig.baseWebUrl + "user_agreement.html");
        });
        SingleClickUtil.onSingleClick(bageVBinding.checkNewVersionLayout, view1 -> checkNewVersion(true));
        checkNewVersion(false);
        String v = BageDeviceUtils.getInstance().getVersionName(this);
        bageVBinding.versionTv.setText(String.format("version %s", v));
        bageVBinding.appNameTv.setText(R.string.app_name);
    }

    @Override
    protected void initListener() {
        bageVBinding.avatarView.setSize(80);
        bageVBinding.avatarView.showAvatar(BageSystemAccount.system_team, BageChannelType.PERSONAL);
    }

    private void checkNewVersion(boolean isShowDialog) {
        BageCommonModel.getInstance().getAppNewVersion(isShowDialog, version -> {
            if (version != null && !TextUtils.isEmpty(version.download_url)) {
                if (isShowDialog) {
                    BageDialogUtils.getInstance().showNewVersionDialog(BageAboutActivity.this, version);
                } else {
                    bageVBinding.newVersionIv.setVisibility(View.VISIBLE);
                }
            } else {
                bageVBinding.newVersionIv.setVisibility(View.GONE);
            }
        });
    }


}
