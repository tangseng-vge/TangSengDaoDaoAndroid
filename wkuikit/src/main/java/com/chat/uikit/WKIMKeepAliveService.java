package com.chat.uikit;

import android.app.JobScheduler;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

import com.chat.base.config.WKConfig;
import com.xinbida.wukongim.WKIM;

/**
 * Keeps the IM process active while the app is in the background. Android can still suspend
 * networking under exceptional system conditions, so the SDK's reconnect path remains enabled.
 */
public class WKIMKeepAliveService extends Service {
    private static final String TAG = "WKIMKeepAliveService";
    private static final String CHANNEL_ID = "wk_im_keep_alive";
    private static final int NOTIFICATION_ID = 10086;
    private static final int LEGACY_JOB_ID = 8;

    public static void start(Context context) {
        if (context == null) return;
        cancelLegacyJob(context);
        try {
            ContextCompat.startForegroundService(
                    context.getApplicationContext(),
                    new Intent(context, WKIMKeepAliveService.class)
            );
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to start IM keep-alive service", e);
        }
    }

    public static void stop(Context context) {
        if (context == null) return;
        cancelLegacyJob(context);
        context.getApplicationContext().stopService(new Intent(context, WKIMKeepAliveService.class));
    }

    private static void cancelLegacyJob(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.cancel(LEGACY_JOB_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!TextUtils.isEmpty(WKConfig.getInstance().getToken())) {
            WKIM.getInstance().getConnectionManager().connection();
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
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent contentIntent = null;
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
