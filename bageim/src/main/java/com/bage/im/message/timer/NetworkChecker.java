package com.bage.im.message.timer;

import android.util.Log;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.message.BageConnection;
import com.bage.im.message.type.BageConnectReason;
import com.bage.im.message.type.BageConnectStatus;
import com.bage.im.utils.BageLoggerUtils;

public class NetworkChecker {
    private final Object lock = new Object(); // 添加锁对象
    public boolean isForcedReconnect;
    public boolean checkNetWorkTimerIsRunning = false;

    public void startNetworkCheck() {
        TimerManager.getInstance().addTask(
                TimerTasks.NETWORK_CHECK,
                () -> {
                    synchronized (lock) {
                        checkNetworkStatus();
                    }
                },
                0,
                1000
        );
    }

    private void checkNetworkStatus() {
        boolean is_have_network = BageIMApplication.getInstance().isNetworkConnected();
        if (!is_have_network) {
            isForcedReconnect = true;
            BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.noNetwork, BageConnectReason.NoNetwork);
            BageLoggerUtils.getInstance().e("无网络连接...");
            BageConnection.getInstance().checkSendingMsg();
        } else {
            //有网络
            if (BageConnection.getInstance().connectionIsNull() || isForcedReconnect) {
                // 网络恢复时，重置重连计数，给予完整的重连机会
                if (isForcedReconnect) {
                    BageConnection.getInstance().resetConnCount();
                }
                BageConnection.getInstance().reconnection();
                isForcedReconnect = false;
            }
        }
        checkNetWorkTimerIsRunning = true;
    }
}
