package com.chat.moments.utils;

import android.app.Activity;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.chat.base.config.BageConfig;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageToastUtils;
import com.chat.base.utils.language.BageMultiLanguageUtil;
import com.chat.moments.R;
import com.chat.moments.entity.Moments;
import com.chat.moments.entity.MomentsReply;
import com.chat.moments.entity.ReportCategory;
import com.chat.moments.entity.ReportResult;
import com.chat.moments.service.MomentsModel;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 朋友圈拉黑、举报动态及举报评论的共用交互。 */
public final class MomentModerationHelper {
    private MomentModerationHelper() {
    }

    public interface IBlockResult {
        void onSuccess(String uid);
    }

    public interface IReportResult {
        void onSuccess(ReportResult result);
    }

    public static void blockPublisher(Activity activity, Moments moment, IBlockResult listener) {
        if (activity == null || moment == null || TextUtils.isEmpty(moment.publisher)
                || TextUtils.equals(moment.publisher, BageConfig.getInstance().getUid())) {
            return;
        }
        BageDialogUtils.getInstance().showDialog(
                activity,
                activity.getString(R.string.block_user),
                activity.getString(R.string.block_user_tips),
                true,
                "",
                activity.getString(R.string.sure),
                0,
                ContextCompat.getColor(activity, R.color.red),
                index -> {
                    if (index != 1) return;
                    LoadingPopupView loading = showLoading(activity);
                    MomentsModel.getInstance().blockUser(moment.publisher, (code, msg) -> {
                        loading.dismiss();
                        if (code == HttpResponseCode.success) {
                            BageToastUtils.getInstance().showToastSuccess(activity.getString(R.string.block_success));
                            if (listener != null) listener.onSuccess(moment.publisher);
                        } else {
                            showFailure(activity, msg);
                        }
                    });
                });
    }

    public static void reportMoment(Activity activity, Moments moment, IReportResult listener) {
        if (activity == null || moment == null || TextUtils.isEmpty(moment.moment_no)
                || TextUtils.equals(moment.publisher, BageConfig.getInstance().getUid())) {
            return;
        }
        loadCategories(activity, moment.moment_no, null, listener);
    }

    public static void reportComment(Activity activity, String momentNo, MomentsReply reply,
                                     IReportResult listener) {
        if (activity == null || reply == null || TextUtils.isEmpty(momentNo)
                || TextUtils.isEmpty(reply.sid)
                || TextUtils.equals(reply.uid, BageConfig.getInstance().getUid())) {
            return;
        }
        loadCategories(activity, momentNo, reply.sid, listener);
    }

    private static void loadCategories(Activity activity, String momentNo, String commentId,
                                       IReportResult listener) {
        LoadingPopupView loading = showLoading(activity);
        Locale locale = BageMultiLanguageUtil.getInstance().resolveLocale();
        String language = locale == null ? null : locale.getLanguage();
        String lang = language != null && language.startsWith("en") ? "en" : "cn";
        MomentsModel.getInstance().reportCategories(lang, (code, msg, categories) -> {
            loading.dismiss();
            if (code != HttpResponseCode.success) {
                showFailure(activity, msg);
                return;
            }
            if (BageReader.isEmpty(categories)) {
                BageToastUtils.getInstance().showToastNormal(activity.getString(R.string.no_report_categories));
                return;
            }
            showCategorySheet(activity, categories, activity.getString(R.string.select_report_reason),
                    momentNo, commentId, listener);
        });
    }

    private static void showCategorySheet(Activity activity, List<ReportCategory> categories,
                                          String title, String momentNo, String commentId,
                                          IReportResult listener) {
        List<BottomSheetItem> items = new ArrayList<>();
        for (ReportCategory category : categories) {
            if (category == null || TextUtils.isEmpty(category.category_name)) continue;
            items.add(new BottomSheetItem(category.category_name, 0, () -> {
                if (BageReader.isNotEmpty(category.children)) {
                    showCategorySheet(activity, category.children, category.category_name,
                            momentNo, commentId, listener);
                } else {
                    submitReport(activity, momentNo, commentId, category.category_no, listener);
                }
            }));
        }
        if (items.isEmpty()) {
            BageToastUtils.getInstance().showToastNormal(activity.getString(R.string.no_report_categories));
            return;
        }
        BageDialogUtils.getInstance().showBottomSheet(activity, title, false, items);
    }

    private static void submitReport(Activity activity, String momentNo, String commentId,
                                     String categoryNo, IReportResult listener) {
        if (TextUtils.isEmpty(categoryNo)) {
            BageToastUtils.getInstance().showToastFail(activity.getString(R.string.select_report_reason));
            return;
        }
        LoadingPopupView loading = showLoading(activity);
        MomentsModel.IReportResult resultListener = (code, msg, result) -> {
            loading.dismiss();
            if (code == HttpResponseCode.success) {
                String tip = result != null && !TextUtils.isEmpty(result.msg)
                        ? result.msg : activity.getString(R.string.report_success);
                BageToastUtils.getInstance().showToastSuccess(tip);
                if (listener != null) listener.onSuccess(result);
            } else {
                showFailure(activity, msg);
            }
        };
        if (TextUtils.isEmpty(commentId)) {
            MomentsModel.getInstance().reportMoment(momentNo, categoryNo, resultListener);
        } else {
            MomentsModel.getInstance().reportComment(momentNo, commentId, categoryNo, resultListener);
        }
    }

    private static LoadingPopupView showLoading(Activity activity) {
        LoadingPopupView loading = new XPopup.Builder(activity)
                .asLoading(activity.getString(com.chat.base.R.string.loading));
        loading.show();
        return loading;
    }

    private static void showFailure(Activity activity, String msg) {
        BageToastUtils.getInstance().showToastFail(TextUtils.isEmpty(msg)
                ? activity.getString(R.string.operation_failed) : msg);
    }
}
