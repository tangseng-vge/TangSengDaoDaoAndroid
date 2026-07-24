package com.chat.uikit.chat;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import static com.chat.advanced.utils.EditUtilKt.checkEditTime;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageBinder;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.emoji.MoonUtil;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.AvatarOtherViewMenu;
import com.chat.base.endpoint.entity.CallingViewMenu;
import com.chat.base.endpoint.entity.RTCMenu;
import com.chat.base.endpoint.entity.ReadMsgMenu;
import com.chat.base.endpoint.entity.SetChatBgMenu;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.entity.UserOnlineStatus;
import com.chat.base.entity.BageChannelCustomerExtras;
import com.chat.base.entity.BageGroupType;
import com.chat.base.msg.ChatAdapter;
import com.chat.base.msg.ChatContentSpanType;
import com.chat.base.msg.IConversationContext;
import com.chat.base.msgitem.BageChannelMemberRole;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageUIChatMsgItemEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.NumberTextView;
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan;
import com.chat.base.utils.ActManagerUtils;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.UserUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BagePermissions;
import com.chat.base.utils.BagePlaySound;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.BageToastUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.base.utils.systembar.BageStatusBarUtils;
import com.chat.base.views.CommonAnim;
import com.chat.base.views.swipeback.SwipeBackActivity;
import com.chat.base.views.swipeback.SwipeBackLayout;
import com.chat.uikit.R;
import com.chat.uikit.BageUIKitApplication;
import com.chat.uikit.chat.manager.SendMsgEntity;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.chat.manager.BageSendMsgUtils;
import com.chat.uikit.chat.msgmodel.BageCardContent;
import com.chat.uikit.contacts.ChooseContactsActivity;
import com.chat.uikit.databinding.ActChatLayoutBinding;
import com.chat.uikit.group.ChooseVideoCallMembersActivity;
import com.chat.uikit.group.GroupDetailActivity;
import com.chat.uikit.group.service.GroupModel;
import com.chat.uikit.message.MsgModel;
import com.chat.uikit.robot.service.BageRobotModel;
import com.chat.uikit.user.service.UserModel;
import com.chat.uikit.view.BagePlayVoiceUtils;
import com.effective.android.panel.PanelSwitchHelper;
import com.effective.android.panel.interfaces.ContentScrollMeasurer;
import com.effective.android.panel.interfaces.listener.OnPanelChangeListener;
import com.effective.android.panel.view.panel.IPanelView;
import com.bage.im.BageIM;
import com.bage.im.entity.BageCMD;
import com.bage.im.entity.BageCMDKeys;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelState;
import com.bage.im.entity.BageChannelStatus;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageConversationMsgExtra;
import com.bage.im.entity.BageMentionType;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgReaction;
import com.bage.im.entity.BageReminder;
import com.bage.im.entity.BageSendOptions;
import com.bage.im.interfaces.IGetOrSyncHistoryMsgBack;
import com.bage.im.message.type.BageConnectStatus;
import com.bage.im.message.type.BageSendMsgResult;
import com.bage.im.msgmodel.BageImageContent;
import com.chat.base.msgmodel.BageChatImageContent;
import com.bage.im.msgmodel.BageMessageContent;
import com.bage.im.msgmodel.BageMsgEntity;
import com.bage.im.msgmodel.BageReply;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ChatActivity extends SwipeBackActivity implements IConversationContext {
    private String channelId = "";
    private byte channelType = BageChannelType.PERSONAL;
    private ChatAdapter chatAdapter;
    //是否在查看历史消息
    private boolean isShowHistory;
    private boolean isSyncLastMsg = false;
    private boolean isToEnd = true;
    private boolean isViewingPicture = false;
    private final boolean showNickName = true; // 是否显示聊天昵称
    private long lastPreviewMsgOrderSeq = 0; //上次浏览消息
    private long unreadStartMsgOrderSeq = 0; //新消息开始位置
    private long tipsOrderSeq = 0; //需要强提示的msg
    private int keepOffsetY = 0; // 上次浏览消息的偏移量
    private int redDot = 0; // 未读消息数量
    private int lastVisibleMsgSeq = 0; // 最后可见消息序号
    private int maxMsgSeq = 0;
    private long maxMsgOrderSeq = 0;
    //回复的消息对象
    private BageMsg replyBageMsg;
    // 编辑对象
    private BageMsg editMsg;
    // 群成员数量
    private int count;
    private int groupType = BageGroupType.normalGroup;
    //已读消息ID
    private final List<String> readMsgIds = new ArrayList<>();
    private Disposable disposable;
    private boolean isUploadReadMsg = true;
    private NumberTextView numberTextView;
    //    boolean isUpdateCoverMsg = false;
    private boolean isCanLoadMore;
    boolean isRefreshLoading = false;
    boolean isMoreLoading = false;
    boolean isCanRefresh = true;
    private boolean isShowChatActivity = true;
    LinearLayoutManager linearLayoutManager;
    private final List<BageReminder> reminderList = new ArrayList<>();
    private final List<BageReminder> groupApproveList = new ArrayList<>();
    private final List<Long> reminderIds = new ArrayList<>();
    private long browseTo = 0;
    private boolean isUpdateRedDot = true;
    private ImageView callIV;
    //查询聊天数据偏移量
    private final int limit = 30;
    private int initialMessageRequestGeneration = 0;
    private boolean isShowPinnedView = false;
    private boolean isShowCallingView = false;
    private boolean isTipMessage = false;
    private int hideChannelAllPinnedMessage = 0;
    private PanelSwitchHelper mHelper;
    private ChatPanelManager chatPanelManager;
    private ActChatLayoutBinding bageVBinding;
    private int unfilledHeight = 0;
    private final String loginUID = BageConfig.getInstance().getUid();
    private final int callingViewHeight = AndroidUtilities.dp(40f);
    private final int pinnedViewHeight = AndroidUtilities.dp(50f);

    private int getTopPinViewHeight() {
        int totalHeight = 0;
        if (isShowCallingView) {
            totalHeight += callingViewHeight;
        }
        if (isShowPinnedView) {
            totalHeight += pinnedViewHeight;
        }
        return totalHeight;
    }

    private void p2pCall(int callType) {
        EndpointManager.getInstance().invoke("bage_p2p_call", new RTCMenu(this, callType));
    }

    private void toggleStatusBarMode() {
        Window window = getWindow();
        if (window == null) return;
        BageStatusBarUtils.transparentStatusBar(window);
        if (!Theme.getDarkModeStatus(this))
            BageStatusBarUtils.setDarkMode(window);
        else BageStatusBarUtils.setLightMode(window);
    }

    private void initParam() {
        toggleStatusBarMode();
        //频道ID
        channelId = getIntent().getStringExtra("channelId");
        //频道类型
        channelType = getIntent().getByteExtra("channelType", BageChannelType.PERSONAL);
        maxMsgOrderSeq = BageIM.getInstance().getMsgManager().getMaxOrderSeqWithChannel(channelId, channelType);
        maxMsgSeq = BageIM.getInstance().getMsgManager().getMaxMessageSeqWithChannel(channelId, channelType);
        resetHideChannelAllPinnedMessage();
        // 是否含有带转发的消息
        if (getIntent().hasExtra("msgContentList")) {
            List<BageMessageContent> msgContentList = getIntent().getParcelableArrayListExtra("msgContentList");
            if (BageReader.isNotEmpty(msgContentList)) {
                List<BageChannel> list = new ArrayList<>();
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelId, channelType);
                list.add(channel);
                BageUIKitApplication.getInstance().showChatConfirmDialog(this, list, msgContentList, (list1, messageContentList) -> {
                    List<SendMsgEntity> msgList = new ArrayList<>();
                    BageSendOptions options = new BageSendOptions();
                    options.setting.receipt = getChatChannelInfo().receipt;
                    for (int i = 0, size = msgContentList.size(); i < size; i++) {
                        msgList.add(new SendMsgEntity(msgContentList.get(i), channel, options));
                    }
                    BageSendMsgUtils.getInstance().sendMessages(msgList);
                });

            }
        }

    }

    private void initSwipeBackFinish() {
        SwipeBackLayout mSwipeBackLayout = getSwipeBackLayout();
        mSwipeBackLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
        mSwipeBackLayout.setEnableGesture(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSwipeBackFinish();
        bageVBinding = DataBindingUtil.setContentView(this, R.layout.act_chat_layout);
//        setContentView(R.layout.act_chat_layout1);
        initParam();
        initView();
        initListener();
        // 首屏消息查询先于输入面板初始化，首次安装时可并行等待网络同步。
        requestInitialMessages();
        //initData();
        ActManagerUtils.getInstance().addActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isShowChatActivity = true;
        BageUIKitApplication.getInstance().chattingChannelID = channelId;
        isUploadReadMsg = true;
        chatPanelManager.initRefreshListener();
        EndpointManager.getInstance().invoke("start_screen_shot", this);
        EndpointManager.getInstance().invoke("set_chat_bg", new SetChatBgMenu(channelId, channelType, bageVBinding.ivBg, bageVBinding.rootView, bageVBinding.blurView));

        Object addSecurityModule = EndpointManager.getInstance().invoke("add_security_module", null);
        if (addSecurityModule instanceof Boolean) {
            boolean disable_screenshot;
            String uid = BageConfig.getInstance().getUid();
            if (!TextUtils.isEmpty(uid)) {
                disable_screenshot = BageSharedPreferencesUtil.getInstance().getBoolean(uid + "_disable_screenshot", false);
            } else {
                disable_screenshot = BageSharedPreferencesUtil.getInstance().getBoolean("disable_screenshot", false);
            }
            if (disable_screenshot)
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mHelper == null) {
            mHelper = new PanelSwitchHelper.Builder(this)
                    //可选
                    .addKeyboardStateListener((visible, height) -> {
                        if (visible && height > 0) {
                            BageConstants.setKeyboardHeight(height);
                        }
                        if (visible && chatPanelManager != null) {
                            chatPanelManager.hideChatFunctionIfKeyboardVisible();
                        }
                    })
                    //可选
                    .addPanelChangeListener(new OnPanelChangeListener() {

                        @Override
                        public void onKeyboard() {
                            chatPanelManager.resetToolBar();
                            SoftKeyboardUtils.getInstance().requestFocus(bageVBinding.editText);
                            chatPanelManager.hideChatFunctionIfKeyboardVisible();
                        }

                        @Override
                        public void onNone() {
                            if (chatPanelManager != null) {
                                chatPanelManager.resetToolBar();
                            }
                        }

                        @Override
                        public void onPanel(IPanelView view) {
                        }


                        @Override
                        public void onPanelSizeChange(IPanelView panelView, boolean portrait, int oldWidth, int oldHeight, int width, int height) {
                            if (chatPanelManager != null) {
                                chatPanelManager.onPanelSizeChange(panelView, width, height);
                            }
                        }
                    }).addContentScrollMeasurer(new ContentScrollMeasurer() {
                        @Override
                        public int getScrollDistance(int i) {
                            View bottomView = findViewById(R.id.bottomView);
                            View followView = findViewById(R.id.followScrollView);
                            int panelHeight = chatPanelManager != null
                                    ? chatPanelManager.getEffectivePanelScrollHeight(i)
                                    : i;
                            return panelHeight - (bottomView.getTop() - followView.getBottom());
                        }

                        @Override
                        public int getScrollViewId() {
                            return R.id.recyclerViewLayout;
                        }
                    }).addContentScrollMeasurer(new ContentScrollMeasurer() {
                        @Override
                        public int getScrollDistance(int i) {
                            return 0;
                        }

                        @Override
                        public int getScrollViewId() {
                            return R.id.scrollViewLayout;
                        }
                    }).addContentScrollMeasurer(new ContentScrollMeasurer() {
                        @Override
                        public int getScrollDistance(int i) {
                            return 0;
                        }

                        @Override
                        public int getScrollViewId() {
                            return R.id.timeTv;
                        }
                    }).addContentScrollMeasurer(new ContentScrollMeasurer() {
                        @Override
                        public int getScrollDistance(int i) {
                            return 0;
                        }

                        @Override
                        public int getScrollViewId() {
                            return R.id.imageView;
                        }
                    }).addContentScrollMeasurer(new ContentScrollMeasurer() {
                        @Override
                        public int getScrollDistance(int i) {
                            int panelHeight = chatPanelManager != null
                                    ? chatPanelManager.getEffectivePanelScrollHeight(i)
                                    : i;
                            return panelHeight - unfilledHeight;
                        }

                        @Override
                        public int getScrollViewId() {
                            return R.id.recyclerView;
                        }
                    })
                    .logTrack(BageBinder.isDebug)
                    .build(false);
        }
        if (chatPanelManager == null) {
            FrameLayout moreView = findViewById(R.id.chatMoreLayout);
            chatPanelManager = new ChatPanelManager(mHelper, findViewById(R.id.bottomView), moreView, findViewById(R.id.followScrollView), this, () -> {
                CommonAnim.getInstance().rotateImage(bageVBinding.topLayout.backIv, 180f, 360f, R.mipmap.ic_ab_back);
                numberTextView.setNumber(0, true);
                CommonAnim.getInstance().showOrHide(numberTextView, false, true);
                CommonAnim.getInstance().showOrHide(callIV, true, true);
                return null;
            }, path -> {
                Intent intent = new Intent(ChatActivity.this, PreviewNewImgActivity.class);
                intent.putExtra("path", path);
                previewNewImgResultLac.launch(intent);
                return null;
            });
            initData();
        }
    }

    protected void initView() {
        EndpointManager.getInstance().invoke("set_chat_bg", new SetChatBgMenu(channelId, channelType, bageVBinding.ivBg, bageVBinding.rootView, bageVBinding.blurView));
        Object pinnedLayoutView = EndpointManager.getInstance().invoke("get_pinned_message_view", this);
        if (pinnedLayoutView instanceof View) {
            bageVBinding.pinnedLayout.addView((View) pinnedLayoutView);
        }
        bageVBinding.timeTv.setShadowLayer(AndroidUtilities.dp(5f), 0f, 0f, 0);
        CommonAnim.getInstance().showOrHide(bageVBinding.timeTv, false, true);
        Theme.setPressedBackground(bageVBinding.topLayout.backIv);
        bageVBinding.topLayout.backIv.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.titleBarIcon), PorterDuff.Mode.MULTIPLY));
        bageVBinding.topLayout.avatarView.setSize(40);
        bageVBinding.chatUnreadLayout.progress.setSize(40);
        bageVBinding.chatUnreadLayout.progress.setStrokeWidth(1.5f);
        bageVBinding.chatUnreadLayout.progress.setProgressColor(ContextCompat.getColor(this, R.color.popupTextColor));

        bageVBinding.chatUnreadLayout.msgCountTv.setColors(R.color.white, R.color.reminderColor);
        bageVBinding.chatUnreadLayout.remindCountTv.setColors(R.color.white, R.color.reminderColor);
        bageVBinding.chatUnreadLayout.approveCountTv.setColors(R.color.white, R.color.reminderColor);

        numberTextView = new NumberTextView(this);
        numberTextView.setTextSize(18);
        numberTextView.setTextColor(Theme.colorAccount);
        bageVBinding.topLayout.rightView.addView(numberTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.END, 0, 0, 15, 0));

        Object isRegisterRTC = EndpointManager.getInstance().invoke("is_register_rtc", null);

        callIV = new AppCompatImageView(this);
        callIV.setImageResource(R.mipmap.ic_call);
        if (isRegisterRTC instanceof Boolean) {
            boolean isRegister = (boolean) isRegisterRTC;
            if (isRegister) {
                bageVBinding.topLayout.rightView.addView(callIV, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.END, 0, 0, 15, 0));
            }
        }
        callIV.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.popupTextColor), PorterDuff.Mode.MULTIPLY));
        callIV.setBackground(Theme.createSelectorDrawable(Theme.getPressedColor()));

        CommonAnim.getInstance().showOrHide(numberTextView, false, false);

        //去除刷新条目闪动动画
        ((DefaultItemAnimator) Objects.requireNonNull(bageVBinding.recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        chatAdapter = new ChatAdapter(this, ChatAdapter.AdapterType.normalMessage);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        bageVBinding.recyclerView.setLayoutManager(linearLayoutManager);
        bageVBinding.recyclerView.setAdapter(chatAdapter);
        bageVBinding.recyclerView.setItemAnimator(new MyItemAnimator());
        chatAdapter.setAnimationFirstOnly(true);
        chatAdapter.setAnimationEnable(false);

    }

    private void initListener() {
        ItemTouchHelper helper = new ItemTouchHelper(new MessageSwipeController(this, new SwipeControllerActions() {
            @Override
            public void showReplyUI(int position) {
                showReply(chatAdapter.getData().get(position).bageMsg);
            }

            @Override
            public void hideSoft() {
                //   mHelper.resetState();
            }
        }));
        helper.attachToRecyclerView(bageVBinding.recyclerView);
        bageVBinding.topLayout.backIv.setOnClickListener(v -> setBackListener());
        callIV.setOnClickListener(view -> {
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
            if (getChatChannelInfo().forbidden == 1 || (member != null && member.forbiddenExpirationTime > 0)) {
                BageToastUtils.getInstance().showToast(getString(R.string.can_not_call_forbidden));
                return;
            }
            String desc = String.format(getString(R.string.microphone_permissions_des), getString(R.string.app_name));
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        if (channelType == BageChannelType.PERSONAL) {
                            if (UserUtils.getInstance().checkMyFriendDelete(channelId) || UserUtils.getInstance().checkFriendRelation(channelId)) {
                                showToast(R.string.non_friend_relationship);
                                return;
                            }
                            if (UserUtils.getInstance().checkBlacklist(channelId)) {
                                showToast(R.string.call_be_blacklist);
                                return;
                            }
                            if (getChatChannelInfo().status == BageChannelStatus.statusBlacklist) {
                                showToast(R.string.call_blacklist);
                                return;
                            }
                            List<PopupMenuItem> list = new ArrayList<>();
                            list.add(new PopupMenuItem(getString(R.string.video_call), R.mipmap.chat_calls_video, () -> p2pCall(1)));
                            list.add(new PopupMenuItem(getString(R.string.audio_call), R.mipmap.chat_calls_voice, () -> p2pCall(0)));
                            BageDialogUtils.getInstance().showScreenPopup(view, list);
                        } else {
                            BageChannelMember channelMember = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
                            if (channelMember != null && channelMember.status == BageChannelStatus.statusBlacklist) {
                                showToast(R.string.call_blacklist_group);
                                return;
                            }
                            Intent intent = new Intent(ChatActivity.this, ChooseVideoCallMembersActivity.class);
                            intent.putExtra("channelID", channelId);
                            intent.putExtra("channelType", channelType);
                            intent.putExtra("isCreate", true);
                            startActivity(intent);
                        }
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);
        });

        BageDialogUtils.getInstance().setViewLongClickPopup(bageVBinding.chatUnreadLayout.groupApproveLayout, getGroupApprovePopupItems());
        bageVBinding.chatUnreadLayout.groupApproveLayout.setOnClickListener(view -> {
            if (BageReader.isNotEmpty(groupApproveList)) {
                BageMsg msg = BageIM.getInstance().getMsgManager().getWithMessageID(groupApproveList.get(0).messageID);
                if (msg != null && !TextUtils.isEmpty(msg.clientMsgNO)) {
                    tipsMsg(msg.clientMsgNO);
                }
            }
        });
        BageDialogUtils.getInstance().setViewLongClickPopup(bageVBinding.chatUnreadLayout.remindLayout, getRemindPopupItems());
        bageVBinding.chatUnreadLayout.remindLayout.setOnClickListener(view -> {

            if (BageReader.isNotEmpty(reminderList)) {
                reminderIds.add(reminderList.get(0).reminderID);
                BageMsg msg = BageIM.getInstance().getMsgManager().getWithMessageID(reminderList.get(0).messageID);
                if (msg != null && !TextUtils.isEmpty(msg.clientMsgNO)) {
                    tipsMsg(msg.clientMsgNO);
                } else {
                    long orderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(reminderList.get(0).messageSeq, channelId, channelType);
                    unreadStartMsgOrderSeq = 0;
                    tipsOrderSeq = orderSeq;
                    getData(1, true, orderSeq, false);
                    isCanLoadMore = true;
                }
            }
        });

        SingleClickUtil.onSingleClick(bageVBinding.topLayout.titleView, view -> {
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);

            if ((member != null && member.isDeleted == 1) || channelType == BageChannelType.CUSTOMER_SERVICE)
                return;
//              SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.toolbarView.editText);
            Intent intent = new Intent(ChatActivity.this, channelType == BageChannelType.GROUP ? GroupDetailActivity.class : ChatPersonalActivity.class);
            intent.putExtra("channelId", channelId);
            startActivity(intent);
        });

        SingleClickUtil.onSingleClick(bageVBinding.topLayout.ivMoreChat, v -> {
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);

            if ((member != null && member.isDeleted == 1) || channelType == BageChannelType.CUSTOMER_SERVICE)
                return;
//              SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.toolbarView.editText);
            Intent intent = new Intent(ChatActivity.this, channelType == BageChannelType.GROUP ? GroupDetailActivity.class : ChatPersonalActivity.class);
            intent.putExtra("channelId", channelId);
            startActivity(intent);
        });

        bageVBinding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (chatAdapter.getData().size() <= 1) return;
                setShowTime();
                int lastItemPosition = linearLayoutManager.findLastVisibleItemPosition();
                if (lastItemPosition < chatAdapter.getItemCount() - 1) {
                    bageVBinding.chatUnreadLayout.newMsgLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.newMsgLayout, dy > 0 || redDot > 0, true, false));
                } else {
                    bageVBinding.chatUnreadLayout.newMsgLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.newMsgLayout, redDot > 0, true, false));
                }
                resetRemindView();
                resetGroupApproveView();

                View lastChildView = linearLayoutManager.findViewByPosition(lastItemPosition);
                if (lastChildView != null) {
                    int bottom = lastChildView.getBottom();
                    int listHeight = bageVBinding.recyclerView.getHeight() - bageVBinding.recyclerView.getPaddingBottom();
                    unfilledHeight = listHeight - bottom;
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int lastItemPosition = linearLayoutManager.findLastVisibleItemPosition();
                isShowHistory = lastItemPosition < chatAdapter.getItemCount() - 1;
                if (newState == SCROLL_STATE_IDLE) {
                    isTipMessage = false;
                    CommonAnim.getInstance().showOrHide(bageVBinding.timeTv, false, true);
                    EndpointManager.getInstance().invoke("stop_reaction_animation", null);
                    if (!bageVBinding.recyclerView.canScrollVertically(1)) { // 到达底部
                        showMoreLoading();
                    } else if (!bageVBinding.recyclerView.canScrollVertically(-1)) { // 到达顶部
                        showRefreshLoading();
                    }
                } else {
                    MsgModel.getInstance().doneReminder(reminderIds);
                    if (!isUpdateRedDot) return;
                    MsgModel.getInstance().clearUnread(channelId, channelType, redDot, (code, msg) -> {
                        if (code == HttpResponseCode.success && redDot == 0) {
                            isUpdateRedDot = false;
                        }
                    });
                }
            }
        });

        bageVBinding.chatUnreadLayout.newMsgLayout.setOnClickListener(v -> {
            redDot = 0;
            MsgModel.getInstance().clearUnread(channelId, channelType, redDot, (code, msg) -> {
                if (code == HttpResponseCode.success && redDot == 0) {
                    isUpdateRedDot = false;
                }
            });
            if (isCanLoadMore) {
                isSyncLastMsg = true;
                // chatAdapter.setList(new ArrayList<>());
                bageVBinding.chatUnreadLayout.progress.setVisibility(View.VISIBLE);
                bageVBinding.chatUnreadLayout.msgDownIv.setVisibility(View.GONE);
                unreadStartMsgOrderSeq = 0;
                lastPreviewMsgOrderSeq = 0;
                long maxSeq = BageIM.getInstance().getMsgManager().getMaxOrderSeqWithChannel(channelId, channelType);
                new Handler().postDelayed(() -> {
                    getData(0, true, maxSeq, true);
                    showUnReadCountView();
                }, 500);
            } else {
                scrollToPosition(chatAdapter.getItemCount() - 1);
                showUnReadCountView();
            }

            isShowHistory = false;
            isCanLoadMore = false;
        });

        //监听频道改变通知
        BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo(channelId, (channel, isEnd) -> {
            if (channel == null) return;
            if (channel.channelID.equals(channelId) && channel.channelType == channelType) { //同一个会话
                showChannelName(channel);
                bageVBinding.topLayout.avatarView.showAvatar(channel);
                EndpointManager.getInstance().invoke("show_avatar_other_info", new AvatarOtherViewMenu(bageVBinding.topLayout.otherLayout, channel, bageVBinding.topLayout.avatarView, true));
                //用户在线状态
                if (channel.channelType == BageChannelType.PERSONAL) {
                    setOnlineView(channel);
                } else {
                    if (channel.remoteExtraMap != null) {
                        Object memberCountObject = channel.remoteExtraMap.get(BageChannelCustomerExtras.memberCount);
                        if (memberCountObject instanceof Integer) {
                            int count = (int) memberCountObject;
                            bageVBinding.topLayout.subtitleTv.setText(String.format(getString(R.string.group_member), count));
                        }
                        Object onlineCountObject = channel.remoteExtraMap.get(BageChannelCustomerExtras.onlineCount);
                        if (onlineCountObject instanceof Integer) {
                            int onlineCount = (int) onlineCountObject;
                            if (onlineCount > 0) {
                                bageVBinding.topLayout.subtitleCountTv.setVisibility(View.VISIBLE);
                                bageVBinding.topLayout.subtitleCountTv.setText(String.format(getString(R.string.online_count), onlineCount));
                            }
                        }
                    }
                }
                EndpointManager.getInstance().invoke("set_chat_bg", new SetChatBgMenu(channelId, channelType, bageVBinding.ivBg, bageVBinding.rootView, bageVBinding.blurView));
            } else {
                for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
                    if (TextUtils.isEmpty(chatAdapter.getData().get(i).bageMsg.fromUID)) continue;
                    boolean isRefresh = false;
                    if (chatAdapter.getData().get(i).bageMsg.fromUID.equals(channel.channelID) && channel.channelType == BageChannelType.PERSONAL) {
                        chatAdapter.getData().get(i).bageMsg.setFrom(channel);
                        isRefresh = true;
                    }
                    if (chatAdapter.getData().get(i).bageMsg.getMemberOfFrom() != null && chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberUID.equals(channel.channelID) && channel.channelType == BageChannelType.PERSONAL) {
                        chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberRemark = channel.channelRemark;
                        chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberName = channel.channelName;
                        chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberAvatar = channel.avatar;
                        chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberAvatarCacheKey = channel.avatarCacheKey;
                        isRefresh = true;
                    }
                    if (chatAdapter.getData().get(i).bageMsg.baseContentMsgModel != null && BageReader.isNotEmpty(chatAdapter.getData().get(i).bageMsg.baseContentMsgModel.entities)) {
                        for (BageMsgEntity entity : chatAdapter.getData().get(i).bageMsg.baseContentMsgModel.entities) {
                            if (entity.type.equals(ChatContentSpanType.getMention()) && !TextUtils.isEmpty(entity.value) && entity.value.equals(channel.channelID)) {
                                isRefresh = true;
                                chatAdapter.getData().get(i).formatSpans(ChatActivity.this, chatAdapter.getData().get(i).bageMsg);
                                break;
                            }
                        }
                    }
                    if (isRefresh) {
                        chatAdapter.getData().get(i).isRefreshAvatarAndName = true;
                        chatAdapter.notifyItemChanged(i, chatAdapter.getData().get(i));
                    }
                }
            }
        });

        //监听频道成员信息改变通知
        BageIM.getInstance().getChannelMembersManager().addOnRefreshChannelMemberInfo(channelId, (channelMember, isEnd) -> {
            if (channelMember != null && !TextUtils.isEmpty(channelMember.channelID)) {
                if (channelMember.channelID.equals(channelId) && channelMember.channelType == channelType) {
                    if (channelMember.channelType == BageChannelType.PERSONAL) {
                        String name = channelMember.memberRemark;
                        if (TextUtils.isEmpty(name)) name = channelMember.memberName;
                        bageVBinding.topLayout.titleCenterTv.setText(name);
                    } else {
                        //成员名字改变
                        for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
                            if (chatAdapter.getData().get(i).bageMsg != null && chatAdapter.getData().get(i).bageMsg.getMemberOfFrom() != null && !TextUtils.isEmpty(chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberUID) && chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberUID.equals(channelMember.memberUID)) {
                                chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberName = channelMember.memberName;
                                chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberRemark = channelMember.memberRemark;
                                chatAdapter.getData().get(i).bageMsg.getMemberOfFrom().memberAvatar = channelMember.memberAvatar;
                                chatAdapter.getData().get(i).isRefreshAvatarAndName = true;
                                chatAdapter.notifyItemChanged(i, chatAdapter.getData().get(i));
                            }
                        }
                    }
                }
            }
            if (isEnd) {
                checkLoginUserInGroupStatus();
            }
        });

        //监听移除频道成员
        BageIM.getInstance().getChannelMembersManager().addOnRemoveChannelMemberListener(channelId, list -> {
            if (BageReader.isNotEmpty(list) && !TextUtils.isEmpty(list.get(0).channelID) && list.get(0).channelID.equals(channelId) && list.get(0).channelType == channelType) {
                if (groupType == BageGroupType.normalGroup) {
                    count = BageIM.getInstance().getChannelMembersManager().getMemberCount(channelId, channelType);
                    bageVBinding.topLayout.subtitleTv.setText(String.format(getString(R.string.group_member), count));
                }
                //查询登录用户是否在本群
                checkLoginUserInGroupStatus();
                BageRobotModel.getInstance().syncRobotData(getChatChannelInfo());
            }
        });
        //监听添加频道成员
        BageIM.getInstance().getChannelMembersManager().addOnAddChannelMemberListener(channelId, list -> {
            if (BageReader.isNotEmpty(list) && !TextUtils.isEmpty(list.get(0).channelID) && list.get(0).channelID.equals(channelId) && list.get(0).channelType == channelType && groupType == BageGroupType.normalGroup) {
                count = BageIM.getInstance().getChannelMembersManager().getMemberCount(channelId, channelType);
                bageVBinding.topLayout.subtitleTv.setText(String.format(getString(R.string.group_member), count));
                BageRobotModel.getInstance().syncRobotData(getChatChannelInfo());
                checkLoginUserInGroupStatus();
            }
        });
        //监听删除消息
        BageIM.getInstance().getMsgManager().addOnDeleteMsgListener(channelId, msg -> {
            if (msg != null) {
                removeMsg(msg);
            }
        });
        // 命令消息监听
        BageIM.getInstance().getCMDManager().addCmdListener(channelId, bageCmd -> {
            if (bageCmd == null || TextUtils.isEmpty(bageCmd.cmdKey)) return;
            // 监听正在输入
            switch (bageCmd.cmdKey) {
                case BageCMDKeys.bage_typing -> typing(bageCmd);
                case BageCMDKeys.bage_unreadClear -> {
                    if (bageCmd.paramJsonObject.has("channel_id") && bageCmd.paramJsonObject.has("channel_type")) {
                        String channelId = bageCmd.paramJsonObject.optString("channel_id");
                        int channelType = bageCmd.paramJsonObject.optInt("channel_type");
                        int unreadCount = bageCmd.paramJsonObject.optInt("unread");
                        if (channelId.equals(this.channelId) && channelType == this.channelType) {
                            if (unreadCount < redDot) {
                                this.redDot = unreadCount;
                                bageVBinding.chatUnreadLayout.newMsgLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.newMsgLayout, redDot > 0, true, false));
                            }
                        }
                    }
                }
                case "sync_channel_state" -> {
                    String sourceChannelId = bageCmd.paramJsonObject.optString("channel_id");
                    int sourceChannelType = bageCmd.paramJsonObject.optInt("channel_type");
                    if (sourceChannelId.equals(channelId) && sourceChannelType == channelType) {
                        getChannelState();
                    }
                }
            }
        });

        //监听消息刷新
        BageIM.getInstance().getMsgManager().addOnRefreshMsgListener(channelId, (bageMsg, left) -> {
            if (bageMsg.remoteExtra.isMutualDeleted == 1) {
                removeMsg(bageMsg);
                return;
            }
            refreshMsg(bageMsg);
        });
        //监听发送消息返回
        BageIM.getInstance().getMsgManager().addOnSendMsgCallback(channelId, this::sendMsgInserted);

        //监听新消息
        BageIM.getInstance().getMsgManager().addOnNewMsgListener(channelId, this::receivedMessages);
        //监听清空聊天记录
        BageIM.getInstance().getMsgManager().addOnClearMsgListener(channelId, (channelID, channelType, fromUID) -> {
            if (!TextUtils.isEmpty(channelID) && ChatActivity.this.channelId.equals(channelID) && ChatActivity.this.channelType == channelType) {
                if (TextUtils.isEmpty(fromUID)) {
                    chatAdapter = new ChatAdapter(ChatActivity.this, ChatAdapter.AdapterType.normalMessage);
                    bageVBinding.recyclerView.setAdapter(chatAdapter);
                } else {
                    for (int i = 0; i < chatAdapter.getData().size(); i++) {
                        if (chatAdapter.getData().get(i).bageMsg != null && !TextUtils.isEmpty(chatAdapter.getData().get(i).bageMsg.fromUID) && chatAdapter.getData().get(i).bageMsg.fromUID.equals(fromUID)) {
                            chatAdapter.removeAt(i);
                            i--;
                        }
                    }
                }
            }

        });

        BageIM.getInstance().getReminderManager().addOnNewReminderListener(channelId, this::resetReminder);
        EndpointManager.getInstance().setMethod(channelId, EndpointCategory.bageExitChat, object -> {
            if (object != null) {
                BageChannel channel = (BageChannel) object;
                if (channelId.equals(channel.channelID) && channel.channelType == channelType) {
                    finish();
                }
            }
            return null;
        });
        BageIM.getInstance().getConnectionManager().addOnConnectionStatusListener(channelId, (i, s) -> {
            if (i == BageConnectStatus.syncCompleted && BageUIKitApplication.getInstance().isRefreshChatActivityMessage) {
                BageUIKitApplication.getInstance().isRefreshChatActivityMessage = false;
                int maxOrderSeq = BageIM.getInstance().getMsgManager().getMaxOrderSeqWithChannel(channelId, channelType);
                long tempMaxOrderSeq = 0;
                if (chatAdapter != null && chatAdapter.getLastMsg() != null) {
                    tempMaxOrderSeq = chatAdapter.getLastMsg().orderSeq;
                }
                if (maxOrderSeq > tempMaxOrderSeq) {
                    // scrollToEnd();
//                    isCanRefresh = true;
//                    isShowHistory = false;
                    getData(0, true, maxOrderSeq, true);
                }
//                int firstItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
//                if (firstItemPosition == -1) return;
//                if (BageReader.isNotEmpty(chatAdapter.getData())) {
//                    BageMsg msg = chatAdapter.getFirstVisibleItem(firstItemPosition);
//                    if (msg != null) {
////                            keepMsgSeq = msg.messageSeq;
//                        lastPreviewMsgOrderSeq = msg.orderSeq;
//                        int index = chatAdapter.getFirstVisibleItemIndex(firstItemPosition);
//                        View view = linearLayoutManager.findViewByPosition(index);
//                        if (view != null) {
//                            keepOffsetY = view.getTop();
//                        }
//                    }
//                }
//                getData(1, true, lastPreviewMsgOrderSeq, false);
            }
        });
        EndpointManager.getInstance().setMethod(channelId, EndpointCategory.refreshProhibitWord, object -> {
            if (BageReader.isEmpty(chatAdapter.getData())) {
                return 1;
            }
            for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
                if (chatAdapter.getData().get(i).bageMsg != null && chatAdapter.getData().get(i).bageMsg.type == BageContentType.Bage_TEXT) {
                    BageIMUtils.getInstance().resetMsgProhibitWord(chatAdapter.getData().get(i).bageMsg);
                    chatAdapter.getData().get(i).formatSpans(ChatActivity.this, chatAdapter.getData().get(i).bageMsg);
                    chatAdapter.notifyItemChanged(i);
                }
            }
            return 1;
        });
        EndpointManager.getInstance().setMethod("hide_pinned_view", object -> {
            if (!isShowPinnedView) return null;
            isShowPinnedView = false;
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) bageVBinding.timeTv.getLayoutParams();
            lp.topMargin = AndroidUtilities.dp(10) + getTopPinViewHeight();
            bageVBinding.timeTv.setVisibility(View.GONE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(bageVBinding.pinnedLayout, "translationY", 0, -AndroidUtilities.dp(53));
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    bageVBinding.pinnedLayout.clearAnimation();
                    bageVBinding.pinnedLayout.setVisibility(View.GONE);
                    if (BageReader.isNotEmpty(chatAdapter.getData()) && chatAdapter.getData().get(0).bageMsg != null && chatAdapter.getData().get(0).bageMsg.type == BageContentType.spanEmptyView) {
                        if (!isShowCallingView) {
                            chatAdapter.getData().remove(0);
                            chatAdapter.notifyItemRemoved(0);
                        }
                        //chatAdapter.notifyDataSetChanged();
                    }
                }

                public void onAnimationStart(Animator animation) {
                    bageVBinding.pinnedLayout.setVisibility(View.VISIBLE);
                }
            });
            bageVBinding.pinnedLayout.setVisibility(View.VISIBLE);
            animator.start();
            return null;
        });
        EndpointManager.getInstance().setMethod("show_pinned_view", object -> {
            if (isShowPinnedView) {
                return null;
            }
            isShowPinnedView = true;

            if (BageReader.isNotEmpty(chatAdapter.getData()) && chatAdapter.getData().get(0).bageMsg != null && chatAdapter.getData().get(0).bageMsg.type != BageContentType.spanEmptyView) {
                BageMsg msg = getSpanEmptyMsg();
                chatAdapter.addData(0, new BageUIChatMsgItemEntity(this, msg, null));
            }
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) bageVBinding.timeTv.getLayoutParams();
            lp.topMargin = AndroidUtilities.dp(10) + getTopPinViewHeight();
            bageVBinding.timeTv.setVisibility(View.GONE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(bageVBinding.pinnedLayout, "translationY", -bageVBinding.pinnedLayout.getHeight(), 0);
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // bageVBinding.pinnedLayout.clearAnimation();
                    bageVBinding.pinnedLayout.setVisibility(View.VISIBLE);
                }
            });
            animator.start();
            bageVBinding.pinnedLayout.setVisibility(View.VISIBLE);
            return null;
        });
        EndpointManager.getInstance().setMethod("tip_msg_in_chat", object -> {
            tipsMsg((String) object);
            return null;
        });
        EndpointManager.getInstance().setMethod("reset_channel_all_pinned_msg", object -> {
            resetHideChannelAllPinnedMessage();
            for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
                if (hideChannelAllPinnedMessage == 1) {
                    if (chatAdapter.getData().get(i).isPinned == 1) {
                        chatAdapter.getData().get(i).isPinned = 0;
                        chatAdapter.notifyStatus(i);
                    }
                } else {
                    if (chatAdapter.getData().get(i).isPinned == 0) {
                        if (chatAdapter.getData().get(i).bageMsg.remoteExtra != null && chatAdapter.getData().get(i).bageMsg.remoteExtra.isPinned == 1) {
                            chatAdapter.getData().get(i).isPinned = 1;
                            chatAdapter.notifyStatus(i);
                        }
                    }
                }
            }
            return null;
        });
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initParam();
        requestInitialMessages();
        initData();
    }

    private void initData() {
        startTimer();
        EndpointManager.getInstance().invoke(EndpointSID.openChatPage, getChatChannelInfo());
        //获取网络频道信息
        BageIM.getInstance().getChannelManager().fetchChannelInfo(channelId, channelType);
        MsgModel.getInstance().syncExtraMsg(channelId, channelType);
        BageRobotModel.getInstance().syncRobotData(getChatChannelInfo());
        getChannelState();

        if (BageSystemAccount.isSystemAccount(channelId) || channelType == BageChannelType.CUSTOMER_SERVICE) {
            CommonAnim.getInstance().showOrHide(callIV, false, false);
        }
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelId, channelType);

        String avatarKey = "";
        if (channel != null) {
            bageVBinding.topLayout.categoryLayout.removeAllViews();
            avatarKey = channel.avatarCacheKey;
            if (channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(BageChannelExtras.groupType)) {
                Object object = channel.remoteExtraMap.get(BageChannelExtras.groupType);
                if (object instanceof Integer) {
                    groupType = (int) object;
                }
            }
            if (!TextUtils.isEmpty(channel.category)) {
                if (channel.category.equals(BageSystemAccount.accountCategorySystem)) {
                    bageVBinding.topLayout.categoryLayout.addView(Theme.getChannelCategoryTV(this, getString(R.string.official), ContextCompat.getColor(this, R.color.transparent), ContextCompat.getColor(this, R.color.reminderColor), ContextCompat.getColor(this, R.color.reminderColor)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (channel.category.equals(BageSystemAccount.accountCategoryCustomerService)) {
                    bageVBinding.topLayout.categoryLayout.addView(Theme.getChannelCategoryTV(this, getString(R.string.customer_service), Theme.colorAccount, ContextCompat.getColor(this, R.color.white), Theme.colorAccount), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (channel.category.equals(BageSystemAccount.accountCategoryVisitor)) {
                    bageVBinding.topLayout.categoryLayout.addView(Theme.getChannelCategoryTV(this, getString(R.string.visitor), ContextCompat.getColor(this, R.color.transparent), ContextCompat.getColor(this, R.color.colorFFC107), ContextCompat.getColor(this, R.color.colorFFC107)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (channel.category.equals(BageSystemAccount.channelCategoryOrganization)) {
                    bageVBinding.topLayout.categoryLayout.addView(Theme.getChannelCategoryTV(this, getString(R.string.all_staff), ContextCompat.getColor(this, R.color.category_org_bg), ContextCompat.getColor(this, R.color.category_org_text), ContextCompat.getColor(this, R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (channel.category.equals(BageSystemAccount.channelCategoryDepartment)) {
                    bageVBinding.topLayout.categoryLayout.addView(Theme.getChannelCategoryTV(this, getString(R.string.department), ContextCompat.getColor(this, R.color.category_org_bg), ContextCompat.getColor(this, R.color.category_org_text), ContextCompat.getColor(this, R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
            }
            showChannelName(channel);
            if (channel.robot == 1) {
                bageVBinding.topLayout.categoryLayout.addView(Theme.getChannelCategoryTV(this, getString(R.string.bot), ContextCompat.getColor(this, R.color.colorFFC107), ContextCompat.getColor(this, R.color.white), ContextCompat.getColor(this, R.color.colorFFC107)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 1, 0));
            }
            EndpointManager.getInstance().invoke("show_avatar_other_info", new AvatarOtherViewMenu(bageVBinding.topLayout.otherLayout, channel, bageVBinding.topLayout.avatarView, true));
        }
        bageVBinding.topLayout.avatarView.showAvatar(channelId, channelType, avatarKey);

        //如果是群聊就同步群成员信息
        if (channelType == BageChannelType.GROUP) {
            if (groupType == BageGroupType.normalGroup) {
                GroupModel.getInstance().groupMembersSync(channelId, (code, msg) -> {
                    if (code == HttpResponseCode.success) {
                        BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
                        hideOrShowRightView(member == null || member.isDeleted != 1);
                        BageRobotModel.getInstance().syncRobotData(getChatChannelInfo());
                        chatPanelManager.showOrHideForbiddenView();
                    }
                });
            } else {
                UserModel.getInstance().getUserInfo(BageConfig.getInstance().getUid(), channelId, null);
            }
            //获取sdk频道信息
            if (channel != null) {
                count = BageIM.getInstance().getChannelMembersManager().getMemberCount(channelId, channelType);
                showChannelName(channel);
                // showNickName = channel.showNick == 1;
                if (channel.forbidden == 1) {
                    chatPanelManager.showOrHideForbiddenView();
                }
                if (channel.status == BageChannelStatus.statusDisabled) {
                    chatPanelManager.showBan();
                } else {
                    chatPanelManager.hideBan();
                }
            }

            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
            hideOrShowRightView(member == null || member.isDeleted == 0);
            if (groupType == BageGroupType.normalGroup) {
                bageVBinding.topLayout.subtitleTv.setText(String.format(getString(R.string.group_member), count));
            }
            bageVBinding.topLayout.subtitleView.setVisibility(View.VISIBLE);
            chatPanelManager.showOrHideForbiddenView();
        } else {
            hideOrShowRightView(true);
            bageVBinding.topLayout.subtitleCountTv.setVisibility(View.GONE);
            if (channel != null) {
                setOnlineView(channel);
                showChannelName(channel);
            }
        }

        reminderList.clear();
        groupApproveList.clear();
        List<BageReminder> allReminder = BageIM.getInstance().getReminderManager().getReminders(channelId, channelType);
        if (BageReader.isNotEmpty(allReminder)) {
            for (BageReminder reminder : allReminder) {
                boolean isPublisher = !TextUtils.isEmpty(reminder.publisher) && reminder.publisher.equals(loginUID);
                if (reminder.type == BageMentionType.BageReminderTypeMentionMe && reminder.done == 0 && !isPublisher) {
                    reminderList.add(reminder);
                }
                if (reminder.type == BageMentionType.BageApplyJoinGroupApprove && reminder.done == 0) {
                    groupApproveList.add(reminder);
                }
            }
        }
        //查询高光内容
        BageConversationMsgExtra extra = BageIM.getInstance().getConversationManager().getMsgExtraWithChannel(channelId, channelType);
        if (extra != null) {
            if (!TextUtils.isEmpty(extra.draft)) {
                chatPanelManager.setEditContent(extra.draft);
            }
            browseTo = extra.browseTo;
        }
        new Handler().postDelayed(() -> {
            resetRemindView();
            resetGroupApproveView();
        }, 150);

    }

    private void requestInitialMessages() {
        final int requestGeneration = ++initialMessageRequestGeneration;
        lastPreviewMsgOrderSeq = 0;
        unreadStartMsgOrderSeq = 0;
        tipsOrderSeq = 0;
        keepOffsetY = 0;
        redDot = 0;
        isCanLoadMore = false;
        isCanRefresh = true;
        isRefreshLoading = false;
        isMoreLoading = false;
        chatAdapter.setList(new ArrayList<>());

        Intent intent = getIntent();
        if (intent.hasExtra("lastPreviewMsgOrderSeq")) {
            lastPreviewMsgOrderSeq = intent.getLongExtra("lastPreviewMsgOrderSeq", 0L);
            isCanLoadMore = lastPreviewMsgOrderSeq > 0;
        }
        if (intent.hasExtra("keepOffsetY")) {
            keepOffsetY = intent.getIntExtra("keepOffsetY", 0);
        }
        if (intent.hasExtra("redDot")) redDot = intent.getIntExtra("redDot", 0);
        if (intent.hasExtra("tipsOrderSeq")) {
            tipsOrderSeq = intent.getLongExtra("tipsOrderSeq", 0);
        }
        if (intent.hasExtra("unreadStartMsgOrderSeq")) {
            unreadStartMsgOrderSeq = intent.getLongExtra("unreadStartMsgOrderSeq", 0);
        }

        boolean isScrollToEnd = unreadStartMsgOrderSeq == 0 && lastPreviewMsgOrderSeq == 0;
        long aroundMsgSeq = 0;
        if (unreadStartMsgOrderSeq != 0) {
            aroundMsgSeq = unreadStartMsgOrderSeq;
            isCanLoadMore = true;
        }
        isUpdateRedDot = unreadStartMsgOrderSeq > 0;
        if (lastPreviewMsgOrderSeq != 0) aroundMsgSeq = lastPreviewMsgOrderSeq;
        if (tipsOrderSeq != 0) {
            aroundMsgSeq = tipsOrderSeq;
            isCanLoadMore = true;
        }
        if (aroundMsgSeq == 0 && intent.hasExtra("aroundMsgSeq")) {
            aroundMsgSeq = intent.getLongExtra("aroundMsgSeq", 0);
        }
        int pullMode = lastPreviewMsgOrderSeq == 0 ? 0 : 1;
        boolean isLocateMessage = unreadStartMsgOrderSeq > 0 || lastPreviewMsgOrderSeq > 0
                || tipsOrderSeq > 0 || aroundMsgSeq > 0;
        if (isLocateMessage) {
            getData(pullMode, unreadStartMsgOrderSeq > 0, aroundMsgSeq, isScrollToEnd);
            return;
        }

        String requestChannelID = channelId;
        byte requestChannelType = channelType;
        // 首次登录时 SDK 已通过会话同步保存了最近 10 条消息。先纯本地显示，
        // 再后台补齐 30 条首屏历史，避免 UI 被 /message/channel/sync 阻塞。
        MsgModel.getInstance().getLocalChannelMessages(channelId, channelType, limit, localMessages -> {
            if (requestGeneration != initialMessageRequestGeneration || isFinishing() || isDestroyed()
                    || !TextUtils.equals(requestChannelID, channelId)
                    || requestChannelType != channelType) {
                return;
            }
            if (BageReader.isNotEmpty(localMessages)) {
                showData(new ArrayList<>(localMessages), 0, true, true);
            }
            // 网络结果作为完整首屏重新设置，避免本地预览与补齐结果产生重复项。
            getData(0, true, 0, true);
        });
    }

    private void getChannelState() {
        BageCommonModel.getInstance().getChannelState(channelId, channelType, channelState -> {
            if (channelState != null) {
                if (channelType == BageChannelType.GROUP && channelState.online_count > 0) {
                    bageVBinding.topLayout.subtitleCountTv.setVisibility(View.VISIBLE);
                    bageVBinding.topLayout.subtitleCountTv.setText(String.format(getString(R.string.online_count), channelState.online_count));
                }
                if (channelType == BageChannelType.PERSONAL) {
                    return;
                }
                if (channelState.call_info == null || BageReader.isEmpty(channelState.call_info.getCalling_participants())) {
                    bageVBinding.callLayout.setVisibility(View.GONE);
                    isShowCallingView = false;
                    if (BageReader.isNotEmpty(chatAdapter.getData()) && chatAdapter.getData().get(0).bageMsg.type == BageContentType.spanEmptyView) {
                        if (!isShowPinnedView) {
                            chatAdapter.getData().remove(0);
                            chatAdapter.notifyItemRemoved(0);
                        } else {
                            chatAdapter.getData().get(0).bageMsg.messageSeq = getTopPinViewHeight();
                            chatAdapter.notifyItemChanged(0);
                        }
                    }
                } else {
                    Object object = EndpointManager.getInstance().invoke("show_calling_participants", new CallingViewMenu(this, channelState.call_info));
                    if (object != null) {
                        View view = (View) object;
                        bageVBinding.callLayout.removeAllViews();
                        bageVBinding.callLayout.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                        bageVBinding.callLayout.setVisibility(View.VISIBLE);
                        isShowCallingView = true;
                        if (isAddedSpanEmptyView()) {
                            chatAdapter.getData().get(0).bageMsg.messageSeq = getTopPinViewHeight();
                            chatAdapter.notifyItemChanged(0);
                        } else {
                            BageMsg msg = getSpanEmptyMsg();
                            chatAdapter.addData(0, new BageUIChatMsgItemEntity(this, msg, null));
                        }
                    } else {
                        isShowCallingView = false;
                    }
                }
            }

            if (BageReader.isEmpty(MsgModel.getInstance().channelStatus)) {
                MsgModel.getInstance().channelStatus = new ArrayList<>();
            }
            boolean isAdd = true;
            for (int i = 0; i < MsgModel.getInstance().channelStatus.size(); i++) {
                if (MsgModel.getInstance().channelStatus.get(i).channel_id.equals(channelId)) {
                    MsgModel.getInstance().channelStatus.get(i).calling = isShowCallingView ? 1 : 0;
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                BageChannelState state = new BageChannelState();
                state.channel_id = channelId;
                state.channel_type = channelType;
                state.calling = isShowCallingView ? 1 : 0;
                MsgModel.getInstance().channelStatus.add(state);
            }
            EndpointManager.getInstance().invoke("refresh_conversation_calling", null);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) bageVBinding.timeTv.getLayoutParams();
            lp.topMargin = AndroidUtilities.dp(10) + getTopPinViewHeight();
            bageVBinding.timeTv.setVisibility(View.GONE);
        });
    }

    // 获取聊天记录
    private void getData(int pullMode, boolean isSetNewData, long aroundMsgOrderSeq, boolean isScrollToEnd) {
        boolean contain = false;
        long oldestOrderSeq;
        if (pullMode == 1) {
            oldestOrderSeq = chatAdapter.getEndMsgOrderSeq();
        } else {
            oldestOrderSeq = chatAdapter.getFirstMsgOrderSeq();
        }
        if (isSyncLastMsg) {
            oldestOrderSeq = 0;
        }
        //定位消息
        if (lastPreviewMsgOrderSeq != 0) {
            contain = true;
            oldestOrderSeq = lastPreviewMsgOrderSeq;
        }
        if (unreadStartMsgOrderSeq != 0) contain = true;
        BageIM.getInstance().getMsgManager().getOrSyncHistoryMessages(channelId, channelType, oldestOrderSeq, contain, pullMode, limit, aroundMsgOrderSeq, new IGetOrSyncHistoryMsgBack() {
            @Override
            public void onSyncing() {

                if (isShowPinnedView && !isRefreshLoading && !isMoreLoading && !isSyncLastMsg) {
                    EndpointManager.getInstance().invoke("is_syncing_message", 1);
                } else {
                    if (BageReader.isEmpty(chatAdapter.getData())) {
                        BageMsg bageMsg = new BageMsg();
                        bageMsg.type = BageContentType.loading;
                        chatAdapter.addData(new BageUIChatMsgItemEntity(ChatActivity.this, bageMsg, null));
                    }
                }
            }

            @Override
            public void onResult(List<BageMsg> list) {
                Log.e("实际多少条", list.size() + "");
                if (isShowPinnedView) {
                    EndpointManager.getInstance().invoke("is_syncing_message", 0);
                }
                if (pullMode == 0) {
                    if (BageReader.isEmpty(list))
                        isCanRefresh = false;
                } else {
                    if (BageReader.isEmpty(list)) {
                        isCanLoadMore = false;
                    }
                }
                isSyncLastMsg = false;
                List<BageMsg> tempList = new ArrayList<>();
                for (BageMsg msg : list) {
                    if (isSetNewData || !chatAdapter.isExist(msg.clientMsgNO, msg.messageID)){
                        tempList.add(msg);
                    }
                }
                showData(tempList, pullMode, isSetNewData, isScrollToEnd);
                bageVBinding.chatUnreadLayout.progress.setVisibility(View.GONE);
                bageVBinding.chatUnreadLayout.msgDownIv.setVisibility(View.VISIBLE);

                if (BageReader.isNotEmpty(chatAdapter.getData())) {
                    for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
                        if (chatAdapter.getData().get(i).bageMsg != null && chatAdapter.getData().get(i).bageMsg.type == BageContentType.loading) {
                            chatAdapter.removeAt(i);
                            break;
                        }
                    }
                }
                isRefreshLoading = false;
                isMoreLoading = false;

            }
        });


    }

    /**
     * 显示数据
     *
     * @param msgList       数据源
     * @param pullMode      拉取模式 0:向下拉取 1:向上拉取
     * @param isSetNewData  是否重新显示新数据
     * @param isScrollToEnd 是否滚动到底部
     */
    private void showData(List<BageMsg> msgList, int pullMode, boolean isSetNewData, boolean isScrollToEnd) {
        boolean isAddEmptyView = BageReader.isNotEmpty(msgList) && msgList.size() < limit;
        if (isAddEmptyView) {
            BageMsg msg = new BageMsg();
            msg.timestamp = 0;
            msg.type = BageContentType.emptyView;
            msgList.add(0, msg);
        }

        if ((isShowCallingView || isShowPinnedView) && pullMode == 0) {
            if (BageReader.isNotEmpty(chatAdapter.getData())) {
                for (int i = 0; i < chatAdapter.getData().size(); i++) {
                    if (chatAdapter.getData().get(i).bageMsg != null && chatAdapter.getData().get(i).bageMsg.type == BageContentType.spanEmptyView) {
                        chatAdapter.removeAt(i);
                        break;
                    }
                }
            }
            msgList.add(0, getSpanEmptyMsg());
        }
        List<BageUIChatMsgItemEntity> list = new ArrayList<>();
        if (BageReader.isNotEmpty(msgList)) {
            long pre_msg_time = chatAdapter.getLastTimeMsg();
            for (int i = 0, size = msgList.size(); i < size; i++) {
                if (!BageTimeUtils.getInstance().isSameDay(msgList.get(i).timestamp, pre_msg_time) && msgList.get(i).type != BageContentType.emptyView && msgList.get(i).type != BageContentType.spanEmptyView) {
                    //显示聊天时间
                    BageUIChatMsgItemEntity uiChatMsgEntity = new BageUIChatMsgItemEntity(this, new BageMsg(), null);
                    uiChatMsgEntity.bageMsg.type = BageContentType.msgPromptTime;
                    uiChatMsgEntity.bageMsg.content = BageTimeUtils.getInstance().getShowDate(msgList.get(i).timestamp * 1000);
                    uiChatMsgEntity.bageMsg.timestamp = msgList.get(i).timestamp;
                    list.add(uiChatMsgEntity);
                }
                pre_msg_time = msgList.get(i).timestamp;
                BageUIChatMsgItemEntity uiMsg = BageIMUtils.getInstance().msg2UiMsg(this, msgList.get(i), count, showNickName, chatAdapter.isShowChooseItem());
                if (msgList.get(i).remoteExtra != null) {
                    if (hideChannelAllPinnedMessage == 1) {
                        uiMsg.isPinned = 0;
                    } else {
                        uiMsg.isPinned = msgList.get(i).remoteExtra.isPinned;
                    }
                }
                list.add(uiMsg);
            }
        }

        if (isSetNewData) {
            if (unreadStartMsgOrderSeq != 0) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    if (list.get(i).bageMsg != null && list.get(i).bageMsg.orderSeq == unreadStartMsgOrderSeq) {
                        //插入一条本地的新消息分割线
                        BageUIChatMsgItemEntity uiChatMsgItemEntity = new BageUIChatMsgItemEntity(this, new BageMsg(), null);
                        uiChatMsgItemEntity.bageMsg.type = BageContentType.msgPromptNewMsg;
                        int index = i;
                        if (index <= 0) index = 0;
                        if (index > list.size() - 1) index = list.size() - 1;
                        list.add(index, uiChatMsgItemEntity);
                        if (index >= 1) {
                            linearLayoutManager.scrollToPositionWithOffset(index, 50);
                        } else bageVBinding.recyclerView.scrollToPosition(index);
                        unreadStartMsgOrderSeq = 0;
                        break;
                    }
                }
            }
            chatAdapter.resetData(list);
            chatAdapter.setNewInstance(list);
        } else {
            chatAdapter.resetData(list);
            if (pullMode == 1) {
                if (BageReader.isNotEmpty(chatAdapter.getData()) && BageReader.isNotEmpty(list))
                    list.get(0).previousMsg = chatAdapter.getData().get(chatAdapter.getData().size() - 1).bageMsg;
                chatAdapter.addData(list);
            } else {
                if (BageReader.isNotEmpty(list) && BageReader.isNotEmpty(chatAdapter.getData())) {
                    list.get(list.size() - 1).nextMsg = chatAdapter.getData().get(0).bageMsg;
                }
                chatAdapter.addData(0, list);
            }
        }
        if (tipsOrderSeq != 0 || lastPreviewMsgOrderSeq != 0) {
            bageVBinding.recyclerView.setVisibility(View.VISIBLE);
            if (tipsOrderSeq != 0) {
                for (int i = 0; i < chatAdapter.getData().size(); i++) {
                    if (chatAdapter.getItem(i).bageMsg.orderSeq == tipsOrderSeq) {
                        linearLayoutManager.scrollToPositionWithOffset(i, AndroidUtilities.dp(50));
                        chatAdapter.getItem(i).isShowTips = true;
                        chatAdapter.notifyItemChanged(i);
                        tipsOrderSeq = 0;
                        break;
                    }
                }
            }
            if (lastPreviewMsgOrderSeq != 0) {
                for (int i = 0; i < chatAdapter.getData().size(); i++) {
                    if (chatAdapter.getItem(i).bageMsg.orderSeq == lastPreviewMsgOrderSeq) {
                        linearLayoutManager.scrollToPositionWithOffset(i, keepOffsetY);
                        break;
                    }
                }
            }
        } else {
            if (isScrollToEnd)
                bageVBinding.recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            else bageVBinding.recyclerView.setVisibility(View.VISIBLE);
        }
        if (isCanLoadMore && BageReader.isNotEmpty(chatAdapter.getData()) && chatAdapter.getData().get(chatAdapter.getData().size() - 1).bageMsg != null) {
            int maxSeq = BageIM.getInstance().getMsgManager().getMaxMessageSeqWithChannel(channelId, channelType);
            if (chatAdapter.getData().get(chatAdapter.getData().size() - 1).bageMsg.messageSeq == maxSeq) {
                isCanLoadMore = false;
            }
        }

        new Handler().postDelayed(() -> {
            if (isUpdateRedDot) {
                MsgModel.getInstance().clearUnread(channelId, channelType, redDot, (code, msg) -> {
                    if (code == HttpResponseCode.success && redDot == 0) {
                        isUpdateRedDot = false;
                    }
                });
            }
        }, 500);
    }


    private void hideOrShowRightView(boolean isShow) {
        if (((channelId.equals(BageSystemAccount.system_file_helper) || channelId.equals(BageSystemAccount.system_team)) && channelType == BageChannelType.PERSONAL) || channelType == BageChannelType.CUSTOMER_SERVICE) {
            isShow = false;
        }
        BageChannel channel = getChatChannelInfo();
        if (channelType == BageChannelType.PERSONAL && (channel.isDeleted == 1 || UserUtils.getInstance().checkFriendRelation(channelId))) {
            isShow = false;
        }
        CommonAnim.getInstance().showOrHide(callIV, isShow, true);
    }

    private void resetReminder(List<BageReminder> list) {
        if (BageReader.isEmpty(list)) {
            return;
        }
        List<BageUIChatMsgItemEntity> msgList = chatAdapter.getData();
        List<Long> ids = new ArrayList<>();
        for (int i = 0, size = msgList.size(); i < size; i++) {
            for (BageReminder reminder : list) {
                if (msgList.get(i).bageMsg != null && !TextUtils.isEmpty(msgList.get(i).bageMsg.messageID) && msgList.get(i).bageMsg.messageID.equals(reminder.messageID)) {
                    if (msgList.get(i).bageMsg.viewed == 1 && reminder.done == 0) {
                        ids.add(reminder.reminderID);
                    }
                }
            }
        }

        // 先完成提醒项
        MsgModel.getInstance().doneReminder(ids);

        for (BageReminder reminder : list) {
            boolean isPublisher = !TextUtils.isEmpty(reminder.publisher) && reminder.publisher.equals(loginUID);
            if (!reminder.channelID.equals(channelId) || isPublisher) continue;
            if (reminder.done == 0) {
                boolean isAdd = true;
                for (int i = 0, size = reminderList.size(); i < size; i++) {
                    if (reminder.reminderID == reminderList.get(i).reminderID && reminder.type == reminderList.get(i).type) {
                        isAdd = false;
                        reminderList.get(i).done = 0;
                        break;
                    }
                }
                for (int i = 0; i < ids.size(); i++) {
                    if (ids.get(i) == reminder.reminderID) {
                        isAdd = false;
                        break;
                    }
                }
                if (isAdd && reminder.type == BageMentionType.BageReminderTypeMentionMe)
                    reminderList.add(reminder);
                boolean isAddApprove = true;
                for (int i = 0, size = groupApproveList.size(); i < size; i++) {
                    if (reminder.reminderID == groupApproveList.get(i).reminderID && reminder.type == groupApproveList.get(i).type) {
                        isAddApprove = false;
                        groupApproveList.get(i).done = 0;
                        break;
                    }
                }
                if (isAddApprove && reminder.type == BageMentionType.BageApplyJoinGroupApprove)
                    groupApproveList.add(reminder);
            } else {
                if (BageReader.isNotEmpty(reminderList)) {
                    for (int i = 0, size = reminderList.size(); i < size; i++) {
                        if (reminder.messageID.equals(reminderList.get(i).messageID)) {
//                            reminderList.get(i).done = 1;
                            reminderList.remove(i);
                            break;
                        }
                    }
                }
                if (BageReader.isNotEmpty(groupApproveList)) {
                    for (int i = 0, size = groupApproveList.size(); i < size; i++) {
                        if (reminder.messageID.equals(groupApproveList.get(i).messageID)) {
//                            groupApproveList.get(i).done = 1;
                            groupApproveList.remove(i);
                            break;
                        }
                    }
                }
            }
        }
        resetRemindView();
        resetGroupApproveView();

//        if (BageReader.isNotEmpty(list)) {
//            List<BageUIChatMsgItemEntity> msgList = chatAdapter.getData();
//            List<Long> ids = new ArrayList<>();
//            for (int i = 0, size = list.size(); i < size; i++) {
//                if (list.get(i).done == 1) continue;
//                for (int j = 0, len = msgList.size(); j < len; j++) {
//                    if (msgList.get(j).bageMsg != null && !TextUtils.isEmpty(msgList.get(j).bageMsg.messageID) && msgList.get(j).bageMsg.messageID.equals(list.get(i).messageID)) {
//                        if (msgList.get(j).bageMsg.viewed == 1) {
//                            ids.add(list.get(i).reminderID);
//                            list.remove(i);
//                            i--;
//                            size--;
//                            break;
//                        }
//                    }
//                }
//            }
//            MsgModel.getInstance().doneReminder(ids);
//            if (BageReader.isEmpty(list)) {
//                return;
//            }
//            for (BageReminder reminder : list) {
//                boolean isPublisher = !TextUtils.isEmpty(reminder.publisher) && reminder.publisher.equals(loginUID);
//                if (!reminder.channelID.equals(channelId) || isPublisher) continue;
//                if (reminder.done == 0) {
//                    boolean isAdd = true;
//                    for (int i = 0, size = reminderList.size(); i < size; i++) {
//                        if (reminder.reminderID == reminderList.get(i).reminderID && reminder.type == reminderList.get(i).type) {
//                            isAdd = false;
//                            reminderList.get(i).done = 0;
//                            break;
//                        }
//                    }
//                    if (isAdd && reminder.type == BageMentionType.BageReminderTypeMentionMe)
//                        reminderList.add(reminder);
//                    boolean isAddApprove = true;
//                    for (int i = 0, size = groupApproveList.size(); i < size; i++) {
//                        if (reminder.reminderID == groupApproveList.get(i).reminderID && reminder.type == groupApproveList.get(i).type) {
//                            isAddApprove = false;
//                            groupApproveList.get(i).done = 0;
//                            break;
//                        }
//                    }
//                    if (isAddApprove && reminder.type == BageMentionType.BageApplyJoinGroupApprove)
//                        groupApproveList.add(reminder);
//                }
//            }
//            resetRemindView();
//            resetGroupApproveView();
//        }
    }

    private void resetRemindView() {
        bageVBinding.chatUnreadLayout.remindCountTv.setCount(reminderList.size(), true);
        bageVBinding.chatUnreadLayout.remindCountTv.setVisibility(BageReader.isNotEmpty(reminderList) ? View.VISIBLE : View.GONE);
        bageVBinding.chatUnreadLayout.remindLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.remindLayout, BageReader.isNotEmpty(reminderList), BageReader.isNotEmpty(reminderList), false));
    }

    private void resetGroupApproveView() {
        bageVBinding.chatUnreadLayout.approveCountTv.setCount(groupApproveList.size(), true);
        bageVBinding.chatUnreadLayout.approveCountTv.setVisibility(BageReader.isNotEmpty(groupApproveList) ? View.VISIBLE : View.GONE);
        bageVBinding.chatUnreadLayout.groupApproveLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.groupApproveLayout, BageReader.isNotEmpty(groupApproveList), BageReader.isNotEmpty(reminderList), false));
    }

    private void showUnReadCountView() {
        bageVBinding.chatUnreadLayout.msgCountTv.setCount(redDot, false);
        bageVBinding.chatUnreadLayout.msgCountTv.setVisibility(redDot > 0 ? View.VISIBLE : View.GONE);
        bageVBinding.chatUnreadLayout.newMsgLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.newMsgLayout, redDot > 0, redDot > 0, false));
    }

    private void showChannelName(BageChannel channel) {
        String showName;
        if (channelId.equals(BageSystemAccount.system_team)) {
            showName = getString(R.string.bage_system_notice);
            bageVBinding.topLayout.titleCenterTv.setText(R.string.bage_system_notice);
        } else if (channelId.equals(BageSystemAccount.system_file_helper)) {
            showName = getString(R.string.bage_file_helper);
            bageVBinding.topLayout.titleCenterTv.setText(R.string.bage_file_helper);
        } else {
            showName = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
            bageVBinding.topLayout.titleCenterTv.setText(showName);
        }
        if (chatPanelManager != null) {
            chatPanelManager.updateEditHint(showName);
        }
    }

    private void removeMsg(BageMsg msg) {
        EndpointManager.getInstance().invoke("stop_reaction_animation", null);
        int tempIndex = 0;
        for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
            if (chatAdapter.getData().get(i).bageMsg != null && (chatAdapter.getData().get(i).bageMsg.clientSeq == msg.clientSeq || chatAdapter.getData().get(i).bageMsg.clientMsgNO.equals(msg.clientMsgNO))) {
                tempIndex = i;
                if (i - 1 >= 0) {
                    if (i + 1 <= chatAdapter.getData().size() - 1) {
                        chatAdapter.getData().get(i - 1).nextMsg = chatAdapter.getData().get(i + 1).bageMsg;
                    } else {
                        chatAdapter.getData().get(i - 1).nextMsg = null;
                    }
                }
                if (i + 1 <= chatAdapter.getData().size() - 1) {
                    if (i - 1 >= 0) {
                        chatAdapter.getData().get(i + 1).previousMsg = chatAdapter.getData().get(i - 1).bageMsg;
                    } else chatAdapter.getData().get(i + 1).previousMsg = null;
                }
                chatAdapter.removeAt(i);
                break;
            }
        }

        int timeIndex = tempIndex - 1;
        if (timeIndex < 0) return;
        //如果是时间也删除
        if (chatAdapter.getData().size() >= timeIndex) {
            if (chatAdapter.getData().get(timeIndex).bageMsg.type == BageContentType.msgPromptTime) {

                if (timeIndex - 1 >= 0) {
                    if (timeIndex + 1 <= chatAdapter.getData().size() - 1) {
                        chatAdapter.getData().get(timeIndex - 1).nextMsg = chatAdapter.getData().get(timeIndex + 1).bageMsg;
                    } else {
                        chatAdapter.getData().get(timeIndex - 1).nextMsg = null;
                    }
                }
                if (timeIndex + 1 <= chatAdapter.getData().size() - 1) {
                    if (timeIndex - 1 >= 0) {
                        chatAdapter.getData().get(timeIndex + 1).previousMsg = chatAdapter.getData().get(timeIndex - 1).bageMsg;
                    } else chatAdapter.getData().get(timeIndex + 1).previousMsg = null;
                }
                chatAdapter.removeAt(timeIndex);
            }
        }
    }

    private void showToast(int textId) {
        BageToastUtils.getInstance().showToast(getString(textId));
    }

    private synchronized void setShowTime() {
        String showTime = "";
        int index = linearLayoutManager.findFirstVisibleItemPosition();
        if (index > 0 && index < chatAdapter.getData().size()) {
            BageUIChatMsgItemEntity BageUIChatMsgItemEntity = chatAdapter.getData().get(index);
            if (BageUIChatMsgItemEntity.bageMsg != null && BageUIChatMsgItemEntity.bageMsg.timestamp > 0) {
                showTime = BageTimeUtils.getInstance().getShowDate(BageUIChatMsgItemEntity.bageMsg.timestamp * 1000);
            }
        }
        if (!TextUtils.isEmpty(showTime)) {
            SpannableString str = new SpannableString(showTime);
            str.setSpan(new SystemMsgBackgroundColorSpan(ContextCompat.getColor(this, R.color.colorSystemBg), AndroidUtilities.dp(5), AndroidUtilities.dp(2 * 5)), 0, showTime.length(), 0);
            bageVBinding.timeTv.setText(str);
            CommonAnim.getInstance().showOrHide(bageVBinding.timeTv, true, true);
        } else {
            CommonAnim.getInstance().showOrHide(bageVBinding.timeTv, false, false);
        }
    }

    private boolean isRefreshReaction(List<BageMsgReaction> oldList, List<BageMsgReaction> newList) {
        if (BageReader.isEmpty(oldList) && BageReader.isEmpty(newList)) return false;
        if ((BageReader.isEmpty(oldList) && BageReader.isNotEmpty(newList)) || (BageReader.isEmpty(newList) && BageReader.isNotEmpty(oldList)) || (oldList.size() != newList.size())) {
            return true;
        }
        boolean isRefresh = false;
        for (BageMsgReaction reaction : newList) {
            boolean refresh = true;
            for (BageMsgReaction reaction1 : oldList) {
                if (reaction1.messageID.equals(reaction.messageID) && reaction1.emoji.equals(reaction.emoji) && reaction1.isDeleted == reaction.isDeleted) {
                    refresh = false;
                    break;
                }
            }
            if (refresh) {
                isRefresh = true;
                break;
            }
        }
        return isRefresh;
    }

    private void scrollToPosition(int index) {
        linearLayoutManager.scrollToPosition(index);
    }


    private void showRefreshLoading() {
        if (isRefreshLoading || !isCanRefresh) return;
        isRefreshLoading = true;
        BageMsg bageMsg = new BageMsg();
        bageMsg.type = BageContentType.loading;
        int index = 0;
        if (isShowPinnedView || isShowCallingView) {
            for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
                if (chatAdapter.getData().get(i).bageMsg != null && chatAdapter.getData().get(i).bageMsg.type == BageContentType.spanEmptyView) {
                    index = i + 1;
                    break;
                }
            }
        }
        chatAdapter.addData(index, new BageUIChatMsgItemEntity(this, bageMsg, null));
        bageVBinding.recyclerView.scrollToPosition(0);
        lastPreviewMsgOrderSeq = 0;
        new Handler().postDelayed(() -> getData(0, false, 0, false), 300);
    }

    private void showMoreLoading() {
        if (isMoreLoading || !isCanLoadMore) return;
        isMoreLoading = true;
        BageMsg bageMsg = new BageMsg();
        bageMsg.type = BageContentType.loading;
        chatAdapter.addData(new BageUIChatMsgItemEntity(this, bageMsg, null));
        bageVBinding.recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        lastPreviewMsgOrderSeq = 0;
        unreadStartMsgOrderSeq = 0;
        new Handler().postDelayed(() -> getData(1, false, 0, false), 300);
    }

    private List<PopupMenuItem> getGroupApprovePopupItems() {
        PopupMenuItem item = new PopupMenuItem(getString(R.string.clear_all_remind), R.mipmap.msg_seen, () -> {
            List<BageReminder> list = BageIM.getInstance().getReminderManager().getRemindersWithType(channelId, channelType, BageMentionType.BageApplyJoinGroupApprove);
            List<Long> ids = new ArrayList<>();
            for (BageReminder reminder : list) {
                if (reminder.done == 0) {
                    ids.add(reminder.reminderID);
                }
            }
            groupApproveList.clear();
            resetGroupApproveView();
            MsgModel.getInstance().doneReminder(ids);
        });

        List<PopupMenuItem> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    private List<PopupMenuItem> getRemindPopupItems() {
        PopupMenuItem item = new PopupMenuItem(getString(R.string.clear_all_remind), R.mipmap.msg_seen, () -> {
            List<BageReminder> list = BageIM.getInstance().getReminderManager().getRemindersWithType(channelId, channelType, BageMentionType.BageReminderTypeMentionMe);
            List<Long> ids = new ArrayList<>();
            for (BageReminder reminder : list) {
                if (reminder.done == 0) {
                    ids.add(reminder.reminderID);
                }
            }
            reminderList.clear();
            resetRemindView();
            MsgModel.getInstance().doneReminder(ids);
        });

        List<PopupMenuItem> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    private void checkLoginUserInGroupStatus() {
        if (channelType == BageChannelType.GROUP) {
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
            hideOrShowRightView(member == null || member.isDeleted == 0);
        }
    }

    private void scrollToEnd() {
        linearLayoutManager.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    // 显示一条时间消息
    private synchronized BageMsg addTimeMsg(long newMsgTime) {
        long lastMsgTime = chatAdapter.getLastTimeMsg();
        BageMsg msg = null;
        if (!BageTimeUtils.getInstance().isSameDay(newMsgTime, lastMsgTime)) {
            int lastIndex = chatAdapter.getData().size() - 1;
            BageUIChatMsgItemEntity uiChatMsgEntity = new BageUIChatMsgItemEntity(this, null, null);
            msg = new BageMsg();
            uiChatMsgEntity.bageMsg = msg;
            uiChatMsgEntity.isChoose = (chatAdapter.getItemCount() > 0 && chatAdapter.getData().get(0).isChoose);
            uiChatMsgEntity.bageMsg.type = BageContentType.msgPromptTime;
            uiChatMsgEntity.bageMsg.content = BageTimeUtils.getInstance().getShowDate(newMsgTime * 1000);
            uiChatMsgEntity.bageMsg.timestamp = BageTimeUtils.getInstance().getCurrentSeconds();
            chatAdapter.addData(uiChatMsgEntity);
            if (lastIndex >= 0) {
                chatAdapter.notifyBackground(lastIndex);
            }
        }
        return msg;
    }

    private boolean setBackListener() {
        if (!isViewingPicture) {

            if (numberTextView.getVisibility() == View.VISIBLE) {
                for (int i = 0, size = chatAdapter.getItemCount(); i < size; i++) {
                    chatAdapter.getItem(i).isChoose = false;
                    chatAdapter.getItem(i).isChecked = false;
                    chatAdapter.notifyItemChanged(i, chatAdapter.getItem(i));
                }
                chatPanelManager.hideMultipleChoice();
                CommonAnim.getInstance().rotateImage(bageVBinding.topLayout.backIv, 180f, 360f, R.mipmap.ic_ab_back);
                numberTextView.setNumber(0, true);
                hideOrShowRightView(true);
                EndpointManager.getInstance().invoke("chat_page_reset", getChatChannelInfo());
                CommonAnim.getInstance().showOrHide(numberTextView, false, true);
            } else {
                if (chatPanelManager.isCanBack()) {
                    new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(this::finish, 150);
                }
            }
        }
        return false;
    }


    // 定时上报已读消息
    private void startTimer() {
        Observable.interval(0, 3, TimeUnit.SECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<>() {
            @Override
            public void onComplete() {
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
            }

            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                disposable = d;
            }

            @Override
            public void onNext(@io.reactivex.rxjava3.annotations.NonNull Long value) {
                if (BageReader.isEmpty(readMsgIds) || !isUploadReadMsg) {
                    return;
                }
                List<String> msgIds = new ArrayList<>(readMsgIds);
                EndpointManager.getInstance().invoke("read_msg", new ReadMsgMenu(channelId, channelType, msgIds));
                readMsgIds.clear();
            }
        });
    }

    private void resetHideChannelAllPinnedMessage() {
        String key = String.format("hide_pin_msg_%s_%s", channelId, channelType);
        hideChannelAllPinnedMessage = BageSharedPreferencesUtil.getInstance().getIntWithUID(key);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            EndpointManager.getInstance().invoke("chat_activity_touch", null);
            if (chatPanelManager != null) {
                chatPanelManager.dismissChatFunctionOnOutsideTouch(ev);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float density = getResources().getDisplayMetrics().density;
        AndroidUtilities.setDensity(density);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            AndroidUtilities.isPORTRAIT = false;
            chatAdapter.notifyItemRangeChanged(0, chatAdapter.getItemCount());
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏
            AndroidUtilities.isPORTRAIT = true;
            chatAdapter.notifyItemRangeChanged(0, chatAdapter.getItemCount());
        }
    }

    @Override
    public void sendMessage(BageMessageContent messageContent) {

        if (messageContent.type == BageContentType.Bage_TEXT && editMsg != null) {
            //han
            if(checkEditTime(editMsg.createdAt)){
                deleteOperationMsg();
                return;
            }
            JSONObject jsonObject = messageContent.encodeMsg();
            if (jsonObject == null) jsonObject = new JSONObject();
            try {
                jsonObject.put("type", messageContent.type);
            } catch (JSONException e) {
                Log.e("消息类型错误", "-->");
            }
            boolean isUpdate = isUpdate(messageContent);
            if (isUpdate) {
                BageIM.getInstance().getMsgManager().updateMsgEdit(editMsg.messageID, channelId, channelType, jsonObject.toString());
            }
            deleteOperationMsg();
            return;
        }
        if (messageContent.type == BageContentType.Bage_TEXT && replyBageMsg != null) {
            BageReply bageReply = new BageReply();
            if (replyBageMsg.remoteExtra != null && replyBageMsg.remoteExtra.contentEditMsgModel != null) {
                bageReply.payload = replyBageMsg.remoteExtra.contentEditMsgModel;
            } else {
                bageReply.payload = replyBageMsg.baseContentMsgModel;
            }
            String showName = "";
            if (replyBageMsg.getFrom() != null) {
                showName = replyBageMsg.getFrom().channelName;
            } else {
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(replyBageMsg.fromUID, BageChannelType.PERSONAL);
                if (channel != null) showName = channel.channelName;
            }
            bageReply.from_name = showName;
            bageReply.from_uid = replyBageMsg.fromUID;
            bageReply.message_id = replyBageMsg.messageID;
            bageReply.message_seq = replyBageMsg.messageSeq;
            if (replyBageMsg.baseContentMsgModel.reply != null && !TextUtils.isEmpty(replyBageMsg.baseContentMsgModel.reply.root_mid)) {
                bageReply.root_mid = replyBageMsg.baseContentMsgModel.reply.root_mid;
            } else {
                bageReply.root_mid = bageReply.message_id;
            }
            messageContent.reply = bageReply;
        }
        sendMsg(messageContent);
        replyBageMsg = null;

    }

    private void sendMsg(BageMessageContent messageContent) {
        if (redDot > 0) {
            bageVBinding.chatUnreadLayout.newMsgLayout.performClick();
        }
        BageMsg bageMsg = new BageMsg();
        bageMsg.channelID = channelId;
        bageMsg.channelType = channelType;
        bageMsg.type = messageContent.type;
        bageMsg.baseContentMsgModel = messageContent;
        BageChannel channel = getChatChannelInfo();
        bageMsg.setChannelInfo(channel);
        BageSendMsgUtils.getInstance().sendMessage(bageMsg);
    }

    private boolean isUpdate(BageMessageContent messageContent) {
        boolean isUpdate = false;
        if (editMsg.remoteExtra != null && editMsg.remoteExtra.contentEditMsgModel != null) {
            if (!editMsg.remoteExtra.contentEditMsgModel.getDisplayContent().equals(messageContent.getDisplayContent())) {
                isUpdate = true;
            }
        }
        if (!editMsg.baseContentMsgModel.getDisplayContent().equals(messageContent.getDisplayContent())) {
            isUpdate = true;
        }
        return isUpdate;
    }

    private void setOnlineView(BageChannel channel) {
        if (channel.online == 1) {
            String device = getString(R.string.phone);
            if (channel.deviceFlag == UserOnlineStatus.Web) device = getString(R.string.web);
            else if (channel.deviceFlag == UserOnlineStatus.PC) device = getString(R.string.pc);
            String content = String.format("%s%s", device, getString(R.string.online));
            bageVBinding.topLayout.subtitleTv.setText(content);
            bageVBinding.topLayout.subtitleView.setVisibility(View.VISIBLE);
        } else {
            if (channel.lastOffline > 0) {
                String showTime = BageTimeUtils.getInstance().getOnlineTime(channel.lastOffline);
                if (TextUtils.isEmpty(showTime)) {
                    bageVBinding.topLayout.subtitleView.setVisibility(View.VISIBLE);
                    String time = BageTimeUtils.getInstance().getShowDateAndMinute(channel.lastOffline * 1000L);
                    String content = String.format("%s%s", getString(R.string.last_seen_time), time);
                    bageVBinding.topLayout.subtitleTv.setText(content);
                } else {
                    bageVBinding.topLayout.subtitleTv.setText(showTime);
                    bageVBinding.topLayout.subtitleView.setVisibility(View.VISIBLE);
                }
            } else bageVBinding.topLayout.subtitleView.setVisibility(View.GONE);
        }
    }

    @Override
    public BageChannel getChatChannelInfo() {
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelId, channelType);
        if (channel == null) {
            channel = new BageChannel(channelId, channelType);
        }
        return channel;
    }

    @Override
    public void showMultipleChoice() {
        chatPanelManager.showMultipleChoice();
        CommonAnim.getInstance().rotateImage(bageVBinding.topLayout.backIv, 180f, 360f, R.mipmap.ic_close_white);
        CommonAnim.getInstance().showOrHide(numberTextView, true, true);
        CommonAnim.getInstance().showOrHide(callIV, false, false);
        EndpointManager.getInstance().invoke("hide_pinned_view", null);
    }

    @Override
    public void setTitleRightText(String text) {
        int num = Integer.parseInt(text);
        chatPanelManager.updateForwardView(num);
        numberTextView.setNumber(num, true);
        CommonAnim.getInstance().showOrHide(numberTextView, true, true);
        CommonAnim.getInstance().showOrHide(callIV, false, false);
    }

    @Override
    public void showReply(BageMsg bageMsg) {
        this.editMsg = null;
        boolean showDialog = false;
        BageChannelMember mChannelMember = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelId, channelType);
        if (channel != null && mChannelMember != null) {
            if ((channel.forbidden == 1 && mChannelMember.role == BageChannelMemberRole.normal) || mChannelMember.forbiddenExpirationTime > 0) {
                //普通成员
                showDialog = true;
            }
        }

        if (showDialog) {
            BageDialogUtils.getInstance().showSingleBtnDialog(this, "", getString(R.string.cannot_reply_msg), "", null);
            return;
        }

        if (channelType == BageChannelType.GROUP && !bageMsg.fromUID.equals(loginUID)) {
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, bageMsg.fromUID);
            if (member != null) {
                chatPanelManager.addSpan(member.memberName, member.memberUID);
            } else {
                BageChannel mChannel = BageIM.getInstance().getChannelManager().getChannel(bageMsg.fromUID, BageChannelType.PERSONAL);
                if (mChannel != null) {
                    chatPanelManager.addSpan(mChannel.channelName, mChannel.channelID);
                }
            }
//            BageVBinding.toolbarView.editText.addAtSpan("@", member.memberName, member.memberUID);
        }
        this.replyBageMsg = bageMsg;
        if (replyBageMsg != null) {
            chatPanelManager.showReplyLayout(replyBageMsg);
        }

    }

    @Override
    public void showEdit(BageMsg bageMsg) {
        boolean showDialog = false;
        BageChannelMember mChannelMember = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, loginUID);
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelId, channelType);
        if (channel != null && mChannelMember != null) {
            if ((channel.forbidden == 1 && mChannelMember.role == BageChannelMemberRole.normal) || mChannelMember.forbiddenExpirationTime > 0) {
                //普通成员
                showDialog = true;
            }
        }

        if (showDialog) {
            BageDialogUtils.getInstance().showSingleBtnDialog(this, "", getString(R.string.cannot_edit_msg), "", null);
            return;
        }
        this.replyBageMsg = null;
        if (bageMsg != null) {
            this.editMsg = bageMsg;
            chatPanelManager.showEditLayout(bageMsg);

            //han
            if(checkEditTime(this.editMsg.createdAt)){
                deleteOperationMsg();
            }
        }

    }

    @Override
    public void tipsMsg(String clientMsgNo) {

        isTipMessage = true;
        int index = -1;
        for (int i = 0, size = chatAdapter.getData().size(); i < size; i++) {
            if (chatAdapter.getData().get(i).bageMsg != null && chatAdapter.getData().get(i).bageMsg.clientMsgNO.equals(clientMsgNo)) {
                chatAdapter.getData().get(i).isShowTips = true;
                index = i;
                break;
            }
        }
        if (index != -1) {
            int lastItemPosition = linearLayoutManager.findLastVisibleItemPosition();
            int firstItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
            if (index < firstItemPosition || index > lastItemPosition) {
                linearLayoutManager.scrollToPositionWithOffset(index, AndroidUtilities.dp(70));
            }
            chatAdapter.notifyItemChanged(index);
        } else {
            BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
            if (msg != null && msg.isDeleted == 0) {
                unreadStartMsgOrderSeq = 0;
                tipsOrderSeq = msg.orderSeq;
                // keepMessageSeq = msg.orderSeq;
                getData(0, true, msg.orderSeq, true);
                isCanLoadMore = true;
            } else {
                showToast(R.string.cannot_tips_msg);
            }
        }

    }

    @Override
    public void setEditContent(String content) {

        int curPosition = chatPanelManager.getEditText().getSelectionStart();
        StringBuilder sb = new StringBuilder(Objects.requireNonNull(chatPanelManager.getEditText().getText()).toString());
        sb.insert(curPosition, content);
        chatPanelManager.getEditText().setText(MoonUtil.getEmotionContent(this, chatPanelManager.getEditText(), sb.toString()));
        // 将光标设置到新增完表情的右侧
        chatPanelManager.getEditText().setSelection(curPosition + content.length());

    }

    @Override
    public AppCompatActivity getChatActivity() {
        return this;
    }

    @Override
    public BageMsg getReplyMsg() {
        return replyBageMsg;
    }

    @Override
    public void hideSoftKeyboard() {
        mHelper.hookSystemBackByPanelSwitcher();
    }

    @Override
    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }

    @Override
    public void sendCardMsg() {

        Intent intent = new Intent(this, ChooseContactsActivity.class);
        intent.putExtra("chooseBack", true);
        intent.putExtra("singleChoose", true);
        if (channelType == BageChannelType.PERSONAL) {
            intent.putExtra("unVisibleUIDs", channelId);
        }
        chooseCardResultLac.launch(intent);
    }

    @Override
    public void chatRecyclerViewScrollToEnd() {
        if (isToEnd) {
            scrollToEnd();
        }

    }

    @Override
    public void deleteOperationMsg() {

        this.replyBageMsg = null;
        this.editMsg = null;
    }

    @Override
    public void onChatAvatarClick(String uid, boolean isLongClick) {
        chatPanelManager.chatAvatarClick(uid, isLongClick);
    }

    @Override
    public void onViewPicture(boolean isViewing) {
        isViewingPicture = isViewing;
    }

    @Override
    public void onMsgViewed(BageMsg bageMsg, int position) {
        if (bageMsg == null) return;
        if (!TextUtils.isEmpty(bageMsg.messageID) && !isTipMessage) {
            EndpointManager.getInstance().invoke("tip_pinned_message", bageMsg.messageID);
        }
        if (bageMsg.flame == 1 && bageMsg.viewed == 0 && bageMsg.type != BageContentType.Bage_IMAGE && bageMsg.type != BageContentType.Bage_VIDEO && bageMsg.type != BageContentType.Bage_VOICE) {

            bageMsg.viewed = 1;
            bageMsg.viewedAt = BageTimeUtils.getInstance().getCurrentMills();
            chatAdapter.updateDeleteTimer(position);
            BageIM.getInstance().getMsgManager().updateViewedAt(1, bageMsg.viewedAt, bageMsg.clientMsgNO);
        }
        if (bageMsg.viewed == 0 && bageMsg.type == BageContentType.Bage_TEXT) {
            bageMsg.viewed = 1;
        }

        if (bageMsg.remoteExtra.readed == 0 && bageMsg.setting != null && bageMsg.setting.receipt == 1 && !TextUtils.isEmpty(bageMsg.fromUID) && !bageMsg.fromUID.equals(loginUID)) {
            boolean isAdd = true;
            for (int j = 0, size = readMsgIds.size(); j < size; j++) {
                if (readMsgIds.get(j).equals(bageMsg.messageID)) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                readMsgIds.add(bageMsg.messageID);
            }
        }
        boolean isResetRemind = false;
        if (BageReader.isNotEmpty(reminderList) && !TextUtils.isEmpty(bageMsg.messageID)) {
            for (int j = 0; j < reminderList.size(); j++) {
                if (reminderList.get(j).messageID.equals(bageMsg.messageID)) {
                    if (reminderList.get(j).done == 0) {
                        reminderIds.add(reminderList.get(j).reminderID);
                    }
                    reminderList.remove(j);
                    j = j - 1;
                    isResetRemind = true;
                }
            }
        }

        boolean isResetGroupApprove = false;
        if (BageReader.isNotEmpty(groupApproveList) && !TextUtils.isEmpty(bageMsg.messageID)) {
            for (int j = 0, size = groupApproveList.size(); j < size; j++) {
                if (groupApproveList.get(j).messageID.equals(bageMsg.messageID) && groupApproveList.get(j).done == 0) {
                    reminderIds.add(groupApproveList.get(j).reminderID);
                    groupApproveList.remove(j);
                    isResetGroupApprove = true;
                    break;
                }
            }
        }

        // 保存最新浏览到的位置
        if (bageMsg.messageSeq > browseTo) {
            browseTo = bageMsg.messageSeq;
        }
        boolean isResetUnread = false;
        if (bageMsg.messageSeq > lastVisibleMsgSeq) {
            lastVisibleMsgSeq = bageMsg.messageSeq;
        }
        if (lastVisibleMsgSeq != 0) {
            long lastVisibleMsgOrderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(lastVisibleMsgSeq, channelId, channelType);
            if (lastVisibleMsgOrderSeq < unreadStartMsgOrderSeq) {
                lastVisibleMsgSeq = (int) BageIM.getInstance().getMsgManager().getReliableMessageSeq(unreadStartMsgOrderSeq);
                lastVisibleMsgSeq = lastVisibleMsgSeq - 1;
            }
        }
        if (redDot > 0) {
            if (lastVisibleMsgSeq != 0) {
                redDot = maxMsgSeq - lastVisibleMsgSeq;
            }
            if (redDot < 0) redDot = 0;
            isResetUnread = true;

        }

        if (isResetGroupApprove) {
            resetGroupApproveView();
        }
        if (isResetUnread) {
            showUnReadCountView();
        }
        if (isResetRemind) {
            resetRemindView();
        }
    }

    @Override
    public View getRecyclerViewLayout() {
        return bageVBinding.recyclerViewLayout;
    }

    @Override
    public boolean isShowChatActivity() {
        return isShowChatActivity;
    }

    @Override
    public void closeActivity() {
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        SoftKeyboardUtils.getInstance().hideSoftKeyboard(this);
        EndpointManager.getInstance().remove(channelId);
        EndpointManager.getInstance().invoke("stop_screen_shot", this);
        BageIM.getInstance().getMsgManager().removeDeleteMsgListener(channelId);
        BageIM.getInstance().getMsgManager().removeNewMsgListener(channelId);
        BageIM.getInstance().getMsgManager().removeRefreshMsgListener(channelId);
        BageIM.getInstance().getMsgManager().removeSendMsgCallBack(channelId);
        BageIM.getInstance().getChannelManager().removeRefreshChannelInfo(channelId);
        BageIM.getInstance().getChannelMembersManager().removeRefreshChannelMemberInfo(channelId);
        BageIM.getInstance().getChannelMembersManager().removeAddChannelMemberListener(channelId);
        BageIM.getInstance().getChannelMembersManager().removeRemoveChannelMemberListener(channelId);
        BageIM.getInstance().getCMDManager().removeCmdListener(channelId);
        BageIM.getInstance().getMsgManager().removeSendMsgAckListener(channelId);
        BageIM.getInstance().getMsgManager().removeClearMsg(channelId);
        BageIM.getInstance().getRobotManager().removeRefreshRobotMenu(channelId);
        BageIM.getInstance().getReminderManager().removeNewReminderListener(channelId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatPanelManager.onDestroy();
        ActManagerUtils.getInstance().removeActivity(this);
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        if (BageReader.isNotEmpty(readMsgIds)) {
            EndpointManager.getInstance().invoke("read_msg", new ReadMsgMenu(channelId, channelType, readMsgIds));
        }
        MsgModel.getInstance().startCheckFlameMsgTimer();
        saveEditContent();

    }

    private void saveEditContent() {
        if (BageReader.isEmpty(chatAdapter.getData())) {
            return;
        }
        //停止语音播放
        //AudioPlaybackManager.getInstance().stopAudio();
        int firstItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
        int endItemPosition = linearLayoutManager.findLastVisibleItemPosition();
        long keepMsgSeq = 0;
        int offsetY = 0;
        if (endItemPosition != chatAdapter.getData().size() - 1) {
            BageMsg msg = chatAdapter.getFirstVisibleItem(firstItemPosition);
            if (msg != null) {
                keepMsgSeq = msg.messageSeq;
                int index = chatAdapter.getFirstVisibleItemIndex(firstItemPosition);
                View view = linearLayoutManager.findViewByPosition(index);
                if (view != null) {
                    offsetY = view.getTop();
                }
            }
        }
//        int unreadCount = bageVBinding.chatUnreadLayout.msgCountTv.getCount();
        MsgModel.getInstance().clearUnread(channelId, channelType, redDot, null);
        String content = Objects.requireNonNull(chatPanelManager.getEditText().getText()).toString();
        MsgModel.getInstance().updateCoverExtra(channelId, channelType, browseTo, keepMsgSeq, offsetY, content);
        MsgModel.getInstance().deleteFlameMsg();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return setBackListener();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isShowChatActivity = false;
        BageUIKitApplication.getInstance().chattingChannelID = "";
        isUploadReadMsg = false;
        BagePlayVoiceUtils.getInstance().stopPlay();
        MsgModel.getInstance().doneReminder(reminderIds);
        EndpointManager.getInstance().invoke("stop_screen_shot", this);
    }


    ActivityResultLauncher<Intent> previewNewImgResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
            String path = result.getData().getStringExtra("path");
            if (!TextUtils.isEmpty(path)) {
                sendMsg(new BageChatImageContent(path));
            }
        }
    });
    ActivityResultLauncher<Intent> chooseCardResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
            String uid = result.getData().getStringExtra("uid");
            if (!TextUtils.isEmpty(uid)) {
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
                BageCardContent BageCardContent = new BageCardContent();
                BageCardContent.name = channel.channelName;
                BageCardContent.uid = channel.channelID;
                if (channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(BageChannelExtras.vercode))
                    BageCardContent.vercode = (String) channel.remoteExtraMap.get(BageChannelExtras.vercode);
                List<BageMessageContent> messageContentList = new ArrayList<>();
                messageContentList.add(BageCardContent);
                List<BageChannel> list = new ArrayList<>();
                list.add(BageIM.getInstance().getChannelManager().getChannel(channelId, channelType));
                BageUIKitApplication.getInstance().showChatConfirmDialog(ChatActivity.this, list, messageContentList, (list1, messageContentList1) -> sendMsg(BageCardContent));
            }
        }
    });

    private synchronized void sendMsgInserted(BageMsg msg) {
        if (msg.channelType == channelType && msg.channelID.equals(channelId) && msg.isDeleted == 0 && !msg.header.noPersist) {
            if (msg.orderSeq > maxMsgOrderSeq) {
                maxMsgOrderSeq = msg.orderSeq;
            }
            BageMsg timeMsg = addTimeMsg(msg.timestamp);
            //判断当前会话是否存在正在输入
            int index = chatAdapter.getData().size() - 1;
            if (chatAdapter.lastMsgIsTyping()) index--;
            if (index < 0) index = 0;
            BageUIChatMsgItemEntity itemEntity = BageIMUtils.getInstance().msg2UiMsg(this, msg, count, showNickName, chatAdapter.isShowChooseItem());
            if (timeMsg == null) {
                if (BageReader.isNotEmpty(chatAdapter.getData())) {
                    chatAdapter.getData().get(index).nextMsg = msg;
                    itemEntity.previousMsg = chatAdapter.getData().get(index).bageMsg;
                }
            } else {
                chatAdapter.getData().get(index).nextMsg = timeMsg;
                itemEntity.previousMsg = timeMsg;
            }
            chatAdapter.addData(index + 1, itemEntity);
            int type = chatAdapter.getData().get(index).bageMsg.type;
            if (BageContentType.isLocalMsg(type) || BageContentType.isSystemMsg(type)) {
                chatAdapter.notifyItemChanged(index);
            } else {
                chatAdapter.notifyBackground(index);
            }

            if (isToEnd) {
                scrollToEnd();
            }
            isToEnd = true;
        }
    }

    private synchronized void receivedMessages(List<BageMsg> list) {
        if (BageReader.isNotEmpty(list)) {
            for (BageMsg msg : list) {
                // 命令消息和撤回消息不显示在聊天
                if (msg.type == BageContentType.Bage_INSIDE_MSG || msg.type == BageContentType.withdrawSystemInfo || msg.isDeleted == 1 || msg.header.noPersist)
                    continue;

                if (msg.remoteExtra.readedCount == 0) {
                    msg.remoteExtra.unreadCount = count - 1;
                }
                if (msg.channelID.equals(channelId) && msg.channelType == channelType) {
                    if (!chatAdapter.isExist(msg.clientMsgNO, msg.messageID)) {
                        if (!isCanLoadMore) {
                            //移除正在输入
                            if (chatAdapter.getItemCount() > 0 && chatAdapter.getData().get(chatAdapter.getItemCount() - 1).bageMsg != null && chatAdapter.getData().get(chatAdapter.getItemCount() - 1).bageMsg.type == BageContentType.typing) {
                                chatAdapter.removeAt(chatAdapter.getItemCount() - 1);
                            }
                            BageMsg timeMsg = addTimeMsg(msg.timestamp);
                            BageUIChatMsgItemEntity itemEntity = BageIMUtils.getInstance().msg2UiMsg(this, msg, count, showNickName, chatAdapter.isShowChooseItem());
                            if (timeMsg != null && chatAdapter.getData().size() > 1) {
                                chatAdapter.getData().get(chatAdapter.getData().size() - 2).nextMsg = timeMsg;
                            }
                            int previousMsgIndex = -1;
                            if (timeMsg == null) {
                                if (BageReader.isNotEmpty(chatAdapter.getData())) {
                                    itemEntity.previousMsg = chatAdapter.getData().get(chatAdapter.getData().size() - 1).bageMsg;
                                    chatAdapter.getData().get(chatAdapter.getData().size() - 1).nextMsg = itemEntity.bageMsg;
                                }
                            } else {
                                itemEntity.previousMsg = timeMsg;
                            }
                            if (BageReader.isNotEmpty(chatAdapter.getData())) {
                                previousMsgIndex = chatAdapter.getData().size() - 1;
                            }
                            if (!isShowHistory && redDot == 0 && itemEntity.bageMsg.flame == 1 && itemEntity.bageMsg.type != BageContentType.Bage_VOICE && itemEntity.bageMsg.type != BageContentType.Bage_IMAGE && itemEntity.bageMsg.type != BageContentType.Bage_VIDEO) {
                                itemEntity.bageMsg.viewed = 1;
                                itemEntity.bageMsg.viewedAt = BageTimeUtils.getInstance().getCurrentMills();
                                BageIM.getInstance().getMsgManager().updateViewedAt(1, itemEntity.bageMsg.viewedAt, itemEntity.bageMsg.clientMsgNO);
                            }
                            BagePlaySound.getInstance().playInMsg(R.raw.sound_in);
                            chatAdapter.addData(itemEntity);
                            if (msg.messageSeq > maxMsgSeq) {
                                maxMsgSeq = msg.messageSeq;
                            }
                            if (msg.orderSeq > maxMsgOrderSeq) {
                                maxMsgOrderSeq = msg.orderSeq;
                            }
                            if (previousMsgIndex != -1) {
                                chatAdapter.notifyBackground(previousMsgIndex);
                            }
                        }
                        if (isShowHistory || redDot > 0) {
                            redDot += 1;
                            showUnReadCountView();
                            bageVBinding.chatUnreadLayout.newMsgLayout.post(() -> CommonAnim.getInstance().showOrHide(bageVBinding.chatUnreadLayout.newMsgLayout, redDot > 0, true, false));
                        } else {
                            scrollToEnd();
                            if (msg.setting.receipt == 1) readMsgIds.add(msg.messageID);
                        }
                    }
                }

            }
        }
    }

    private synchronized void typing(BageCMD bageCmd) {

        if (redDot > 0) return;
        String channel_id = bageCmd.paramJsonObject.optString("channel_id");
        byte channel_type = (byte) bageCmd.paramJsonObject.optInt("channel_type");
        String from_uid = bageCmd.paramJsonObject.optString("from_uid");
        String from_name = bageCmd.paramJsonObject.optString("from_name");
        int isRobot;
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(from_uid, BageChannelType.PERSONAL);
        if (channel == null) {
            channel = new BageChannel(from_uid, BageChannelType.PERSONAL);
            channel.channelName = from_name;
        }
        isRobot = channel.robot;
        if (channelId.equals(channel_id) && channelType == channel_type && !TextUtils.equals(from_uid, loginUID)) {
            BageChannelMember mChannelMember = null;
            if (channelType == BageChannelType.GROUP && isRobot == 0) {
                // 没在群内的cmd不显示
                mChannelMember = BageIM.getInstance().getChannelMembersManager().getMember(channelId, channelType, from_uid);
                if (mChannelMember == null || mChannelMember.isDeleted == 1) return;
            }
            if (chatAdapter.getItemCount() > 0 && chatAdapter.getData().get(chatAdapter.getItemCount() - 1).bageMsg.type == BageContentType.typing) {
                chatAdapter.getData().get(chatAdapter.getItemCount() - 1).bageMsg.setFrom(channel);
                chatAdapter.getData().get(chatAdapter.getItemCount() - 1).bageMsg.fromUID = from_uid;
                chatAdapter.getData().get(chatAdapter.getItemCount() - 1).bageMsg.setMemberOfFrom(mChannelMember);
                chatAdapter.notifyItemChanged(chatAdapter.getItemCount() - 1);
            } else {
                addTimeMsg(BageTimeUtils.getInstance().getCurrentSeconds());
                int index = chatAdapter.getData().size() - 1;
                if (chatAdapter.lastMsgIsTyping()) index--;
                if (index < 0) index = 0;

                BageUIChatMsgItemEntity msgItemEntity = new BageUIChatMsgItemEntity(this, new BageMsg(), null);
                msgItemEntity.bageMsg.channelType = channelType;
                msgItemEntity.bageMsg.channelID = channelId;
                msgItemEntity.bageMsg.type = BageContentType.typing;
                msgItemEntity.bageMsg.setFrom(channel);
                msgItemEntity.showNickName = showNickName;
                msgItemEntity.bageMsg.fromUID = channel.channelID;
                BageChannelMember member = new BageChannelMember();
                member.memberUID = channel.channelID;
                member.channelID = channelId;
                member.channelType = channelType;
                member.memberName = channel.channelName;
                member.memberRemark = channel.channelRemark;
                msgItemEntity.bageMsg.setMemberOfFrom(member);
                msgItemEntity.previousMsg = chatAdapter.getLastMsg();
                chatAdapter.addData(msgItemEntity);
                chatAdapter.getData().get(index).nextMsg = msgItemEntity.bageMsg;

                int type = chatAdapter.getData().get(index).bageMsg.type;
                if (BageContentType.isLocalMsg(type) || BageContentType.isSystemMsg(type)) {
                    chatAdapter.notifyItemChanged(index);
                } else {
                    chatAdapter.notifyBackground(index);
                }

                if (!isShowHistory && !isCanLoadMore) {
                    scrollToEnd();
                }
            }
        }
    }

    private synchronized void refreshMsg(BageMsg bageMsg) {
        BageIMUtils.getInstance().resetMsgProhibitWord(bageMsg);
        List<BageUIChatMsgItemEntity> list = chatAdapter.getData();
        chatAdapter.refreshReplyMsg(bageMsg);
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).bageMsg == null) {
                continue;
            }
            boolean isNotify = false;
            if (list.get(i).bageMsg.clientSeq == bageMsg.clientSeq
                    || list.get(i).bageMsg.clientMsgNO.equals(bageMsg.clientMsgNO)
                    || (!TextUtils.isEmpty(list.get(i).bageMsg.messageID) && !TextUtils.isEmpty(bageMsg.messageID) && list.get(i).bageMsg.messageID.equals(bageMsg.messageID))) {
                if (bageMsg.messageSeq > maxMsgSeq) {
                    maxMsgSeq = bageMsg.messageSeq;
                }
                if (bageMsg.messageSeq > lastVisibleMsgSeq) {
                    lastVisibleMsgSeq = bageMsg.messageSeq;
                }
                if (list.get(i).bageMsg.remoteExtra.revoke != bageMsg.remoteExtra.revoke) {
                    isNotify = true;
                }
                // 消息撤回
                list.get(i).bageMsg.remoteExtra.revoke = bageMsg.remoteExtra.revoke;
                list.get(i).bageMsg.remoteExtra.revoker = bageMsg.remoteExtra.revoker;
                if (list.get(i).bageMsg.status != BageSendMsgResult.send_success && bageMsg.status == BageSendMsgResult.send_success) {
                    BagePlaySound.getInstance().playOutMsg(R.raw.sound_out);
                }
                boolean isResetStatus = false;
                boolean isResetListener = false;
                boolean isResetData = false;
                boolean isResetReaction = false;
                if (list.get(i).bageMsg.status != bageMsg.status
                        || (list.get(i).bageMsg.remoteExtra.readedCount != bageMsg.remoteExtra.readedCount && list.get(i).bageMsg.remoteExtra.readedCount == 0)
                        || list.get(i).bageMsg.remoteExtra.editedAt != bageMsg.remoteExtra.editedAt
                ) {
                    list.get(i).isUpdateStatus = true;
                    isResetStatus = true;
                }
                if (list.get(i).bageMsg.remoteExtra.isPinned != bageMsg.remoteExtra.isPinned) {
                    isResetStatus = true;
                }
                list.get(i).bageMsg.voiceStatus = bageMsg.voiceStatus;

                if (hideChannelAllPinnedMessage == 0) {
                    list.get(i).isPinned = bageMsg.remoteExtra.isPinned;
                } else {
                    list.get(i).isPinned = 0;
                }
                if (list.get(i).bageMsg.remoteExtra.readedCount != bageMsg.remoteExtra.readedCount && !isResetStatus) {
                    isResetListener = true;
                }
                list.get(i).bageMsg.remoteExtra.isPinned = bageMsg.remoteExtra.isPinned;
                list.get(i).bageMsg.remoteExtra.readed = bageMsg.remoteExtra.readed;
                list.get(i).bageMsg.remoteExtra.readedCount = bageMsg.remoteExtra.readedCount;
                list.get(i).bageMsg.remoteExtra.needUpload = bageMsg.remoteExtra.needUpload;
                if (list.get(i).bageMsg.remoteExtra.readedCount == 0) {
                    list.get(i).bageMsg.remoteExtra.unreadCount = count - 1;
                } else
                    list.get(i).bageMsg.remoteExtra.unreadCount = bageMsg.remoteExtra.unreadCount;
                if ((TextUtils.isEmpty(list.get(i).bageMsg.remoteExtra.contentEdit) && !TextUtils.isEmpty(bageMsg.remoteExtra.contentEdit)) || (!TextUtils.isEmpty(list.get(i).bageMsg.remoteExtra.contentEdit) && !TextUtils.isEmpty(bageMsg.remoteExtra.contentEdit) && !list.get(i).bageMsg.remoteExtra.contentEdit.equals(bageMsg.remoteExtra.contentEdit))) {
                    list.get(i).bageMsg.remoteExtra.editedAt = bageMsg.remoteExtra.editedAt;
                    list.get(i).bageMsg.remoteExtra.contentEdit = bageMsg.remoteExtra.contentEdit;
                    list.get(i).bageMsg.remoteExtra.contentEditMsgModel = bageMsg.remoteExtra.contentEditMsgModel;
                    list.get(i).isUpdateStatus = true;
                    list.get(i).formatSpans(ChatActivity.this, chatAdapter.getData().get(i).bageMsg);
                    isResetData = true;
                }

                list.get(i).bageMsg.isDeleted = bageMsg.isDeleted;
                list.get(i).bageMsg.messageID = bageMsg.messageID;
                list.get(i).bageMsg.messageSeq = bageMsg.messageSeq;
                list.get(i).bageMsg.orderSeq = bageMsg.orderSeq;
                if ((bageMsg.localExtraMap != null && !bageMsg.localExtraMap.isEmpty())) {
                    isNotify = true;
                }
                if (isRefreshReaction(list.get(i).bageMsg.reactionList, bageMsg.reactionList)) {
                    isResetReaction = true;
                }
                list.get(i).bageMsg.localExtraMap = bageMsg.localExtraMap;
                list.get(i).bageMsg.content = bageMsg.content;
                list.get(i).bageMsg.reactionList = bageMsg.reactionList;
                list.get(i).bageMsg.baseContentMsgModel = bageMsg.baseContentMsgModel;
                list.get(i).bageMsg.status = bageMsg.status;
                if (isNotify) {
                    EndpointManager.getInstance().invoke("stop_reaction_animation", null);
                    chatAdapter.notifyItemChanged(i);
                } else {
                    if (isResetStatus) {
                        chatAdapter.notifyStatus(i);
                    }
                    if (isResetListener) {
                        chatAdapter.notifyListener(i);
                    }
                    if (isResetData) {
                        chatAdapter.notifyData(i);
                    }
                    if (isResetReaction) {
                        list.get(i).isRefreshReaction = true;
                        chatAdapter.notifyItemChanged(i, list.get(i));
                        //chatAdapter.notifyReaction(i, bageMsg.reactionList);
                    }
                }

                if (list.get(i).bageMsg.remoteExtra.revoke == 1) {
                    int finalI = i;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        int previousIndex = finalI - 1;
                        int nextIndex = finalI + 1;
                        if (previousIndex >= 0 && list.get(previousIndex).bageMsg.remoteExtra.revoke == 0) {
                            chatAdapter.notifyItemChanged(previousIndex);
                        }
                        if (nextIndex <= chatAdapter.getData().size() - 1 && list.get(nextIndex).bageMsg.remoteExtra.revoke == 0) {
                            chatAdapter.notifyItemChanged(nextIndex);
                        }
                    }, 200);
                }

                if ((bageMsg.status == BageSendMsgResult.no_relation || bageMsg.status == BageSendMsgResult.not_on_white_list) && channelType == BageChannelType.PERSONAL) {
                    if (UserUtils.getInstance().checkBlacklist(channelId)) {
                        return;
                    }
                    // 不是好友
                    BageMsg noRelationMsg = new BageMsg();
                    noRelationMsg.channelID = channelId;
                    noRelationMsg.channelType = channelType;
                    noRelationMsg.type = BageContentType.noRelation;
                    long tempOrderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(0, bageMsg.channelID, bageMsg.channelType);
                    noRelationMsg.orderSeq = tempOrderSeq + 1;
                    noRelationMsg.status = BageSendMsgResult.send_success;

                    int index = chatAdapter.getData().size() - 1;
                    if (chatAdapter.lastMsgIsTyping()) index--;
                    BageUIChatMsgItemEntity itemEntity = BageIMUtils.getInstance().msg2UiMsg(this, noRelationMsg, count, showNickName, chatAdapter.isShowChooseItem());
                    chatAdapter.getData().get(index).nextMsg = noRelationMsg;
                    itemEntity.previousMsg = chatAdapter.getData().get(index).bageMsg;

                    chatAdapter.notifyItemChanged(index);
                    chatAdapter.addData(index + 1, itemEntity);
                    if (isToEnd) {
                        scrollToEnd();
                    }
                    BageIM.getInstance().getMsgManager().saveAndUpdateConversationMsg(noRelationMsg, false);
                }
                break;
            }
        }
    }

    private BageMsg getSpanEmptyMsg() {
        BageMsg msg = new BageMsg();
        msg.timestamp = 0;
        // 为了方便直接用该字段替换
        msg.messageSeq = getTopPinViewHeight();
        msg.type = BageContentType.spanEmptyView;
        return msg;
    }

    private boolean isAddedSpanEmptyView() {
        return BageReader.isNotEmpty(chatAdapter.getData()) && chatAdapter.getData().get(0).bageMsg != null && chatAdapter.getData().get(0).bageMsg.type == BageContentType.spanEmptyView;
    }
}
