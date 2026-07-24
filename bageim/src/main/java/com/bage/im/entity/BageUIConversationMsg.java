package com.bage.im.entity;


import android.text.TextUtils;

import com.bage.im.BageIM;
import com.bage.im.db.MsgDbManager;
import com.bage.im.db.ReminderDBManager;
import com.bage.im.interfaces.IReminderResult;
import com.bage.im.manager.ChannelManager;

import java.util.HashMap;
import java.util.List;

/**
 * 2019-12-01 17:50
 * UI层显示最近会话消息
 */
public class BageUIConversationMsg {
    public long lastMsgSeq;
    public String clientMsgNo;
    //频道ID
    public String channelID;
    //频道类型
    public byte channelType;
    //最后一条消息时间
    public long lastMsgTimestamp;
    //消息频道
    private BageChannel bageChannel;
    //消息正文
    private BageMsg bageMsg;
    //未读消息数量
    public int unreadCount;
    public int isDeleted;
    private BageConversationMsgExtra remoteMsgExtra;
    //高亮内容[{type:1,text:'[有人@你]'}]
    private List<BageReminder> reminderList;
    //扩展字段
    public HashMap<String, Object> localExtraMap;
    public String parentChannelID;
    public byte parentChannelType;


    public BageMsg getBageMsg() {
        if (bageMsg == null) {
            bageMsg = MsgDbManager.getInstance().queryWithClientMsgNo(clientMsgNo);
            if (bageMsg != null && bageMsg.isDeleted == 1) bageMsg = null;
        }
        return bageMsg;
    }

    public void setBageMsg(BageMsg bageMsg) {
        this.bageMsg = bageMsg;
    }

    public BageChannel getBageChannel() {
        if (bageChannel == null) {
            bageChannel = ChannelManager.getInstance().getChannel(channelID, channelType);
        }
        return bageChannel;
    }

    public void setBageChannel(BageChannel bageChannel) {
        this.bageChannel = bageChannel;
    }

    public List<BageReminder> getReminderList() {
        if (reminderList == null) {
            reminderList = BageIM.getInstance().getReminderManager().getReminders(channelID, channelType);
//            reminderList = ReminderDBManager.getInstance().queryWithChannelAndDone(channelID, channelType, 0);
        }

        return reminderList;
    }

//    public void getReminderListAsync(IReminderResult iReminderResult) {
//        ReminderDBManager.getInstance().queryWithChannelAndDoneAsync(channelID, channelType, 0, iReminderResult);
//    }

    public void setReminderList(List<BageReminder> list) {
        this.reminderList = list;
    }

    public BageConversationMsgExtra getRemoteMsgExtra() {
        return remoteMsgExtra;
    }

    public void setRemoteMsgExtra(BageConversationMsgExtra extra) {
        this.remoteMsgExtra = extra;
    }

    public long getSortTime() {
        if (getRemoteMsgExtra() != null && !TextUtils.isEmpty(getRemoteMsgExtra().draft)) {
            return getRemoteMsgExtra().draftUpdatedAt;
        }
        return lastMsgTimestamp;
    }

}
