package com.chat.login.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageReader;
import com.chat.login.R;
import com.chat.login.databinding.ActPerfectUserInfoLayoutBinding;
import com.chat.login.service.LoginModel;
import com.bage.im.entity.BageChannelType;

import java.util.List;
import java.util.Objects;

/**
 * 2020-08-28 13:43
 * 完善个人资料
 */
public class PerfectUserInfoActivity extends BageBaseActivity<ActPerfectUserInfoLayoutBinding> {

    String path;

    @Override
    protected ActPerfectUserInfoLayoutBinding getViewBinding() {
        return ActPerfectUserInfoLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.bagelogin_perfect_userinfo);
    }

    @Override
    protected void initView() {
        bageVBinding.avatarView.setSize(120);
        bageVBinding.avatarView.setStrokeWidth(0);
        bageVBinding.avatarView.imageView.setImageResource(R.mipmap.icon_default_header);
    }

    @Override
    protected void initListener() {
        bageVBinding.sureBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.avatarView.setOnClickListener(v -> chooseIMG());
        bageVBinding.sureBtn.setOnClickListener(v -> {

            if (TextUtils.isEmpty(path)) {
                showToast(R.string.bagelogin_must_upload_header);
                return;
            }
            if (!checkEditInputIsEmpty(bageVBinding.nameEt, R.string.nickname_not_null)) {
                loadingPopup.show();
                LoginModel.getInstance().updateUserInfo("name", Objects.requireNonNull(bageVBinding.nameEt.getText()).toString(), (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        UserInfoEntity userInfoEntity = BageConfig.getInstance().getUserInfo();
                        userInfoEntity.name = bageVBinding.nameEt.getText().toString();
                        BageConfig.getInstance().saveUserInfo(userInfoEntity);
                        BageConfig.getInstance().setUserName(bageVBinding.nameEt.getText().toString());
                        List<LoginMenu> list = EndpointManager.getInstance().invokes(EndpointCategory.loginMenus, null);
                        if (BageReader.isNotEmpty(list)) {
                            for (LoginMenu menu : list) {
                                if (menu.iMenuClick != null)
                                    menu.iMenuClick.onClick();
                            }
                        }
                        loadingPopup.dismiss();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }

        });
    }

    private void chooseIMG() {
        GlideUtils.getInstance().chooseIMG(this, 1, true, ChooseMimeType.img, false, new GlideUtils.ISelectBack() {
            @Override
            public void onBack(List<ChooseResult> paths) {
                if (BageReader.isNotEmpty(paths)) {
                    path = paths.get(0).path;
                    LoginModel.getInstance().uploadAvatar(path, code -> {
                        if (code == HttpResponseCode.success) {
                            GlideUtils.getInstance().showAvatarImg(PerfectUserInfoActivity.this, BageConfig.getInstance().getUid(), BageChannelType.PERSONAL, "", bageVBinding.avatarView.imageView);
                            bageVBinding.coverIv.setVisibility(View.GONE);
                        }
                    });

                }
            }

            @Override
            public void onCancel() {

            }
        });
    }
}
