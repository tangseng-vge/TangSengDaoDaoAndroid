package com.chat.uikit.chat.manager;

import android.text.TextUtils;

import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.BageSendMsgMenu;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.net.ud.BageUploader;
import com.chat.base.net.entity.UploadResultEntity;
import com.chat.base.msgmodel.BageChatImageContent;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageSendOptions;
import com.bage.im.interfaces.IUploadAttacResultListener;
import com.bage.im.msgmodel.BageMediaMessageContent;
import com.bage.im.msgmodel.BageVideoContent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * 2019-11-20 13:20
 * 发送消息管理
 */
public class BageSendMsgUtils {
    private BageSendMsgUtils() {

    }

    private static class SendMsgUtilsBinder {
        private static final BageSendMsgUtils utils = new BageSendMsgUtils();
    }

    public static BageSendMsgUtils getInstance() {
        return SendMsgUtilsBinder.utils;
    }

    public void sendMessage(BageMsg bageMsg) {
        BageSendOptions options = new BageSendOptions();
        options.robotID = bageMsg.robotID;
        BageChannel channel = bageMsg.getChannelInfo();
        if (channel == null) {
            channel = new BageChannel(bageMsg.channelID, bageMsg.channelType);
        }
        EndpointManager.getInstance().invokes(EndpointSID.sendMessage, new BageSendMsgMenu(channel, options));
        BageIM.getInstance().getMsgManager().sendWithOptions(bageMsg.baseContentMsgModel, channel, options);
    }

    public void sendMessages(List<SendMsgEntity> list) {
        final Timer[] timer = {new Timer()};
        final int[] i = {0};
        timer[0].schedule(new TimerTask() {
            @Override
            public void run() {
                if (i[0] == list.size() - 1) {
                    timer[0].cancel();
                    timer[0] = null;
                }
                BageMsg bageMsg = new BageMsg();
                bageMsg.channelID = list.get(i[0]).bageChannel.channelID;
                bageMsg.channelType = list.get(i[0]).bageChannel.channelType;
                bageMsg.type = list.get(i[0]).messageContent.type;
                bageMsg.baseContentMsgModel = list.get(i[0]).messageContent;
                sendMessage(bageMsg);
                i[0]++;
            }
        }, 0, 150);
    }

    /**
     * 上传聊天附件
     *
     * @param msg      消息
     * @param listener 上传返回
     */
    void uploadChatAttachment(BageMsg msg, IUploadAttacResultListener listener) {
        //存在附件待上传
        if (msg.type == BageContentType.Bage_IMAGE || msg.type == BageContentType.Bage_GIF || msg.type == BageContentType.Bage_VOICE || msg.type == BageContentType.Bage_LOCATION || msg.type == BageContentType.Bage_FILE) {
            BageMediaMessageContent contentMsgModel = (BageMediaMessageContent) msg.baseContentMsgModel;
            //已经有网络地址无需在上传
            if (!TextUtils.isEmpty(contentMsgModel.url)) {
                listener.onUploadResult(true, contentMsgModel);
            } else {
                if (!TextUtils.isEmpty(contentMsgModel.localPath)) {
                    BageUploader.getInstance().getUploadFileUrl(msg.channelID, msg.channelType, contentMsgModel.localPath, msg.type == BageContentType.Bage_IMAGE, (url, filePath) -> {
                        if (!TextUtils.isEmpty(url)) {
                            BageUploader.getInstance().upload(url, contentMsgModel.localPath, msg.clientSeq, new BageUploader.IUploadBack() {
                                @Override
                                public void onSuccess(String url) {
                                    contentMsgModel.url = url;
                                    listener.onUploadResult(true, contentMsgModel);
                                }

                                @Override
                                public void onSuccess(UploadResultEntity result) {
                                    contentMsgModel.url = result.path;
                                    if (contentMsgModel instanceof BageChatImageContent) {
                                        BageChatImageContent image = (BageChatImageContent) contentMsgModel;
                                        image.previewUrl = result.preview_path == null ? result.path : result.preview_path;
                                        image.originalUrl = result.original_path == null ? image.previewUrl : result.original_path;
                                        image.originalSize = result.original_size;
                                    }
                                    listener.onUploadResult(true, contentMsgModel);
                                }

                                @Override
                                public void onError() {
                                    listener.onUploadResult(false, contentMsgModel);
                                }
                            });
                        } else {
                            listener.onUploadResult(false, contentMsgModel);
                        }
                    });
                } else {
                    listener.onUploadResult(false, msg.baseContentMsgModel);
                }
            }

        } else if (msg.type == BageContentType.Bage_VIDEO) {
            //视频
            BageVideoContent videoMsgModel = (BageVideoContent) msg.baseContentMsgModel;
            if (!TextUtils.isEmpty(videoMsgModel.cover) && !TextUtils.isEmpty(videoMsgModel.url)) {
                listener.onUploadResult(true, msg.baseContentMsgModel);
            } else {
                if (TextUtils.isEmpty(videoMsgModel.cover)) {
                    BageUploader.getInstance().getUploadFileUrl(msg.channelID, msg.channelType, videoMsgModel.coverLocalPath, (url, filePath) -> {
                        if (!TextUtils.isEmpty(url)) {
                            BageUploader.getInstance().upload(url, videoMsgModel.coverLocalPath, UUID.randomUUID().toString().replaceAll("-", ""),
                                    new BageUploader.IUploadBack() {
                                        @Override
                                        public void onSuccess(String url) {
                                            videoMsgModel.cover = url;
                                            BageUploader.getInstance().getUploadFileUrl(msg.channelID, msg.channelType, videoMsgModel.localPath, (url1, fileUrl) -> BageUploader.getInstance().upload(url1, videoMsgModel.localPath, msg.clientSeq, new BageUploader.IUploadBack() {
                                                @Override
                                                public void onSuccess(String url1) {
                                                    videoMsgModel.url = url1;
                                                    listener.onUploadResult(true, videoMsgModel);
                                                }

                                                @Override
                                                public void onError() {
                                                    listener.onUploadResult(false, videoMsgModel);
                                                }
                                            }));
                                        }

                                        @Override
                                        public void onError() {
                                            listener.onUploadResult(false, msg.baseContentMsgModel);
                                        }
                                    });
                        } else {
                            listener.onUploadResult(false, msg.baseContentMsgModel);
                        }
                    });
                }
            }
        }

    }
}
