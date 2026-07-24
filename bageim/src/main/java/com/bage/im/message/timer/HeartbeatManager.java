package com.bage.im.message.timer;

import com.bage.im.message.BageConnection;
import com.bage.im.protocol.BagePingMsg;

import java.util.concurrent.locks.ReentrantLock;

public class HeartbeatManager {
    private final ReentrantLock heartbeatLock = new ReentrantLock();
    public void startHeartbeat() {
        TimerManager.getInstance().addTask(
                TimerTasks.HEARTBEAT,
                () -> {
                    heartbeatLock.lock();
                    try {
                        BageConnection.getInstance().sendMessage(new BagePingMsg());
                    } finally {
                        heartbeatLock.unlock();
                    }
                },
                0,
                1000 * 60
        );
    }
}
