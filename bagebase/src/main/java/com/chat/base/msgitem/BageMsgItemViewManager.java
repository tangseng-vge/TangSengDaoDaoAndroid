package com.chat.base.msgitem;

import com.chad.library.adapter.base.provider.BaseItemProvider;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 2020-08-05 17:41
 * 各类消息itemView管理
 */
public class BageMsgItemViewManager {

    private BageMsgItemViewManager() {
    }

    private static class MsgItemViewManagerBinder {
        final static BageMsgItemViewManager itemView = new BageMsgItemViewManager();
    }

    public static BageMsgItemViewManager getInstance() {
        return MsgItemViewManagerBinder.itemView;
    }

    private ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> chatItemProviderList;
    private ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> pinnedChatItemProviderList;


    public void addChatItemViewProvider(int type, BaseItemProvider<BageUIChatMsgItemEntity> itemProvider) {
        if (chatItemProviderList == null) {
            chatItemProviderList = new ConcurrentHashMap<>();
            chatItemProviderList.put(BageContentType.Bage_SIGNAL_DECRYPT_ERROR, new BageSignalDecryptErrorProvider());
            chatItemProviderList.put(BageContentType.Bage_CONTENT_FORMAT_ERROR, new BageChatFormatErrorProvider());
            chatItemProviderList.put(BageContentType.unknown_msg, new BageUnknownProvider());
            chatItemProviderList.put(BageContentType.typing, new BageTypingProvider());
            chatItemProviderList.put(BageContentType.revoke, new BageRevokeProvider());
            chatItemProviderList.put(BageContentType.systemMsg, new BageSystemProvider(BageContentType.systemMsg));
            chatItemProviderList.put(BageContentType.msgPromptTime, new BageSystemProvider(BageContentType.msgPromptTime));
            for (int i = 1000; i <= 2000; i++) {
                chatItemProviderList.put(i, new BageSystemProvider(i));
            }
        }
        chatItemProviderList.put(type, itemProvider);
        // 置顶消息的itemProvider
        if (pinnedChatItemProviderList == null) {
            pinnedChatItemProviderList = new ConcurrentHashMap<>();
            pinnedChatItemProviderList.put(BageContentType.Bage_SIGNAL_DECRYPT_ERROR, new BageSignalDecryptErrorProvider());
            pinnedChatItemProviderList.put(BageContentType.Bage_CONTENT_FORMAT_ERROR, new BageChatFormatErrorProvider());
            pinnedChatItemProviderList.put(BageContentType.unknown_msg, new BageUnknownProvider());
            pinnedChatItemProviderList.put(BageContentType.typing, new BageTypingProvider());
            pinnedChatItemProviderList.put(BageContentType.revoke, new BageRevokeProvider());
            pinnedChatItemProviderList.put(BageContentType.systemMsg, new BageSystemProvider(BageContentType.systemMsg));
            pinnedChatItemProviderList.put(BageContentType.msgPromptTime, new BageSystemProvider(BageContentType.msgPromptTime));
            for (int i = 1000; i <= 2000; i++) {
                pinnedChatItemProviderList.put(i, new BageSystemProvider(i));
            }
        }
        try {
            Object myObject = itemProvider.getClass().newInstance();
            pinnedChatItemProviderList.put(type, (BaseItemProvider<BageUIChatMsgItemEntity>) myObject);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

    }

    public ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> getChatItemProviderList() {
        return chatItemProviderList;
    }

    public ConcurrentHashMap<Integer, BaseItemProvider<BageUIChatMsgItemEntity>> getPinnedChatItemProviderList() {
        return pinnedChatItemProviderList;
    }

    public BaseItemProvider<BageUIChatMsgItemEntity> getItemProvider(Integer type) {
        if (chatItemProviderList != null) {
            return chatItemProviderList.get(type);
        }
        return null;
    }

    public BaseItemProvider<BageUIChatMsgItemEntity> getPinnedItemProvider(Integer type) {
        if (pinnedChatItemProviderList != null) {
            return pinnedChatItemProviderList.get(type);
        }
        return null;
    }
}
