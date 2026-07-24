package com.bage.im.protocol;

import com.bage.im.message.type.BageMsgType;

/**
 * 2019-11-11 10:14
 * talk service 基础消息对象
 *
 * @see BageMsgType 对应packetType类型
 */
public class BageBaseMsg {
    //报文类型
    public short packetType;
    //标示位（目前为固定值）
    public short flag;
    //报文剩余长度
    public int remainingLength;
    //是否持久化[是否保存在数据库]
    public boolean no_persist;
    //是否显示红点
    public boolean red_dot = true;
    //是否只显示一次
    public boolean sync_once;

    public int getFixedHeaderLength() {
        return 2;
    }
}
