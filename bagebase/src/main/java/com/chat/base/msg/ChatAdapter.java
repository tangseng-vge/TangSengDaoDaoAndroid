package com.chat.base.msg;

import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseProviderMultiAdapter;
import com.chad.library.adapter.base.provider.BaseItemProvider;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.R;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ShowMsgReactionMenu;
import com.chat.base.msgitem.BageChatBaseProvider;
import com.chat.base.msgitem.BageChatIteMsgFromType;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageMsgItemViewManager;
import com.chat.base.msgitem.BageUIChatMsgItemEntity;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.ui.components.SecretDeleteTimer;
import com.chat.base.utils.BageReader;
import com.chat.base.views.ChatItemView;
import com.chat.base.views.pinnedsectionitemdecoration.utils.FullSpanUtil;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageMsgReaction;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2020-08-03 13:46
 * 消息适配器
 */
public class ChatAdapter extends BaseProviderMultiAdapter<BageUIChatMsgItemEntity> {
    private final IConversationContext iConversationContext;

    public enum AdapterType {
        normalMessage, pinnedMessage
    }


    @Override
    public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, BageContentType.msgPromptTime);
    }

    @Override
    public void onViewAttachedToWindow(@NotNull BaseViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        FullSpanUtil.onViewAttachedToWindow(holder, this, BageContentType.msgPromptTime);
    }

    private final AdapterType adapterType;

    ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> getItemProviderList() {
        return adapterType == AdapterType.normalMessage ? BageMsgItemViewManager.getInstance().getChatItemProviderList() : BageMsgItemViewManager.getInstance().getPinnedChatItemProviderList();
    }

    public ChatAdapter(@NonNull IConversationContext iConversationContext, AdapterType adapterType) {
        super();
        this.adapterType = adapterType;
        this.iConversationContext = iConversationContext;
        ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> list = getItemProviderList();
        for (int type : list.keySet()) {
            addItemProvider(Objects.requireNonNull(list.get(type)));
        }
    }

    @Override
    protected int getItemType(@NotNull List<? extends BageUIChatMsgItemEntity> list, int i) {
        if (list.get(i).bageMsg.remoteExtra != null && list.get(i).bageMsg.remoteExtra.revoke == 1) {
            //撤回消息
            return BageContentType.revoke;
        }
        if (getItemProviderList().containsKey(list.get(i).bageMsg.type)) {
            return list.get(i).bageMsg.type;
        }
        if (list.get(i).bageMsg.type >= 1000 && list.get(i).bageMsg.type <= 2000) {
            //系统消息
            return BageContentType.systemMsg;
        }
        return BageContentType.unknown_msg;
    }

    public long getLastTimeMsg() {
        long timestamp = 0;
        for (int i = getData().size() - 1; i >= 0; i--) {
            if (getData().get(i).bageMsg != null && getData().get(i).bageMsg.timestamp > 0) {
                timestamp = getData().get(i).bageMsg.timestamp;
                break;
            }
        }
        return timestamp;
    }


    public IConversationContext getConversationContext() {
        return iConversationContext;
    }

    //显示多选
    public void showMultipleChoice() {
        iConversationContext.showMultipleChoice();
    }

    public void hideSoftKeyboard() {
        iConversationContext.hideSoftKeyboard();
    }

    //回复某条消息
    public void replyMsg(BageMsg bageMsg) {
        iConversationContext.showReply(bageMsg);
    }

    public void showTitleRightText(String content) {
        iConversationContext.setTitleRightText(content);
    }

    //提示某条消息
    public void showTipsMsg(String clientMsgNo) {
        iConversationContext.tipsMsg(clientMsgNo);
    }

    //设置输入框内容
    public void setEditContent(String content) {
        iConversationContext.setEditContent(content);
    }

    //是否存在某条消息
    public boolean isExist(String clientMsgNo, String messageId) {
        if (TextUtils.isEmpty(clientMsgNo)) return false;
        boolean isExist = false;
        for (int i = 0, size = getData().size(); i < size; i++) {
            if (getData().get(i).bageMsg == null) {
                continue;
            }
            if (!TextUtils.isEmpty(messageId) && !TextUtils.isEmpty(getData().get(i).bageMsg.messageID) && getData().get(i).bageMsg.messageID.equals(messageId)) {
                isExist = true;
                break;
            }

            if (!TextUtils.isEmpty(getData().get(i).bageMsg.clientMsgNO) && getData().get(i).bageMsg.clientMsgNO.equals(clientMsgNo)) {
                isExist = true;
                break;
            }
        }
        return isExist;
    }

    //获取最后一条消息
    public BageMsg getLastMsg() {
        BageMsg bageMsg = null;
        for (int i = getData().size() - 1; i >= 0; i--) {
            if (getData().get(i).bageMsg != null
                    && getData().get(i).bageMsg.type != BageContentType.msgPromptNewMsg
                    && getData().get(i).bageMsg.type != BageContentType.typing) {
                bageMsg = getData().get(i).bageMsg;
                break;
            }
        }
        return bageMsg;
    }

    //获取最后一条消息是否为正在输入
    public boolean lastMsgIsTyping() {
        if (BageReader.isEmpty(getData())) {
            return false;
        }
        BageMsg lastMsg = getData().get(getData().size() - 1).bageMsg;
        return lastMsg != null && lastMsg.type == BageContentType.typing;
    }

    public long getEndMsgOrderSeq() {
        long oldestOrderSeq = 0;
        for (int i = getData().size() - 1; i >= 0; i--) {
            if (getData().get(i).bageMsg != null && getData().get(i).bageMsg.orderSeq != 0) {
                oldestOrderSeq = getData().get(i).bageMsg.orderSeq;
                break;
            }
        }
        return oldestOrderSeq;
    }

    public long getFirstMsgOrderSeq() {
        long oldestOrderSeq = 0;
        for (int i = 0, size = getData().size(); i < size; i++) {
            if (getData().get(i).bageMsg != null && getData().get(i).bageMsg.orderSeq != 0) {
                oldestOrderSeq = getData().get(i).bageMsg.orderSeq;
                break;
            }
        }
        return oldestOrderSeq;
    }

    public void resetData(List<BageUIChatMsgItemEntity> list) {
        if (BageReader.isEmpty(list)) return;
        for (int i = 0, size = list.size(); i < size; i++) {
            int previousIndex = i - 1;
            int nextIndex = i + 1;
            if (previousIndex >= 0) {
                list.get(i).previousMsg = list.get(previousIndex).bageMsg;
            }
            if (nextIndex <= list.size() - 1) {
                list.get(i).nextMsg = list.get(nextIndex).bageMsg;
            }
        }
    }

    public int getFirstVisibleItemIndex(int startIndex) {
        int index = startIndex;
        if (startIndex <= getData().size() - 1) {
            if (getData().get(startIndex).bageMsg == null || getData().get(startIndex).bageMsg.orderSeq == 0) {
                for (int i = startIndex; i < getData().size(); i++) {
                    if (getData().get(i).bageMsg != null && getData().get(i).bageMsg.orderSeq != 0) {
                        index = i;
                        break;
                    }
                }
            }
        }
        return index;
    }

    public BageMsg getFirstVisibleItem(int startIndex) {
        BageMsg bageMsg = null;
        if (startIndex <= getData().size() - 1) {
            if (getData().get(startIndex).bageMsg == null || getData().get(startIndex).bageMsg.orderSeq == 0) {
                for (int i = startIndex; i < getData().size(); i++) {
                    if (getData().get(i).bageMsg != null && getData().get(i).bageMsg.orderSeq != 0) {
                        bageMsg = getData().get(i).bageMsg;
                        break;
                    }
                }
            } else {
                bageMsg = getData().get(startIndex).bageMsg;
            }
        }
        return bageMsg;
    }

    public boolean isShowChooseItem() {
        boolean isShowChoose = false;
        for (int i = 0, size = getData().size(); i < size; i++) {
            if (getData().get(i).isChoose) {
                isShowChoose = true;
                break;
            }
        }
        return isShowChoose;
    }

    public boolean isCanSwipe(int index) {
        if (index < 0 || index >= getData().size()) {
            return false;
        }
        int type = getData().get(index).bageMsg.type;
        if (type <= 0 || getData().get(index).bageMsg.flame == 1 || (getData().get(index).bageMsg.remoteExtra != null && getData().get(index).bageMsg.remoteExtra.revoke == 1)) {
            return false;
        }
        BageChannel channel = iConversationContext.getChatChannelInfo();
        ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> list = getItemProviderList();
        BageChatBaseProvider baseItemProvider = (BageChatBaseProvider) list.get(type);
        if (baseItemProvider != null && channel.status == 1)
            return baseItemProvider.getMsgConfig(type).isCanReply;
        return false;
    }

    public void updateDeleteTimer(int position) {
        BageUIChatMsgItemEntity entity = getData().get(position);
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        if (linearLayoutManager == null) return;
        View view = linearLayoutManager.findViewByPosition(position);
        LinearLayout baseView = null;
        if (view != null) {
            baseView = view.findViewById(R.id.bageBaseContentLayout);
        }
        if (baseView == null) return;
        ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> list = getItemProviderList();
        BageChatBaseProvider baseItemProvider = (BageChatBaseProvider) list.get(entity.bageMsg.type);
        if (baseItemProvider != null) {
            SecretDeleteTimer deleteTimer = null;
            BageChatIteMsgFromType from = baseItemProvider.getMsgFromType(entity.bageMsg);
            if (baseView.getChildCount() > 1) {
                if (from == BageChatIteMsgFromType.SEND) {
                    View childView = baseView.getChildAt(0);
                    if (childView instanceof SecretDeleteTimer) {
                        deleteTimer = (SecretDeleteTimer) childView;
                    }
                } else if (from == BageChatIteMsgFromType.RECEIVED) {
                    View childView = baseView.getChildAt(1);
                    if (childView instanceof SecretDeleteTimer) {
                        deleteTimer = (SecretDeleteTimer) childView;
                    }
                }
            }

            if (deleteTimer != null) {
                deleteTimer.setVisibility(View.VISIBLE);
                deleteTimer.setDestroyTime(entity.bageMsg.clientMsgNO, entity.bageMsg.flameSecond, entity.bageMsg.viewedAt, false);
            }
        }
    }


    public enum RefreshType {
        status, background, data, reaction, reply, listener
    }

    public void notifyStatus(int position) {
        notify(position, RefreshType.status, null);
    }

    public void notifyData(int position) {
        notify(position, RefreshType.data, null);
    }

    public void notifyListener(int position) {
        notify(position, RefreshType.listener, null);
    }

    public void notifyBackground(int position) {
        notify(position, RefreshType.background, null);
    }

    public void notifyReaction(int position, List<BageMsgReaction> reactionList) {
        notify(position, RefreshType.reaction, reactionList);
    }

    private void notify(int position, RefreshType refreshType, List<BageMsgReaction> reactionList) {
        BageUIChatMsgItemEntity entity = getData().get(position);
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getRecyclerView().getLayoutManager();
        if (linearLayoutManager == null) return;
        View view = linearLayoutManager.findViewByPosition(position);
        View baseView = null;
        if (view != null) {
            baseView = view.findViewById(R.id.bageBaseContentLayout);
        }
        if (baseView == null) return;
        ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> list = getItemProviderList();
        BageChatBaseProvider baseItemProvider = (BageChatBaseProvider) list.get(entity.bageMsg.type);
        if (baseItemProvider != null) {
            BageChatIteMsgFromType from = baseItemProvider.getMsgFromType(entity.bageMsg);
            // 刷新
            if (refreshType == RefreshType.data) {
                baseItemProvider.refreshData(position, baseView, entity, from);
                return;
            }
            if (refreshType == RefreshType.reaction) {
                FrameLayout reactionsView = view.findViewById(R.id.reactionsView);
                EndpointManager.getInstance().invoke(
                        "refresh_msg_reaction", new ShowMsgReactionMenu(
                                reactionsView,
                                from,
                                this,
                                reactionList)
                );
                AvatarView avatarView = view.findViewById(R.id.avatarView);
                if (avatarView != null) {
                    baseItemProvider.setAvatarLayoutParams(entity, from, avatarView);
                }
                return;
            }
            if (refreshType == RefreshType.background) {
                AvatarView avatarView = view.findViewById(R.id.avatarView);
                if (avatarView != null) {
                    baseItemProvider.setAvatarLayoutParams(entity, from, avatarView);
                }
                baseItemProvider.resetCellBackground(baseView, entity, from);
                LinearLayout fullContentLayout = view.findViewById(R.id.fullContentLayout);
                if (fullContentLayout != null) {
                    baseItemProvider.setFullLayoutParams(entity, from, fullContentLayout);
                }
                ChatItemView viewGroupLayout = view.findViewById(R.id.viewGroupLayout);
                if (viewGroupLayout != null) {
                    baseItemProvider.setItemPadding(position, viewGroupLayout);
                }
                return;
            }

            if (refreshType == RefreshType.status) {
                baseItemProvider.resetCellListener(position, baseView, entity, from);
                baseItemProvider.setMsgTimeAndStatus(
                        entity,
                        baseView,
                        from
                );
                return;
            }
            if (refreshType == RefreshType.listener) {
                baseItemProvider.resetCellListener(position, baseView, entity, from);
                return;
            }

            if (refreshType == RefreshType.reply) {
                baseItemProvider.refreshReply(position, baseView, entity, from);
            }
        }

    }

    public void refreshReplyMsg(BageMsg bageMsg) {
        if (bageMsg == null || bageMsg.remoteExtra == null || TextUtils.isEmpty(bageMsg.remoteExtra.messageID))
            return;
        List<BageUIChatMsgItemEntity> list = getData();
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).bageMsg.baseContentMsgModel == null || list.get(i).bageMsg.baseContentMsgModel.reply == null) {
                continue;
            }
            if (list.get(i).bageMsg.baseContentMsgModel.reply.message_seq == bageMsg.messageSeq) {
                list.get(i).bageMsg.baseContentMsgModel.reply.contentEditMsgModel = bageMsg.remoteExtra.contentEditMsgModel;
                list.get(i).bageMsg.baseContentMsgModel.reply.contentEdit = bageMsg.remoteExtra.contentEdit;
                list.get(i).bageMsg.baseContentMsgModel.reply.editAt = bageMsg.remoteExtra.editedAt;
                list.get(i).bageMsg.baseContentMsgModel.reply.revoke = bageMsg.remoteExtra.revoke;
                notify(i, RefreshType.reply, null);
            }
        }

    }
}
