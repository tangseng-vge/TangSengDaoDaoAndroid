//package com.bage.im.message;
//
//import com.bage.im.BageIM;
//import com.bage.im.BageIMApplication;
//import com.bage.im.message.type.BageConnectReason;
//import com.bage.im.message.type.BageConnectStatus;
//import com.bage.im.protocol.BagePingMsg;
//import com.bage.im.utils.BageLoggerUtils;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
///**
// * 5/21/21 11:19 AM
// */
//class BageTimers {
//    private BageTimers() {
//    }
//
//    private static class ConnectionTimerHandlerBinder {
//        static final BageTimers timeHandle = new BageTimers();
//    }
//
//    public static BageTimers getInstance() {
//        return ConnectionTimerHandlerBinder.timeHandle;
//    }
//
//
//    // 发送心跳定时器
//    private Timer heartBeatTimer;
//    // 检查心跳定时器
//    private Timer checkHeartTimer;
//    // 检查网络状态定时器
//    private Timer checkNetWorkTimer;
//    boolean checkNetWorkTimerIsRunning = false;
//
//    //关闭所有定时器
//    void stopAll() {
//        stopHeartBeatTimer();
//        stopCheckHeartTimer();
//        stopCheckNetWorkTimer();
//    }
//
//    //开启所有定时器
//    void startAll() {
//        startHeartBeatTimer();
//        startCheckHeartTimer();
//        startCheckNetWorkTimer();
//    }
//
//    //检测网络
//    private void stopCheckNetWorkTimer() {
//        if (checkNetWorkTimer != null) {
//            checkNetWorkTimer.cancel();
//            checkNetWorkTimer.purge();
//            checkNetWorkTimer = null;
//            checkNetWorkTimerIsRunning = false;
//        }
//    }
//
//    //检测心跳
//    private void stopCheckHeartTimer() {
//        if (checkHeartTimer != null) {
//            checkHeartTimer.cancel();
//            checkHeartTimer.purge();
//            checkHeartTimer = null;
//        }
//    }
//
//    //停止心跳Timer
//    private void stopHeartBeatTimer() {
//        if (heartBeatTimer != null) {
//            heartBeatTimer.cancel();
//            heartBeatTimer.purge();
//            heartBeatTimer = null;
//        }
//    }
//
//    //开始心跳
//    private void startHeartBeatTimer() {
//        stopHeartBeatTimer();
//        heartBeatTimer = new Timer();
//        // 心跳时间
//        int heart_time = 60 * 2;
//        heartBeatTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                //发送心跳
//                BageConnection.getInstance().sendMessage(new BagePingMsg());
//            }
//        }, 0, heart_time * 1000);
//    }
//
//    //开始检查心跳Timer
//    private void startCheckHeartTimer() {
//        stopCheckHeartTimer();
//        checkHeartTimer = new Timer();
//        checkHeartTimer.schedule(new TimerTask() {
//
//            @Override
//            public void run() {
//                if (BageConnection.getInstance().connection == null || heartBeatTimer == null) {
//                    BageConnection.getInstance().reconnection();
//                }
//                BageConnection.getInstance().checkHeartIsTimeOut();
//            }
//        }, 1000 * 7, 1000 * 7);
//    }
//
//    boolean isForcedReconnect;
//
//    //开启检测网络定时器
//    void startCheckNetWorkTimer() {
//        stopCheckNetWorkTimer();
//        checkNetWorkTimer = new Timer();
//        checkNetWorkTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                boolean is_have_network = BageIMApplication.getInstance().isNetworkConnected();
//                if (!is_have_network) {
//                    isForcedReconnect = true;
//                    BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.noNetwork, BageConnectReason.NoNetwork);
//                    BageLoggerUtils.getInstance().e("No network connection...");
//                    BageConnection.getInstance().checkSendingMsg();
//                } else {
//                    //有网络
//                    if (BageConnection.getInstance().connectionIsNull() || isForcedReconnect  ) {
//                        BageConnection.getInstance().reconnection();
//                        isForcedReconnect = false;
//                    }
//                }
//                if (BageConnection.getInstance().connection == null || !BageConnection.getInstance().connection.isOpen()) {
//                    BageConnection.getInstance().reconnection();
//                }
//                checkNetWorkTimerIsRunning = true;
//            }
//        }, 0, 1000);
//    }
//}
