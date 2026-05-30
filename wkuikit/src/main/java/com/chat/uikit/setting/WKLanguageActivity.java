package com.chat.uikit.setting;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConstants;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.utils.language.WKLanguageType;
import com.chat.base.utils.language.WKMultiLanguageUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActLanguageLayoutBinding;

/**
 * 2020-12-09 15:31
 * 多语言
 */
public class WKLanguageActivity extends WKBaseActivity<ActLanguageLayoutBinding> {

    private int selectedLanguage;

    @Override
    protected ActLanguageLayoutBinding getViewBinding() {
        return ActLanguageLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.language);
    }

    @Override
    protected void initView() {
        showOrHideRightBtn(false);
        selectedLanguage = WKMultiLanguageUtil.getInstance().getLanguageType();
        updateSelectionIndicators(selectedLanguage);
    }

    @Override
    protected void initListener() {
        wkVBinding.autoLayout.setOnClickListener(v -> onLanguageChosen(WKLanguageType.LANGUAGE_FOLLOW_SYSTEM));
        wkVBinding.simplifiedChineseLayout.setOnClickListener(v -> onLanguageChosen(WKLanguageType.LANGUAGE_CHINESE_SIMPLIFIED));
        wkVBinding.englishLayout.setOnClickListener(v -> onLanguageChosen(WKLanguageType.LANGUAGE_EN));
    }

    /** 用户点击切换语言：一次完成 App Locale + 刷新已注册的端点菜单。 */
    private void onLanguageChosen(int languageType) {
        updateSelectionIndicators(languageType);
        if (selectedLanguage == languageType) {
            return;
        }
        selectedLanguage = languageType;
        applyLocaleChange(selectedLanguage);
    }

    private void applyLocaleChange(int languageType) {
        WKMultiLanguageUtil.getInstance().updateLanguage(languageType);
        applyAppLocale(languageType);
        refreshRegisteredMenus();
    }

    /** AppCompat 全局 Locale（含跟随系统），不跳转 MainActivity / 不杀进程。 */
    private void applyAppLocale(int languageType) {
        if (languageType == WKLanguageType.LANGUAGE_EN) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
        } else if (languageType == WKLanguageType.LANGUAGE_CHINESE_SIMPLIFIED) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"));
        } else if (languageType == WKLanguageType.LANGUAGE_CHINESE_TRADITIONAL) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-TW"));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }
    }

    /** 通知项目里已注册的刷新回调（通讯录/发现等），无需重启应用。 */
    private void refreshRegisteredMenus() {
        EndpointManager.getInstance().invokes(EndpointCategory.wkRefreshMailList, this);
        EndpointManager.getInstance().invokes(EndpointCategory.wkRefreshPersonalCenter, this);
        EndpointManager.getInstance().invokes(EndpointCategory.wkRefreshChatConversation, null);
        EndpointManager.getInstance().invoke(WKConstants.refreshContacts, null);
    }

    private void updateSelectionIndicators(int languageType) {
        if (languageType == WKLanguageType.LANGUAGE_FOLLOW_SYSTEM) {
            wkVBinding.autoIv.setVisibility(View.VISIBLE);
            wkVBinding.englishIv.setVisibility(View.INVISIBLE);
            wkVBinding.simplifiedChineseIv.setVisibility(View.INVISIBLE);
        } else if (languageType == WKLanguageType.LANGUAGE_EN) {
            wkVBinding.autoIv.setVisibility(View.INVISIBLE);
            wkVBinding.englishIv.setVisibility(View.VISIBLE);
            wkVBinding.simplifiedChineseIv.setVisibility(View.INVISIBLE);
        } else if (languageType == WKLanguageType.LANGUAGE_CHINESE_SIMPLIFIED) {
            wkVBinding.autoIv.setVisibility(View.INVISIBLE);
            wkVBinding.englishIv.setVisibility(View.INVISIBLE);
            wkVBinding.simplifiedChineseIv.setVisibility(View.VISIBLE);
        }
    }
}
