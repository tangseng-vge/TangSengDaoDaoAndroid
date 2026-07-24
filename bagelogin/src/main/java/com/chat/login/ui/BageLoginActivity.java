package com.chat.login.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.endpoint.entity.OtherLoginResultMenu;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.login.R;
import com.chat.login.databinding.ActLoginLayoutBinding;
import com.chat.login.entity.CountryCodeEntity;
import com.chat.login.service.LoginContract;
import com.chat.login.service.LoginPresenter;

import java.util.List;
import java.util.Objects;

/**
 * 2020-02-26 15:55
 * 登录
 */
public class BageLoginActivity extends BageBaseActivity<ActLoginLayoutBinding> implements LoginContract.LoginView {
    private BageAPPConfig bageappConfig;
    private String code = "0086";
    private LoginPresenter loginPresenter;

    @Override
    protected ActLoginLayoutBinding getViewBinding() {
        return ActLoginLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        loginPresenter = new LoginPresenter(this);
    }

    @Override
    protected void initView() {
        Log.d("AppFlow", "[BageLoginActivity] initView called - Stack trace:");
        Log.d("AppFlow", "[BageLoginActivity] " + android.util.Log.getStackTraceString(new Exception()));
        
        bageVBinding.loginBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.privacyPolicyTv.setTextColor(Theme.colorAccount);
        bageVBinding.userAgreementTv.setTextColor(Theme.colorAccount);
        bageVBinding.registerTv.setTextColor(Theme.colorAccount);
//        bageVBinding.forgetPwdTv.setTextColor(Theme.colorAccount);
        bageVBinding.checkbox.setResId(getContext(), R.mipmap.round_check2);
        bageVBinding.checkbox.setDrawBackground(true);
        bageVBinding.checkbox.setHasBorder(true);
        bageVBinding.checkbox.setStrokeWidth(AndroidUtilities.dp(1));
        bageVBinding.checkbox.setBorderColor(ContextCompat.getColor(getContext(), R.color.color999));
        bageVBinding.checkbox.setSize(18);
        bageVBinding.checkbox.setColor(Theme.colorAccount, ContextCompat.getColor(getContext(), R.color.white));
        bageVBinding.checkbox.setVisibility(View.VISIBLE);
        bageVBinding.checkbox.setEnabled(true);
        bageVBinding.checkbox.setChecked(false, true);
        int from = getIntent().getIntExtra("from", 0);
        if (from == 1 || from == 2) {
            String content = getString(R.string.bage_ban);
            if (from == 1) {
                content = getString(R.string.other_device_login);
            }
            BageDialogUtils.getInstance().showSingleBtnDialog(this, "", content, getString(R.string.sure), index -> {
            });
        }
        UserInfoEntity userInfoEntity = BageConfig.getInstance().getUserInfo();
        if (userInfoEntity != null) {
            if (!TextUtils.isEmpty(userInfoEntity.phone)) {
                bageVBinding.nameEt.setText(userInfoEntity.phone);
                bageVBinding.nameEt.setSelection(userInfoEntity.phone.length());

                String zone = BageConfig.getInstance().getUserInfo().zone;
                if (!TextUtils.isEmpty(zone)) {
                    code = zone;
                    String codeName = code.substring(2);
                    bageVBinding.codeTv.setText(String.format("+%s", codeName));
                }
            }
        }
        bageVBinding.loginTitleTv.setText(String.format(getString(R.string.login_title), getString(R.string.app_name)));
        bageVBinding.privacyPolicyTv.setOnClickListener(v -> showWebView(BageApiConfig.baseWebUrl + "privacy_policy.html"));
        bageVBinding.userAgreementTv.setOnClickListener(v -> showWebView(BageApiConfig.baseWebUrl + "user_agreement.html"));
        //  EndpointManager.getInstance().invoke("other_login_view", new OtherLoginViewMenu(this, bageVBinding.otherView));
    }

    @Override
    public boolean supportSlideBack() {
        return false;
    }

    @Override
    protected void initListener() {
        bageVBinding.myTv.setOnClickListener(view1 -> bageVBinding.checkbox.setChecked(!bageVBinding.checkbox.isChecked(), true));
        bageVBinding.checkbox.setOnClickListener(view1 -> bageVBinding.checkbox.setChecked(!bageVBinding.checkbox.isChecked(), true));

        bageVBinding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                bageVBinding.pwdEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                bageVBinding.pwdEt.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            bageVBinding.pwdEt.setSelection(Objects.requireNonNull(bageVBinding.pwdEt.getText()).length());
        });
        bageVBinding.loginBtn.setOnClickListener(v -> {
            if (checkEditInputIsEmpty(bageVBinding.nameEt, R.string.name_not_null)) return;
            if (checkEditInputIsEmpty(bageVBinding.pwdEt, R.string.pwd_not_null)) return;
//            if (code.equals("0086") && Objects.requireNonNull(bageVBinding.nameEt.getText()).toString().length() != 11) {
//                showSingleBtnDialog(getString(R.string.phone_error));
//                return;
//            }
//            if (!bageVBinding.checkbox.isChecked()) {
//                showSingleBtnDialog(getString(R.string.agree_auth_tips));
//                return;
//            }
            if (Objects.requireNonNull(bageVBinding.pwdEt.getText()).toString().length() < 6 || bageVBinding.pwdEt.getText().toString().length() > 16) {
                showSingleBtnDialog(getString(R.string.pwd_length_error));
                return;
            }
            loadingPopup.show();
            loadingPopup.setTitle(getString(R.string.logging_in));
            String name = Objects.requireNonNull(bageVBinding.nameEt.getText()).toString();
            loginPresenter.login(code + name, bageVBinding.pwdEt.getText().toString());
        });
        SingleClickUtil.onSingleClick(bageVBinding.registerTv, v -> startActivity(new Intent(this, BageRegisterActivity.class)));
        SingleClickUtil.onSingleClick(bageVBinding.chooseCodeTv, v -> {
            Intent intent = new Intent(this, ChooseAreaCodeActivity.class);
            intentActivityResultLauncher.launch(intent);
        });
//        SingleClickUtil.onSingleClick(bageVBinding.forgetPwdTv, v -> {
//            Intent intent = new Intent(this, BageResetLoginPwdActivity.class);
//            intent.putExtra("canEditPhone", true);
//            startActivity(intent);
//        });

        EndpointManager.getInstance().setMethod("other_login_result", object -> {
            OtherLoginResultMenu menu = (OtherLoginResultMenu) object;
            if (menu.getCode() == 0) {
                loginResult(menu.getUserInfoEntity());
            } else {
                setLoginFail(menu.getCode(), menu.getUserInfoEntity().uid, menu.getUserInfoEntity().phone);
            }
            return null;
        });
        bageVBinding.baseUrlTv.setOnClickListener(v -> {
            if (bageappConfig == null || bageappConfig.can_modify_api_url == 0) {
                return;
            }
            String url = BageSharedPreferencesUtil.getInstance().getSP("api_base_url", "");
            BageDialogUtils.getInstance().showInputDialog(this, getString(R.string.update_api), getString(R.string.update_api_content), url, getString(R.string.update_api_ip), 100, text -> {
                if (!TextUtils.isEmpty(text)) {
                    if (!text.toLowerCase().startsWith("http")) {
                        text = "http://" + text;
                    }
                    BageSharedPreferencesUtil.getInstance().putSP("api_base_url", text);
                    showBaseUrl();
                    EndpointManager.getInstance().invoke("update_base_url", text);
                }
            });
        });
        bageVBinding.resetTv.setOnClickListener(view -> {
            BageSharedPreferencesUtil.getInstance().putSP("api_base_url", "");
            EndpointManager.getInstance().invoke("update_base_url", "");
        });
        showBaseUrl();
    }

    @Override
    protected void initData() {
        super.initData();
        BageCommonModel.getInstance().getAppConfig((code, msg, bageappConfig) -> {
            this.bageappConfig = bageappConfig;
            if (bageappConfig != null && bageappConfig.can_modify_api_url == 1) {
                bageVBinding.settingLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showBaseUrl() {
        String apiURL = BageSharedPreferencesUtil.getInstance().getSP("api_base_url");
        if (!TextUtils.isEmpty(apiURL)) {
            bageVBinding.baseUrlTv.setText(apiURL);
            bageVBinding.resetTv.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.baseUrlTv.setText(R.string.update_api);
            bageVBinding.resetTv.setVisibility(View.GONE);
        }
    }

    @Override
    public void loginResult(UserInfoEntity userInfoEntity) {
        SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.pwdEt);

        if (TextUtils.isEmpty(userInfoEntity.name)) {
            Intent intent = new Intent(this, PerfectUserInfoActivity.class);
            startActivity(intent);
            finish();
        } else {
            hideLoading();
            new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> {
                List<LoginMenu> list = EndpointManager.getInstance().invokes(EndpointCategory.loginMenus, null);
                if (BageReader.isNotEmpty(list)) {
                    for (LoginMenu menu : list) {
                        if (menu.iMenuClick != null) menu.iMenuClick.onClick();
                    }
                }
                finish();
            }, 200);
        }
    }

    @Override
    public void setCountryCode(List<CountryCodeEntity> list) {

    }

    @Override
    public void setRegisterCodeSuccess(int code, String msg, int exist) {

    }

    @Override
    public void setLoginFail(int code, String uid, String phone) {
        Intent intent = new Intent(this, LoginAuthActivity.class);
        intent.putExtra("phone", phone);
        intent.putExtra("uid", uid);
        startActivity(intent);
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
        return null;
    }

    @Override
    public EditText getNameEt() {
        return null;
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
    public void finish() {
        super.finish();
        EndpointManager.getInstance().remove("other_login_result");
    }
}
