package com.chat.uikit;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.Application;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.chat.base.BageBaseApplication;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageBinder;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatChooseContacts;
import com.chat.base.endpoint.entity.ChatFunctionMenu;
import com.chat.base.endpoint.entity.ChatItemPopupMenu;
import com.chat.base.endpoint.entity.ChatToolBarMenu;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.endpoint.entity.ChooseChatMenu;
import com.chat.base.endpoint.entity.ChooseContactsMenu;
import com.chat.base.endpoint.entity.ContactsMenu;
import com.chat.base.endpoint.entity.DBMenu;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.endpoint.entity.MsgConfig;
import com.chat.base.endpoint.entity.PersonalInfoMenu;
import com.chat.base.endpoint.entity.ScanResultMenu;
import com.chat.base.endpoint.entity.SearchChatContentMenu;
import com.chat.base.endpoint.entity.UserDetailMenu;
import com.chat.base.endpoint.entity.BageMsg2UiMsgMenu;
import com.chat.base.endpoint.entity.WithdrawMsgMenu;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.ChooseResultModel;
import com.chat.base.glide.GlideUtils;
import com.chat.base.msg.IConversationContext;
import com.chat.base.msg.model.BageGifContent;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageMsgItemViewManager;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.components.AlertDialog;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.ActManagerUtils;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.BageDeviceUtils;
import com.chat.base.utils.BageFileUtils;
import com.chat.base.utils.BageMediaFileUtils;
import com.chat.base.utils.BagePermissions;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.language.EndpointLocaleHelper;
import com.chat.base.utils.BageToastUtils;
import com.chat.uikit.chat.ChooseChatActivity;
import com.chat.uikit.chat.face.BageVoiceViewManager;
import com.chat.uikit.chat.manager.FaceManger;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.chat.msgmodel.BageCardContent;
import com.chat.uikit.chat.msgmodel.BageMultiForwardContent;
import com.chat.uikit.chat.provider.LoadingProvider;
import com.chat.uikit.chat.provider.BageCardProvider;
import com.chat.uikit.chat.provider.BageEmptyProvider;
import com.chat.uikit.chat.provider.BageImageProvider;
import com.chat.uikit.chat.provider.BageMultiForwardProvider;
import com.chat.uikit.chat.provider.BageNoRelationProvider;
import com.chat.uikit.chat.provider.BagePromptNewMsgProvider;
import com.chat.uikit.chat.provider.BageSensitiveWordsProvider;
import com.chat.uikit.chat.provider.BageSpanEmptyProvider;
import com.chat.uikit.chat.provider.BageTextProvider;
import com.chat.uikit.chat.provider.BageVoiceProvider;
import com.chat.uikit.chat.search.date.SearchWithDateActivity;
import com.chat.uikit.chat.search.image.SearchWithImgActivity;
import com.chat.uikit.contacts.ChooseContactsActivity;
import com.chat.uikit.contacts.NewFriendsActivity;
import com.chat.uikit.enity.SensitiveWords;
import com.chat.uikit.group.SavedGroupsActivity;
import com.chat.uikit.group.BageAllMembersActivity;
import com.chat.uikit.message.MsgModel;
import com.chat.uikit.message.ProhibitWordModel;
import com.chat.uikit.message.favorite.FavoriteMessageListActivity;
import com.chat.uikit.message.mass.MassMessageSelectActivity;
import com.chat.moments.activities.MyReportsActivity;
import com.chat.uikit.search.AddFriendsActivity;
import com.chat.uikit.setting.MsgNoticesSettingActivity;
import com.chat.uikit.setting.SettingActivity;
import com.chat.uikit.user.UserDetailActivity;
import com.tencent.bugly.crashreport.CrashReport;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMsg;
import com.bage.im.msgmodel.BageImageContent;
import com.chat.base.msgmodel.BageChatImageContent;
import com.bage.im.msgmodel.BageMessageContent;
import com.bage.im.msgmodel.BageTextContent;
import com.bage.im.msgmodel.BageVideoContent;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 2020-03-01 17:32
 * ui kit
 */
public class BageUIKitApplication {
    int totalMsgCount = 0;
    public String chattingChannelID;
    public SensitiveWords sensitiveWords;
    public boolean isRefreshChatActivityMessage = false;

    private BageUIKitApplication() {
    }

    private static class KitApplicationBinder {
        private static final BageUIKitApplication uikit = new BageUIKitApplication();
    }

    public static BageUIKitApplication getInstance() {
        return KitApplicationBinder.uikit;
    }

    private WeakReference<Application> mContext;

    public void init(Application mContext) {
        this.mContext = new WeakReference<>(mContext);
        initIM();
        //初始化im事件及监听
        BageIMUtils.getInstance().initIMListener();
        initKitModuleListener();
        String json = BageSharedPreferencesUtil.getInstance().getSP("bage_sensitive_words");
        if (!TextUtils.isEmpty(json)) {
            sensitiveWords = JSON.parseObject(json, SensitiveWords.class);
        }
        MsgModel.getInstance().syncSensitiveWords();
        ProhibitWordModel.Companion.getInstance().sync();
        MsgModel.getInstance().deleteFlameMsg();
    }

    public Context getContext() {
        return mContext.get();
    }


    public void initIM() {
        if (!TextUtils.isEmpty(BageConfig.getInstance().getToken())) {
            //设置开发模式
            BageIM.getInstance().setDebug(BageBinder.isDebug);
            BageIM.getInstance().setFileCacheDir("bageIMFile");

            String imToken = BageConfig.getInstance().getImToken();
            String uid = BageConfig.getInstance().getUid();
            BageIM.getInstance().init(mContext.get(), uid, imToken);

            // CrashReport.initCrashReport(getContext(), "b8bf09f25f", false);
            try {
            CrashReport.setUserId(BageConfig.getInstance().getUid());
            CrashReport.setDeviceModel(getContext(), BageDeviceUtils.getInstance().getSystemModel());
        } catch (Exception e) {
                    Log.e("BageUIKitApplication", "Failed to set Bugly user info", e);
                }

        }
    }

    public void startChat() {
        if (!TextUtils.isEmpty(BageConfig.getInstance().getToken())) {
            Log.e("去连接", "-->");
            BageIMKeepAliveService.start(mContext.get());
            BageIM.getInstance().getConnectionManager().connection();
        }
    }

    public void stopConn() {
        EndpointManager.getInstance().invoke("push_update_device_badge", totalMsgCount);
        BageIM.getInstance().getConnectionManager().disconnect(false);
    }

    private void initKitModuleListener() {
        // 注册消息model到sdk
        BageIM.getInstance().getMsgManager().registerContentMsg(BageCardContent.class);

        // Override the SDK's image decoder while keeping the standard image type.
        BageChatImageContent.registerDecoder();


        BageIM.getInstance().getMsgManager().registerContentMsg(BageMultiForwardContent.class);
        //添加消息item
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.sensitiveWordsTips, new BageSensitiveWordsProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.noRelation, new BageNoRelationProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.msgPromptNewMsg, new BagePromptNewMsgProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.Bage_TEXT, new BageTextProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.Bage_IMAGE, new BageImageProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.emptyView, new BageEmptyProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.spanEmptyView, new BageSpanEmptyProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.Bage_VOICE, new BageVoiceProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.Bage_CARD, new BageCardProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.Bage_MULTIPLE_FORWARD, new BageMultiForwardProvider());
        BageMsgItemViewManager.getInstance().addChatItemViewProvider(BageContentType.loading, new LoadingProvider());
        // 设置消息长按选项
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.Bage_TEXT, object -> new MsgConfig(true));
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.Bage_IMAGE, object -> new MsgConfig(true));
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.Bage_CARD, object -> new MsgConfig(true));
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.Bage_VOICE, object -> new MsgConfig(true));
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.Bage_MULTIPLE_FORWARD, object -> new MsgConfig(true));
        EndpointManager.getInstance().setMethod("uikit_sql", EndpointCategory.bageDBMenus, object -> new DBMenu("uikit_sql"));
        //注册消息长按菜单配置
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.Bage_VOICE, object -> new MsgConfig(false, true, true, false, false, false));
        EndpointManager.getInstance().setMethod(EndpointCategory.msgConfig + BageContentType.typing, object -> new MsgConfig(false));
        EndpointManager.getInstance().setMethod("", EndpointCategory.bageChatPopupItem, 90, object -> {
            BageMsg bageMsg = (BageMsg) object;
            if (bageMsg.type == BageContentType.Bage_TEXT) {
                return new ChatItemPopupMenu(R.mipmap.msg_copy, getContext().getString(R.string.copy), (msg, iConversationContext) -> {
                    BageTextContent textContent = (BageTextContent) msg.baseContentMsgModel;
                    String content = textContent.content;
                    if (msg.remoteExtra.contentEditMsgModel != null) {
                        content = msg.remoteExtra.contentEditMsgModel.getDisplayContent();
                    }
                    ClipboardManager cm = (ClipboardManager) iConversationContext.getChatActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData mClipData = ClipData.newPlainText("Label", content);
                    assert cm != null;
                    cm.setPrimaryClip(mClipData);
                    BageToastUtils.getInstance().showToastNormal(iConversationContext.getChatActivity().getString(R.string.copyed));
                });
            }
            return null;
        });

        EndpointManager.getInstance().setMethod("favorite_item", EndpointCategory.bageChatPopupItem, 80, object -> {
            if (!(object instanceof BageMsg msg) || msg.type != BageContentType.Bage_TEXT) return null;
            return new ChatItemPopupMenu(com.chat.base.R.mipmap.msg_fave,
                    getContext().getString(R.string.message_favorite), (favoriteMsg, conversationContext) ->
                    MsgModel.getInstance().favoriteMessage(favoriteMsg, true, (code, message) -> {
                        Context context = conversationContext.getChatActivity();
                        if (code == HttpResponseCode.success) {
                            BageToastUtils.getInstance().showToastNormal(context.getString(R.string.favorited));
                        } else {
                            BageToastUtils.getInstance().showToastFail(TextUtils.isEmpty(message)
                                    ? context.getString(R.string.favorite_failed) : message);
                        }
                    }));
        });

        //添加个人中心
        EndpointManager.getInstance().setMethod("personal_center_currency", EndpointCategory.personalCenter, 600, object -> new PersonalInfoMenu(R.mipmap.icon_setting, EndpointLocaleHelper.getString(object, R.string.currency), () -> {
            Intent intent = new Intent(mContext.get(),  SettingActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod("personal_center_new_msg_notice", EndpointCategory.personalCenter, 800, object -> new PersonalInfoMenu(R.mipmap.icon_notice, EndpointLocaleHelper.getString(object, R.string.new_msg_notice), () -> {
            Intent intent = new Intent(mContext.get(), MsgNoticesSettingActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod("personal_center_mass_message", EndpointCategory.personalCenter, 700, object -> new PersonalInfoMenu(R.drawable.icon_mass_message, EndpointLocaleHelper.getString(object, R.string.mass_message), () -> {
            Intent intent = new Intent(mContext.get(), MassMessageSelectActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod("personal_center_favorite", EndpointCategory.personalCenter, 680, object -> new PersonalInfoMenu(R.drawable.icon_message_favorite, EndpointLocaleHelper.getString(object, R.string.message_favorite), () -> {
            Intent intent = new Intent(mContext.get(), FavoriteMessageListActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod("personal_center_my_reports", EndpointCategory.personalCenter, 660, object -> new PersonalInfoMenu(R.drawable.icon_my_report, EndpointLocaleHelper.getString(object, R.string.my_reports), () -> {
            Intent intent = new Intent(mContext.get(), MyReportsActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod("personal_center_web_login", EndpointCategory.personalCenter, 1800, object -> new PersonalInfoMenu(R.mipmap.icon_web_login, EndpointLocaleHelper.getString(object, R.string.web_login), () -> EndpointManager.getInstance().invoke("show_web_login_desc", mContext.get())));

        //添加通讯录
        EndpointManager.getInstance().setMethod(EndpointCategory.mailList + "_friends", EndpointCategory.mailList, 100, object -> new ContactsMenu("friend", R.mipmap.ic_new_friends, EndpointLocaleHelper.getString(object, R.string.new_friends), () -> {
            Intent intent = new Intent(mContext.get(), NewFriendsActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod(EndpointCategory.mailList + "_groups", EndpointCategory.mailList, 90, object -> new ContactsMenu("group", R.mipmap.ic_group_chat, EndpointLocaleHelper.getString(object, R.string.saved_groups), () -> {
            Intent intent = new Intent(mContext.get(), SavedGroupsActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));

        // 添加聊天工具栏菜单语音
        EndpointManager.getInstance().setMethod(EndpointCategory.bageChatToolBar + "_voice", EndpointCategory.bageChatToolBar, 97, object -> {
            IConversationContext iConversationContext = (IConversationContext) object;
            View voiceView = BageVoiceViewManager.getInstance().getVoiceView(iConversationContext);
            return new ChatToolBarMenu("bage_chat_toolbar_voice", R.mipmap.icon_chat_toolbar_voice, R.mipmap.icon_chat_toolbar_voice, voiceView, (isSelected, iConversationContext14) -> {
                // TODO: 1/1/21
            });
        });
        //聊天工具栏相册
        EndpointManager.getInstance().setMethod(EndpointCategory.bageChatToolBar + "_album", EndpointCategory.bageChatToolBar, 99, object -> new ChatToolBarMenu("bage_chat_toolbar_album", R.mipmap.icon_chat_toolbar_album, -1, null, (isSelected, iConversationContext1) -> {
            if (isSelected) {
                chooseIMG(iConversationContext1);
            }
        }));
        //聊天工具栏@
        EndpointManager.getInstance().setMethod(EndpointCategory.bageChatToolBar + "_remind", EndpointCategory.bageChatToolBar, 96, object
                -> {
            IConversationContext iConversationContext = (IConversationContext) object;
            if (iConversationContext.getChatChannelInfo().channelType == BageChannelType.PERSONAL)
                return null;
            return new ChatToolBarMenu("bage_chat_toolbar_remind", R.mipmap.icon_chat_toolbar_aite, -1, null, (isSelected, iConversationContext12) -> {

            });
        });

        // 添加聊天工具栏菜单
        // 更多：功能菜单已常驻输入栏下方，此处仅保留工具栏入口（如新图提示）
        EndpointManager.getInstance().setMethod(EndpointCategory.bageChatToolBar + "_more", EndpointCategory.bageChatToolBar, 40, object -> {
            IConversationContext iConversationContext = (IConversationContext) object;
            return new ChatToolBarMenu("bage_chat_toolbar_more", R.mipmap.icon_chat_toolbar_more, R.mipmap.icon_chat_toolbar_more, null, (isSelected, iConversationContext13) -> {
            });
        });
        //添加聊天功能面板
        EndpointManager.getInstance().setMethod(EndpointCategory.chatFunction + "_chooseImg", EndpointCategory.chatFunction, 100, object -> new ChatFunctionMenu("chooseImg", R.mipmap.ic_send_image, mContext.get().getString(R.string.image), this::chooseIMG));
        EndpointManager.getInstance().setMethod(EndpointCategory.chatFunction + "_chooseCard", EndpointCategory.chatFunction, 95, object -> new ChatFunctionMenu("chooseCard", R.mipmap.ic_send_contact, mContext.get().getString(R.string.card), IConversationContext::sendCardMsg));

        //添加tab页
        EndpointManager.getInstance().setMethod(EndpointCategory.tabMenus + "_start_chat", EndpointCategory.tabMenus, 200, object -> new PopupMenuItem(mContext.get().getString(R.string.start_group_chat), R.mipmap.ic_send_new_msg, () -> {
            Intent intent = new Intent(mContext.get(), ChooseContactsActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));
        EndpointManager.getInstance().setMethod(EndpointCategory.tabMenus + "_add_friends", EndpointCategory.tabMenus, 99, object -> new PopupMenuItem(mContext.get().getString(R.string.add_friends), R.mipmap.ic_add_new_friend, () -> {
            Intent intent = new Intent(mContext.get(), AddFriendsActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
        }));

        //显示聊天页面
        EndpointManager.getInstance().setMethod(EndpointSID.chatView, object -> {
            if (object instanceof ChatViewMenu chatViewMenu) {
                if (!TextUtils.isEmpty(chatViewMenu.channelID)) {
                    BageIMUtils.getInstance().startChatActivity(chatViewMenu);
                }
            }
            return null;
        });

        //撤回消息
        EndpointManager.getInstance().setMethod("chat_withdraw_msg", object -> {
            final WithdrawMsgMenu withdrawMsgMenu = (WithdrawMsgMenu) object;
            if (withdrawMsgMenu != null) {
                MsgModel.getInstance().revokeMsg(withdrawMsgMenu.message_id, withdrawMsgMenu.channel_id, withdrawMsgMenu.channel_type, withdrawMsgMenu.client_msg_no, (code, msg) -> {
                    if (code != HttpResponseCode.success) {
                        BageToastUtils.getInstance().showToastNormal(msg);
                        //  BageIM.getInstance().getMsgManager().updateMsgRevokeWithMessageID(withdrawMsgMenu.message_id, 1);
//                        BageIM.getInstance().getMessageManager().deleteMsgByClientMsgNo(client_msg_no);
                    }
                });
            }
            return null;
        });
        EndpointManager.getInstance().setMethod("str_delete_msg", object -> {
            BageMsg msg = (BageMsg) object;
            if (msg != null) {
                List<BageMsg> list = new ArrayList<>();
                list.add(msg);
                MsgModel.getInstance().deleteMsg(list, null);
            }
            return null;
        });
        //选择会话
        EndpointManager.getInstance().setMethod(EndpointSID.showChooseChatView, object -> {
            ChooseChatMenu messageContent = (ChooseChatMenu) object;
            Intent intent = new Intent(mContext.get(), ChooseChatActivity.class);
            intent.putExtra("isChoose", true);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
            BageUIKitApplication.this.messageContentList = messageContent.list;
            BageUIKitApplication.this.chooseChatCallBack = messageContent.mChatChooseContacts;
            return null;
        });

        //处理扫一扫结果
        EndpointManager.getInstance().setMethod("", EndpointCategory.bageScan, object -> new ScanResultMenu(hashMap -> {
            String type = Objects.requireNonNull(hashMap.get("type")).toString();
            if (type.equals("userInfo")) {
                JSONObject dataJson = (JSONObject) hashMap.get("data");
                if (dataJson != null && dataJson.has("uid")) {
                    String uid = dataJson.optString("uid");
                    String verCode = dataJson.optString("vercode");
                    if (!TextUtils.isEmpty(uid)) {
                        Intent intent = new Intent(mContext.get(), UserDetailActivity.class);
                        intent.putExtra("uid", uid);
                        intent.putExtra("vercode", verCode);
                        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                        mContext.get().startActivity(intent);
                    }
                }
                return true;
            } else return false;

        }));
        //选择联系人
        EndpointManager.getInstance().setMethod("choose_contacts", object -> {
            Intent intent = new Intent(mContext.get(), ChooseContactsActivity.class);
            intent.putExtra("type", 2);
            this.contactsMenu = (ChooseContactsMenu) object;
            if (contactsMenu != null) {
                intent.putParcelableArrayListExtra("defaultSelected", (ArrayList<? extends Parcelable>) contactsMenu.defaultSelected);
                intent.putExtra("isShowSaveLabelDialog", contactsMenu.isShowSaveLabelDialog);
                if (BageReader.isNotEmpty(contactsMenu.defaultSelected) && !contactsMenu.isCanDeselect) {
                    String unSelectUids = "";
                    for (int i = 0, size = contactsMenu.defaultSelected.size(); i < size; i++) {
                        if (TextUtils.isEmpty(unSelectUids)) {
                            unSelectUids = contactsMenu.defaultSelected.get(i).channelID;
                        } else
                            unSelectUids = unSelectUids + "," + contactsMenu.defaultSelected.get(i).channelID;
                    }
                    intent.putExtra("unSelectUids", unSelectUids);
                }
            }
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
            return null;
        });
        EndpointManager.getInstance().setMethod("exit_login", object -> {
            exitLogin(0);
            return null;
        });
        //查看用户详情
        EndpointManager.getInstance().setMethod(EndpointSID.userDetailView, object -> {
            UserDetailMenu bageUserDetailMenu = (UserDetailMenu) object;
            if (bageUserDetailMenu != null) {
                if (!TextUtils.isEmpty(bageUserDetailMenu.uid)) {
                    Intent intent = new Intent(mContext.get(), UserDetailActivity.class);
                    intent.putExtra("uid", bageUserDetailMenu.uid);
                    if (!TextUtils.isEmpty(bageUserDetailMenu.groupID)) {
                        intent.putExtra("groupID", bageUserDetailMenu.groupID);
                    }
                    bageUserDetailMenu.context.startActivity(intent);
                }

            }
            return null;
        });

        EndpointManager.getInstance().setMethod("show_tab_main", object -> {
            Intent intent = new Intent(mContext.get(), TabActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
            return null;
        });
        //监听登录状态
        EndpointManager.getInstance().setMethod("", EndpointCategory.loginMenus, object -> new LoginMenu(() -> {
            Log.e("接受登录", "-->3");
            BageSharedPreferencesUtil.getInstance().putInt("bage_lock_screen_pwd_count", 5);
            BageSharedPreferencesUtil.getInstance().putBoolean("sync_friend", true);
            //初始化im
            BageUIKitApplication.getInstance().initIM();
            //初始化密钥
//            BageIM.getInstance().getSignalProtocolManager().init();
            UserInfoEntity userInfo = BageConfig.getInstance().getUserInfo();
            if (userInfo != null) {
                BageIM.getInstance().getCMDManager().setRSAPublicKey(userInfo.rsa_public_key);
                BageIM.getInstance().getChannelManager().updateAvatarCacheKey(userInfo.uid, BageChannelType.PERSONAL, UUID.randomUUID().toString().replaceAll("-", ""));
            }
            Intent intent = new Intent(mContext.get(), TabActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.get().startActivity(intent);
            startChat();
            ProhibitWordModel.Companion.getInstance().sync();
            MsgModel.getInstance().deleteFlameMsg();
            //更新文件传输助手时间
            // BageIM.getInstance().getConversationManager().updateLastMsgTime(BageSystemAccount.system_file_helper, BageChannelType.PERSONAL, TimeUtils.getInstance().getCurrentSeconds());
        }));

        EndpointManager.getInstance().setMethod("syncExtraMsg", object -> {
            if (object != null) {
                BageChannel channel = (BageChannel) object;
                MsgModel.getInstance().syncExtraMsg(channel.channelID, channel.channelType);
            }
            return null;
        });

        EndpointManager.getInstance().setMethod("deleteRemoteMsg", object -> {
            if (object instanceof String clientMsgNo) {
                BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                if (msg != null) {
                    List<BageMsg> list = new ArrayList<>();
                    list.add(msg);
                    MsgModel.getInstance().deleteMsg(list, null);
                }
            }
            return null;
        });
        EndpointManager.getInstance().setMethod("get_chat_uid_msg", object -> {
            if (object instanceof BageMsg2UiMsgMenu bageMsg2UiMsgMenu) {
                return BageIMUtils.getInstance().msg2UiMsg(bageMsg2UiMsgMenu.getIConversationContext(), bageMsg2UiMsgMenu.getBageMsg(), bageMsg2UiMsgMenu.getMemberCount(), bageMsg2UiMsgMenu.getShowNickName(), bageMsg2UiMsgMenu.isChoose());
            }
            return null;
        });

        // 搜索消息按群成员搜索
        EndpointManager.getInstance().setMethod("search_message_with_member", EndpointCategory.bageSearchChatContent, 101, object -> {
            if (object instanceof BageChannel) {
                if (((BageChannel) object).channelType == BageChannelType.GROUP) {
                    return new SearchChatContentMenu(BageBaseApplication.getInstance().getContext().getString(R.string.uikit_search_member), (channelID, channelType) -> {
                        Intent intent = new Intent(BageBaseApplication.getInstance().getContext(), BageAllMembersActivity.class);
                        intent.putExtra("channelID", ((BageChannel) object).channelID);
                        intent.putExtra("channelType", BageChannelType.GROUP);
                        intent.putExtra("searchMessage", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        BageBaseApplication.getInstance().getContext().startActivity(intent);
                    });
                }
            }
            return null;
        });

        // 搜索消息按日期搜索
        EndpointManager.getInstance().setMethod("search_message_with_date", EndpointCategory.bageSearchChatContent, 96, object -> {
            if (object instanceof BageChannel) {
                return new SearchChatContentMenu(BageBaseApplication.getInstance().getContext().getString(R.string.uikit_search_for_date), (channelID, channelType) -> {
                    Intent intent = new Intent(BageBaseApplication.getInstance().getContext(), SearchWithDateActivity.class);
                    intent.putExtra("channel_id", ((BageChannel) object).channelID);
                    intent.putExtra("channel_type", ((BageChannel) object).channelType);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    BageBaseApplication.getInstance().getContext().startActivity(intent);
                });
            }
            return null;
        });


        // 搜索消息按图片搜索
        EndpointManager.getInstance().setMethod("search_message_with_img", EndpointCategory.bageSearchChatContent, 98, object -> {
            if (object instanceof BageChannel) {
                return new SearchChatContentMenu(BageBaseApplication.getInstance().getContext().getString(R.string.uikit_search_for_image), (channelID, channelType) -> {
                    Intent intent = new Intent(BageBaseApplication.getInstance().getContext(), SearchWithImgActivity.class);
                    intent.putExtra("channel_id", ((BageChannel) object).channelID);
                    intent.putExtra("channel_type", ((BageChannel) object).channelType);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    BageBaseApplication.getInstance().getContext().startActivity(intent);
                });
            }
            return null;
        });

    }

    public void sendChooseChatBack(List<BageChannel> list) {
        if (chooseChatCallBack != null) {
            chooseChatCallBack.iChoose.onResult(list);
            chooseChatCallBack = null;
        }
    }

    public List<BageMessageContent> getMessageContentList() {
        return messageContentList;
    }

    public void setChooseContactsBack(List<BageChannel> list) {
        if (contactsMenu != null) {
            contactsMenu.iChooseBack.onBack(list);
            contactsMenu = null;
        }
    }

    private ChatChooseContacts chooseChatCallBack;
    private ChooseContactsMenu contactsMenu;
    private List<BageMessageContent> messageContentList;

    public void exitLogin(int from) {
        MsgModel.getInstance().stopTimer();
        BageIMKeepAliveService.stop(mContext.get());
        EndpointManager.getInstance().invoke("bage_logout", null);
        BageConfig.getInstance().clearInfo();
        BageIM.getInstance().getConnectionManager().disconnect(true);
        ActManagerUtils.getInstance().clearAllActivity();
        EndpointManager.getInstance().invoke("main_show_home_view", from);
        //关闭UI层数据库
        BageBaseApplication.getInstance().closeDbHelper();

    }

    private void chooseIMG(IConversationContext iConversationContext) {
        String[] permissionStr = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
//        String permissionStr = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT >= 33) {
//            permissionStr = Manifest.permission.READ_MEDIA_IMAGES;
            permissionStr = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO};
        }
        String desc = String.format(iConversationContext.getChatActivity().getString(R.string.album_permissions_desc), iConversationContext.getChatActivity().getString(R.string.app_name));
        BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
            @Override
            public void onResult(boolean result) {
                ChooseMimeType mimeType = ChooseMimeType.img;
                if (result) {
                    Object isRegisterVideo = EndpointManager.getInstance().invoke("is_register_video", null);
                    if (isRegisterVideo instanceof Boolean) {
                        boolean isRegister = (boolean) isRegisterVideo;
                        if (isRegister) {
                            mimeType = ChooseMimeType.all;
                        }
                    }
                    GlideUtils.getInstance().chooseIMG(iConversationContext.getChatActivity(), 9, true, mimeType, true, new GlideUtils.ISelectBack() {
                        @Override
                        public void onBack(List<ChooseResult> paths) {
                            if (paths.size() == 1 && paths.get(0).model == ChooseResultModel.video) {
//                                EndpointManager.getInstance().invoke("videoCompress",paths.get(0).path);
                                BageVideoContent videoContent = new BageVideoContent();
                                videoContent.coverLocalPath = BageMediaFileUtils.getInstance().getVideoCover(paths.get(0).path);
                                videoContent.localPath = paths.get(0).path;
                                videoContent.second = BageMediaFileUtils.getInstance().getVideoTime(paths.get(0).path) / 1000;
                                videoContent.size = BageFileUtils.getInstance().getFileSize(paths.get(0).path);
                                iConversationContext.sendMessage(videoContent);
                                return;
                            }

                            for (int i = 0, size = paths.size(); i < size; i++) {
                                String path = paths.get(i).path;
                                if (paths.get(i).model == ChooseResultModel.video) {
                                    BageVideoContent videoContent = new BageVideoContent();
                                    videoContent.coverLocalPath = BageMediaFileUtils.getInstance().getVideoCover(path);
                                    videoContent.localPath = path;
                                    videoContent.second = BageMediaFileUtils.getInstance().getVideoTime(path) / 1000;
                                    videoContent.size = BageFileUtils.getInstance().getFileSize(path);
                                    iConversationContext.sendMessage(videoContent);
                                } else {
                                    if (BageFileUtils.getInstance().isGif(path)) {
                                        Object isRegisterSticker = EndpointManager.getInstance().invoke("is_register_sticker", null);
                                        if (isRegisterSticker instanceof Boolean) {
                                            BageGifContent mGifContent = new BageGifContent();
                                            mGifContent.format = "gif";
                                            mGifContent.localPath = path;
                                            Bitmap bitmap = BitmapFactory.decodeFile(path);
                                            if (bitmap != null) {
                                                mGifContent.height = bitmap.getHeight();
                                                mGifContent.width = bitmap.getWidth();
                                            }
                                            iConversationContext.sendMessage(mGifContent);
                                            return;
                                        }
                                    }
                                    BageImageContent imageContent = new BageChatImageContent(path);
                                    iConversationContext.sendMessage(imageContent);

                                }

                            }
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
                }
            }

            @Override
            public void clickResult(boolean isCancel) {
            }
        }, iConversationContext.getChatActivity(), desc, permissionStr);
    }

    public interface IShowChatConfirm {
        void onBack(@NonNull List<BageChannel> list, @NonNull List<BageMessageContent> messageContentList);
    }

    public void showChatConfirmDialog(@NonNull Context context, @NonNull List<BageChannel> list, @NonNull List<BageMessageContent> messageContentList, final IShowChatConfirm iShowChatConfirm) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_confirm_dialog_view, null, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        AvatarView avatarView = view.findViewById(R.id.avatarView);
        TextView nameTv = view.findViewById(R.id.nameTv);
        ImageView imageView = view.findViewById(R.id.imageView);
        TextView contentTv = view.findViewById(R.id.contentTv);
        if (list.size() == 1) {
            avatarView.showAvatar(list.get(0));
            String showName = list.get(0).channelRemark;
            if (TextUtils.isEmpty(showName)) showName = list.get(0).channelName;
            if (list.get(0).channelID.equals(BageSystemAccount.system_file_helper)) {
                showName = context.getString(R.string.bage_file_helper);
            }
            if (list.get(0).channelID.equals(BageSystemAccount.system_team)) {
                showName = context.getString(R.string.bage_system_notice);
            }
            nameTv.setText(showName);
            recyclerView.setVisibility(View.GONE);
            avatarView.setVisibility(View.VISIBLE);
            nameTv.setVisibility(View.VISIBLE);
        } else {
            class AvatarViewHolder extends RecyclerView.ViewHolder {
                final AvatarView avatarView;

                public AvatarViewHolder(@NonNull View itemView) {
                    super(itemView);
                    avatarView = itemView.findViewWithTag("avatar");
                }
            }
            recyclerView.setLayoutManager(new GridLayoutManager(context, 5));
            recyclerView.setAdapter(new RecyclerView.Adapter<AvatarViewHolder>() {
                @NonNull
                @Override
                public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    LinearLayout view1 = new LinearLayout(parent.getContext());
                    AvatarView avatarView1 = new AvatarView(parent.getContext());
                    avatarView1.setTag("avatar");
                    view1.addView(avatarView1, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 5, 5, 5));
                    return new AvatarViewHolder(view1);
                }

                @Override
                public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
                    holder.avatarView.setSize(40);
                    holder.avatarView.showAvatar(list.get(position));
                }

                @Override
                public int getItemCount() {
                    return list.size();
                }
            });
            nameTv.setVisibility(View.GONE);
            avatarView.setVisibility(View.GONE);
            contentTv.setVisibility(View.GONE);
        }

        if (messageContentList.size() == 1) {
            BageMessageContent messageContent = messageContentList.get(0);
            if (messageContent.type == BageContentType.Bage_IMAGE) {
                BageImageContent imgMsgModel = (BageImageContent) messageContent;
                ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                int[] ints = ImageUtils.getInstance().getImageWidthAndHeightToTalk(imgMsgModel.width, imgMsgModel.height);
                layoutParams.height = ints[1];
                layoutParams.width = ints[0];
                imageView.setLayoutParams(layoutParams);
                String showUrl;
                if (!TextUtils.isEmpty(imgMsgModel.localPath)) {
                    showUrl = imgMsgModel.localPath;
                    File file = new File(showUrl);
                    if (!file.exists()) {
                        //如果本地文件被删除就显示网络图片
                        showUrl = BageApiConfig.getShowUrl(imgMsgModel.url);
                    }
                } else {
                    showUrl = BageApiConfig.getShowUrl(imgMsgModel.url);
                }
                GlideUtils.getInstance().showImg(context, showUrl, ints[0], ints[1], imageView);
                imageView.setVisibility(View.VISIBLE);
                contentTv.setVisibility(View.GONE);
            } else {
                String content = messageContent.getDisplayContent();
                if (messageContent.type == BageContentType.Bage_CARD) {
                    BageCardContent BageCardContent = (BageCardContent) messageContent;
                    content = content + BageCardContent.name;
                }
                contentTv.setText(content);
                imageView.setVisibility(View.GONE);
                contentTv.setVisibility(View.VISIBLE);
            }
        } else {
            imageView.setVisibility(View.GONE);
            contentTv.setVisibility(View.VISIBLE);
            contentTv.setText(String.format(context.getString(R.string.item_forward_count), messageContentList.size()));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.send_to));

        builder.setView(view);
        builder.setPositiveButton(context.getString(R.string.sure), (dialog, which) -> iShowChatConfirm.onBack(list, messageContentList));
        builder.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> {

        });

        AlertDialog dialog = builder.create();
        dialog.setBlurParams(1f, true, true);
        dialog.show();
        TextView sureTv = (TextView) dialog.getButton(Dialog.BUTTON_POSITIVE);
        sureTv.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));

    }
}
