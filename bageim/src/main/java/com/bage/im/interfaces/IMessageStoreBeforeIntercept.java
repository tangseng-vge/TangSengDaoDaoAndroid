package com.bage.im.interfaces;


import com.bage.im.entity.BageMsg;

/**
 * 2020-12-04 17:33
 * 存库之前拦截器
 */
public interface IMessageStoreBeforeIntercept {
    boolean isSaveMsg(BageMsg msg);
}
