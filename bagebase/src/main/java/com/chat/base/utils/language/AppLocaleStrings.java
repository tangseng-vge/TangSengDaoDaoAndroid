package com.chat.base.utils.language;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.chat.base.BageBaseApplication;

import java.util.Locale;

/**
 * 按当前应用语言取字符串。避免使用 init 时缓存的 Application Context，
 * 在 AppCompat 切换 Locale 后仍读到旧语言的 getString。
 */
public final class AppLocaleStrings {

    private AppLocaleStrings() {
    }

    @NonNull
    public static String getString(@StringRes int resId) {
        return localizedContext().getString(resId);
    }

    @NonNull
    public static Context localizedContext() {
        Context base = BageBaseApplication.getInstance().application;
        if (base == null) {
            Context weak = BageBaseApplication.getInstance().getContext();
            if (weak != null) {
                return weak;
            }
            throw new IllegalStateException("Application context is not initialized");
        }
        Locale locale = BageMultiLanguageUtil.getInstance().resolveLocale();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        return base.createConfigurationContext(config);
    }
}
