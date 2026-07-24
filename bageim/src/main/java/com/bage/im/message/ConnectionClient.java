package com.bage.im.message;

import android.text.TextUtils;

import com.bage.im.BageIMApplication;
import com.bage.im.utils.BageLoggerUtils;

import org.xsocket.connection.IConnectExceptionHandler;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IConnectionTimeoutHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.IIdleTimeoutHandler;
import org.xsocket.connection.INonBlockingConnection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2020-12-18 10:28
 * 连接客户端
 */
class ConnectionClient implements IDataHandler, IConnectHandler,
        IDisconnectHandler, IConnectExceptionHandler,
        IConnectionTimeoutHandler, IIdleTimeoutHandler {
    private final String TAG = "ConnectionClient";
    private boolean isConnectSuccess;
    private static final int MAX_TIMEOUT_RETRIES = 3;
    private final AtomicInteger timeoutRetryCount = new AtomicInteger(0);

    interface IConnResult {
        void onResult(INonBlockingConnection iNonBlockingConnection);
    }
    IConnResult iConnResult;
    ConnectionClient(IConnResult iConnResult) {
        this.iConnResult = iConnResult;
        isConnectSuccess = false;
    }

    @Override
    public boolean onConnectException(INonBlockingConnection iNonBlockingConnection, IOException e) {
        BageLoggerUtils.getInstance().e(TAG, "连接异常: " + e.getMessage());
        close(iNonBlockingConnection);
        // 检查网络状态，有网络才累加重连计数
        if (BageIMApplication.getInstance().isNetworkConnected()) {
            BageConnection.getInstance().forcedReconnection();
        } else {
            BageLoggerUtils.getInstance().i(TAG, "无网络导致连接异常，不累加重连计数");
        }
        return true;
    }

    @Override
    public boolean onConnect(INonBlockingConnection iNonBlockingConnection) throws BufferUnderflowException {
        isConnectSuccess = true;
        iConnResult.onResult( iNonBlockingConnection);
//        if (BageConnection.getInstance().connection == null) {
//            BageLoggerUtils.getInstance().e(TAG, "onConnect connection is null");
//        }
//        try {
//            if (BageConnection.getInstance().connection != null && iNonBlockingConnection != null) {
//                if (!BageConnection.getInstance().connection.getId().equals(iNonBlockingConnection.getId())) {
//                    close(iNonBlockingConnection);
//                    BageConnection.getInstance().forcedReconnection();
//                } else {
//                    //连接成功
//                    isConnectSuccess = true;
//                    BageLoggerUtils.getInstance().e(TAG, "connection success");
//                    BageConnection.getInstance().sendConnectMsg();
//                }
//            } else {
//                close(iNonBlockingConnection);
//                BageLoggerUtils.getInstance().e(TAG, "Connection successful but connection object is empty");
//                BageConnection.getInstance().forcedReconnection();
//            }
//        } catch (Exception ignored) {
//            BageLoggerUtils.getInstance().e(TAG, "onConnect error");
//        }
        return false;

    }

    @Override
    public boolean onConnectionTimeout(INonBlockingConnection iNonBlockingConnection) {
        // 使用 volatile 读取 connection，无需锁保护（只读操作）
        if (!isConnectSuccess) {
            int retryCount = timeoutRetryCount.incrementAndGet();
            BageLoggerUtils.getInstance().e(TAG, String.format("Connection timeout (attempt %d/%d)", retryCount, MAX_TIMEOUT_RETRIES));

            // Check if this is the current connection (connection 是 volatile，可安全读取)
            INonBlockingConnection currentConn = BageConnection.getInstance().connection;
            if (currentConn != null && currentConn.getId().equals(iNonBlockingConnection.getId())) {

                if (retryCount >= MAX_TIMEOUT_RETRIES) {
                    BageLoggerUtils.getInstance().e(TAG, "Maximum timeout retries reached, initiating reconnection");
                    timeoutRetryCount.set(0);
                    // 检查网络状态，有网络才累加重连计数
                    if (BageIMApplication.getInstance().isNetworkConnected()) {
                        BageConnection.getInstance().forcedReconnection();
                    } else {
                        BageLoggerUtils.getInstance().i(TAG, "无网络导致连接超时，不累加重连计数");
                    }
                } else {
                    // Log retry attempt
                    BageLoggerUtils.getInstance().i(TAG, "Retrying connection after timeout");

                    // Attempt to reset connection state
                    try {
                        iNonBlockingConnection.setConnectionTimeoutMillis(
                            Math.min(3000 * (retryCount + 1), 10000) // Increase timeout with each retry
                        );
                    } catch (Exception e) {
                        BageLoggerUtils.getInstance().e(TAG, "Failed to adjust connection timeout: " + e.getMessage());
                    }
                }
            } else {
                BageLoggerUtils.getInstance().w(TAG, "Timeout for old connection, ignoring");
                timeoutRetryCount.set(0);
            }
        } else {
            BageLoggerUtils.getInstance().i(TAG, "Connection timeout ignored - connection already successful");
        }
        return true;
    }

    @Override
    public boolean onData(INonBlockingConnection iNonBlockingConnection) throws BufferUnderflowException {
        if (BageConnection.getInstance().connectionIsNull() || BageConnection.getInstance().isReConnecting) {
            return true;
        }
        Object id = iNonBlockingConnection.getAttachment();
        if (id instanceof String) {
            if (id.toString().startsWith("close")) {
                return true;

            }
            if (!TextUtils.isEmpty(BageConnection.getInstance().socketSingleID) && !BageConnection.getInstance().socketSingleID.equals(id)) {
                BageLoggerUtils.getInstance().e(TAG, "非当前连接的消息");
                try {
                    iNonBlockingConnection.close();
                    if (BageConnection.getInstance().connection != null) {
                        BageConnection.getInstance().connection.close();
                    }
                } catch (IOException e) {
                    BageLoggerUtils.getInstance().e(TAG, "关闭连接异常");
                }
                if (BageIMApplication.getInstance().isCanConnect) {
                    BageConnection.getInstance().reconnection();
                }
                return true;
            }
        }
        MessageHandler.getInstance().handlerOnlineBytes(iNonBlockingConnection);
        return true;
    }

    @Override
    public boolean onDisconnect(INonBlockingConnection iNonBlockingConnection) {
        try {
            if (iNonBlockingConnection != null && !TextUtils.isEmpty(iNonBlockingConnection.getId()) && iNonBlockingConnection.getAttachment() != null) {
                String id = iNonBlockingConnection.getId();
                Object attachmentObject = iNonBlockingConnection.getAttachment();
                if (attachmentObject instanceof String) {
                    String att = (String) attachmentObject;
                    // Check if this is a planned closure
                    if (att.startsWith("closing_") || att.equals("close" + id)) {
                        BageLoggerUtils.getInstance().e("主动断开不重连");
                        return true;
                    }
                }
            }
            
            // Reset timeout counter on disconnect
            timeoutRetryCount.set(0);
            
            // Only attempt reconnection if we're allowed to connect and it's not a planned closure
            if (BageIMApplication.getInstance().isCanConnect && !BageConnection.getInstance().isClosing.get()) {
                // 检查网络状态，有网络才累加重连计数
                if (BageIMApplication.getInstance().isNetworkConnected()) {
                    BageLoggerUtils.getInstance().e("连接断开需要重连");
                    BageConnection.getInstance().forcedReconnection();
                } else {
                    BageLoggerUtils.getInstance().i(TAG, "无网络导致连接断开，不累加重连计数");
                }
            }
            close(iNonBlockingConnection);
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public boolean onIdleTimeout(INonBlockingConnection iNonBlockingConnection) {
        if (!isConnectSuccess) {
            close(iNonBlockingConnection);
            // 检查网络状态，有网络才累加重连计数
            if (BageIMApplication.getInstance().isNetworkConnected()) {
                BageConnection.getInstance().forcedReconnection();
            } else {
                BageLoggerUtils.getInstance().i(TAG, "无网络导致空闲超时，不累加重连计数");
            }
        }
        return true;
    }

    private void close(INonBlockingConnection iNonBlockingConnection) {
        try {
            if (iNonBlockingConnection != null)
                iNonBlockingConnection.close();
        } catch (IOException e) {
            BageLoggerUtils.getInstance().e(TAG, "关闭连接异常");
        }
    }
}