package com.bage.im.interfaces;


import com.bage.im.entity.BageCMD;

/**
 * 2/3/21 2:23 PM
 * cmd监听
 */
public interface ICMDListener {
    void onMsg(BageCMD bagecmd);
}
