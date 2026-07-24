package com.bage.im.message;


import com.bage.im.entity.BageMsg;

/**
 * 4/22/21 4:26 PM
 * 需要保存的消息
 */
class SavedMsg {
    public BageMsg bageMsg;
    public int redDot;

    public SavedMsg(BageMsg msg, int redDot) {
        this.redDot = redDot;
        this.bageMsg = msg;
    }
}
