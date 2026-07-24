package com.chat.uikit.setting;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConstants;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.utils.language.BageLanguageType;
import com.chat.base.utils.language.BageMultiLanguageUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActLanguageLayoutBinding;

/**
 * 2020-12-09 15:31
 * 多语言
 */
public class BageLanguageActivity extends BageBaseActivity<ActLanguageLayoutBinding> {

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
        selectedLanguage = BageMultiLanguageUtil.getInstance().getLanguageType();
        updateSelectionIndicators(selectedLanguage);
    }

    @Override
    protected void initListener() {
        bageVBinding.autoLayout.setOnClickListener(v -> onLanguageChosen(BageLanguageType.LANGUAGE_FOLLOW_SYSTEM));
        bageVBinding.simplifiedChineseLayout.setOnClickListener(v -> onLanguageChosen(BageLanguageType.LANGUAGE_CHINESE_SIMPLIFIED));
        bageVBinding.englishLayout.setOnClickListener(v -> onLanguageChosen(BageLanguageType.LANGUAGE_EN));
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
        BageMultiLanguageUtil.getInstance().updateLanguage(languageType);
        applyAppLocale(languageType);
        refreshRegisteredMenus();
    }

    /** AppCompat 全局 Locale（含跟随系统），不跳转 MainActivity / 不杀进程。 */
    private void applyAppLocale(int languageType) {
        if (languageType == BageLanguageType.LANGUAGE_EN) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
        } else if (languageType == BageLanguageType.LANGUAGE_CHINESE_SIMPLIFIED) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"));
        } else if (languageType == BageLanguageType.LANGUAGE_CHINESE_TRADITIONAL) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-TW"));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }
    }

    /** 通知项目里已注册的刷新回调（通讯录/发现等），无需重启应用。 */
    private void refreshRegisteredMenus() {
        EndpointManager.getInstance().invokes(EndpointCategory.bageRefreshMailList, this);
        EndpointManager.getInstance().invokes(EndpointCategory.bageRefreshPersonalCenter, this);
        EndpointManager.getInstance().invokes(EndpointCategory.bageRefreshChatConversation, null);
        EndpointManager.getInstance().invoke(BageConstants.refreshContacts, null);
    }

    private void updateSelectionIndicators(int languageType) {
        if (languageType == BageLanguageType.LANGUAGE_FOLLOW_SYSTEM) {
            bageVBinding.autoIv.setVisibility(View.VISIBLE);
            bageVBinding.englishIv.setVisibility(View.INVISIBLE);
            bageVBinding.simplifiedChineseIv.setVisibility(View.INVISIBLE);
        } else if (languageType == BageLanguageType.LANGUAGE_EN) {
            bageVBinding.autoIv.setVisibility(View.INVISIBLE);
            bageVBinding.englishIv.setVisibility(View.VISIBLE);
            bageVBinding.simplifiedChineseIv.setVisibility(View.INVISIBLE);
        } else if (languageType == BageLanguageType.LANGUAGE_CHINESE_SIMPLIFIED) {
            bageVBinding.autoIv.setVisibility(View.INVISIBLE);
            bageVBinding.englishIv.setVisibility(View.INVISIBLE);
            bageVBinding.simplifiedChineseIv.setVisibility(View.VISIBLE);
        }
    }
}
