package com.chat.uikit.setting;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.ICommonListener;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.CommonResponse;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageToastUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActUpdatePasswordLayoutBinding;
import com.chat.uikit.user.service.UserModel;

import java.util.List;
import java.util.Objects;

public class UpdatePwdActivity extends BageBaseActivity<ActUpdatePasswordLayoutBinding> {

    private BageAPPConfig bageappConfig;
    private String code = "0086";

    @Override
    protected ActUpdatePasswordLayoutBinding getViewBinding() {
        return ActUpdatePasswordLayoutBinding.inflate(getLayoutInflater());
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    protected void initView() {
        bageVBinding.updateBtn.getBackground().setTint(Theme.colorAccount);
//        if (Theme.getDarkModeStatus(this)) {
//            bageVBinding.bgIv.setVisibility(android.view.View.GONE);
//            Theme.setColorFilter(this, bageVBinding.backIv, R.color.white);
//        } else {
//            bageVBinding.bgIv.setVisibility(android.view.View.VISIBLE);
//            Theme.setColorFilter(this, bageVBinding.backIv, R.color.colorDark);
//        }
        Theme.setPressedBackground(bageVBinding.backIv);
    }

    @Override
    protected void initListener() {
        super.initListener();
        bindPasswordToggle(bageVBinding.oldPwdCheckBox, bageVBinding.updatePwdEt);
        bindPasswordToggle(bageVBinding.newPwdCheckBox, bageVBinding.newPwdEt);
        bindPasswordToggle(bageVBinding.confirmPwdCheckBox, bageVBinding.pwdConfirmEt);
        bageVBinding.backIv.setOnClickListener(v -> finish());
        bageVBinding.updateBtn.setOnClickListener(v -> {
            if (checkEditInputIsEmpty(bageVBinding.updatePwdEt, R.string.placeholder_pwd)) return;
            if (checkEditInputIsEmpty(bageVBinding.newPwdEt, R.string.update_pwd)) return;
            if (checkEditInputIsEmpty(bageVBinding.pwdConfirmEt, R.string.update_confirm_password)) return;

            if (Objects.requireNonNull(bageVBinding.updatePwdEt.getText()).toString().length() < 6 || bageVBinding.newPwdEt.getText().toString().length() > 16) {
                showSingleBtnDialog(getString(R.string.update_pwd_error));
                return;
            }

            if (!bageVBinding.newPwdEt.getText().toString().equals(Objects.requireNonNull(bageVBinding.pwdConfirmEt.getText()).toString())) {
                showSingleBtnDialog(getString(R.string.update_pwd_error));
                return;
            }

            loadingPopup.show();
            loadingPopup.setTitle(getString(R.string.loading));

            String oldPassword = Objects.requireNonNull(bageVBinding.updatePwdEt.getText()).toString();
            String newPassword = Objects.requireNonNull(bageVBinding.newPwdEt.getText()).toString();
            UserModel.getInstance().updatePassword(oldPassword, newPassword, new ICommonListener() {
                @Override
                public void onResult(int code, String msg) {
                    loadingPopup.dismiss();
                    if (code == HttpResponseCode.success) {
                        BageToastUtils.getInstance().showToastSuccess("OK");
                        finish();
                    } else {
                        BageToastUtils.getInstance().showToastFail(msg);
                    }
                }
            });
        });
    }

    private void bindPasswordToggle(CheckBox checkBox, EditText editText) {
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            editText.setSelection(Objects.requireNonNull(editText.getText()).length());
        });
    }
}
