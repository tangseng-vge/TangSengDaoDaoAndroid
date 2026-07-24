package com.chat.uikit.chat;

import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.utils.BageTimeUtils;
import com.chat.uikit.R;
import com.chat.uikit.chat.adapter.ChatMultiForwardDetailAdapter;
import com.chat.uikit.chat.msgmodel.BageMultiForwardContent;
import com.chat.uikit.databinding.ActCommonListLayoutWhiteBinding;
import com.chat.uikit.enity.ChatMultiForwardEntity;
import com.bage.im.BageIM;
import com.bage.im.entity.BageCMDKeys;
import com.bage.im.entity.BageMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-09-22 11:57
 * 合并转发消息详情
 */
public class ChatMultiForwardDetailActivity extends BageBaseActivity<ActCommonListLayoutWhiteBinding> {

    BageMultiForwardContent BageMultiForwardContent;
    String clientMsgNo = "";

    @Override
    protected ActCommonListLayoutWhiteBinding getViewBinding() {
        return ActCommonListLayoutWhiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        String title;
        if (BageMultiForwardContent.channelType == 1) {
            if (BageMultiForwardContent.userList.size() > 1) {
                StringBuilder sBuilder = new StringBuilder();
                for (int i = 0; i < BageMultiForwardContent.userList.size(); i++) {
                    if (!TextUtils.isEmpty(sBuilder))
                        sBuilder.append("、");
                    sBuilder.append(BageMultiForwardContent.userList.get(i).channelName);
                }
                title = sBuilder.toString();
            } else title = BageMultiForwardContent.userList.get(0).channelName;
        } else {
            title = getString(R.string.group_chat);
        }
        titleTv.setText(String.format(getString(R.string.chat_title_records), title));
    }

    @Override
    protected void initPresenter() {
        clientMsgNo = getIntent().getStringExtra("client_msg_no");
        BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
        BageMultiForwardContent = (BageMultiForwardContent) msg.baseContentMsgModel;
        if (BageMultiForwardContent == null) {
            showToast("传入数据有误！");
            finish();
        }
        long minTime = 0;
        long maxTime = 0;
        for (int i = 0, size = BageMultiForwardContent.msgList.size(); i < size; i++) {
            if (BageMultiForwardContent.msgList.get(i).timestamp > maxTime || maxTime == 0)
                maxTime = BageMultiForwardContent.msgList.get(i).timestamp;
            if (BageMultiForwardContent.msgList.get(i).timestamp < minTime || minTime == 0)
                minTime = BageMultiForwardContent.msgList.get(i).timestamp;
        }
        String time;
        boolean showDetailTime;
        if (!BageTimeUtils.getInstance().isSameDayOfMillis(minTime * 1000, maxTime * 1000)) {
            showDetailTime = true;
            String tempTime1 = BageTimeUtils.getInstance().time2DataDay1(minTime * 1000);
            String tempTime2 = BageTimeUtils.getInstance().time2DataDay1(maxTime * 1000);
            time = String.format(getString(R.string.time_section), tempTime1, tempTime2);
        } else {
            showDetailTime = false;
            time = BageTimeUtils.getInstance().time2DataDay1(minTime * 1000);
        }
        List<ChatMultiForwardEntity> list = new ArrayList<>();
        ChatMultiForwardEntity entity = new ChatMultiForwardEntity();
        entity.itemType = 1;
        entity.title = time;
        list.add(entity);
        for (int i = 0, size = BageMultiForwardContent.msgList.size(); i < size; i++) {
            ChatMultiForwardEntity temp = new ChatMultiForwardEntity();
            temp.msg = BageMultiForwardContent.msgList.get(i);
//            if (temp.msg.type != 0)
            list.add(temp);
        }
        ChatMultiForwardEntity view = new ChatMultiForwardEntity();
        view.itemType = 2;
        list.add(view);
        ChatMultiForwardDetailAdapter adapter = new ChatMultiForwardDetailAdapter(showDetailTime, list);
        initAdapter(bageVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        BageIM.getInstance().getCMDManager().addCmdListener("chat_multi_forward_detail", cmd -> {
            if (!TextUtils.isEmpty(cmd.cmdKey)) {
                if (cmd.cmdKey.equals(BageCMDKeys.bage_messageRevoke)) {
                    if (cmd.paramJsonObject != null && cmd.paramJsonObject.has("message_id")) {
                        String msgID = cmd.paramJsonObject.optString("message_id");
                        BageMsg msg = BageIM.getInstance().getMsgManager().getWithMessageID(msgID);
                        if (msg != null) {
                            if (msg.clientMsgNO.equals(clientMsgNo)) {
                                showToast(getString(R.string.msg_revoked));
                                finish();
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BageIM.getInstance().getMsgManager().removeRefreshMsgListener("chat_multi_forward_detail");
    }
}
