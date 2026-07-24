package com.chat.login.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.BageReader;
import com.chat.login.R;
import com.chat.login.databinding.ActRegisterLayoutBinding;
import com.chat.login.entity.CountryCodeEntity;
import com.chat.login.service.LoginContract;
import com.chat.login.service.LoginPresenter;

import java.util.List;
import java.util.Objects;

/**
 * 2020-06-19 15:42
 * 注册
 */
public class BageRegisterActivity extends BageBaseActivity<ActRegisterLayoutBinding> implements LoginContract.LoginView {
    private String code = "0086";
    private LoginPresenter presenter;
    private BageAPPConfig appConfig;

    @Override
    protected ActRegisterLayoutBinding getViewBinding() {
        return ActRegisterLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        presenter = new LoginPresenter(this);
    }

    @Override
    protected void initView() {
        bageVBinding.getVCodeBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.registerBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.privacyPolicyTv.setTextColor(Theme.colorAccount);
        bageVBinding.userAgreementTv.setTextColor(Theme.colorAccount);
        bageVBinding.loginTv.setTextColor(Theme.colorAccount);
        bageVBinding.authCheckBox.setResId(getContext(), R.mipmap.round_check2);
        bageVBinding.authCheckBox.setDrawBackground(true);
        bageVBinding.authCheckBox.setHasBorder(true);
        bageVBinding.authCheckBox.setStrokeWidth(AndroidUtilities.dp(1));
        bageVBinding.authCheckBox.setBorderColor(ContextCompat.getColor(getContext(), R.color.color999));
        bageVBinding.authCheckBox.setSize(18);
        bageVBinding.authCheckBox.setColor(Theme.colorAccount, ContextCompat.getColor(getContext(), R.color.white));
//        bageVBinding.authCheckBox.setVisibility(View.VISIBLE);
        bageVBinding.authCheckBox.setEnabled(true);
        bageVBinding.authCheckBox.setChecked(false, true);

        bageVBinding.privacyPolicyTv.setOnClickListener(v -> showWebView(BageApiConfig.baseWebUrl + "privacy_policy.html"));
        bageVBinding.userAgreementTv.setOnClickListener(v -> showWebView(BageApiConfig.baseWebUrl + "user_agreement.html"));
        bageVBinding.registerAppTv.setText(String.format(getString(R.string.register_app), getString(R.string.app_name)));
        bageVBinding.nameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    bageVBinding.getVCodeBtn.setAlpha(1f);
                    bageVBinding.getVCodeBtn.setEnabled(true);
                } else {
                    bageVBinding.getVCodeBtn.setEnabled(false);
                    bageVBinding.getVCodeBtn.setAlpha(0.2f);
                }
                checkStatus();
            }
        });
        bageVBinding.verfiEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkStatus();
            }
        });
        bageVBinding.pwdEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkStatus();
            }
        });
        bageVBinding.loginTv.setOnClickListener(v -> startActivity(new Intent(this, BageLoginActivity.class)));
        bageVBinding.chooseCodeTv.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChooseAreaCodeActivity.class);
            intentActivityResultLauncher.launch(intent);
        });
        bageVBinding.registerBtn.setOnClickListener(v -> {
//            if (!bageVBinding.authCheckBox.isChecked()) {
//                showToast(R.string.agree_auth_tips);
//                return;
//            }

            String phone = Objects.requireNonNull(bageVBinding.nameEt.getText()).toString();
            String smsCode = Objects.requireNonNull(bageVBinding.verfiEt.getText()).toString();
            String pwd = Objects.requireNonNull(bageVBinding.pwdEt.getText()).toString();
            String inviteCode = Objects.requireNonNull(bageVBinding.inviteCodeTv.getText()).toString();
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(pwd)) {
                if (pwd.length() < 6 || pwd.length() > 16) {
                    showSingleBtnDialog(getString(R.string.pwd_length_error));
                } else {
//                    if (appConfig != null && appConfig.register_invite_on == 1 && TextUtils.isEmpty(inviteCode)) {
//                        showSingleBtnDialog(getString(R.string.invite_code_not_null));
//                        return;
//                    }
                    loadingPopup.show();
                    presenter.registerApp("123456", code, "", phone, pwd, "");
                }
            }
        });
        bageVBinding.getVCodeBtn.setOnClickListener(v -> {
            String phone = Objects.requireNonNull(bageVBinding.nameEt.getText()).toString();
            if (!TextUtils.isEmpty(phone)) {
                presenter.registerCode(code, phone);
            }
        });

        bageVBinding.myTv.setOnClickListener(view1 -> bageVBinding.authCheckBox.setChecked(!bageVBinding.authCheckBox.isChecked(), true));
        bageVBinding.authCheckBox.setOnClickListener(view1 -> bageVBinding.authCheckBox.setChecked(!bageVBinding.authCheckBox.isChecked(), true));
    }

    @Override
    protected void initListener() {
        bageVBinding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                bageVBinding.pwdEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                bageVBinding.pwdEt.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            bageVBinding.pwdEt.setSelection(Objects.requireNonNull(bageVBinding.pwdEt.getText()).length());
        });
    }

    @Override
    protected void initData() {
        BageCommonModel.getInstance().getAppConfig((code, msg, bageappConfig) -> {
            if (code == HttpResponseCode.success) {
                appConfig = bageappConfig;
                if (appConfig != null && appConfig.register_invite_on == 1) {
                    bageVBinding.inviteCodeTv.setHint(R.string.input_invite_code_must);
                    bageVBinding.inviteLayout.setVisibility(View.VISIBLE);
                    bageVBinding.inviteLineView.setVisibility(View.VISIBLE);
                } else {
                    bageVBinding.inviteCodeTv.setHint(R.string.input_invite_code_not_must);
                    bageVBinding.inviteLayout.setVisibility(View.GONE);
                    bageVBinding.inviteLineView.setVisibility(View.GONE);
                }
            } else {
                showToast(msg);
            }
        });
    }

    private void checkStatus() {
        String phone = Objects.requireNonNull(bageVBinding.nameEt.getText()).toString();
        String smsCode = Objects.requireNonNull(bageVBinding.verfiEt.getText()).toString();
        String pwd = Objects.requireNonNull(bageVBinding.pwdEt.getText()).toString();
//        if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(smsCode) && !TextUtils.isEmpty(pwd)) {
        if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(pwd)) {
//            bageVBinding.registerBtn.setAlpha(1f);
            bageVBinding.registerBtn.setEnabled(true);
        } else {
//            bageVBinding.registerBtn.setAlpha(0.2f);
            bageVBinding.registerBtn.setEnabled(false);
        }
    }


    ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        //此处是跳转的result回调方法
        if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
            CountryCodeEntity entity = result.getData().getParcelableExtra("entity");
            assert entity != null;
            code = entity.code;
            String codeName = code.substring(2);
            bageVBinding.codeTv.setText(String.format("+%s", codeName));
        }
    });

    @Override
    public void loginResult(UserInfoEntity userInfoEntity) {
        loadingPopup.dismiss();
        SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.pwdEt);
        hideLoading();

        if (TextUtils.isEmpty(userInfoEntity.name)) {
            Intent intent = new Intent(this, PerfectUserInfoActivity.class);
            startActivity(intent);
            finish();
        } else {
            new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                List<LoginMenu> list = EndpointManager.getInstance().invokes(EndpointCategory.loginMenus, null);
                if (BageReader.isNotEmpty(list)) {
                    for (LoginMenu menu : list) {
                        if (menu.iMenuClick != null) menu.iMenuClick.onClick();
                    }
                }
                finish();
            }, 500);
        }
    }

    @Override
    public void setCountryCode(List<CountryCodeEntity> list) {

    }

    @Override
    public void setRegisterCodeSuccess(int code, String msg, int exist) {
        if (code == HttpResponseCode.success) {
            if (exist == 1) {
                showSingleBtnDialog(getString(R.string.account_exist));
            } else {
                bageVBinding.nameEt.setEnabled(false);
                presenter.startTimer();
            }
        } else {
            showToast(msg);
        }
    }

    @Override
    public void setLoginFail(int code, String uid, String phone) {

    }

    @Override
    public void setSendCodeResult(int code, String msg) {

    }

    @Override
    public void setResetPwdResult(int code, String msg) {
    }

//    @Override
//    public void setUpdatePwdResult(int code, String msg) {
//
//    }

    @Override
    public Button getVerfiCodeBtn() {
        return bageVBinding.getVCodeBtn;
    }

    @Override
    public EditText getNameEt() {
        return bageVBinding.nameEt;
    }

    @Override
    public void showError(String msg) {
        showSingleBtnDialog(msg);
    }

    @Override
    public void hideLoading() {
        loadingPopup.dismiss();
    }


    @Override
    public Context getContext() {
        return this;
    }

}
