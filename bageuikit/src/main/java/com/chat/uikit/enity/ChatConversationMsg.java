package com.chat.uikit.enity;

import android.text.TextUtils;

import com.chat.base.config.BageConfig;
import com.chat.base.utils.BageReader;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageReminder;
import com.bage.im.entity.BageUIConversationMsg;

import java.util.ArrayList;
import java.util.List;

public class ChatConversationMsg {
    public BageUIConversationMsg uiConversationMsg;
    public boolean isRefreshChannelInfo;
    public boolean isResetCounter;
    public boolean isResetReminders;
    public boolean isResetContent;
    public boolean isResetTime;
    public boolean isResetTyping;
    public boolean isRefreshStatus;
    public long typingStartTime = 0;
    public String typingUserName;
    public int isTop;
    /** 置顶状态变化时为 true，用于会话项置顶样式过渡动画 */
    public boolean stickyStateChanged;
    public List<ChatConversationMsg> childList;
    private final String loginUID;
    public int isCalling = 0;

    public ChatConversationMsg(BageUIConversationMsg msg) {
        this.uiConversationMsg = msg;
        if (uiConversationMsg.getBageChannel() != null) {
            isTop = uiConversationMsg.getBageChannel().top;
        }
        loginUID = BageConfig.getInstance().getUid();
        BageIMUtils.getInstance().resetMsgProhibitWord(msg.getBageMsg());
    }

    public int getUnReadCount() {
        if (BageReader.isEmpty(childList))
            return uiConversationMsg.unreadCount;
        int count = 0;
        for (ChatConversationMsg msg : childList) {
            count += msg.uiConversationMsg.unreadCount;
        }
        return count;
    }

    public List<BageReminder> getReminders() {
        List<BageReminder> list = new ArrayList<>();
        if (BageReader.isEmpty(childList)) {
            list.addAll(uiConversationMsg.getReminderList());
        } else {
            for (ChatConversationMsg msg : childList) {
                list.addAll(msg.uiConversationMsg.getReminderList());
            }
        }
        List<BageReminder> resultList = new ArrayList<>();
        for (BageReminder reminder : list) {
            if (TextUtils.isEmpty(reminder.publisher) || (!TextUtils.isEmpty(reminder.publisher) && !reminder.publisher.equals(loginUID))) {
                resultList.add(reminder);
            }
        }
        return resultList;
    }

    private BageMsg lastMsg;
    private String lastClientMsgNo = "";

    public BageMsg getMsg() {
        if (BageReader.isEmpty(childList))
            return uiConversationMsg.getBageMsg();
        String clientMsgNo = "";
        long lastMsgTimestamp = 0;
        for (ChatConversationMsg msg : childList) {
            if (msg.uiConversationMsg.lastMsgTimestamp > lastMsgTimestamp) {
                lastMsgTimestamp = msg.uiConversationMsg.lastMsgTimestamp;
                clientMsgNo = msg.uiConversationMsg.clientMsgNo;
            }
        }
        if (lastClientMsgNo.equals(clientMsgNo) && lastMsg != null) {
            return lastMsg;
        }

        lastClientMsgNo = clientMsgNo;
        lastMsg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(lastClientMsgNo);
        return lastMsg;
    }
}
