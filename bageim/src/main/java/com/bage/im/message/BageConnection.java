package com.bage.im.message;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.BageIMApplication;
import com.bage.im.db.MsgDbManager;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsgExtra;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgSetting;
import com.bage.im.entity.BageSyncMsgMode;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.interfaces.IReceivedMsgListener;
import com.bage.im.manager.ConnectionManager;
import com.bage.im.message.timer.HeartbeatManager;
import com.bage.im.message.timer.NetworkChecker;
import com.bage.im.message.timer.TimerManager;
import com.bage.im.message.type.BageConnectReason;
import com.bage.im.message.type.BageConnectStatus;
import com.bage.im.message.type.BageMsgType;
import com.bage.im.message.type.BageSendMsgResult;
import com.bage.im.message.type.BageSendingMsg;
import com.bage.im.msgmodel.BageImageContent;
import com.bage.im.msgmodel.BageMediaMessageContent;
import com.bage.im.msgmodel.BageVideoContent;
import com.bage.im.protocol.BageBaseMsg;
import com.bage.im.protocol.BageConnectAckMsg;
import com.bage.im.protocol.BageConnectMsg;
import com.bage.im.protocol.BageDisconnectMsg;
import com.bage.im.protocol.BagePongMsg;
import com.bage.im.protocol.BageSendAckMsg;
import com.bage.im.protocol.BageSendMsg;
import com.bage.im.utils.DateUtils;
import com.bage.im.utils.DispatchQueue;
import com.bage.im.utils.DispatchQueuePool;
import com.bage.im.utils.FileUtils;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONObject;
import org.xsocket.connection.IConnection;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 5/21/21 10:51 AM
 * IM connect
 */
public class BageConnection {
    private final String TAG = "BageConnection";

    private BageConnection() {
    }

    private static class ConnectHandleBinder {
        private static final BageConnection CONNECT = new BageConnection();
    }

    public static BageConnection getInstance() {
        return ConnectHandleBinder.CONNECT;
    }

    private final DispatchQueuePool dispatchQueuePool = new DispatchQueuePool(3);
    // 消息重发专用队列（单线程保证顺序）
    private final DispatchQueue resendQueue = new DispatchQueue("BageResendQueue");
    // 正在发送的消息
    private final ConcurrentHashMap<Integer, BageSendingMsg> sendingMsgHashMap = new ConcurrentHashMap<>();
    // 正在重连中
    public volatile boolean isReConnecting = false;
    // 连接状态
    private int connectStatus;
    private long lastMsgTime = 0;
    private String ip;
    private int port;
    public volatile INonBlockingConnection connection;
    volatile ConnectionClient connectionClient;
    private long requestIPTime;
    private long connAckTime;
    private final long requestIPTimeoutTime = 6;
    private final long connAckTimeoutTime = 10;
    public String socketSingleID;
    private String lastRequestId;
    public volatile Handler reconnectionHandler = new Handler(Looper.getMainLooper());
    Runnable reconnectionRunnable = this::reconnection;
    private int connCount = 0;
    private HeartbeatManager heartbeatManager;
    private NetworkChecker networkChecker;

    private final Handler checkRequestAddressHandler = new Handler(Looper.getMainLooper());
    private final Runnable checkRequestAddressRunnable = new Runnable() {
        @Override
        public void run() {
            long nowTime = DateUtils.getInstance().getCurrentSeconds();
            if (nowTime - requestIPTime >= requestIPTimeoutTime) {
                if (TextUtils.isEmpty(ip) || port == 0) {
                    BageLoggerUtils.getInstance().e(TAG, "获取连接地址超时");
                    isReConnecting = false;
                    reconnection();
                }
            } else {
                if (TextUtils.isEmpty(ip) || port == 0) {
                    BageLoggerUtils.getInstance().e(TAG, "请求连接地址--->" + (nowTime - requestIPTime));
                    // 继续检查
                    checkRequestAddressHandler.postDelayed(this, 1000);
                }
            }
        }
    };

    private final Handler checkConnAckHandler = new Handler(Looper.getMainLooper());
    private final Runnable checkConnAckRunnable = new Runnable() {
        @Override
        public void run() {
            long nowTime = DateUtils.getInstance().getCurrentSeconds();
            if (nowTime - connAckTime > connAckTimeoutTime && connectStatus != BageConnectStatus.success && connectStatus != BageConnectStatus.syncMsg) {
                BageLoggerUtils.getInstance().e(TAG, "连接确认超时");
                isReConnecting = false;
                closeConnect();
                reconnection();
            } else {
                if (connectStatus == BageConnectStatus.success || connectStatus == BageConnectStatus.syncMsg) {
                    BageLoggerUtils.getInstance().e(TAG, "连接确认成功");
                } else {
                    BageLoggerUtils.getInstance().e(TAG, "等待连接确认--->" + (nowTime - connAckTime));
                    // 继续检查
                    checkConnAckHandler.postDelayed(this, 1000);
                }
            }
        }
    };

    // 统一使用 ReentrantLock（非公平锁，性能更好）
    private final ReentrantLock connectionLock = new ReentrantLock();
    // 后台线程锁超时时间（主线程不应该等待锁）
    private static final long LOCK_TIMEOUT_MS = 3000;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final long CONNECTION_CLOSE_TIMEOUT = 5000;

    public final AtomicBoolean isClosing = new AtomicBoolean(false);

    private final int maxReconnectAttempts = 5;
    private final long baseReconnectDelay = 500;

    // 使用原子变量替代锁保护的状态
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean isReconnectScheduled = new AtomicBoolean(false);

    private final Object executorLock = new Object();
    private volatile ExecutorService connectionExecutor;

    private ExecutorService getOrCreateExecutor() {
        synchronized (executorLock) {
            if (connectionExecutor == null || connectionExecutor.isShutdown() || connectionExecutor.isTerminated()) {
                connectionExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread thread = new Thread(r, "BageConnection-Worker");
                    thread.setDaemon(true);
                    return thread;
                });
                BageLoggerUtils.getInstance().i(TAG, "创建新的连接线程池");
            }
            return connectionExecutor;
        }
    }

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    private void shutdownExecutor() {
        if (!isShuttingDown.compareAndSet(false, true)) {
            BageLoggerUtils.getInstance().w(TAG, "Executor is already shutting down");
            return;
        }

        ExecutorService executorToShutdown;
        synchronized (executorLock) {
            executorToShutdown = connectionExecutor;
            connectionExecutor = null;
        }

        if (executorToShutdown != null && !executorToShutdown.isShutdown()) {
            dispatchQueuePool.execute(() -> {
                try {
                    BageLoggerUtils.getInstance().i(TAG, "Starting executor shutdown");
                    executorToShutdown.shutdown();

                    if (!executorToShutdown.awaitTermination(3, TimeUnit.SECONDS)) {
                        BageLoggerUtils.getInstance().w(TAG, "Executor did not terminate in time, forcing shutdown");
                        executorToShutdown.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    BageLoggerUtils.getInstance().e(TAG, "Executor shutdown interrupted: " + e.getMessage());
                    executorToShutdown.shutdownNow();
                    Thread.currentThread().interrupt();
                } finally {
                    isShuttingDown.set(false);
                    BageLoggerUtils.getInstance().i(TAG, "Executor shutdown completed");
                }
            });
        }
    }

    private void startAll() {
        heartbeatManager = new HeartbeatManager();
        networkChecker = new NetworkChecker();
        heartbeatManager.startHeartbeat();
        networkChecker.startNetworkCheck();
    }

    /**
     * 重置重连计数器
     * 在网络恢复后调用，给予完整的重连机会
     */
    public void resetConnCount() {
        connCount = 0;
        isReconnectScheduled.set(false);
        isReConnecting = false;  // 重置连接中标志，允许立即重新获取地址
        isConnecting.set(false); // 重置正在连接标志
        ip = "";  // 清空旧地址，强制重新获取
        port = 0;
        BageLoggerUtils.getInstance().i(TAG, "重连计数已重置");
    }

    public void forcedReconnection() {
        // 无网络时不累加重连计数，直接返回等待网络恢复
        if (!BageIMApplication.getInstance().isNetworkConnected()) {
            BageLoggerUtils.getInstance().i(TAG, "无网络，跳过重连计数累加");
            BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.noNetwork, BageConnectReason.NoNetwork);
            return;
        }

        // 使用 CAS 操作避免重复调度，无需锁
        if (!isReconnectScheduled.compareAndSet(false, true)) {
            BageLoggerUtils.getInstance().w(TAG, "已经在重连计划中，忽略重复请求");
            return;
        }

        // 检查线程池状态
        ExecutorService executor = getOrCreateExecutor();
        if (executor.isShutdown() || executor.isTerminated()) {
            BageLoggerUtils.getInstance().e(TAG, "线程池已关闭，无法执行重连");
            isReconnectScheduled.set(false);
            return;
        }

        connCount++;
        if (connCount > maxReconnectAttempts) {
            BageLoggerUtils.getInstance().e(TAG, "达到最大重连次数，停止重连");
            isReconnectScheduled.set(false);
            stopAll();
            return;
        }

        isReConnecting = false;
        requestIPTime = 0;

        // 使用指数退避延迟，最大延迟改为8秒
        long delay = Math.min(baseReconnectDelay * (1L << (connCount - 1)), 8000);
        BageLoggerUtils.getInstance().e(TAG, "重连延迟: " + delay + "ms");

        try {
            executor.execute(() -> {
                try {
                    Thread.sleep(delay);
                    if (BageIMApplication.getInstance().isCanConnect && !executor.isShutdown()) {
                        reconnection();
                    }
                } catch (InterruptedException e) {
                    BageLoggerUtils.getInstance().e(TAG, "重连等待被中断");
                    Thread.currentThread().interrupt();
                } finally {
                    isReconnectScheduled.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            BageLoggerUtils.getInstance().e(TAG, "重连任务被拒绝执行: " + e.getMessage());
            isReconnectScheduled.set(false);
        }
    }

    public void reconnection() {
        // 如果正在关闭连接，延迟到后台线程重试（避免主线程阻塞）
        if (isClosing.get()) {
            BageLoggerUtils.getInstance().e(TAG, "等待连接关闭完成后再重连");
            scheduleReconnectionOnBackground(500);
            return;
        }

        if (!BageIMApplication.getInstance().isCanConnect) {
            BageLoggerUtils.getInstance().e(TAG, "断开");
            stopAll();
            return;
        }

        ip = "";
        port = 0;
        if (isReConnecting) {
            long nowTime = DateUtils.getInstance().getCurrentSeconds();
            if (nowTime - requestIPTime > requestIPTimeoutTime) {
                BageLoggerUtils.getInstance().e("重置了正在连接");
                isReConnecting = false;
            }
            return;
        }

        connectStatus = BageConnectStatus.fail;
        reconnectionHandler.removeCallbacks(reconnectionRunnable);
        boolean isHaveNetwork = BageIMApplication.getInstance().isNetworkConnected();
        if (isHaveNetwork) {
            closeConnect();
            isReConnecting = true;
            requestIPTime = DateUtils.getInstance().getCurrentSeconds();
            getConnAddress();
        } else {
            // 无网络时只更新状态，不累加重连计数
            // 等待网络恢复后由 NetworkChecker 触发重连
            BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.noNetwork, BageConnectReason.NoNetwork);
            BageLoggerUtils.getInstance().i(TAG, "无网络，等待网络恢复后自动重连");
        }
    }

    /**
     * 在后台线程延迟执行重连，避免主线程阻塞
     */
    private void scheduleReconnectionOnBackground(long delayMs) {
        ExecutorService executor = getOrCreateExecutor();
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.execute(() -> {
                    try {
                        Thread.sleep(delayMs);
                        reconnection();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                BageLoggerUtils.getInstance().e(TAG, "延迟重连任务被拒绝: " + e.getMessage());
            }
        }
    }

    private void getConnAddress() {
        ExecutorService executor = getOrCreateExecutor();
        if (executor.isShutdown()) {
            BageLoggerUtils.getInstance().e(TAG, "线程池已关闭，重新初始化后重试");
            executor = getOrCreateExecutor();
        }

        try {
            executor.execute(() -> {
                try {
                    if (!BageIMApplication.getInstance().isCanConnect) {
                        BageLoggerUtils.getInstance().e(TAG, "不允许连接");
                        return;
                    }

                    final long startTime = System.currentTimeMillis();
                    final long ADDRESS_TIMEOUT = 10000; // 10秒超时

                    BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.connecting, BageConnectReason.Connecting);
                    String currentRequestId = UUID.randomUUID().toString().replace("-", "");
                    lastRequestId = currentRequestId;

                    CountDownLatch addressLatch = new CountDownLatch(1);
                    AtomicReference<String> receivedIp = new AtomicReference<>();
                    AtomicInteger receivedPort = new AtomicInteger();

                    ConnectionManager.getInstance().getIpAndPort(currentRequestId, (requestId, ip, port) -> {
                        if (!currentRequestId.equals(requestId)) {
                            BageLoggerUtils.getInstance().w(TAG, "收到过期的地址响应");
                            addressLatch.countDown();
                            return;
                        }

                        receivedIp.set(ip);
                        receivedPort.set(port);
                        addressLatch.countDown();
                    });

                    // 等待地址响应或超时
                    boolean gotAddress = addressLatch.await(ADDRESS_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (!gotAddress) {
                        BageLoggerUtils.getInstance().e(TAG, "获取连接地址超时");
                        isReConnecting = false;
                        // 地址获取超时使用延迟重试，不累加重连计数
                        scheduleReconnectionOnBackground(2000);
                        return;
                    }

                    String ip = receivedIp.get();
                    int port = receivedPort.get();

                    if (TextUtils.isEmpty(ip) || port == 0) {
                        BageLoggerUtils.getInstance().e(TAG, "无效的连接地址: " + ip + ":" + port);
                        isReConnecting = false;
                        // 无效地址使用延迟重试，不累加重连计数
                        scheduleReconnectionOnBackground(2000);
                        return;
                    }

                    BageConnection.this.ip = ip;
                    BageConnection.this.port = port;
                    if (connectionIsNull()) {
                        connSocket();
                    }
                } catch (Exception e) {
                    BageLoggerUtils.getInstance().e(TAG, "获取地址异常: " + e.getMessage());
                    isReConnecting = false;
                    // 获取地址异常使用延迟重试，不累加重连计数
                    scheduleReconnectionOnBackground(2000);
                }
            });
        } catch (RejectedExecutionException e) {
            BageLoggerUtils.getInstance().e(TAG, "任务提交被拒绝，重试: " + e.getMessage());
            isReConnecting = false;
            // 在后台线程延迟重试，避免主线程阻塞
            scheduleReconnectionOnBackground(2000);
        }
    }

    private void connSocket() {
        // 检查地址有效性，防止使用空地址连接
        if (TextUtils.isEmpty(ip) || port == 0) {
            BageLoggerUtils.getInstance().e(TAG, "连接地址无效，跳过连接: " + ip + ":" + port);
            isReConnecting = false;
            // 延迟重新获取地址
            scheduleReconnectionOnBackground(2000);
            return;
        }

        // 检查线程池状态
        ExecutorService executor = getOrCreateExecutor();
        if (executor.isShutdown() || executor.isTerminated()) {
            BageLoggerUtils.getInstance().e(TAG, "线程池已关闭，无法执行连接");
            return;
        }

        // 使用CAS操作检查连接状态
        if (!setConnectingState(true)) {
            BageLoggerUtils.getInstance().e(TAG, "已经在连接中，忽略重复连接请求");
            return;
        }

        // 保存当前地址到局部变量，避免在连接过程中被修改
        final String connIp = ip;
        final int connPort = port;

        try {
            executor.execute(() -> {
                try {
                    // 关闭现有连接
                    closeConnect();

                    // 生成新的连接ID
                    String newSocketId = UUID.randomUUID().toString().replace("-", "");

                    CountDownLatch connectLatch = new CountDownLatch(1);
                    CountDownLatch connectionPublishedLatch = new CountDownLatch(1);
                    AtomicBoolean connectSuccess = new AtomicBoolean(false);

                    ConnectionClient newClient = new ConnectionClient(iNonBlockingConnection -> {
                        try {
                            if (!connectionPublishedLatch.await(2, TimeUnit.SECONDS)) {
                                BageLoggerUtils.getInstance().e(TAG, "TLS 连接对象发布超时");
                                connectLatch.countDown();
                                return;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            connectLatch.countDown();
                            return;
                        }
                        // 使用 volatile 读取，无需锁（只是读取检查）
                        INonBlockingConnection currentConn = connection;

                        if (iNonBlockingConnection == null || currentConn == null ||
                                !currentConn.getId().equals(iNonBlockingConnection.getId())) {
                            BageLoggerUtils.getInstance().e(TAG, "无效的连接回调");
                            connectLatch.countDown();
                            return;
                        }

                        try {
                            if (!iNonBlockingConnection.isSecure()) {
                                BageLoggerUtils.getInstance().e(TAG, "TLS 握手未完成，拒绝发送 IM CONNECT 包");
                                iNonBlockingConnection.close();
                                return;
                            }
                            iNonBlockingConnection.setIdleTimeoutMillis(1000 * 3);
                            iNonBlockingConnection.setConnectionTimeoutMillis(1000 * 3);
                            iNonBlockingConnection.setFlushmode(IConnection.FlushMode.ASYNC);
                            iNonBlockingConnection.setAutoflush(true);

                            connectSuccess.set(true);
                            isReConnecting = false;
                            connCount = 0;
                        } catch (Exception e) {
                            BageLoggerUtils.getInstance().e(TAG, "设置连接参数失败: " + e.getMessage());
                        } finally {
                            connectLatch.countDown();
                        }
                    });

                    // Modified in Bage: establish TLS before sending any IM protocol data.
                    // The SSLContext uses Android's system trust store and configures both SNI and
                    // HTTPS endpoint identification for the server name returned by tcp_addr.
                    INonBlockingConnection newConnection = new NonBlockingConnection(
                            InetAddress.getByName(connIp),
                            connPort,
                            newClient,
                            false,
                            5000,
                            BageClientSSLContext.create(connIp, connPort),
                            true
                    );
                    newConnection.setAttachment(newSocketId);

                    // 原子性地更新连接相关的字段
                    connectionLock.lock();
                    try {
                        connectionClient = newClient;
                        connection = newConnection;
                        socketSingleID = newSocketId;
                    } finally {
                        connectionLock.unlock();
                    }
                    connectionPublishedLatch.countDown();

                    // 等待连接完成或超时
                    boolean connected = connectLatch.await(5000, TimeUnit.MILLISECONDS);

                    if (!connected || !connectSuccess.get()) {
                        BageLoggerUtils.getInstance().e(TAG, "连接建立超时或失败，地址：" + connIp + ":" + connPort);
                        closeConnect();
                        if (!executor.isShutdown()) {
                            // 检查网络状态，有网络才累加重连计数
                            if (BageIMApplication.getInstance().isNetworkConnected()) {
                                forcedReconnection();
                            } else {
                                BageLoggerUtils.getInstance().i(TAG, "无网络导致连接失败，不累加重连计数");
                                scheduleReconnectionOnBackground(2000);
                            }
                        }
                    } else {
                        BageLoggerUtils.getInstance().i(TAG, "连接成功，地址：" + connIp + ":" + connPort);
                        sendConnectMsg();
                    }
                } catch (GeneralSecurityException e) {
                    BageLoggerUtils.getInstance().e(TAG, "TLS 上下文初始化失败: " + e.getMessage()
                            + " 连接地址：" + connIp + ":" + connPort);
                    closeConnect();
                    if (!executor.isShutdown()) {
                        forcedReconnection();
                    }
                } catch (Exception e) {
                    BageLoggerUtils.getInstance().e(TAG, "连接异常: " + e.getMessage() + " 连接地址：" + connIp + ":" + connPort);
                    if (!executor.isShutdown()) {
                        // 检查网络状态，有网络才累加重连计数
                        if (BageIMApplication.getInstance().isNetworkConnected()) {
                            forcedReconnection();
                        } else {
                            BageLoggerUtils.getInstance().i(TAG, "无网络导致连接异常，不累加重连计数");
                            scheduleReconnectionOnBackground(2000);
                        }
                    }
                } finally {
                    setConnectingState(false);
                }
            });
        } catch (RejectedExecutionException e) {
            BageLoggerUtils.getInstance().e(TAG, "连接任务被拒绝执行: " + e.getMessage());
            setConnectingState(false);
        }
    }

    // 使用 CAS 操作设置连接状态（无锁）
    private boolean setConnectingState(boolean connecting) {
        if (connecting) {
            // 尝试从 false 设置为 true
            return isConnecting.compareAndSet(false, true);
        } else {
            // 设置为 false
            isConnecting.set(false);
            return true;
        }
    }

    //发送连接消息
    void sendConnectMsg() {
        startConnAckTimer();
        sendMessage(new BageConnectMsg());
    }

    void receivedData(byte[] data) {
        MessageHandler.getInstance().cutBytes(data,
                new IReceivedMsgListener() {

                    public void sendAckMsg(
                            BageSendAckMsg talkSendStatus) {
                        // 删除队列中正在发送的消息对象
                        BageSendingMsg object = sendingMsgHashMap.get(talkSendStatus.clientSeq);
                        if (object != null) {
                            object.isCanResend = false;
                            sendingMsgHashMap.put(talkSendStatus.clientSeq, object);
                        }
                    }


                    @Override
                    public void reconnect() {
                        BageIMApplication.getInstance().isCanConnect = true;
                        reconnection();
                    }

                    @Override
                    public void loginStatusMsg(BageConnectAckMsg connectAckMsg) {
                        handleLoginStatus(connectAckMsg);
                    }

                    @Override
                    public void pongMsg(BagePongMsg msgHeartbeat) {
                        // 心跳消息
                        lastMsgTime = DateUtils.getInstance().getCurrentSeconds();
                    }

                    @Override
                    public void kickMsg(BageDisconnectMsg disconnectMsg) {
                        BageIM.getInstance().getConnectionManager().disconnect(true);
                        BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.kicked, BageConnectReason.ReasonConnectKick);
                    }

                });
    }


    //重发未发送成功的消息（使用 DispatchQueue 延迟发送，避免 socket 通道阻塞）
    public void resendMsg() {
        removeSendingMsg();
        
        // 先取消之前未执行的重发任务
        resendQueue.cleanupQueue();
        
        // 获取所有可重发的消息并按 clientSeq 排序
        List<Integer> sortedKeys = new ArrayList<>();
        for (Map.Entry<Integer, BageSendingMsg> entry : sendingMsgHashMap.entrySet()) {
            if (entry.getValue().isCanResend) {
                sortedKeys.add(entry.getKey());
            }
        }
        Collections.sort(sortedKeys);
        
        // 按顺序延迟发送，每条消息间隔 100ms
        for (int i = 0; i < sortedKeys.size(); i++) {
            final Integer clientSeq = sortedKeys.get(i);
            long delay = i * 150L;
            
            resendQueue.postRunnable(() -> {
                BageSendingMsg sendingMsg = sendingMsgHashMap.get(clientSeq);
                if (sendingMsg != null && sendingMsg.isCanResend) {
                    sendMessage(sendingMsg.bageSendMsg);
                }
            }, delay);
        }
        
        if (!sortedKeys.isEmpty()) {
            BageLoggerUtils.getInstance().i(TAG, "计划重发 " + sortedKeys.size() + " 条消息");
        }
    }

    // 将要发送的消息添加到队列（使用 ConcurrentHashMap，无需 synchronized）
    private void addSendingMsg(BageSendMsg sendingMsg) {
        removeSendingMsg();
        sendingMsgHashMap.put(sendingMsg.clientSeq, new BageSendingMsg(1, sendingMsg, true));
    }

    //处理登录消息状态
    private void handleLoginStatus(BageConnectAckMsg connectAckMsg) {
        short status = connectAckMsg.reasonCode;
        boolean locked = false;
        BageLoggerUtils.getInstance().e(TAG, "连接状态：" + status + "，连接节点：" + connectAckMsg.nodeId);
        try {
            locked = tryLockWithTimeout();
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，handleLoginStatus失败");
                return;
            }

            BageLoggerUtils.getInstance().e(TAG, "Connection state : " + connectStatus + " -> " + status);
            String reason = BageConnectReason.ConnectSuccess;
            if (status == BageConnectStatus.kicked) {
                reason = BageConnectReason.ReasonAuthFail;
            }

            if (!isValidStateTransition(connectStatus, status)) {
                BageLoggerUtils.getInstance().e(TAG, "Invalid state transition attempted: " + connectStatus + " -> " + status);
                return;
            }

            connectStatus = status;
            BageIM.getInstance().getConnectionManager().setConnectionStatus(status, reason);

            if (status == BageConnectStatus.success) {
                connCount = 0;
                isReConnecting = false;
                connectStatus = BageConnectStatus.syncMsg;
                BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.syncMsg, BageConnectReason.SyncMsg);
                startAll();

                if (BageIMApplication.getInstance().getSyncMsgMode() == BageSyncMsgMode.WRITE) {
                    BageIM.getInstance().getMsgManager().setSyncOfflineMsg((isEnd, list) -> {
                        if (isEnd) {
                            boolean innerLocked = false;
                            try {
                                innerLocked = tryLockWithTimeout();
                                if (!innerLocked) {
                                    BageLoggerUtils.getInstance().e(TAG, "获取锁超时，setSyncOfflineMsg回调处理失败");
                                    return;
                                }
                                if (connection != null && !isClosing.get()) {
                                    connectStatus = BageConnectStatus.success;
                                    MessageHandler.getInstance().saveReceiveMsg();
                                    BageIMApplication.getInstance().isCanConnect = true;
                                    MessageHandler.getInstance().sendAck();
                                    resendMsg();
                                    BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.success, BageConnectReason.ConnectSuccess);
                                }
                            } catch (Throwable t) {
                                // Bugly#33246 防御：saveReceiveMsg 若撞到 DB 关闭竞态，兜底吞异常
                                BageLoggerUtils.getInstance().e(TAG, "setSyncOfflineMsg callback aborted: " + t.getMessage());
                            } finally {
                                if (innerLocked) {
                                    connectionLock.unlock();
                                }
                            }
                        }
                    });
                } else {
                    BageIM.getInstance().getConversationManager().setSyncConversationListener(syncChat -> {
                        boolean innerLocked = false;
                        try {
                            innerLocked = tryLockWithTimeout();
                            if (!innerLocked) {
                                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，setSyncConversationListener回调处理失败");
                                return;
                            }
                            if (connection != null && !isClosing.get()) {
                                connectStatus = BageConnectStatus.success;
                                BageIMApplication.getInstance().isCanConnect = true;
                                MessageHandler.getInstance().sendAck();
                                resendMsg();
                                BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.success, BageConnectReason.ConnectSuccess);
                            }
                        } catch (Throwable t) {
                            // Bugly#33246 防御：回调内 DB 访问若撞到关闭竞态，兜底吞异常
                            BageLoggerUtils.getInstance().e(TAG, "setSyncConversationListener callback aborted: " + t.getMessage());
                        } finally {
                            if (innerLocked) {
                                connectionLock.unlock();
                            }
                        }
                    });
                }
            } else if (status == BageConnectStatus.kicked) {
                BageLoggerUtils.getInstance().e(TAG, "Received kick message");
                try {
                    MessageHandler.getInstance().updateLastSendingMsgFail();
                } catch (Throwable t) {
                    // Bugly#33246 防御：kicked 场景 DB 可能已被登出路径关闭
                    BageLoggerUtils.getInstance().e(TAG, "updateLastSendingMsgFail aborted: " + t.getMessage());
                }
                BageIMApplication.getInstance().isCanConnect = false;
                stopAll();
            } else {
                if (BageIMApplication.getInstance().isCanConnect) {
                    reconnection();
                }
                BageLoggerUtils.getInstance().e(TAG, "Login status: " + status);
                stopAll();
            }
        } finally {
            if (locked) {
                connectionLock.unlock();
            }
        }
    }

    private boolean isValidStateTransition(int currentState, int newState) {
        // Define valid state transitions
        return switch (currentState) {
            case BageConnectStatus.fail ->
                // From fail state, can move to connecting or success
                    newState == BageConnectStatus.connecting ||
                            newState == BageConnectStatus.success;
            case BageConnectStatus.connecting ->
                // From connecting, can move to success, fail, or no network
                    newState == BageConnectStatus.success ||
                            newState == BageConnectStatus.fail ||
                            newState == BageConnectStatus.noNetwork;
            case BageConnectStatus.success ->
                // From success, can move to syncMsg, kicked, or fail
                    newState == BageConnectStatus.syncMsg ||
                            newState == BageConnectStatus.kicked ||
                            newState == BageConnectStatus.fail;
            case BageConnectStatus.syncMsg ->
                // From syncMsg, can move to success or fail
                    newState == BageConnectStatus.success ||
                            newState == BageConnectStatus.fail;
            case BageConnectStatus.noNetwork ->
                // From noNetwork, can move to connecting or fail
                    newState == BageConnectStatus.connecting ||
                            newState == BageConnectStatus.fail;
            default ->
                // For any other state, allow transition to fail state
                    newState == BageConnectStatus.fail;
        };
    }

    public void sendMessage(BageBaseMsg mBaseMsg) {
        if (mBaseMsg == null) {
            BageLoggerUtils.getInstance().w(TAG, "sendMessage called with null mBaseMsg.");
            return;
        }
        if (mBaseMsg.packetType == BageMsgType.SEND) {
            BageSendMsg sendMsg = (BageSendMsg) mBaseMsg;
            if (TextUtils.isEmpty(sendMsg.channelId) || TextUtils.isEmpty(sendMsg.clientMsgNo)) {
                BageLoggerUtils.getInstance().e(TAG, "sendMessage: SEND msg missing channelId or clientMsgNo, skip sending");
                sendingMsgHashMap.remove(sendMsg.clientSeq);
                return;
            }
        }

        // 快速读取状态，减少锁持有时间
        int currentStatus;
        INonBlockingConnection currentConnection;

        boolean locked = false;
        try {
            locked = tryLockWithTimeout();
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，sendMessage失败");
                return;
            }
            // 只在锁内读取状态
            currentStatus = connectStatus;
            currentConnection = this.connection;
        } finally {
            if (locked) {
                connectionLock.unlock();
            }
        }

        // 在锁外进行状态检查和发送操作
        if (mBaseMsg.packetType != BageMsgType.CONNECT) {
            if (currentStatus == BageConnectStatus.syncMsg) {
                BageLoggerUtils.getInstance().i(TAG, " sendMessage: In syncMsg status, message not sent: " + mBaseMsg.packetType);
                return;
            }
            if (currentStatus != BageConnectStatus.success) {
                BageLoggerUtils.getInstance().w(TAG, " sendMessage: Not in success status (is " + currentStatus + "), attempting reconnection for: " + mBaseMsg.packetType);
                reconnection();
                return;
            }
        }

        if (currentConnection == null || !currentConnection.isOpen()) {
            BageLoggerUtils.getInstance().w(TAG, " sendMessage: Connection is null or not open, attempting reconnection for: " + mBaseMsg.packetType);
            reconnection();
            return;
        }

        // 发送操作在锁外执行，避免长时间持有锁
        int status = MessageHandler.getInstance().sendMessage(currentConnection, mBaseMsg);
        if (status == 0) {
            BageLoggerUtils.getInstance().e(TAG, "发消息失败 (status 0 from MessageHandler), attempting reconnection for: " + mBaseMsg.packetType);
            reconnection();
        }
    }

    private void removeSendingMsg() {
        if (!sendingMsgHashMap.isEmpty()) {
            Iterator<Map.Entry<Integer, BageSendingMsg>> it = sendingMsgHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, BageSendingMsg> entry = it.next();
                if (!entry.getValue().isCanResend) {
                    it.remove();
                }
            }
        }
    }

    // 检测正在发送的消息（使用 ConcurrentHashMap 的安全迭代）
    public void checkSendingMsg() {
        removeSendingMsg();
        if (!sendingMsgHashMap.isEmpty()) {
            Iterator<Map.Entry<Integer, BageSendingMsg>> it = sendingMsgHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, BageSendingMsg> item = it.next();
                BageSendingMsg bageSendingMsg = sendingMsgHashMap.get(item.getKey());
                if (bageSendingMsg != null) {
                    if (bageSendingMsg.sendCount == 5 && bageSendingMsg.isCanResend) {
                        //标示消息发送失败
                        MsgDbManager.getInstance().updateMsgStatus(item.getKey(), BageSendMsgResult.send_fail);
                        it.remove();
                        bageSendingMsg.isCanResend = false;
                    } else {
                        long nowTime = DateUtils.getInstance().getCurrentSeconds();
                        if (nowTime - bageSendingMsg.sendTime > 10) {
                            bageSendingMsg.sendTime = DateUtils.getInstance().getCurrentSeconds();
                            sendingMsgHashMap.put(item.getKey(), bageSendingMsg);
                            bageSendingMsg.sendCount++;
                            sendMessage(Objects.requireNonNull(sendingMsgHashMap.get(item.getKey())).bageSendMsg);
                        }
                    }
                }
            }
        }
    }


    public void sendMessage(BageMsg msg) {
        if (msg == null) {
            return;
        }
        if (TextUtils.isEmpty(msg.channelID) || TextUtils.isEmpty(msg.clientMsgNO)) {
            BageLoggerUtils.getInstance().e(TAG, "sendMessage: channelID or clientMsgNO is empty, skip sending");
            MsgDbManager.getInstance().updateMsgStatus(msg.clientSeq, BageSendMsgResult.send_fail);
            return;
        }
        if (TextUtils.isEmpty(msg.fromUID)) {
            msg.fromUID = BageIMApplication.getInstance().getUid();
        }
        if (msg.expireTime > 0) {
            msg.expireTimestamp = DateUtils.getInstance().getCurrentSeconds() + msg.expireTime;
        }
        boolean hasAttached = false;
        //如果是图片消息
        if (msg.baseContentMsgModel instanceof BageImageContent imageContent) {
            if (!TextUtils.isEmpty(imageContent.localPath)) {
//                try {
//                    File file = new File(imageContent.localPath);
//                    if (file.exists() && file.length() > 0) {
//                        hasAttached = true;
//                        Bitmap bitmap = BitmapFactory.decodeFile(imageContent.localPath);
//                        if (bitmap != null) {
//                            imageContent.width = bitmap.getWidth();
//                            imageContent.height = bitmap.getHeight();
//                            msg.baseContentMsgModel = imageContent;
//                        }
//                    }
//                } catch (Exception ignored) {
//                }

                try {
                    File file = new File(imageContent.localPath);
                    if (file.exists() && file.length() > 0) {
                        hasAttached = true;
                        // 使用 Options 只解码尺寸信息
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true; // 只获取图片信息,不加载到内存
                        BitmapFactory.decodeFile(imageContent.localPath, options);

                        imageContent.width = options.outWidth;
                        imageContent.height = options.outHeight;
                        msg.baseContentMsgModel = imageContent;
                    }
                } catch (Exception e) {
                    BageLoggerUtils.getInstance().e("BageConnection", "Get image size failed: " + e.getMessage());
                }
            }
        }
        //视频消息
        if (msg.baseContentMsgModel instanceof BageVideoContent videoContent) {
            if (!TextUtils.isEmpty(videoContent.localPath)) {
                try {
                    File file = new File(videoContent.coverLocalPath);
                    if (file.exists() && file.length() > 0) {
                        hasAttached = true;
//                        Bitmap bitmap = BitmapFactory.decodeFile(videoContent.coverLocalPath);
//                        if (bitmap != null) {
//                            videoContent.width = bitmap.getWidth();
//                            videoContent.height = bitmap.getHeight();
//                            msg.baseContentMsgModel = videoContent;
//                        }

                        // 使用 Options 只解码尺寸信息
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true; // 只获取图片信息,不加载到内存
                        BitmapFactory.decodeFile(videoContent.coverLocalPath, options);

                        videoContent.width = options.outWidth;
                        videoContent.height = options.outHeight;
                        msg.baseContentMsgModel = videoContent;
                    }
                } catch (Exception ignored) {

                }
            }

        }
        saveSendMsg(msg);
        BageSendMsg sendMsg = BageProto.getInstance().getSendBaseMsg(msg);
        if (BageMediaMessageContent.class.isAssignableFrom(msg.baseContentMsgModel.getClass())) {
            //如果是多媒体消息类型说明存在附件
            String url = ((BageMediaMessageContent) msg.baseContentMsgModel).url;
            if (TextUtils.isEmpty(url)) {
                String localPath = ((BageMediaMessageContent) msg.baseContentMsgModel).localPath;
                if (!TextUtils.isEmpty(localPath)) {
                    hasAttached = true;
                    ((BageMediaMessageContent) msg.baseContentMsgModel).localPath = FileUtils.getInstance().saveFile(localPath, msg.channelID, msg.channelType, msg.clientSeq + "");
                }
            }
            if (msg.baseContentMsgModel instanceof BageVideoContent) {
                String coverLocalPath = ((BageVideoContent) msg.baseContentMsgModel).coverLocalPath;
                if (!TextUtils.isEmpty(coverLocalPath)) {
                    ((BageVideoContent) msg.baseContentMsgModel).coverLocalPath = FileUtils.getInstance().saveFile(coverLocalPath, msg.channelID, msg.channelType, msg.clientSeq + "_1");
                    hasAttached = true;
                }
            }
            if (hasAttached) {
                JSONObject jsonObject = BageProto.getInstance().getSendPayload(msg);
                if (jsonObject != null) {
                    msg.content = jsonObject.toString();
                } else {
                    msg.content = msg.baseContentMsgModel.encodeMsg().toString();
                }
                BageIM.getInstance().getMsgManager().updateContentAndRefresh(msg.clientMsgNO, msg.content, false);
            }
        }
        //获取发送者信息
        BageChannel from = BageIM.getInstance().getChannelManager().getChannel(BageIMApplication.getInstance().getUid(), BageChannelType.PERSONAL);
        if (from == null) {
            BageIM.getInstance().getChannelManager().getChannel(BageIMApplication.getInstance().getUid(), BageChannelType.PERSONAL, channel -> BageIM.getInstance().getChannelManager().saveOrUpdateChannel(channel));
        } else {
            msg.setFrom(from);
        }
        //将消息push回UI层
        BageIM.getInstance().getMsgManager().setSendMsgCallback(msg);
        if (hasAttached) {
            //存在附件处理
            BageIM.getInstance().getMsgManager().setUploadAttachment(msg, (isSuccess, messageContent) -> {
                if (isSuccess) {
                    msg.baseContentMsgModel = messageContent;
                    JSONObject jsonObject = BageProto.getInstance().getSendPayload(msg);
                    if (jsonObject != null) {
                        msg.content = jsonObject.toString();
                    } else {
                        msg.content = msg.baseContentMsgModel.encodeMsg().toString();
                    }
                    BageIM.getInstance().getMsgManager().updateContentAndRefresh(msg.clientMsgNO, msg.content, false);
                    if (!sendingMsgHashMap.containsKey((int) msg.clientSeq)) {
                        BageSendMsg base1 = BageProto.getInstance().getSendBaseMsg(msg);
                        if (base1 != null) {
                            addSendingMsg(base1);
                            sendMessage(base1);
                        }
                    }
                } else {
                    MsgDbManager.getInstance().updateMsgStatus(msg.clientSeq, BageSendMsgResult.send_fail);
                }
            });
        } else {
            if (sendMsg != null) {
                if (msg.header != null && !msg.header.noPersist) {
                    addSendingMsg(sendMsg);
                }
                sendMessage(sendMsg);
            }
        }
    }

    /**
     * 检查连接是否为空（后台线程使用，可等待锁）
     */
    public boolean connectionIsNull() {
        boolean locked = false;
        try {
            locked = tryLockWithTimeout();
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，connectionIsNull检查失败");
                return true;
            }
            return connection == null || !connection.isOpen();
        } finally {
            if (locked) {
                connectionLock.unlock();
            }
        }
    }

    /**
     * 非阻塞检查连接状态（主线程安全，不会阻塞）
     * 利用 volatile 特性直接读取，无需锁
     */
    public boolean connectionIsNullFast() {
        INonBlockingConnection conn = connection;
        if (conn == null) {
            return true;
        }
        try {
            return !conn.isOpen();
        } catch (Exception e) {
            return true;
        }
    }

    private void startConnAckTimer() {
        // 移除之前的回调
        checkConnAckHandler.removeCallbacks(checkConnAckRunnable);
        connAckTime = DateUtils.getInstance().getCurrentSeconds();
        // 开始新的检查
        checkConnAckHandler.postDelayed(checkConnAckRunnable, 1000);
    }

    private void saveSendMsg(BageMsg msg) {
        if (msg.setting == null) msg.setting = new BageMsgSetting();
        JSONObject jsonObject = BageProto.getInstance().getSendPayload(msg);
        msg.content = jsonObject.toString();
        long tempOrderSeq = MsgDbManager.getInstance().queryMaxOrderSeqWithChannel(msg.channelID, msg.channelType);
        msg.orderSeq = tempOrderSeq + 1;
        // 需要存储的消息入库后更改消息的clientSeq
        if (!msg.header.noPersist) {
            msg.clientSeq = (int) MsgDbManager.getInstance().insert(msg);
            if (msg.clientSeq > 0) {
                BageUIConversationMsg uiMsg = BageIM.getInstance().getConversationManager().updateWithBageMsg(msg);
                if (uiMsg != null) {
                    long browseTo = BageIM.getInstance().getMsgManager().getMaxMessageSeqWithChannel(uiMsg.channelID, uiMsg.channelType);
                    if (uiMsg.getRemoteMsgExtra() == null) {
                        uiMsg.setRemoteMsgExtra(new BageConversationMsgExtra());
                    }
                    uiMsg.getRemoteMsgExtra().browseTo = browseTo;
                    BageIM.getInstance().getConversationManager().setOnRefreshMsg(uiMsg, "getSendBaseMsg");
                }
            }
        }
    }

    public void stopAll() {
        boolean locked = false;
        try {
            locked = tryLockWithTimeout();
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，stopAll失败");
                return;
            }

            // 先设置连接状态为失败
            BageIM.getInstance().getConnectionManager().setConnectionStatus(BageConnectStatus.fail, "");
            // 清理连接相关资源
            closeConnect();
            // 关闭定时器管理器
            TimerManager.getInstance().shutdown();
            MessageHandler.getInstance().clearCacheData();
            // 移除所有Handler回调
            if (checkRequestAddressHandler != null) {
                checkRequestAddressHandler.removeCallbacks(checkRequestAddressRunnable);
            }
            if (checkConnAckHandler != null) {
                checkConnAckHandler.removeCallbacks(checkConnAckRunnable);
            }
            if (reconnectionHandler != null) {
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
            }
            // 清理待发送消息队列中的任务
            if (resendQueue != null) {
                resendQueue.cleanupQueue();
            }

            // 重置所有状态
            connectStatus = BageConnectStatus.fail;
            isReConnecting = false;
            isConnecting.set(false);
            isReconnectScheduled.set(false);
            ip = "";
            port = 0;
            requestIPTime = 0;
            connAckTime = 0;
            lastMsgTime = 0;
            connCount = 0;

            // 注意：不清空 sendingMsgHashMap，保留待发送的消息
            // 这样网络恢复后可以继续发送
            // if (sendingMsgHashMap != null) {
            //     sendingMsgHashMap.clear();
            // }
            // 清理连接客户端
            connectionClient = null;

            // 关闭线程池
            shutdownExecutor();

            System.gc();
        } finally {
            if (locked) {
                connectionLock.unlock();
            }
        }
    }

    private void closeConnect() {
        final INonBlockingConnection connectionToCloseActual;

        if (!isClosing.compareAndSet(false, true)) {
            BageLoggerUtils.getInstance().i(TAG, " Close operation already in progress");
            return;
        }

        boolean locked = false;
        try {
            locked = tryLockWithTimeout();
            if (!locked) {
                BageLoggerUtils.getInstance().e(TAG, "获取锁超时，closeConnect失败");
                isClosing.set(false);
                return;
            }

            if (connection == null) {
                isClosing.set(false);
                BageLoggerUtils.getInstance().i(TAG, " closeConnect called but connection is already null.");
                return;
            }
            connectionToCloseActual = connection;
            String connId = connectionToCloseActual.getId();

            try {
                connectionToCloseActual.setAttachment("closing_" + System.currentTimeMillis() + "_" + connId);
            } catch (Exception e) {
                BageLoggerUtils.getInstance().e(TAG, "Failed to set closing attachment: " + e.getMessage());
            }

            connection = null;
            connectionClient = null;
            BageLoggerUtils.getInstance().i(TAG, " Connection object nulled, preparing for async close of: " + connId);
        } finally {
            if (locked) {
                connectionLock.unlock();
            }
        }

        // Create a timeout handler to force close after timeout
        final Runnable timeoutRunnable = () -> {
            try {
                if (connectionToCloseActual.isOpen()) {
                    String connId = connectionToCloseActual.getId();
                    BageLoggerUtils.getInstance().w(TAG, " Connection close timeout reached for: " + connId);
                    connectionToCloseActual.close();
                }
            } catch (Exception e) {
                BageLoggerUtils.getInstance().e(TAG, "Force close connection exception: " + e.getMessage());
            } finally {
                isClosing.set(false);
            }
        };

        // Schedule the timeout
        mainHandler.postDelayed(timeoutRunnable, CONNECTION_CLOSE_TIMEOUT);

        // Execute the close operation on a background thread
        Thread closeThread = new Thread(() -> {
            try {
                if (connectionToCloseActual.isOpen()) {
                    String connId = connectionToCloseActual.getId();
                    BageLoggerUtils.getInstance().i(TAG, " Attempting to close connection: " + connId);
                    connectionToCloseActual.close();
                    // Remove the timeout handler since we closed successfully
                    mainHandler.removeCallbacks(timeoutRunnable);
                    BageLoggerUtils.getInstance().i(TAG, " Successfully closed connection: " + connId);
                } else {
                    BageLoggerUtils.getInstance().i(TAG, " Connection was already closed or not open when async close executed: " + connectionToCloseActual.getId());
                }
            } catch (IOException e) {
                BageLoggerUtils.getInstance().e(TAG, "IOException during async connection close for " + connectionToCloseActual.getId() + ": " + e.getMessage());
            } catch (Exception e) {
                BageLoggerUtils.getInstance().e(TAG, "Exception during async connection close for " + connectionToCloseActual.getId() + ": " + e.getMessage());
            } finally {
                isClosing.set(false);
            }
        }, "ConnectionCloser");
        closeThread.setDaemon(true);
        closeThread.start();
    }

    private boolean tryLockWithTimeout() {
        try {
            return connectionLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            BageLoggerUtils.getInstance().e(TAG, "获取锁被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
