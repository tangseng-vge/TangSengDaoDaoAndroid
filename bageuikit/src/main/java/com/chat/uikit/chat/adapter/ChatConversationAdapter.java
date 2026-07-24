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
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.emoji.MoonUtil;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.AvatarOtherViewMenu;
import com.chat.base.endpoint.entity.ShowCommunityAvatarMenu;
import com.chat.base.entity.BageChannelState;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageMsgItemViewManager;
import com.chat.base.msgitem.BageRevokeProvider;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.ui.components.CounterView;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.StringUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.uikit.R;
import com.chat.uikit.enity.ChatConversationMsg;
import com.chat.uikit.message.MsgModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMentionType;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.message.type.BageSendMsgResult;

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
        BageUIConversationMsg item = conversationMsg.uiConversationMsg;
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
            BageUIConversationMsg item = chatConversationMsg.uiConversationMsg;
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
        void onClick(ItemMenu menu, BageUIConversationMsg item);
    }


    private String getFromName(byte channelType, BageMsg msg) {
        String fromName = "";
        if (msg != null && (BageContentType.isSystemMsg(msg.type)
                || msg.type == BageContentType.revoke
                || msg.remoteExtra.revoke == 1 || msg.type == BageContentType.screenshot)) {
            return fromName;
        }
        if (channelType == BageChannelType.PERSONAL || channelType == BageChannelType.CUSTOMER_SERVICE || msg == null || TextUtils.isEmpty(msg.fromUID) || msg.fromUID.equals(BageConfig.getInstance().getUid())) {
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

    private String getContent(BageMsg msg) {
        String content = "";
        if (msg == null || msg.isDeleted == 1) return content;
        if (msg.baseContentMsgModel != null) {
            content = msg.baseContentMsgModel.getDisplayContent();
        }

        if (TextUtils.isEmpty(content) || BageContentType.isSystemMsg(msg.type)) {
            content = getShowContent(msg.content);
        }
        if (msg.remoteExtra.contentEditMsgModel != null) {
            content = msg.remoteExtra.contentEditMsgModel.getDisplayContent();
        }
        //判断是否被撤回
        if (msg.remoteExtra.revoke == 1)
            content = BageRevokeProvider.Companion.showRevokeMsg(msg);
        else if (msg.type == BageContentType.Bage_CONTENT_FORMAT_ERROR) {
            content = getContext().getString(R.string.str_content_format_err);
        } else if (msg.type == BageContentType.Bage_SIGNAL_DECRYPT_ERROR) {
            content = getContext().getString(R.string.str_signal_decrypt_err);
        } else if (msg.type == BageContentType.noRelation) {
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
            if (!BageMsgItemViewManager.getInstance().getChatItemProviderList().containsKey(msg.type)) {
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

    private void setStatus(BaseViewHolder helper, BageUIConversationMsg item, boolean isPlayAnimation) {
        RLottieImageView sendingMsgIv = helper.getView(R.id.statusIV);
        RLottieDrawable drawable;
        boolean autoRepeat = false;
        int status = BageSendMsgResult.send_success;
        if (item.getBageMsg() != null) {
            status = item.getBageMsg().status;
        }
        boolean isSend = item.getBageMsg() != null && item.getBageMsg().isDeleted == 0 && !TextUtils.isEmpty(item.getBageMsg().fromUID) && item.getBageMsg().fromUID.equals(BageConfig.getInstance().getUid());
        if (isSend) {
            boolean isSingle = true;
            sendingMsgIv.setVisibility(View.VISIBLE);
            boolean isError = false;
            if (status == BageSendMsgResult.send_success) {
                // 自己发送
                if (item.getBageMsg().setting.receipt == 1 && item.getBageMsg().remoteExtra.readedCount > 0) {
                    drawable = new RLottieDrawable(getContext(), R.raw.ticks_double, "ticks_double", AndroidUtilities.dp(22), AndroidUtilities.dp(22));
                    isSingle = false;
                } else {
                    drawable = new RLottieDrawable(getContext(), R.raw.ticks_single, "ticks_single", AndroidUtilities.dp(22), AndroidUtilities.dp(22));
                }
                sendingMsgIv.setColorFilter(new PorterDuffColorFilter(Theme.colorAccount, PorterDuff.Mode.MULTIPLY));
            } else if (status == BageSendMsgResult.send_loading) {
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
            if (finalStatus != BageSendMsgResult.send_success && finalStatus != BageSendMsgResult.send_loading && item.getBageMsg() != null) {
                String content = getContext().getString(R.string.str_resend_msg_tips);
                if (finalStatus == BageSendMsgResult.no_relation) {
                    content = getContext().getString(R.string.no_relation_group);
                } else if (finalStatus == BageSendMsgResult.black_list) {
                    content =
                            getContext().getString(item.channelType == BageChannelType.GROUP ? R.string.blacklist_group : R.string.blacklist_user);

                } else if (finalStatus == BageSendMsgResult.not_on_white_list) {
                    content = getContext().getString(R.string.no_relation_user);
                }
                BageDialogUtils.getInstance().showDialog(getContext(), getContext().getString(R.string.msg_send_fail), content, true, "", getContext().getString(R.string.msg_send_fail_resend), 0, Theme.colorAccount, index -> {
                    if (index == 1) {
                        BageMsg msg = new BageMsg();
                        msg.channelID = item.channelID;
                        msg.channelType = item.channelType;
                        msg.setting = item.getBageMsg().setting;
                        msg.header = item.getBageMsg().header;
                        msg.type = item.getBageMsg().type;
                        msg.content = item.getBageMsg().content;
                        msg.baseContentMsgModel = item.getBageMsg().baseContentMsgModel;
                        msg.fromUID = BageConfig.getInstance().getUid();
                        BageIM.getInstance().getMsgManager()
                                .deleteWithClientMsgNO(item.getBageMsg().clientMsgNO);
                        BageIM.getInstance().getMsgManager().sendMessage(msg);
                    }
                });
            }
        });
    }

    private void setUnreadCount(@NotNull BaseViewHolder baseViewHolder, ChatConversationMsg item, boolean isAnimated) {
        CounterView counterView = baseViewHolder.getView(R.id.msgCountTv);
        boolean isMute;
        if (item.uiConversationMsg.getBageChannel() != null) {
            isMute = item.uiConversationMsg.getBageChannel().mute == 1;
        } else isMute = false;
        counterView.setColors(R.color.white, isMute ? R.color.color999 : R.color.reminderColor);
        counterView.setCount(item.getUnReadCount(), isAnimated);
        counterView.setGravity(Gravity.END);
        counterView.setVisibility(item.getUnReadCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void showTime(@NotNull BaseViewHolder helper, BageUIConversationMsg item) {
        long msgTimestamp = item.lastMsgTimestamp;
        if (item.getBageMsg() != null) {
            if (item.getBageMsg().remoteExtra.editedAt != 0) {
                msgTimestamp = item.getBageMsg().remoteExtra.editedAt;
            }
        }
        String chatTime = BageTimeUtils.getInstance().getNewChatTime(msgTimestamp * 1000);
        helper.setText(R.id.timeTv, chatTime);
    }

    private void showContent(@NotNull BaseViewHolder helper, BageUIConversationMsg item) {
        String content = getContent(item.getBageMsg());
        androidx.emoji2.widget.EmojiTextView contentTv = helper.getView(R.id.contentTv);
        boolean isSetChatPwd = isSetChatPwd(item.getBageChannel());
        // 聊天密码
        if (isSetChatPwd) {
            content = "❊❊❊❊❊❊❊❊❊❊❊❊❊";
        } else {
            String fromName = getFromName(item.channelType, item.getBageMsg());
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
        if (BageReader.isNotEmpty(item.getReminders())) {
            for (int i = 0, size = item.getReminders().size(); i < size; i++) {
                if (!mention && item.getReminders().get(i).type == BageMentionType.BageReminderTypeMentionMe && item.getReminders().get(i).done == 0) {
                    //存在@
                    mention = true;
                    // break;
                }
                if (item.getReminders().get(i).type == BageMentionType.BageApplyJoinGroupApprove && item.getReminders().get(i).done == 0) {
                    approveContent = getContext().getString(R.string.apply_join_group);
                }
            }
        }
        if (item.uiConversationMsg.getRemoteMsgExtra() != null) {
            draft = item.uiConversationMsg.getRemoteMsgExtra().draft;
        }
        boolean isSetChatPwd = isSetChatPwd(item.uiConversationMsg.getBageChannel());
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
        BageUIConversationMsg item = conversationMsg.uiConversationMsg;
        addEvent(helper, item, conversationMsg);
        String showName = "";
        if (item.channelID.equals(BageSystemAccount.system_file_helper)) {
            showName = getContext().getString(R.string.bage_file_helper);
        } else if (item.channelID.equals(BageSystemAccount.system_team)) {
            showName = getContext().getString(R.string.bage_system_notice);
        }
        helper.setGone(R.id.groupIV, item.channelType != BageChannelType.GROUP);
        AvatarView avatarView = helper.getView(R.id.avatarView);
        avatarView.setSize(50);
        if (item.getBageChannel() != null) {
            if (item.channelType == BageChannelType.COMMUNITY) {
                EndpointManager.getInstance().invoke("show_community_avatar", new ShowCommunityAvatarMenu(getContext(), avatarView, item.getBageChannel()));
            } else {
                avatarView.defaultAvatarTv.setVisibility(View.GONE);
                avatarView.imageView.setVisibility(View.VISIBLE);
                avatarView.showAvatar(item.getBageChannel(), true);
            }
            EndpointManager.getInstance().invoke("show_avatar_other_info", new AvatarOtherViewMenu(helper.getView(R.id.otherLayout), item.getBageChannel(), avatarView, false));
            if (TextUtils.isEmpty(showName))
                showName = TextUtils.isEmpty(item.getBageChannel().channelRemark) ? item.getBageChannel().channelName : item.getBageChannel().channelRemark;
            if (TextUtils.isEmpty(showName)) {
                showName = getContext().getString(R.string.chat);
//                if (!isScrolling)
                BageIM.getInstance().getChannelManager().fetchChannelInfo(item.channelID, item.channelType);
            }
            LinearLayout categoryLayout = helper.getView(R.id.categoryLayout);
            categoryLayout.removeAllViews();
            ImageView forbiddenIv = helper.getView(R.id.forbiddenIv);
            forbiddenIv.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.color999), PorterDuff.Mode.MULTIPLY));
            if (item.getBageChannel().mute == 1) {
                ImageView muteIV = new ImageView(getContext());
                muteIV.setImageResource(R.mipmap.list_mute);
                Theme.setColorFilter(muteIV, ContextCompat.getColor(getContext(), R.color.popupTextColor));
                categoryLayout.addView(muteIV, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 3, 1, 0, 0));
            }
            if (!TextUtils.isEmpty(item.getBageChannel().category)) {

                if (item.getBageChannel().category.equals(BageSystemAccount.accountCategorySystem)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.official), ContextCompat.getColor(getContext(), R.color.transparent), ContextCompat.getColor(getContext(), R.color.reminderColor), ContextCompat.getColor(getContext(), R.color.reminderColor)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getBageChannel().category.equals(BageSystemAccount.accountCategoryCustomerService)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.customer_service), Theme.colorAccount, ContextCompat.getColor(getContext(), R.color.white), Theme.colorAccount), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getBageChannel().category.equals(BageSystemAccount.accountCategoryVisitor)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.visitor), ContextCompat.getColor(getContext(), R.color.transparent), ContextCompat.getColor(getContext(), R.color.colorFFC107), ContextCompat.getColor(getContext(), R.color.colorFFC107)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getBageChannel().category.equals(BageSystemAccount.channelCategoryOrganization)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.all_staff), ContextCompat.getColor(getContext(), R.color.category_org_bg), ContextCompat.getColor(getContext(), R.color.category_org_text), ContextCompat.getColor(getContext(), R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
                if (item.getBageChannel().category.equals(BageSystemAccount.channelCategoryDepartment)) {
                    categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.department), ContextCompat.getColor(getContext(), R.color.category_org_bg), ContextCompat.getColor(getContext(), R.color.category_org_text), ContextCompat.getColor(getContext(), R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
                }
            }
            if (item.channelType == BageChannelType.COMMUNITY) {
                categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.community), ContextCompat.getColor(getContext(), R.color.category_community_bg), ContextCompat.getColor(getContext(), R.color.category_community_text), ContextCompat.getColor(getContext(), R.color.transparent)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
            }
            if (item.getBageChannel().robot == 1)
                categoryLayout.addView(Theme.getChannelCategoryTV(getContext(), getContext().getString(R.string.bot), ContextCompat.getColor(getContext(), R.color.colorFFC107), ContextCompat.getColor(getContext(), R.color.white), ContextCompat.getColor(getContext(), R.color.colorFFC107)), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 5, 1, 0, 0));
            //判断是否禁言
            if (item.getBageChannel().forbidden == 1) {
                BageChannelMember mChannelMember = BageIM.getInstance().getChannelMembersManager().getMember(item.channelID, item.channelType, BageConfig.getInstance().getUid());
                if (mChannelMember != null && mChannelMember.role == 0) {
                    helper.setGone(R.id.forbiddenIv, false);
                } else helper.setGone(R.id.forbiddenIv, true);
            } else {
                helper.setGone(R.id.forbiddenIv, true);
            }
            //消息头像

//            GlideUtils.getInstance().showAvatarImg(getContext(), item.channelID, item.channelType, item.getBageChannel().avatar, helper.getView(R.id.avatarIv));
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
            BageIM.getInstance().getChannelManager().fetchChannelInfo(item.channelID, item.channelType);
        }
        applyStickyStyle(helper, conversationMsg);
        helper.setText(R.id.nameTv, showName);
    }

    private void applyStickyStyle(@NotNull BaseViewHolder helper, @NotNull ChatConversationMsg conversationMsg) {
        BageUIConversationMsg item = conversationMsg.uiConversationMsg;
        boolean isTop = item.getBageChannel() != null && item.getBageChannel().top == 1;
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

    private boolean isSetChatPwd(BageChannel channel) {
        if (channel == null || channel.remoteExtraMap == null || !channel.remoteExtraMap.containsKey(BageChannelExtras.chatPwdOn))
            return false;
        boolean isSetChatPwd;
        Object object = channel.remoteExtraMap.get(BageChannelExtras.chatPwdOn);
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
            if (item.uiConversationMsg.channelType == BageChannelType.GROUP) {
                String name = item.typingUserName;
                content = String.format(getContext().getString(R.string.user_is_typing), name);
            } else {
                content = getContext().getString(R.string.other_is_typing);
            }
            helper.setText(R.id.contentTv, content);
        }
    }

    private void addEvent(@NotNull BaseViewHolder helper, BageUIConversationMsg item, @NotNull ChatConversationMsg conversationMsg) {
        boolean top;
        boolean mute;
        if (item.getBageChannel() != null) {
            top = item.getBageChannel().top == 1;
            mute = item.getBageChannel().mute == 1;
        } else {
            top = false;
            mute = false;
        }

        View muteAction = helper.getView(R.id.swipeActionMute);
        View topAction = helper.getView(R.id.swipeActionTop);
        View deleteAction = helper.getView(R.id.swipeActionDelete);

        if (item.getBageChannel() != null) {
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
