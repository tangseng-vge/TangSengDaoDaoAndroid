package com.bage.im.interfaces;


import com.bage.im.entity.BageMsg;

/**
 * 2020-08-02 00:29
 * 上传聊天附件
 */
public interface IUploadAttachmentListener {
    void onUploadAttachmentListener(BageMsg msg, IUploadAttacResultListener attacResultListener);
}
