package com.bage.im.interfaces;


import com.bage.im.entity.BageSyncMsg;

import java.util.List;

/**
 * 2020-09-28 15:10
 * 同步消息完成回调
 */
public interface ISyncOfflineMsgBack {
    void onBack(boolean isEnd, List<BageSyncMsg> list);
}
