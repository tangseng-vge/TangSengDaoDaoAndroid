package com.bage.im.manager;

import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.BageDBHelper;
import com.bage.im.interfaces.IConnectionStatus;
import com.bage.im.interfaces.IGetIpAndPort;
import com.bage.im.message.MessageHandler;
import com.bage.im.message.BageConnection;
import com.bage.im.utils.BageLoggerUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/21/21 10:31 AM
 * connect manager
 */
public class ConnectionManager extends BaseManager {
    private final String TAG = "ConnectionManager";
    private ConnectionManager() {

    }

    private static class ConnectionManagerBinder {
        static final ConnectionManager connectManager = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return ConnectionManagerBinder.connectManager;
    }


    private IGetIpAndPort iGetIpAndPort;
    private ConcurrentHashMap<String, IConnectionStatus> connectionListenerMap;

    // 连接
    public void connection() {
        if (TextUtils.isEmpty(BageIMApplication.getInstance().getToken()) || TextUtils.isEmpty(BageIMApplication.getInstance().getUid())) {
            BageLoggerUtils.getInstance().e(TAG,"connection Uninitialized UID and token");
            return;
        }
        BageIMApplication.getInstance().isCanConnect = true;
        if (BageConnection.getInstance().connectionIsNull()) {
            BageConnection.getInstance().reconnection();
        }
    }


    public void disconnect(boolean isLogout) {
        if (TextUtils.isEmpty(BageIMApplication.getInstance().getToken())) return;
        if (isLogout) {
            logoutChat();
        } else {
            stopConnect();
        }
    }

    /**
     * 断开连接
     */
    private void stopConnect() {
        BageIMApplication.getInstance().isCanConnect = false;
        BageConnection.getInstance().stopAll();
    }

    /**
     * 退出登录
     */
    private void logoutChat() {
        BageLoggerUtils.getInstance().e(TAG,"exit");
        BageIMApplication.getInstance().isCanConnect = false;
        BageIMApplication.getInstance().setToken("");
        BageConnection.getInstance().stopAll();
        BageIM.getInstance().getChannelManager().clearARMCache();
        BageIM.getInstance().getReminderManager().clearAllCache();
        // 捕获当前 DBHelper 引用，延迟关闭只关这个实例：
        // 避免 500ms sleep 期间用户快速重登 → getDbHelper() 返回新实例 → 被误关
        final BageDBHelper targetDbHelper = BageIMApplication.getInstance().getDbHelper();
        new Thread(() -> {
            try {
                MessageHandler.getInstance().saveReceiveMsg();
                MessageHandler.getInstance().updateLastSendingMsgFail();
            } catch (Throwable t) {
                // Bugly#33246 防御：登出落盘若撞到 DB 关闭竞态，不让进程崩溃
                BageLoggerUtils.getInstance().e(TAG, "logout save aborted: " + t.getMessage());
            } finally {
                // 延迟关闭DB，等待其他in-flight DB操作完成
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                // 500ms 窗口内若用户已快速重登（token 非空），则跳过关闭——
                // 因为 BageDBHelper 同 uid 复用同一实例，关了就把新会话的活实例也关了
                if (TextUtils.isEmpty(BageIMApplication.getInstance().getToken())) {
                    BageIMApplication.getInstance().closeDbHelper(targetDbHelper);
                }
            }
        }, "logout-db").start();
    }

    public interface IRequestIP {
        void onResult(String requestId, String ip, int port);
    }

    public void getIpAndPort(String requestId, IRequestIP iRequestIP) {
        if (iGetIpAndPort != null) {
            runOnMainThread(() -> iGetIpAndPort.getIP((ip, port) -> iRequestIP.onResult(requestId, ip, port)));
        } else {
            BageLoggerUtils.getInstance().e(TAG,"未注册获取连接地址的事件");
        }
    }

    // 监听获取IP和port
    public void addOnGetIpAndPortListener(IGetIpAndPort iGetIpAndPort) {
        this.iGetIpAndPort = iGetIpAndPort;
    }

    public void setConnectionStatus(int status, String reason) {
        if (connectionListenerMap != null && !connectionListenerMap.isEmpty()) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IConnectionStatus> entry : connectionListenerMap.entrySet()) {
                    entry.getValue().onStatus(status, reason);
                }
            });
        }
    }

    // 监听连接状态
    public void addOnConnectionStatusListener(String key, IConnectionStatus iConnectionStatus) {
        if (iConnectionStatus == null || TextUtils.isEmpty(key)) return;
        if (connectionListenerMap == null) connectionListenerMap = new ConcurrentHashMap<>();
        connectionListenerMap.put(key, iConnectionStatus);
    }

    // 移除监听
    public void removeOnConnectionStatusListener(String key) {
        if (!TextUtils.isEmpty(key) && connectionListenerMap != null) {
            connectionListenerMap.remove(key);
        }
    }
}
