package com.chat.uikit.setting;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.advanced.ui.ChatBgListActivity;
import com.chat.base.act.BageWebViewActivity;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageApiConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatBgItemMenu;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.DataCleanManager;
import com.chat.base.utils.BageDeviceUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.BageUIKitApplication;
import com.chat.uikit.databinding.ActSettingLayoutBinding;
import com.chat.uikit.message.BackupRestoreMessageActivity;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

/**
 * 2020-03-22 21:11
 * 设置页面
 */
public class SettingActivity extends BageBaseActivity<ActSettingLayoutBinding> {
    private String str;

    @Override
    protected ActSettingLayoutBinding getViewBinding() {
        return ActSettingLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.setting);
    }

    @Override
    protected void initPresenter() {
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
    }

    @Override
    protected void initView() {
        String version = BageDeviceUtils.getInstance().getVersionName(this);
        bageVBinding.newVersionIv.setText(version);
        getCacheSize();
//        EndpointManager.getInstance().invoke("set_chat_bg_view", new ChatBgItemMenu(this, bageVBinding.chatBgLayout, "", BageChannelType.PERSONAL));
    }

    @Override
    protected void initListener() {
        String bage_theme_pref = Theme.getTheme();
        if (bage_theme_pref.equals(Theme.DARK_MODE)) {
            bageVBinding.darkStatusTv.setText(R.string.enabled);
        } else {
            bageVBinding.darkStatusTv.setText(R.string.disabled);
        }
        bageVBinding.loginOutTv.setOnClickListener(v -> BageDialogUtils.getInstance().showDialog(this, getString(R.string.login_out), getString(R.string.login_out_dialog), true, "", getString(R.string.login_out), 0, 0, index -> {
            if (index == 1) {
                UserModel.getInstance().quit(null);
                BageUIKitApplication.getInstance().exitLogin(0);
            }
        }));
        SingleClickUtil.onSingleClick(bageVBinding.languageLayout, view1 -> startActivity(new Intent(this, BageLanguageActivity.class)));
        SingleClickUtil.onSingleClick(bageVBinding.darkLayout, view1 -> startActivity(new Intent(this, BageThemeSettingActivity.class)));
        bageVBinding.clearImgCacheLayout.setOnClickListener(v -> showDialog(getString(R.string.clear_img_cache_tips), index -> {
            if (index == 1) {
                DataCleanManager.clearAllCache(SettingActivity.this);
                str = "0.00M";
                bageVBinding.imageCacheTv.setText(str);
            }
        }));
        bageVBinding.clearChatMsgLayout.setOnClickListener(v -> showDialog(getString(R.string.clear_all_msg_tips), index -> {
            if (index == 1) {
                BageIM.getInstance().getConversationManager().clearAll();
                BageIM.getInstance().getMsgManager().clearAll();
            }
        }));
        SingleClickUtil.onSingleClick(bageVBinding.moduleLayout, view1 -> startActivity(new Intent(this, UpdatePwdActivity.class)));
//        SingleClickUtil.onSingleClick(bageVBinding.aboutLayout, view1 -> startActivity(new Intent(this, BageAboutActivity.class)));
        SingleClickUtil.onSingleClick(bageVBinding.fontSizeLayout, view1 -> startActivity(new Intent(this, BageSetFontSizeActivity.class)));
//        BageCommonModel.getInstance().getAppNewVersion(false, version -> {
//            if (version != null && !TextUtils.isEmpty(version.download_url)) {
//                bageVBinding.newVersionIv.setVisibility(View.VISIBLE);
//            } else {
//                bageVBinding.newVersionIv.setVisibility(View.GONE);
//            }
//        });

        SingleClickUtil.onSingleClick(bageVBinding.msgBackupLayout, view1 -> {
            Intent intent = new Intent(this, BackupRestoreMessageActivity.class);
            intent.putExtra("handle_type", 1);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.msgRecoveryLayout, view1 -> {
            Intent intent = new Intent(this, BackupRestoreMessageActivity.class);
            intent.putExtra("handle_type", 2);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.thirdShareLayout, view1 -> {
            Intent intent = new Intent(this, BageWebViewActivity.class);
            intent.putExtra("url", BageApiConfig.baseWebUrl + "sdkinfo.html");
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.errorLogLayout, view1 -> startActivity(new Intent(this, ErrorLogsActivity.class)));

        SingleClickUtil.onSingleClick(bageVBinding.llPrivacy, view1 -> {
            // 隐私政策
            showWebView(BageApiConfig.baseWebUrl + "privacy_policy.html");
        });
        SingleClickUtil.onSingleClick(bageVBinding.llUserAgreement, view1 -> {
            // 用户协议
            showWebView(BageApiConfig.baseWebUrl + "user_agreement.html");
        });
        SingleClickUtil.onSingleClick(bageVBinding.changeBgLayout,view1 -> {
            Intent intent = new Intent(this, ChatBgListActivity.class);
            intent.putExtra("channelID", "");
            intent.putExtra("channelType", BageChannelType.PERSONAL);
            startActivity(intent);
        });

        SingleClickUtil.onSingleClick(bageVBinding.blackListLayout, view1 ->
                startActivity(new Intent(this, BlackListActivity.class)));
    }


    //获取缓存大小
    private void getCacheSize() {
        new Thread(() -> {
            try {
                str = DataCleanManager.getTotalCacheSize(SettingActivity.this);
                if (str.equalsIgnoreCase("0.0Byte")) {
                    str = "0.00M";
                }
                AndroidUtilities.runOnUIThread(() -> bageVBinding.imageCacheTv.setText(str));
            } catch (Exception e) {
                BageLogUtils.e("获取图片缓存大小错误");
            }
        }).start();

    }

}
