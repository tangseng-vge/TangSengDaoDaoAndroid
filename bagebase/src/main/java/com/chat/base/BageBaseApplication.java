package com.chat.base;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.chat.base.act.PlayVideoActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.db.DBHelper;
import com.chat.base.emoji.EmojiManager;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.PlayVideoMenu;
import com.chat.base.entity.AppModule;
import com.chat.base.glide.OkHttpUrlLoader;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.CrashHandler;
import com.chat.base.utils.BageDeviceUtils;
import com.chat.base.utils.BageFileUtils;
import com.chat.base.utils.BageReader;

import org.telegram.ui.Components.RLottieApplication;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;


/**
 * 2020-02-26 09:52
 */
public class BageBaseApplication {
    private WeakReference<Context> context;
    private DBHelper mDbHelper;
    private String fileDir = "bageIM";// 缓存目录

    public boolean disconnect = true;

    public String versionName;
    public String appID = "bagechat";

    public static volatile Handler applicationHandler;

    private BageBaseApplication() {
    }

    private static class WApplicationBinder {
        final static BageBaseApplication wb = new BageBaseApplication();
    }

    public static BageBaseApplication getInstance() {
        return WApplicationBinder.wb;
    }

    public String packageName;
    public Application application;
    private List<AppModule> appModules;

    public void init(@NonNull String packageName, Application context) {
        applicationHandler = new Handler(context.getMainLooper());
        this.packageName = packageName;
        this.application = context;
        this.context = new WeakReference<>(context);
        float density = context.getResources().getDisplayMetrics().density;
        AndroidUtilities.setDensity(density);
        boolean isShowDialog = BageSharedPreferencesUtil.getInstance().getBoolean("show_agreement_dialog");
        if (isShowDialog) {
            return;
        }
        String json = BageSharedPreferencesUtil.getInstance().getSPWithUID("app_module");
        if (!TextUtils.isEmpty(json)) {
            appModules = JSON.parseArray(json, AppModule.class);
        }
        versionName = BageDeviceUtils.getInstance().getVersionName(context);
        Glide.get(context).getRegistry().replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
        initCacheDir();
        new Thread(() -> {
            EmojiManager.getInstance().init();
            RLottieApplication.getInstance().init(context);
            CrashHandler.getInstance().init(context);
            //158638
//            HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
        }).start();
        //监听视频播放
        EndpointManager.getInstance().setMethod("play_video", object -> {
            if (object instanceof PlayVideoMenu playVideoMenu) {
                @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(Objects.requireNonNull(playVideoMenu.activity), new Pair<>(playVideoMenu.view, "coverIv"));
                Intent intent = new Intent(playVideoMenu.activity, PlayVideoActivity.class);
                intent.putExtra("coverImg", playVideoMenu.coverUrl);
                intent.putExtra("url", playVideoMenu.playUrl);
                intent.putExtra("title", playVideoMenu.videoTitle);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                playVideoMenu.activity.startActivity(intent, activityOptions.toBundle());
                playVideoMenu.activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
            return null;
        });
    }

    public Context getContext() {
        return context.get();
    }

    /**
     * 获取数据库
     *
     * @return dbHelper
     */
    public synchronized DBHelper getDbHelper() {
        if (mDbHelper == null) {
            String uid = BageConfig.getInstance().getUid();
            if (!TextUtils.isEmpty(uid) && context != null && context.get() != null) {
                mDbHelper = DBHelper.getInstance(context.get(), uid);
            }
        }
        return mDbHelper;
    }

    public void closeDbHelper() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
        }
    }

    public String getFileDir() {
        if (TextUtils.isEmpty(fileDir))
            fileDir = "bageIM";
        if (!TextUtils.isEmpty(BageConfig.getInstance().getUid())) {
            fileDir = String.format("%s/%s", fileDir, BageConfig.getInstance().getUid());
        }
        return fileDir;
    }

    private void initCacheDir() {
        BageConstants.avatarCacheDir = Objects.requireNonNull(getContext().getExternalFilesDir("bageAvatars")).getAbsolutePath() + "/";
        BageFileUtils.getInstance().createFileDir(BageConstants.avatarCacheDir);
        BageConstants.imageDir = Objects.requireNonNull(getContext().getExternalFilesDir("bageImages")).getAbsolutePath() + "/";
        BageFileUtils.getInstance().createFileDir(BageConstants.imageDir);
        BageConstants.videoDir = Objects.requireNonNull(getContext().getExternalFilesDir("bageVideos")).getAbsolutePath() + "/";
        BageFileUtils.getInstance().createFileDir(BageConstants.videoDir);
        BageConstants.voiceDir = Objects.requireNonNull(getContext().getExternalFilesDir("bageVoices")).getAbsolutePath() + "/";
        BageFileUtils.getInstance().createFileDir(BageConstants.voiceDir);
        BageConstants.chatBgCacheDir = Objects.requireNonNull(getContext().getExternalFilesDir("bageChatBg")).getAbsolutePath() + "/";
        BageFileUtils.getInstance().createFileDir(BageConstants.chatBgCacheDir);
        BageConstants.messageBackupDir = Objects.requireNonNull(getContext().getExternalFilesDir("messageBackup")).getAbsolutePath() + "/";
        BageFileUtils.getInstance().createFileDir(BageConstants.messageBackupDir);
        BageConstants.chatDownloadFileDir = Objects.requireNonNull(getContext().getExternalFilesDir("chatDownloadFile")).getAbsolutePath() + "/";
    }

    public AppModule getAppModuleWithSid(String sid) {
        AppModule appModule = null;
        if (BageReader.isNotEmpty(appModules)) {
            for (AppModule appModule1 : appModules) {
                if (appModule1.getSid().equals(sid)) {
                    appModule = appModule1;
                    break;
                }
            }
        }
        return appModule;
    }

    public boolean appModuleIsInjection(AppModule appModule) {
        if (appModule == null) {
            return true;
        }
        return appModule.getStatus() != 0 && appModule.getChecked();
    }
}
