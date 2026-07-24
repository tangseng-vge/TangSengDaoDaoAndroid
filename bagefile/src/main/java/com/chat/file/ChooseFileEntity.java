package com.chat.file;


import com.bage.im.entity.BageMsg;

/**
 * 2020-08-13 16:39
 */
public class ChooseFileEntity {
    boolean checked;
    BageMsg msg;

    ChooseFileEntity(BageMsg msg) {
        this.msg = msg;
    }
}
