package com.chat.uikit.chat.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKSystemAccount;
import com.chat.base.emoji.MoonUtil;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.AvatarOtherViewMenu;
import com.chat.base.endpoint.entity.ShowCommunityAvatarMenu;
import com.chat.base.entity.WKChannelState;
import com.chat.base.msgitem.WKContentType;
import com.chat.base.msgitem.WKMsgItemViewManager;
import com.chat.base.msgitem.WKRevokeProvider;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.ui.components.CounterView;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.StringUtils;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.WKTimeUtils;
import com.chat.uikit.R;
import com.chat.uikit.enity.ChatConversationMsg;
import com.chat.uikit.message.MsgModel;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelExtras;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.entity.WKMentionType;
import com.xinbida.wukongim.entity.WKMsg;
import com.xinbida.wukongim.entity.WKUIConversationMsg;
import com.xinbida.wukongim.message.type.WKSendMsgResult;

import org.jetbrains.annotations.NotNull;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2019-11-15 13:46
 * 会话记录适配器
 */
public class ChatConversationAdapter extends BaseQuickAdapter<ChatConversationMsg, BaseViewHolder> {
    /** 左滑后露出右侧 2/3 操作区，会话内容保留 1/3 宽度 */
    private static final float SWIPE_OPEN_RATIO = 2f / 3f;
    private static final int STICKY_TRANSITION_MS = 220;
    private static final int SWIPE_CLOSE_DURATION_MS = 260;
    private static final float SWIPE_CLOSE_THRESHOLD_RATIO = 0.35f;

    private IListener iListener;
    private RecyclerView recyclerView;
    private int openedPosition = RecyclerView.NO_POSITION;
    private int touchSlop;

    public ChatConversationAdapter(@Nullable List<ChatConversationMsg> data) {
        super(R.layout.item_chat_conv_layout, data);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.touchSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
    }

    public void closeOpenedSwipeIfNeeded(RecyclerView rv, MotionEvent e) {
        if (openedPosition == RecyclerView.NO_POSITION || e.getAction() != MotionEvent.ACTION_DOWN) {
            return;
        }
        View child = rv.findChildViewUnder(e.getX(), e.getY());
        int touchedPosition = child == null ? RecyclerView.NO_POSITION : rv.getChildAdapterPosition(child);
        if (touchedPosition != openedPosition) {
            closeOpenedSwipe();
        }
    }

    public void closeOpenedSwipe() {
        if (openedPosition == RecyclerView.NO_POSITION || recyclerView == null) {
            return;
        }
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(openedPosition);
        openedPosition = RecyclerView.NO_POSITION;
        if (holder instanceof BaseViewHolder baseHolder) {
            View contentLayout = baseHolder.getView(R.id.contentLayout);
            if (contentLayout.getTranslationX() < 0f) {
                animateSwipeClosed(baseHolder);
                return;
            }
            resetSwipeViews(baseHolder);
        }
    }

    public boolean isSwipeOpen(int position) {
        return openedPosition == position;
    }

    /** 收起所有已左滑展开的会话项（离开页面、切换 Tab、打开搜索等场景） */
    public void dismissAllSwipe() {
        openedPosition = RecyclerView.NO_POSITION;
        if (recyclerView == null) {
            return;
        }
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof BaseViewHolder) {
                resetSwipeViews((BaseViewHolder) holder);
            }
        }
    }

    @Override
    protected void convert(@NonNull final BaseViewHolder helper, ChatConversationMsg conversationMsg) {
        WKUIConversationMsg item = conversationMsg.uiConversationMsg;
        restoreOrResetSwipe(helper);
        setUnreadCount(helper, conversationMsg, false);
        showTime(helper, item);
        showChannel(helper, conversationMsg);
        showContent(helper, item);
        showReminders(helper, conversationMsg);
        setStatus(helper, item, false);
        showTyping(helper, conversationMsg);
        showCalling(helper, conversationMsg);
    }

    public void addListener(IListener iItemMenuClick) {
        this.iListener = iItemMenuClick;
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, ChatConversationMsg uiConversationMsg, @NotNull List<?> payloads) {
        ChatConversationMsg chatConversationMsg = (ChatConversationMsg) payloads.get(0);
        if (chatConversationMsg != null && chatConversationMsg.uiConversationMsg != null) {
            WKUIConversationMsg item = chatConversationMsg.uiConversationMsg;
//            showContent(baseViewHolder, item);
            if (chatConversationMsg.isResetCounter) {
                setUnreadCount(baseViewHolder, chatConversationMsg, true);
                chatConversationMsg.isResetCounter = false;
            }
            if (chatConversationMsg.isResetTime) {
                showTime(baseViewHolder, item);
                chatConversationMsg.isResetTime = false;
            }
            if (chatConversationMsg.isResetTyping) {
                showTyping(baseViewHolder, chatConversationMsg);
                chatConversationMsg.isResetTyping = false;
            }
            if (chatConversationMsg.isRefreshChannelInfo) {
                showChannel(baseViewHolder, chatConversationMsg);
                chatConversationMsg.isRefreshChannelInfo = false;
            }
            if (chatConversationMsg.isResetReminders) {
                showReminders(baseViewHolder, chatConversationMsg);
                chatConversationMsg.isResetReminders = false;
            }
            if (chatConversationMsg.isRefreshStatus) {
                setStatus(baseViewHolder, item, true);
                chatConversationMsg.isRefreshStatus = false;
            }
            if (chatConversationMsg.isResetContent) {
                showContent(baseViewHolder, item);
                chatConversationMsg.isResetContent = false;
            }
            showCalling(baseViewHolder, chatConversationMsg);
        }
    }

    public interface IListener {
        void onClick(ItemMenu menu, WKUIConversationMsg item);
    }


    private String getFromName(byte channelType, WKMsg msg) {
        String fromName = "";
        if (msg != null && (WKContentType.isSystemMsg(msg.type)
                || msg.type == WKContentType.revoke
                || msg.remoteExtra.revoke == 1 || msg.type == WKContentType.screenshot)) {
            return fromName;
        }
        if (channelType == WKChannelType.PERSONAL || channelType == WKChannelType.CUSTOMER_SERVICE || msg == null || TextUtils.isEmpty(msg.fromUID) || msg.fromUID.equals(WKConfig.getInstance().getUid())) {
            return fromName;
        }
        String channelName = "";
        String channelRemark = "";
        String memberRemark = "";
        String memberName = "";
        if (msg.getFrom() != null) {
            channelRemark = msg.getFrom().channelRemark;
            channelName = msg.getFrom().channelName;
        }
        if (!TextUtils.isEmpty(channelRemark)) {
            return channelRemark;
        }
        if (msg.getMemberOfFrom() != null) {
            memberName = msg.getMemberOfFrom().memberName;
            memberRemark = msg.getMemberOfFrom().memberRemark;
        }
        if (!TextUtils.isEmpty(memberRemark)) {
            return memberRemark;
        }
        fromName = TextUtils.isEmpty(channelName) ? memberName : channelName;
        return fromName;
    }

    private String getContent(WKMsg msg) {
        String content = "";
        if (msg == null || msg.isDeleted == 1) return content;
        if (msg.baseContentMsgModel != null) {
            content = msg.baseContentMsgModel.getDisplayContent();
        }

        if (TextUtils.isEmpty(content) || WKContentType.isSystemMsg(msg.type)) {
            content = getShowContent(msg.content);
        }
        if (msg.remoteExtra.contentEditMsgModel != null) {
            content = msg.remoteExtra.contentEditMsgModel.getDisplayContent();
        }
        //判断是否被撤回
        if (msg.remoteExtra.revoke == 1)
            content = WKRevokeProvider.Companion.showRevokeMsg(msg);
        else if (msg.type == WKContentType.WK_CONTENT_FORMAT_ERROR) {
            content = getContext().getString(R.string.str_content_format_err);
        } else if (msg.type == WKContentType.WK_SIGNAL_DECRYPT_ERROR) {
            content = getContext().getString(R.string.str_signal_decrypt_err);
        } else if (msg.type == WKContentType.noRelation) {
            String showName = "";
            if (msg.getChannelInfo() != null) {
                if (TextUtils.isEmpty(msg.getChannelInfo().channelRemark)) {
                    showName = msg.getChannelInfo().channelName;
                } else {
                    showName = msg.getChannelInfo().channelRemark;
                }
            }
            content = String.format(getContext().getString(R.string.no_relation_request), showName);
        } else {
            if (!WKMsgItemViewManager.getInstance().getChatItemProviderList().containsKey(msg.type)) {
                if (TextUtils.isEmpty(content)) {
                    content = getContext().getString(R.string.unknow_msg_type);
                }
            }
        }
        return content;
    }

    private String getShowContent(String contentJson) {
        return StringUtils.getShowContent(getContext(), contentJson);
    }

    private void setStatus(BaseViewHolder helper, WKUIConversationMsg item, boolean isPlayAnimation) {
        RLottieImageView sendingMsgIv = helper.getView(R.id.statusIV);
        RLottieDrawable drawable;
        boolean autoRepeat = false;
        int status = WKSendMsgResult.send_success;
        if (item.getWkMsg() != null) {
            status = item.getWkMsg().status;
        }
        boolean isSend = item.getWkMsg() != null && item.getWkMsg().isDeleted == 0 && !TextUtils.isEmpty(item.getWkMsg().fromUID) && item.getWkMsg().fromUID.equals(WKConfig.getInstance().getUid());
        if (isSend) {
            boolean isSingle = true;
            sendingMsgIv.setVisibility(View.VISIBLE);
            boolean isError = false;
            if (status == WKSendMsgResult.send_success) {
                // 自己发送
                if (item.getWkMsg().setting.receipt == 1 && item.getWkMsg().remoteExtra.readedCount > 0) {
                    drawable = new RLottieDrawable(getContext(), R.raw.ticks_double, "ticks_double", AndroidUtilities.dp(22), AndroidUtilities.dp(22));
                    isSingle = false;
                } else {
                    drawable = new RLottieDrawable(getContext(), R.raw.ticks_single, "ticks_single", AndroidUtilities.dp(22), AndroidUtilities.dp(22));
                }
                sendingMsgIv.setColorFilter(new PorterDuffColorFilter(Theme.colorAccount, PorterDuff.Mode.MULTIPLY));
            } else if (status == WKSendMsgResult.send_loading) {
                autoRepeat = true;
                drawable = new RLottieDrawable(getContext(), R.raw.msg_sending, "msg_sending", AndroidUtilities.dp(22), AndroidUtilities.dp(22));
                sendingMsgIv.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.color999), PorterDuff.Mode.MULTIPLY));
            } else {
                isError = true;
                sendingMsgIv.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.white), PorterDuff.Mode.MULTIPLY));
                drawable = new RLottieDrawable(getContext(), R.raw.error, "error", AndroidUtilities.dp(22), AndroidUtilities.dp(22));
            }
            sendingMsgIv.setAutoRepeat(autoRepeat);

            if (autoRepeat || isPlayAnimation) {
                sendingMsgIv.setAnimation(drawable);
                sendingMsgIv.playAnimation();
            } else {
                if (isError) {
                    sendingMsgIv.setAnimation(drawable);
                } else {
                    if (isSingle) {
                        sendingMsgIv.setImageDrawable(Theme.getTicksSingleDrawable());
                    } else sendingMsgIv.setImageDrawable(Theme.getTicksDoubleDrawable());
                }
            }
        } else {
            sendingMsgIv.setVisibility(View.GONE);
        }
        int finalStatus = status;
        sendingMsgIv.setOnClickListener(view -> {
            if (finalStatus != WKSendMsgResult.send_success && finalStatus != WKSendMsgResult.send_loading && item.getWkMsg() != null) {
                String content = getContext().getString(R.string.str_resend_msg_tips);
                if (finalStatus == WKSendMsgResult.no_relation) {
                    content = getContext().getString(R.string.no_relation_group);
                } else if (finalStatus == WKSendMsgResult.black_list) {
                    content =
                            getContext().getString(item.channelType == WKChannelType.GROUP ? R.string.blacklist_group : R.string.blacklist_user);

                } else if (finalStatus == WKSendMsgResult.not_on_white_list) {
                    content = getContext().getString(R.string.no_relation_user);
                }
                WKDialogUtils.getInstance().showDialog(getContext(), getContext().getString(R.string.msg_send_fail), content, true, "", getContext().getString(R.string.msg_send_fail_resend), 0, Theme.colorAccount, index -> {
                    if (index == 1) {
                        WKMsg msg = new WKMsg();
                        msg.channelID = item.channelID;
                        msg.channelType = item.channelType;
                        msg.setting = item.getWkMsg().setting;
                        msg.header = item.getWkMsg().header;
                        msg.type = item.getWkMsg().type;
                        msg.content = item.getWkMsg().content;
                        msg.baseContentMsgModel = item.getWkMsg().baseContentMsgModel;
                        msg.fromUID = WKConfig.getInstance().getUid();
                        WKIM.getInstance().getMsgManager()
                                .deleteWithClientMsgNO(item.getWkMsg().clientMsgNO);
                        WKIM.getInstance().getMsgManager().sendMessage(msg);
                    }
                });
            }
        });
    }

    private void setUnreadCount(@NotNull BaseViewHolder baseViewHolder, ChatConversationMsg item, boolean isAnimated) {
        CounterView counterView = baseViewHolder.getView(R.id.msgCountTv);
        boolean isMute;
        if (item.uiConversationMsg.getWkChannel() != null) {
            isMute = item.uiConversationMsg.getWkChannel().mute == 1;
        } else isMute = false;
        counterView.setColors(R.color.white, isMute ? R.color.color999 : R.color.reminderColor);
        counterView.setCount(item.getUnReadCount(), isAnimated);
        counterView.setGravity(Gravity.END);
        counterView.setVisibility(item.getUnReadCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void showTime(@NotNull BaseViewHolder helper, WKUIConversationMsg item) {
        long msgTimestamp = item.lastMsgTimestamp;
        if (item.getWkMsg() != null) {
            if (item.getWkMsg().remoteExtra.editedAt != 0) {
                msgTimestamp = item.getWkMsg().remoteExtra.editedAt;
            }
        }
        String chatTime = WKTimeUtils.getInstance().getNewChatTime(msgTimestamp * 1000);
        helper.setText(R.id.timeTv, chatTime);
    }

    private void showContent(@NotNull BaseViewHolder helper, WKUIConversationMsg item) {
        String content = getContent(item.getWkMsg());
        androidx.emoji2.widget.EmojiTextView contentTv = helper.getView(R.id.contentTv);
        boolean isSetChatPwd = isSetChatPwd(item.getWkChannel());
        // 聊天密码
        if (isSetChatPwd) {
            content = "❊❊❊❊❊❊❊❊❊❊❊❊❊";
        } else {
            String fromName = getFromName(item.channelType, item.getWkMsg());
            if (!TextUtils.isEmpty(fromName)) {
                content = fromName + "：" + content;
            }
        }
        //  contentTv.setText(content);
        MoonUtil.identifyFaceExpression(getContext(), contentTv, content, MoonUtil.SMALL_SCALE);
    }

    private void showReminders(@NotNull BaseViewHolder helper, ChatConversationMsg item) {
        TextView contentTv = helper.getView(R.id.contentTv);
        String draft = "";
        String approveContent = "";
        boolean mention = false;
        if (WKReader.isNotEmpty(item.getReminders())) {
            for (int i = 0, size = item.getReminders().size(); i < size; i++) {
                if (!mention && item.getReminders().get(i).type == WKMentionType.WKReminderTypeMentionMe && item.getReminders().get(i).done == 0) {
                    //存在@
                    mention = true;
                    // break;
                }
                if (item.getReminders().get(i).type == WKMentionType.WKApplyJoinGroupApprove && item.getReminders().get(i).done == 0) {
                    approveContent = getContext().getString(R.string.apply_join_group);
                }
            }
        }
        if (item.uiConversationMsg.getRemoteMsgExtra() != null) {
            draft = item.uiConversationMsg.getRemoteMsgExtra().draft;
        }
        boolean isSetChatPwd = isSetChatPwd(item.uiConversationMsg.getWkChannel());
        // 聊天密码
        if (isSetChatPwd) {
            if (!TextUtils.isEmpty(draft))
                draft = "❊❊❊❊❊❊❊❊❊❊❊❊❊";
        }
        LinearLayout remindLayout = helper.getView(R.id.remindLayout);
        remindLayout.removeAllViews();
        if (mention) {
            TextView textView = new TextView(getContext());
            textView.setTypeface(null, Typeface.BOLD);
            textView.setText(R.string.last_msg_remind);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.reminderColor));
            textView.setTextSize(13f);
            remindLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 5, 0));
        }
        if (!TextUtils.isEmpty(draft)) {
            TextView textView = new TextView(getContext());
            textView.setText(R.string.last_msg_draft);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.reminderColor));
            textView.setTextSize(13f);
            remindLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 5, 0));
            MoonUtil.identifyFaceExpression(getContext(), contentTv, draft, MoonUtil.SMALL_SCALE);
        } else {
            showContent(helper, item.uiConversationMsg);
        }
        if (!TextUtils.isEmpty(approveContent)) {
            TextView textView = new TextView(getContext());
            textView.setText(approveContent);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.reminderColor));
            textView.setTextSize(13f);
            remindLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 5, 0));
        }
    }

    private void showChannel(@NotNull BaseViewHolder helper, @NotNull ChatConversationMsg conversationMsg) {
        WKUIConversationMsg item = conversationMsg.uiConversationMsg;
        addEvent(helper, item, conversationMsg);
        String showName = "";
        if (item.channelID.equals(WKSystemAccount.system_file_helper)) {
            showName = getContext().getString(R.string.wk_file_helper);
        } else if (item.channelID.equals(WKSystemAccount.system_team)) {
            showName = getContext().getString(R.string.wk_system_notice);
        }
        helper.setGone(R.id.groupIV, item.channelType != WKChannelType.GROUP);
        AvatarView avatarView = helper.getView(R.id.avatarView);
        avatarView.setSize(50);
        if (item.getWkChannel() != null) {
            if (item.channelType == WKChannelType.COMMUNITY) {
                EndpointManager.getInstance().invoke("show_community_avatar", new ShowCommunityAvatarMenu(getContext(), avatarView, item.getWkChannel()));
            } else {
                avatarView.defaultAvatarTv.setVisibility(View.GONE);
                avatarView.imageView.setVisibility(View.VISIBLE);
                avatarView.showAvatar(item.getWkChannel(), true);
            }
            EndpointManager.getInstance().invoke("show_avatar_other_info", new AvatarOtherViewMenu(helper.getView(R.id.otherLayout), item.getWkChannel(), avatarView, false));
            if (TextUtils.isEmpty(showName))
                showName = TextUtils.isEmpty(item.getWkChannel().channelRemark) ? item.getWkChannel().channelName : item.getWkChannel().channelRemark;
            if (TextUtils.isEmpty(showName)) {
                showName = getContext().getString(R.string.chat);
//                if (!isScrolling)
                WKIM.getInstance().getChannelManager().fetchChannelInfo(item.channelID, item.channelType);
            }
            LinearLayout categoryLayout = helper.getView(R.id.categoryLayout);
            categoryLayout.removeAllViews();
            ImageView forbiddenIv = helper.getView(R.id.forbiddenIv);
            forbiddenIv.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.color999), PorterDuff.Mode.MULTIPLY));
            if (item.getWkChannel().mute == 1) {
                ImageView muteIV = new ImageView(getContext());
                muteIV.setImageResource(R.mipmap.list_mute);
                Theme.setColorFilter(muteIV, ContextCompat.getColor(getContext(), R.color.popupTextColor));
                categoryLayout.addView(muteIV, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 3, 1, 0, 0));
            }
            if (!TextUtils.isEmpty(item.getWkChannel().category)) {

                if (item.getWkChannel().category.equals(WKSystemAccount.accountCategorySystem)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.official), ContextCompat.getColor(getContext(), R.color.transparent), ContextCompat.getColor(getContext(), R.color.reminderColor), ContextCompat.getColor(getContext(), R.color.reminderColor)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getWkChannel().category.equals(WKSystemAccount.accountCategoryCustomerService)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.customer_service), Theme.colorAccount, ContextCompat.getColor(getContext(), R.color.white), Theme.colorAccount), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getWkChannel().category.equals(WKSystemAccount.accountCategoryVisitor)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.visitor), ContextCompat.getColor(getContext(), R.color.transparent), ContextCompat.getColor(getContext(), R.color.colorFFC107), ContextCompat.getColor(getContext(), R.color.colorFFC107)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getWkChannel().category.equals(WKSystemAccount.channelCategoryOrganization)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.all_staff), ContextCompat.getColor(getContext(), R.color.category_org_bg), ContextCompat.getColor(getContext(), R.color.category_org_text), ContextCompat.getColor(getContext(), R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getWkChannel().category.equals(WKSystemAccount.channelCategoryDepartment)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.department), ContextCompat.getColor(getContext(), R.color.category_org_bg), ContextCompat.getColor(getContext(), R.color.category_org_text), ContextCompat.getColor(getContext(), R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
            }
            if (item.channelType == WKChannelType.COMMUNITY) {
                categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.community), ContextCompat.getColor(getContext(), R.color.category_community_bg), ContextCompat.getColor(getContext(), R.color.category_community_text), ContextCompat.getColor(getContext(), R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
            }
            if (item.getWkChannel().robot == 1)
                categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.bot), ContextCompat.getColor(getContext(), R.color.colorFFC107), ContextCompat.getColor(getContext(), R.color.white), ContextCompat.getColor(getContext(), R.color.colorFFC107)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
            //判断是否禁言
            if (item.getWkChannel().forbidden == 1) {
                WKChannelMember mChannelMember = WKIM.getInstance().getChannelMembersManager().getMember(item.channelID, item.channelType, WKConfig.getInstance().getUid());
                if (mChannelMember != null && mChannelMember.role == 0) {
                    helper.setGone(R.id.forbiddenIv, false);
                } else helper.setGone(R.id.forbiddenIv, true);
            } else {
                helper.setGone(R.id.forbiddenIv, true);
            }
            //消息头像

//            GlideUtils.getInstance().showAvatarImg(getContext(), item.channelID, item.channelType, item.getWkChannel().avatar, helper.getView(R.id.avatarIv));
        } else {
            if (TextUtils.isEmpty(showName))
                showName = getContext().getString(R.string.chat);
            avatarView.defaultAvatarTv.setVisibility(View.GONE);
            avatarView.imageView.setVisibility(View.VISIBLE);
            avatarView.imageView.setImageResource(R.drawable.default_view_bg);
            //消息头像
//            avatarView.showAvatar(item.channelID, item.channelType);
//            GlideUtils.getInstance().showAvatarImg(getContext(), item.channelID, item.channelType, "", helper.getView(R.id.avatarIv));
            //重新获取频道信息
//            if (!isScrolling)
            WKIM.getInstance().getChannelManager().fetchChannelInfo(item.channelID, item.channelType);
        }
        applyStickyStyle(helper, conversationMsg);
        helper.setText(R.id.nameTv, showName);
    }

    private void applyStickyStyle(@NotNull BaseViewHolder helper, @NotNull ChatConversationMsg conversationMsg) {
        WKUIConversationMsg item = conversationMsg.uiConversationMsg;
        boolean isTop = item.getWkChannel() != null && item.getWkChannel().top == 1;
        boolean animate = conversationMsg.stickyStateChanged;
        conversationMsg.stickyStateChanged = false;
        conversationMsg.isTop = isTop ? 1 : 0;

        View contentLayout = helper.getView(R.id.contentLayout);
        View indicator = helper.getView(R.id.topStickIndicator);
        indicator.animate().cancel();
        contentLayout.animate().cancel();

        if (isTop) {
            if (animate) {
                animateStickyBackground(contentLayout, true);
                animateStickyIndicator(indicator, true);
            } else {
                contentLayout.setBackgroundResource(R.drawable.bg_chat_conv_sticky);
                indicator.setAlpha(1f);
                indicator.setTranslationX(0f);
                indicator.setVisibility(View.VISIBLE);
            }
        } else if (animate) {
            animateStickyBackground(contentLayout, false);
            animateStickyIndicator(indicator, false);
        } else {
            contentLayout.setBackgroundResource(R.drawable.layout_bg);
            indicator.setVisibility(View.GONE);
            indicator.setAlpha(1f);
            indicator.setTranslationX(0f);
        }
    }

    private void animateStickyBackground(View contentLayout, boolean toSticky) {
        Drawable normal = ContextCompat.getDrawable(getContext(), R.drawable.layout_bg);
        Drawable sticky = ContextCompat.getDrawable(getContext(), R.drawable.bg_chat_conv_sticky);
        if (normal == null || sticky == null) {
            contentLayout.setBackgroundResource(toSticky ? R.drawable.bg_chat_conv_sticky : R.drawable.layout_bg);
            return;
        }
        Drawable[] layers = toSticky
                ? new Drawable[]{normal.mutate(), sticky.mutate()}
                : new Drawable[]{sticky.mutate(), normal.mutate()};
        TransitionDrawable transition = new TransitionDrawable(layers);
        contentLayout.setBackground(transition);
        transition.startTransition(STICKY_TRANSITION_MS);
    }

    private void animateStickyIndicator(View indicator, boolean show) {
        int width = indicator.getWidth();
        if (width <= 0) {
            width = getContext().getResources().getDimensionPixelSize(R.dimen.chat_sticky_indicator_width);
        }
        if (show) {
            indicator.setVisibility(View.VISIBLE);
            indicator.setAlpha(0f);
            indicator.setTranslationX(-width);
            indicator.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(STICKY_TRANSITION_MS)
                    .start();
        } else if (indicator.getVisibility() == View.VISIBLE) {
            indicator.animate()
                    .alpha(0f)
                    .translationX(-width)
                    .setDuration(STICKY_TRANSITION_MS)
                    .withEndAction(() -> {
                        indicator.setVisibility(View.GONE);
                        indicator.setAlpha(1f);
                        indicator.setTranslationX(0f);
                    })
                    .start();
        } else {
            indicator.setVisibility(View.GONE);
        }
    }

    private boolean isSetChatPwd(WKChannel channel) {
        if (channel == null || channel.remoteExtraMap == null || !channel.remoteExtraMap.containsKey(WKChannelExtras.chatPwdOn))
            return false;
        boolean isSetChatPwd;
        Object object = channel.remoteExtraMap.get(WKChannelExtras.chatPwdOn);
        if (object != null) {
            isSetChatPwd = (int) object == 1;
        } else {
            isSetChatPwd = false;
        }
        return isSetChatPwd;
    }

    private void showTyping(@NotNull BaseViewHolder helper, ChatConversationMsg item) {
        helper.setGone(R.id.spinKit, item.typingStartTime <= 0);
        if (item.typingStartTime > 0) {
            String content;
            if (item.uiConversationMsg.channelType == WKChannelType.GROUP) {
                String name = item.typingUserName;
                content = String.format(getContext().getString(R.string.user_is_typing), name);
            } else {
                content = getContext().getString(R.string.other_is_typing);
            }
            helper.setText(R.id.contentTv, content);
        }
    }

    private void addEvent(@NotNull BaseViewHolder helper, WKUIConversationMsg item, @NotNull ChatConversationMsg conversationMsg) {
        boolean top;
        boolean mute;
        if (item.getWkChannel() != null) {
            top = item.getWkChannel().top == 1;
            mute = item.getWkChannel().mute == 1;
        } else {
            top = false;
            mute = false;
        }

        View muteAction = helper.getView(R.id.swipeActionMute);
        View topAction = helper.getView(R.id.swipeActionTop);
        View deleteAction = helper.getView(R.id.swipeActionDelete);

        if (item.getWkChannel() != null) {
            muteAction.setVisibility(View.VISIBLE);
            helper.setImageResource(R.id.swipeMuteIcon, mute ? R.mipmap.msg_unmute : R.mipmap.msg_mute);
            helper.setText(R.id.swipeMuteText, getContext().getString(mute ? R.string.open_channel_notice : R.string.close_channel_notice));
            muteAction.setOnClickListener(v -> {
                if (iListener != null) {
                    closeOpenedSwipe();
                    iListener.onClick(ItemMenu.mute, item);
                }
            });
        } else {
            muteAction.setVisibility(View.GONE);
            muteAction.setOnClickListener(null);
        }

        helper.setImageResource(R.id.swipeTopIcon, top ? R.mipmap.msg_unpin : R.mipmap.msg_pin);
        helper.setText(R.id.swipeTopText, getContext().getString(top ? R.string.cancel_top : R.string.msg_top));
        topAction.setOnClickListener(v -> {
            if (iListener != null) {
                conversationMsg.stickyStateChanged = true;
                closeOpenedSwipe();
                iListener.onClick(ItemMenu.top, item);
            }
        });

        helper.setImageResource(R.id.swipeDeleteIcon, R.mipmap.msg_delete);
        helper.setText(R.id.swipeDeleteText, getContext().getString(R.string.delete_msg));

        int white = ContextCompat.getColor(getContext(), R.color.white);
        Theme.setColorFilter(helper.getView(R.id.swipeMuteIcon), white);
        Theme.setColorFilter(helper.getView(R.id.swipeTopIcon), white);
        Theme.setColorFilter(helper.getView(R.id.swipeDeleteIcon), white);
        deleteAction.setOnClickListener(v -> {
            if (iListener != null) {
                closeOpenedSwipe();
                iListener.onClick(ItemMenu.delete, item);
            }
        });

        bindSwipeTouch(helper);
    }

    private void restoreOrResetSwipe(@NotNull BaseViewHolder helper) {
        int position = helper.getBindingAdapterPosition();
        View contentLayout = helper.getView(R.id.contentLayout);
        View swipeActionsContainer = helper.getView(R.id.swipeActionsContainer);
        View swipeRootLayout = helper.getView(R.id.swipeRootLayout);
        if (position == openedPosition) {
            swipeRootLayout.post(() -> {
                float maxSwipe = swipeRootLayout.getWidth() * SWIPE_OPEN_RATIO;
                contentLayout.setTranslationX(-maxSwipe);
                swipeActionsContainer.setVisibility(View.VISIBLE);
                swipeActionsContainer.setAlpha(1f);
            });
        } else {
            resetSwipeViews(helper);
        }
    }

    private void resetSwipeViews(@NotNull BaseViewHolder helper) {
        View contentLayout = helper.getView(R.id.contentLayout);
        View swipeActionsContainer = helper.getView(R.id.swipeActionsContainer);
        contentLayout.animate().cancel();
        swipeActionsContainer.animate().cancel();
        contentLayout.setTranslationX(0f);
        contentLayout.setTag(R.id.contentLayout, null);
        swipeActionsContainer.setAlpha(1f);
        swipeActionsContainer.setVisibility(View.GONE);
        if (helper.getBindingAdapterPosition() == openedPosition) {
            openedPosition = RecyclerView.NO_POSITION;
        }
    }

    private void updateSwipeActionsAlpha(@NotNull View swipeActionsContainer, float translationX, float maxSwipe) {
        if (translationX >= 0f || maxSwipe <= 0f) {
            swipeActionsContainer.setVisibility(View.GONE);
            swipeActionsContainer.setAlpha(1f);
            return;
        }
        swipeActionsContainer.setVisibility(View.VISIBLE);
        swipeActionsContainer.setAlpha(Math.min(1f, -translationX / maxSwipe));
    }

    private void animateSwipeClosed(@NotNull BaseViewHolder helper) {
        View contentLayout = helper.getView(R.id.contentLayout);
        View swipeActionsContainer = helper.getView(R.id.swipeActionsContainer);
        View swipeRootLayout = helper.getView(R.id.swipeRootLayout);
        float maxSwipe = swipeRootLayout.getWidth() * SWIPE_OPEN_RATIO;
        if (maxSwipe <= 0f || contentLayout.getTranslationX() >= 0f) {
            resetSwipeViews(helper);
            return;
        }
        contentLayout.animate().cancel();
        swipeActionsContainer.animate().cancel();
        swipeActionsContainer.setVisibility(View.VISIBLE);
        contentLayout.animate()
                .translationX(0f)
                .setDuration(SWIPE_CLOSE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setUpdateListener(animation ->
                        updateSwipeActionsAlpha(swipeActionsContainer, contentLayout.getTranslationX(), maxSwipe))
                .withEndAction(() -> resetSwipeViews(helper))
                .start();
    }

    private void animateSwipeOpen(@NotNull BaseViewHolder helper, float maxSwipe, int position) {
        View contentLayout = helper.getView(R.id.contentLayout);
        View swipeActionsContainer = helper.getView(R.id.swipeActionsContainer);
        contentLayout.animate().cancel();
        swipeActionsContainer.setVisibility(View.VISIBLE);
        contentLayout.animate()
                .translationX(-maxSwipe)
                .setDuration(SWIPE_CLOSE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setUpdateListener(animation ->
                        updateSwipeActionsAlpha(swipeActionsContainer, contentLayout.getTranslationX(), maxSwipe))
                .withEndAction(() -> {
                    swipeActionsContainer.setAlpha(1f);
                    openedPosition = position;
                })
                .start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindSwipeTouch(@NotNull BaseViewHolder helper) {
        View contentLayout = helper.getView(R.id.contentLayout);
        if (Boolean.TRUE.equals(contentLayout.getTag(R.id.swipeRootLayout))) {
            return;
        }
        contentLayout.setTag(R.id.swipeRootLayout, Boolean.TRUE);

        View swipeRootLayout = helper.getView(R.id.swipeRootLayout);
        View swipeActionsContainer = helper.getView(R.id.swipeActionsContainer);
        final float[] startX = {0f};
        final float[] startY = {0f};
        final float[] startTrans = {0f};
        final boolean[] tracking = {false};
        final boolean[] consumedSwipe = {false};

        contentLayout.setOnTouchListener((v, event) -> {
            if (recyclerView == null) {
                return false;
            }
            int position = helper.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = event.getX();
                    startY[0] = event.getY();
                    startTrans[0] = contentLayout.getTranslationX();
                    tracking[0] = false;
                    consumedSwipe[0] = false;
                    contentLayout.setTag(R.id.contentLayout, null);
                    return false;
                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getX() - startX[0];
                    float dy = event.getY() - startY[0];
                    if (!tracking[0] && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        tracking[0] = Math.abs(dx) > Math.abs(dy);
                        if (tracking[0]) {
                            if (openedPosition != RecyclerView.NO_POSITION && openedPosition != position) {
                                closeOpenedSwipe();
                            }
                            recyclerView.requestDisallowInterceptTouchEvent(true);
                        } else {
                            return false;
                        }
                    }
                    if (!tracking[0]) {
                        return false;
                    }
                    float maxSwipe = swipeRootLayout.getWidth() * SWIPE_OPEN_RATIO;
                    float newTrans = Math.max(-maxSwipe, Math.min(0f, startTrans[0] + dx));
                    contentLayout.setTranslationX(newTrans);
                    updateSwipeActionsAlpha(swipeActionsContainer, newTrans, maxSwipe);
                    consumedSwipe[0] = true;
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    recyclerView.requestDisallowInterceptTouchEvent(false);
                    float maxSwipe = swipeRootLayout.getWidth() * SWIPE_OPEN_RATIO;
                    float currentTrans = contentLayout.getTranslationX();
                    boolean wasOpened = startTrans[0] < -touchSlop || openedPosition == position;
                    if (!tracking[0]) {
                        if (wasOpened && currentTrans < -touchSlop) {
                            animateSwipeClosed(helper);
                            contentLayout.setTag(R.id.contentLayout, Boolean.TRUE);
                            return true;
                        }
                        return false;
                    }
                    float totalDx = event.getX() - startX[0];
                    if (wasOpened && (totalDx > touchSlop * 0.5f || currentTrans > -maxSwipe * SWIPE_CLOSE_THRESHOLD_RATIO)) {
                        if (openedPosition == position) {
                            openedPosition = RecyclerView.NO_POSITION;
                        }
                        animateSwipeClosed(helper);
                    } else if (Math.abs(currentTrans) > maxSwipe * 0.25f) {
                        if (openedPosition != position) {
                            openedPosition = RecyclerView.NO_POSITION;
                        }
                        animateSwipeOpen(helper, maxSwipe, position);
                    } else {
                        if (openedPosition == position) {
                            openedPosition = RecyclerView.NO_POSITION;
                        }
                        animateSwipeClosed(helper);
                    }
                    if (consumedSwipe[0]) {
                        contentLayout.setTag(R.id.contentLayout, Boolean.TRUE);
                    }
                    return consumedSwipe[0];
                default:
                    return false;
            }
        });
    }

    public boolean wasSwipeConsumed(BaseViewHolder helper) {
        View contentLayout = helper.getView(R.id.contentLayout);
        return Boolean.TRUE.equals(contentLayout.getTag(R.id.contentLayout));
    }

    private void showCalling(final BaseViewHolder helper, ChatConversationMsg conversationMsg) {
        helper.setGone(R.id.callingIv, conversationMsg.isCalling == 0);
    }

    public enum ItemMenu {
        delete, top, mute
    }
}
