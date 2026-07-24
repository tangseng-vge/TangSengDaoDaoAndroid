package com.chat.uikit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.chat.base.config.BageConfig;
import com.bage.im.BageIM;

/**
 * Keeps the IM process active while the app is in the background. Android can still suspend
 * networking under exceptional system conditions, so the SDK's reconnect path remains enabled.
 */
public class BageIMKeepAliveService extends Service {
    private static final String TAG = "BageIMKeepAliveService";
    private static final String CHANNEL_ID = "bage_im_keep_alive";
    private static final int NOTIFICATION_ID = 10086;
    private static final int LEGACY_JOB_ID = 8;

    public static void start(Context context) {
        if (context == null) return;
        cancelLegacyJob(context);
        try {
            ContextCompat.startForegroundService(
                    context.getApplicationContext(),
                    new Intent(context, BageIMKeepAliveService.class)
            );
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to start IM keep-alive service", e);
        }
    }

    public static void stop(Context context) {
        if (context == null) return;
        cancelLegacyJob(context);
        context.getApplicationContext().stopService(new Intent(context, BageIMKeepAliveService.class));
    }

    private static void cancelLegacyJob(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.cancel(LEGACY_JOB_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void updateForegroundNotification() {
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        Log.i(TAG, "IM keep-alive foreground notification posted");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForegroundService() 对已经存在的 Service 只会再次触发
        // onStartCommand()，不会重新执行 onCreate()。每次都重新发布通知，
        // 这样用户刚授予通知权限、系统恢复 Service 或厂商系统隐藏通知后，
        // 下一次前台唤醒都能恢复常驻通知。
        updateForegroundNotification();
        if (!TextUtils.isEmpty(BageConfig.getInstance().getToken())) {
            BageIM.getInstance().getConnectionManager().connection();
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.im_keep_alive_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setShowBadge(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            NotificationChannel currentChannel = manager.getNotificationChannel(CHANNEL_ID);
            if (currentChannel != null
                    && currentChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                Log.w(TAG, "IM keep-alive notification channel is disabled");
            }
        }
    }

    private Notification createNotification() {
        // 常驻通知不能使用 Launcher Intent：Launcher 指向 SplashActivity，会让已经初始化的
        // 应用再次执行配置加载和启动流程。TabActivity 是登录后的任务栈根 Activity，singleTask
        // 配合 CLEAR_TOP 会直接复用现有实例并回到主界面。
        Intent launchIntent;
        if (!TextUtils.isEmpty(BageConfig.getInstance().getToken())) {
            launchIntent = new Intent(this, TabActivity.class);
            launchIntent.setAction(getPackageName() + ".action.OPEN_MAIN_FROM_KEEP_ALIVE");
        } else {
            launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        }
        PendingIntent contentIntent = null;
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            contentIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }

        CharSequence appName = getApplicationInfo().loadLabel(getPackageManager());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(getApplicationInfo().icon)
                .setContentTitle(appName)
                .setContentText(getString(R.string.im_keep_alive_notification))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false);
        if (contentIntent != null) builder.setContentIntent(contentIntent);
        return builder.build();
    }
}
