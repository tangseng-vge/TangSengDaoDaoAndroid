package com.chat.base.utils.language;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.chat.base.BageBaseApplication;

/**
 * Endpoint 回调里取文案：优先用 invoke 传入的 Activity Context（随 AppCompat Locale 更新），
 * 避免在 Application.init 时缓存的 mContext 上 getString 得到旧语言。
 */
public final class EndpointLocaleHelper {

    private EndpointLocaleHelper() {
    }

    @NonNull
    public static String getString(Object invokeParam, @StringRes int resId) {
        Context ctx = resolveContext(invokeParam);
        return ctx.getString(resId);
    }

    @NonNull
    private static Context resolveContext(Object invokeParam) {
        if (invokeParam instanceof Context) {
            return (Context) invokeParam;
        }
        return AppLocaleStrings.localizedContext();
    }
}
