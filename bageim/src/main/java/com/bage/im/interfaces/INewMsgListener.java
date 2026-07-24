package com.bage.im.interfaces;


import com.bage.im.entity.BageMsg;

import java.util.List;

/**
 * 2019-11-18 11:44
 * 新消息监听
 */
public interface INewMsgListener {
    void newMsg(List<BageMsg> msgs);
}
