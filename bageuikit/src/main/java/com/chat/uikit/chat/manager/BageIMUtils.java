package com.chat.uikit.chat.manager;

import static com.chat.advanced.utils.EditUtilKt.checkEditTime;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Vibrator;
import android.text.TextUtils;

import com.chat.base.BageBaseApplication;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.db.ApplyDB;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.entity.NewFriendEntity;
import com.chat.base.entity.UserInfoSetting;
import com.chat.base.entity.BageGroupType;
import com.chat.base.msg.IConversationContext;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageUIChatMsgItemEntity;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.NotificationCompatUtil;
import com.chat.base.utils.BageCommonUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.BageToastUtils;
import com.chat.base.views.pwdview.NumPwdDialog;
import com.chat.uikit.R;
import com.chat.uikit.BageUIKitApplication;
import com.chat.uikit.chat.ChatActivity;
import com.chat.uikit.contacts.service.FriendModel;
import com.chat.uikit.db.BageContactsDB;
import com.chat.uikit.enity.ProhibitWord;
import com.chat.uikit.group.service.GroupModel;
import com.chat.uikit.message.MsgModel;
import com.chat.uikit.message.ProhibitWordModel;
import com.chat.uikit.search.SearchUserActivity;
import com.chat.uikit.user.UserDetailActivity;
import com.chat.uikit.user.service.UserModel;
import com.chat.uikit.utils.PushNotificationHelper;
import com.luck.picture.lib.utils.ToastUtils;
import com.bage.im.BageIM;
import com.bage.im.entity.BageCMDKeys;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsg;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.message.type.BageSendMsgResult;
import com.bage.im.msgmodel.BageTextContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 2019-11-18 11:30
 * im监听相关处理
 */
public class BageIMUtils {

    private BageIMUtils() {
    }

    private static class IMUtilsBinder {
        private final static BageIMUtils util = new BageIMUtils();
    }

    public static BageIMUtils getInstance() {
        return IMUtilsBinder.util;
    }

    /**
     * 初始化事件
     */
    public void initIMListener() {
        EndpointManager.getInstance().setMethod("show_rtc_notification", object -> {
            if (object instanceof String fromUID) {
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(fromUID, BageChannelType.PERSONAL);
                var fromName = "";
                if (channel != null) {
                    if (TextUtils.isEmpty(channel.channelRemark)) {
                        fromName = channel.channelName;
                    } else fromName = channel.channelRemark;
                }

                Vibrator mVibrator = (Vibrator) BageBaseApplication.getInstance().getContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 1000, 1000};
                AudioAttributes audioAttributes;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION) //key
                            .build();
                    mVibrator.vibrate(pattern, 0, audioAttributes);
                } else {
                    mVibrator.vibrate(pattern, 0);
                }
                PushNotificationHelper.INSTANCE.notifyCall(BageUIKitApplication.getInstance().getContext(), 2, fromName, BageBaseApplication.getInstance().getContext().getString(R.string.invite_call));
            }
            return null;
        });
        EndpointManager.getInstance().setMethod("cancel_rtc_notification", object -> {
            Vibrator vibrator = (Vibrator) BageBaseApplication.getInstance().getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.cancel();
            NotificationCompatUtil.Companion.cancel(BageUIKitApplication.getInstance().getContext(), 2);
            return null;
        });
        // 获取用户密钥
//        BageIM.getInstance().getSignalProtocolManager().addOnCryptoSignalDataListener((channelID, channelTyp, iCryptoSignalDataResult) -> {
//            if (channelTyp == BageChannelType.PERSONAL) {
//                BageCryptoModel.getInstance().getUserKey(channelID, (code, msg, data) -> {
//                    if (code == HttpResponseCode.success && data != null) {
//                        BageSignalKey signalKey = new BageSignalKey();
//                        signalKey.UID = data.uid;
//                        signalKey.registrationID = data.registration_id;
//                        signalKey.identityKey = data.identity_key;
//                        signalKey.signedPubKey = data.signed_pubkey;
//                        signalKey.signedSignature = data.signed_signature;
//                        signalKey.signedPreKeyID = data.signed_prekey_id;
//                        BageOneTimePreKey oneTimePreKey = new BageOneTimePreKey();
//                        oneTimePreKey.pubKey = data.onetime_prekey.pubkey;
//                        oneTimePreKey.keyID = data.onetime_prekey.key_id;
//                        signalKey.oneTimePreKey = oneTimePreKey;
//                        iCryptoSignalDataResult.onResult(signalKey);
//                    } else {
//                        iCryptoSignalDataResult.onResult(null);
//                    }
//                });
//            }
//        });

        //监听sdk获取IP和port
        BageIM.getInstance().getConnectionManager().addOnGetIpAndPortListener(andPortListener ->
                MsgModel.getInstance().getChatIp((code, ip, port) -> {
                    int socketPort = 0;
                    try {
                        socketPort = Integer.parseInt(port);
                    } catch (NumberFormatException ignored) {
                    }
                    andPortListener.onGetSocketIpAndPort(ip == null ? "" : ip, socketPort);
                }));
        //消息存库拦截器监听
        BageIM.getInstance().getMsgManager().addMessageStoreBeforeIntercept(msg -> {
            if (msg != null && msg.type == BageContentType.screenshot) {
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(msg.channelID, msg.channelType);
                if (channel != null && channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(BageChannelExtras.screenshot)) {
                    Object object = channel.remoteExtraMap.get(BageChannelExtras.screenshot);
                    int screenshot = 0;
                    if (object != null) {
                        screenshot = (int) object;
                    }
                    return screenshot != 0;
                } else {
                    return true;
                }
            }
            return true;
        });
        //监听聊天附件上传
        BageIM.getInstance().getMsgManager().addOnUploadAttachListener((msg, listener) -> BageSendMsgUtils.getInstance().uploadChatAttachment(msg, listener));
        //监听同步会话
        BageIM.getInstance().getConversationManager().addOnSyncConversationListener((s, i, l, iSyncConvChatBack) -> MsgModel.getInstance().syncChat(s, i, l, iSyncConvChatBack));
        //监听同步频道会话
        BageIM.getInstance().getMsgManager().addOnSyncChannelMsgListener((channelID, channelType, startMessageSeq, endMessageSeq, limit, pullMode, iSyncChannelMsgBack) -> MsgModel.getInstance().syncChannelMsg(channelID, channelType, startMessageSeq, endMessageSeq, limit, pullMode, iSyncChannelMsgBack));
        //新消息监听
        BageIM.getInstance().getMsgManager().addOnNewMsgListener("system", msgList -> {
            boolean isAlertMsg = false;
            String channelID = "";
            byte channelType = BageChannelType.PERSONAL;
            BageMsg sensitiveWordsMsg = null;
            String loginUID = BageConfig.getInstance().getUid();
            if (BageReader.isNotEmpty(msgList)) {
                channelID = msgList.get(msgList.size() - 1).channelID;
                channelType = msgList.get(msgList.size() - 1).channelType;
                for (int i = 0, size = msgList.size(); i < size; i++) {
                    if (msgList.get(i).type == BageContentType.setNewGroupAdmin) {
                        GroupModel.getInstance().groupMembersSync(msgList.get(i).channelID, null);
                    } else if (msgList.get(i).type == BageContentType.groupSystemInfo) {
                        BageCommonModel.getInstance().getChannel(msgList.get(i).channelID, BageChannelType.GROUP, null);
                        GroupModel.getInstance().groupMembersSync(msgList.get(i).channelID, null);
                    } else if (msgList.get(i).type == BageContentType.addGroupMembersMsg || msgList.get(i).type == BageContentType.removeGroupMembersMsg) {
                        //同步信息
                        GroupModel.getInstance().groupMembersSync(msgList.get(i).channelID, null);
                    } else {
                        if (msgList.get(i).type != BageContentType.Bage_INSIDE_MSG) {
                            isAlertMsg = true;
                        }
                    }

                    if (msgList.get(i).header.noPersist || !msgList.get(i).header.redDot || !BageContentType.isSupportNotification(msgList.get(i).type)) {
                        isAlertMsg = false;
                    }
                    if (!TextUtils.isEmpty(loginUID) && !TextUtils.isEmpty(msgList.get(i).fromUID) && msgList.get(i).fromUID.equals(loginUID)) {
                        isAlertMsg = false;
                    }
                    if (msgList.get(i).type == BageContentType.Bage_TEXT) {
                        boolean isContains = false;
                        BageTextContent textContent = (BageTextContent) msgList.get(i).baseContentMsgModel;
                        // 判断是否包含敏感词
                        if (BageUIKitApplication.getInstance().sensitiveWords != null
                                && BageReader.isNotEmpty(BageUIKitApplication.getInstance().sensitiveWords.list)
                                && textContent != null && !TextUtils.isEmpty(textContent.getDisplayContent())) {
                            for (String word : BageUIKitApplication.getInstance().sensitiveWords.list) {
                                if (textContent.getDisplayContent().contains(word)) {
                                    isContains = true;
                                    break;
                                }
                            }
                        }
                        if (isContains) {
                            sensitiveWordsMsg = new BageMsg();
                            sensitiveWordsMsg.channelID = msgList.get(i).channelID;
                            sensitiveWordsMsg.channelType = msgList.get(i).channelType;
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("content", BageUIKitApplication.getInstance().sensitiveWords.tips);
                                jsonObject.put("type", BageContentType.sensitiveWordsTips);
                            } catch (JSONException e) {
                                BageLogUtils.e("解析敏感词错误");
                            }
                            BageChannel channel = new BageChannel(msgList.get(i).channelID, msgList.get(i).channelType);
                            sensitiveWordsMsg.setChannelInfo(channel);
                            sensitiveWordsMsg.content = jsonObject.toString();
                            sensitiveWordsMsg.type = BageContentType.sensitiveWordsTips;
                            long tempOrderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(0, msgList.get(i).channelID, msgList.get(i).channelType);
                            sensitiveWordsMsg.orderSeq = tempOrderSeq + 1;
                            sensitiveWordsMsg.status = BageSendMsgResult.send_success;

                        }
                    }
                }
            }
            boolean isVibrate = true;
            boolean playNewMsgMedia = true;
            boolean newMsgNotice = true;
            UserInfoSetting setting = BageConfig.getInstance().getUserInfo().setting;
            int msgShowDetail = 1;
            if (setting != null) {
                msgShowDetail = setting.msg_show_detail;
                if (setting.new_msg_notice == 0) {
                    newMsgNotice = false;
                    playNewMsgMedia = false;
                    isVibrate = false;
                } else {
                    if (setting.voice_on == 0) {
                        playNewMsgMedia = false;
                    }
                    if (setting.shock_on == 0) {
                        isVibrate = false;
                    }
                }
            }
            if (newMsgNotice && isAlertMsg && (TextUtils.isEmpty(BageUIKitApplication.getInstance().chattingChannelID) || !BageUIKitApplication.getInstance().chattingChannelID.equals(channelID))) {
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
                if (channel != null && channel.mute == 0) {
                    showNotification(msgList.get(msgList.size() - 1), msgShowDetail, channel, playNewMsgMedia, isVibrate);
                }
            }

            assert msgList != null;

            if (sensitiveWordsMsg != null) {
                BageMsg finalSensitiveWordsMsg = sensitiveWordsMsg;
                new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(() -> BageIM.getInstance().getMsgManager().saveAndUpdateConversationMsg(finalSensitiveWordsMsg, false), 1000 * 2);
            }
        });
        BageIM.getInstance().getMsgManager().addOnUploadMsgExtraListener(msgExtra -> {
            BageMsg msg = BageIM.getInstance().getMsgManager().getWithMessageID(msgExtra.messageID);
            int msgSeq = 0;
            if (msg != null) {
                msgSeq = msg.messageSeq;
            }
            if(msg != null && checkEditTime(msg.createdAt)){
                return;
            }
            MsgModel.getInstance().editMsg(msgExtra.messageID, msgSeq, msgExtra.channelID, msgExtra.channelType, msgExtra.contentEdit, null);

        });

        /*
         * 设置获取频道信息的监听
         */
        BageIM.getInstance().getChannelManager().addOnGetChannelInfoListener((channelId, channelType, iChannelInfoListener) -> {
            BageCommonModel.getInstance().getChannel(channelId, channelType, null);
            return null;
        });
        BageIM.getInstance().getChannelMembersManager().addOnGetChannelMembersListener((channelID, b, keyword, page, limit, iChannelMemberListResult) -> GroupModel.getInstance().getChannelMembers(channelID, keyword, page, limit, iChannelMemberListResult));
        /*
         * 获取频道成员
         */
        BageIM.getInstance().getChannelMembersManager().addOnGetChannelMemberListener((channelId, channelType, uid, iChannelMemberInfoListener) -> {
            BageCommonModel.getInstance().getChannel(uid, BageChannelType.PERSONAL, (code, msg, entity) -> {
                BageChannelMember channelMember = new BageChannelMember();
                channelMember.memberName = entity.name;
                channelMember.memberUID = entity.channel.channel_id;
                channelMember.channelID = channelId;
                channelMember.channelType = channelType;
                BageIM.getInstance().getChannelMembersManager().refreshChannelMemberCache(channelMember);
                iChannelMemberInfoListener.onResult(channelMember);
            });
            return null;
        });

        //监听频道修改头像
        BageIM.getInstance().getChannelManager().addOnRefreshChannelAvatar((s, b) -> {
            // 头像需要本地修改
            String key = UUID.randomUUID().toString().replace("-", "");
            AvatarView.clearCache(s, b);
            BageIM.getInstance().getChannelManager().updateAvatarCacheKey(s, b, key);
        });
        //刷新群成员
        BageIM.getInstance().getChannelMembersManager().addOnSyncChannelMembers((channelID, channelType) -> {
            if (!TextUtils.isEmpty(channelID) && channelType == BageChannelType.GROUP) {
                GroupModel.getInstance().groupMembersSync(channelID, null);
            }
        });

        BageIM.getInstance().getCMDManager().addCmdListener("system", cmd -> {
            if (!TextUtils.isEmpty(cmd.cmdKey)) {
                switch (cmd.cmdKey) {
                    case BageCMDKeys.bage_messageRevoke -> revokeMsg(cmd.paramJsonObject);
                    case BageCMDKeys.bage_friendRequest ->
                            FriendModel.getInstance().saveNewFriendsMsg(cmd.paramJsonObject.toString());
                    case BageCMDKeys.bage_friendDeleted, BageCMDKeys.bage_friendAccept -> {
                        FriendModel.getInstance().syncFriends(null);
                        if (cmd.cmdKey.equals(BageCMDKeys.bage_friendAccept)
                                && cmd.paramJsonObject != null && cmd.paramJsonObject.has("to_uid")) {
                            String uid = cmd.paramJsonObject.optString("to_uid");
                            BageContactsDB.getInstance().updateFriendStatus(uid, 1);
                            NewFriendEntity entity = ApplyDB.getInstance().query(uid);
                            if (entity != null && entity.status == 0) {
                                entity.status = 1;
                                ApplyDB.getInstance().update(entity);
                            }
                        }
                    }
                    case BageCMDKeys.bage_sync_message_extra -> {
                        if (cmd.paramJsonObject == null) {
                            return;
                        }
                        String channelID = cmd.paramJsonObject.optString("channel_id");
                        byte channelType = (byte) cmd.paramJsonObject.optInt("channel_type");
                        if (TextUtils.isEmpty(channelID)) {
                            return;
                        }
                        MsgModel.getInstance().syncExtraMsg(channelID, channelType);
                    }
                    case BageCMDKeys.bage_memberUpdate -> {
                        if (cmd.paramJsonObject == null) {
                            return;
                        }
                        String groupNo = cmd.paramJsonObject.optString("group_no");
                        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(groupNo, BageChannelType.GROUP);
                        if (channel == null || channel.remoteExtraMap == null) {
                            return;
                        }
                        Object groupTypeObject = channel.remoteExtraMap.get(BageChannelExtras.groupType);
                        if (groupTypeObject instanceof Integer) {
                            int groupType = (int) groupTypeObject;
                            if (groupType == BageGroupType.superGroup) {
                                String uid = cmd.paramJsonObject.optString("uid");
                                if (!TextUtils.isEmpty(uid)) {
                                    UserModel.getInstance().getUserInfo(uid, groupNo, null);
                                }
                            }
                        }
                    }
                    case BageCMDKeys.bage_sync_reminders -> MsgModel.getInstance().syncReminder();
                    case BageCMDKeys.bage_sync_conversation_extra ->
                            MsgModel.getInstance().syncCoverExtra();
                }
            }
        });
    }

    public BageUIChatMsgItemEntity msg2UiMsg(IConversationContext context, BageMsg msg, int memberCount, boolean showNickName, boolean isChoose) {
        if (msg.remoteExtra.readedCount == 0) {
            msg.remoteExtra.unreadCount = memberCount - 1;
        }
        if (msg.type == BageContentType.Bage_TEXT) {
//            BageTextContent textContent = (BageTextContent) msg.baseContentMsgModel;
//            if (textContent != null && !TextUtils.isEmpty(textContent.getDisplayContent())) {
//                List<String> urls = StringUtils.getStrUrls(textContent.getDisplayContent());
//                if (urls.size() > 0) {
//                    String url = urls.get(urls.size() - 1);
//                    String contentJson = BageSharedPreferencesUtil.getInstance().getSP(url);
//                    if (!TextUtils.isEmpty(contentJson)) {
//                        try {
//                            JSONObject jsonObject = new JSONObject(contentJson);
//                            long expirationTime = jsonObject.optLong("expirationTime");
//                            long tempTime = BageTimeUtils.getInstance().getCurrentSeconds() - expirationTime;
//                            if (tempTime >= 60 * 60 * 24 * 360) {
//                                BageJsoupUtils.getInstance().getURLContent(url, msg.clientMsgNO);
//                            }
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    } else {
//                        BageJsoupUtils.getInstance().getURLContent(url, msg.clientMsgNO);
//                    }
//                }
//
//            }
            resetMsgProhibitWord(msg);
        }
        BageUIChatMsgItemEntity uiChatMsgItemEntity = new BageUIChatMsgItemEntity(context, msg, new BageUIChatMsgItemEntity.ILinkClick() {
            @Override
            public void onShowUserDetail(String uid, String groupNo) {
                Intent intent = new Intent(context.getChatActivity(), UserDetailActivity.class);
                intent.putExtra("uid", uid);
                if (!TextUtils.isEmpty(groupNo)) {
                    intent.putExtra("groupID", groupNo);
                }
                context.getChatActivity().startActivity(intent);
            }

            @Override
            public void onShowSearchUser(String phone) {
                Intent intent = new Intent(context.getChatActivity(), SearchUserActivity.class);
                intent.putExtra("phone", phone);
                context.getChatActivity().startActivity(intent);
            }
        });
        uiChatMsgItemEntity.bageMsg = msg;
        uiChatMsgItemEntity.isChoose = isChoose;
        uiChatMsgItemEntity.showNickName = showNickName;

        // 计算气泡类型
        return uiChatMsgItemEntity;
    }

    public void resetMsgProhibitWord(BageMsg msg) {
        if (msg == null || msg.type != BageContentType.Bage_TEXT) {
            return;
        }
        List<ProhibitWord> list = ProhibitWordModel.Companion.getInstance().getAll();
        if (BageReader.isNotEmpty(list)) {
            String content = getContent(msg);
            for (ProhibitWord word : list) {
                if (content.contains(word.content)) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < word.content.length(); i++) {
                        sb.append("*");
                    }
                    content = content.replaceAll(word.content, sb.toString());
                }
            }

            if (msg.remoteExtra.contentEditMsgModel != null && !TextUtils.isEmpty(msg.remoteExtra.contentEditMsgModel.getDisplayContent())) {
                msg.remoteExtra.contentEditMsgModel.content = content;
            } else {
                msg.baseContentMsgModel.content = content;
            }
        }
    }

    private String getContent(BageMsg msg) {
        String showContent = msg.baseContentMsgModel.getDisplayContent();
        if (msg.remoteExtra.contentEditMsgModel != null && !TextUtils.isEmpty(msg.remoteExtra.contentEditMsgModel.getDisplayContent())) {
            showContent = msg.remoteExtra.contentEditMsgModel.getDisplayContent();
        }
        return showContent;
    }


    public void revokeMsg(JSONObject jsonObject) {
        //撤回消息
        if (jsonObject != null) {
            if (jsonObject.has("message_id")) {
                String messageId = jsonObject.optString("message_id");
                //  String client_msg_no = jsonObject.optString("client_msg_no");
                String channelID = jsonObject.optString("channel_id");
                byte channelType = (byte) jsonObject.optInt("channel_type");
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
                //是否撤回提醒
                int revokeRemind = 1;
                if (channel != null && channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(BageChannelExtras.revokeRemind)) {
                    Object object = channel.remoteExtraMap.get(BageChannelExtras.revokeRemind);
                    if (object != null) {
                        revokeRemind = (int) object;
                    }
                }
                if (revokeRemind == 1) {
                    // todo 同步消息接口
                    MsgModel.getInstance().syncExtraMsg(channelID, channelType);
//                    BageIM.getInstance().getMsgManager().updateMsgRevokeWithMessageID(messageId, 1);
                } else {
                    // todo 删除服务器消息
                    BageMsg bageMsg = BageIM.getInstance().getMsgManager().getWithMessageID(messageId);
                    if (bageMsg != null) {
                        List<BageMsg> list = new ArrayList<>();
                        list.add(bageMsg);
                        MsgModel.getInstance().deleteMsg(list, null);
                    }

                    int rowNo = BageIM.getInstance().getMsgManager().getRowNoWithMessageID(channelID, channelType, messageId);
                    //要先删除
                    BageIM.getInstance().getMsgManager().deleteWithMessageID(messageId);
                    BageConversationMsg msg = BageIM.getInstance().getConversationManager().getWithChannel(channelID, channelType);
                    if (msg != null) {
                        if (rowNo < msg.unreadCount) {
                            msg.unreadCount--;
                        }
                        BageIM.getInstance().getConversationManager().updateWithMsg(msg);
                    }
                }

            }
        }
    }


    /**
     * 显示聊天
     *
     * @param chatViewMenu 参数
     */
    public void startChatActivity(ChatViewMenu chatViewMenu) {
        if (chatViewMenu == null || chatViewMenu.activity == null || TextUtils.isEmpty(chatViewMenu.channelID)) {
            return;
        }
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(chatViewMenu.channelID, chatViewMenu.channelType);
        int chatPwdON = 0;
        if (channel != null && channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(BageChannelExtras.chatPwdOn)) {
            Object object = channel.remoteExtraMap.get(BageChannelExtras.chatPwdOn);
            if (object instanceof Integer) {
                chatPwdON = (int) object;
            }
        }
        if (chatPwdON == 1) {
            showChatPwdDialog(chatViewMenu, channel);
            return;
        }
        startChat(chatViewMenu);
    }

    private void startChat(ChatViewMenu chatViewMenu) {
        if (BageTimeUtils.isFastDoubleClick()) {
            return;
        }
        MsgModel.getInstance().deleteFlameMsg();
        Intent intent = new Intent(chatViewMenu.activity, ChatActivity.class);
        intent.putExtra("channelId", chatViewMenu.channelID);
        intent.putExtra("channelType", chatViewMenu.channelType);
        BageConversationMsg conversationMsg = BageIM.getInstance().getConversationManager().getWithChannel(chatViewMenu.channelID, chatViewMenu.channelType);
        BageMsg msg = null;
        int redDot = 0;
        long aroundMsgSeq = 0;
        if (conversationMsg != null) {
            redDot = conversationMsg.unreadCount;
            msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(conversationMsg.lastClientMsgNO);
            if (msg != null) {
                aroundMsgSeq = msg.orderSeq;
            }
        }
        if (chatViewMenu.tipMsgOrderSeq != 0) {
            // 强提醒某条消息
            intent.putExtra("tipsOrderSeq", chatViewMenu.tipMsgOrderSeq);
        } else {
            if (redDot > 0) {
                long orderSeq;
                int messageSeq = 0;
                if (msg != null) {
                    if (msg.messageSeq == 0) {
                        int maxMsgSeq = BageIM.getInstance().getMsgManager().getMaxMessageSeqWithChannel(chatViewMenu.channelID, chatViewMenu.channelType);
                        messageSeq = maxMsgSeq - redDot + 1;
                    } else {
                        messageSeq = msg.messageSeq - redDot + 1;
                    }
                    if (messageSeq <= 0) {
                        messageSeq = BageIM.getInstance().getMsgManager().getMinMessageSeqWithChannel(chatViewMenu.channelID, chatViewMenu.channelType);
                    }
                }
                orderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(messageSeq, chatViewMenu.channelID, chatViewMenu.channelType);
                intent.putExtra("unreadStartMsgOrderSeq", orderSeq);
                intent.putExtra("redDot", redDot);
            } else {
                BageUIConversationMsg uiMsg = BageIM.getInstance().getConversationManager().getUIConversationMsg(chatViewMenu.channelID, chatViewMenu.channelType);
                if (uiMsg != null && uiMsg.getRemoteMsgExtra() != null && uiMsg.getRemoteMsgExtra().keepMessageSeq != 0) {
                    long lastPreviewMsgOrderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(uiMsg.getRemoteMsgExtra().keepMessageSeq, chatViewMenu.channelID, chatViewMenu.channelType);
                    intent.putExtra("lastPreviewMsgOrderSeq", lastPreviewMsgOrderSeq);
                    intent.putExtra("keepOffsetY", uiMsg.getRemoteMsgExtra().keepOffsetY);
                }
            }
        }
        if (chatViewMenu.isNewTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        if (BageReader.isNotEmpty(chatViewMenu.forwardMsgList)) {
            intent.putParcelableArrayListExtra("msgContentList", (ArrayList<? extends Parcelable>) chatViewMenu.forwardMsgList);
        }
        intent.putExtra("aroundMsgSeq", aroundMsgSeq);
        chatViewMenu.activity.startActivity(intent);
    }

    private void showChatPwdDialog(ChatViewMenu chatViewMenu, BageChannel channel) {
        NumPwdDialog.getInstance().showNumPwdDialog(chatViewMenu.activity, chatViewMenu.activity.getString(R.string.chat_pwd), chatViewMenu.activity.getString(R.string.input_chat_pwd), channel.channelName, new NumPwdDialog.IPwdInputResult() {
            @Override
            public void onResult(String numPwd) {

                if (!BageCommonUtils.digest(numPwd + BageConfig.getInstance().getUid()).equals(BageConfig.getInstance().getUserInfo().chat_pwd)) {
                    int chatPwdCount = BageSharedPreferencesUtil.getInstance().getInt("bage_chat_pwd_count", 3);
                    if (chatPwdCount == 0) {
                        // 清空聊天记录
                        BageSharedPreferencesUtil.getInstance().putInt("bage_chat_pwd_count", 0);
                        BageIM.getInstance().getMsgManager().clearWithChannel(channel.channelID, channel.channelType);
                        BageToastUtils.getInstance().showToastNormal(chatViewMenu.activity.getString(R.string.chat_msg_is_cleard));
                        return;
                    }

                    String content = String.format(chatViewMenu.activity.getString(R.string.forget_chat_pwd), chatPwdCount, chatPwdCount);
                    BageDialogUtils.getInstance().showDialog(chatViewMenu.activity, chatViewMenu.activity.getString(R.string.chat_pwd_error), content, false, chatViewMenu.activity.getString(R.string.cancel), chatViewMenu.activity.getString(R.string.chat_pwd_reset_pwd), 0, Theme.colorAccount, index -> {
                        if (index == 1) {
                            EndpointManager.getInstance().invoke("show_set_chat_pwd", null);
                        }
                    });
                    BageSharedPreferencesUtil.getInstance().putInt("bage_chat_pwd_count", --chatPwdCount);
                } else {
                    BageSharedPreferencesUtil.getInstance().putInt("bage_chat_pwd_count", 3);
                    startChat(chatViewMenu);
                }

            }

            @Override
            public void forgetPwd() {
                EndpointManager.getInstance().invoke("show_set_chat_pwd", null);
            }
        });

    }


    private void showNotification(BageMsg msg, int msgShowDetail, BageChannel channel, boolean playNewMsgMedia, boolean isVibrate) {
        int msgNotice = BageConfig.getInstance().getUserInfo().setting.new_msg_notice;
        if (msgNotice == 0) {
            return;
        }
//        Activity activity = ActManagerUtils.getInstance().getCurrentActivity();
//        if (activity == null || activity.getComponentName().getClassName().equals(TabActivity.class.getName())) {
        if (playNewMsgMedia) {
            defaultMediaPlayer();
        }
        if (isVibrate) {
            vibrate();
        }
//            return;
//        }
        String showTitle = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
        String showContent = BageBaseApplication.getInstance().getContext().getString(R.string.default_new_msg);
        if (msgShowDetail == 1 && msg.baseContentMsgModel != null && !TextUtils.isEmpty(msg.baseContentMsgModel.getDisplayContent())) {
            showContent = msg.baseContentMsgModel.getDisplayContent();
        }
//        String url;
//        if (!TextUtils.isEmpty(channel.avatar) && channel.avatar.contains("/")) {
//            url = BageApiConfig.getShowUrl(channel.avatar);
//        } else {
//            url = BageApiConfig.getShowAvatar(channel.channelID, channel.channelType);
//        }
//        String finalShowContent = showContent;
//        if (isVibrate) {
//            PushNotificationHelper.INSTANCE.notifyMention(BageUIKitApplication.getInstance().getContext(), 1, showTitle, showContent);
//        } else {
        PushNotificationHelper.INSTANCE.notifyMessage(BageUIKitApplication.getInstance().getContext(), 1, showTitle, showContent);
//        }
//        showNotice(showTitle, finalShowContent, null, isVibrate);
//        getChannelLogo(url, activity, logo -> showNotice(showTitle, finalShowContent, logo, isVibrate));
    }


    private void defaultMediaPlayer() {
        EndpointManager.getInstance().invoke("play_new_msg_Media", null);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) BageUIKitApplication.getInstance().getContext().getSystemService(Service.VIBRATOR_SERVICE);
        long[] pattern = {100, 200};
        vibrator.vibrate(pattern, -1);
    }

    public void removeListener() {
        BageIM.getInstance().getCMDManager().removeCmdListener("system");
        BageIM.getInstance().getMsgManager().removeNewMsgListener("system");
    }


}
