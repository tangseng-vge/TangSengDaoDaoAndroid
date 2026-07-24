package com.chat.advanced.ui.search;

import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.advanced.R;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.BageTimeUtils;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageChannelType;
import com.bage.im.msgmodel.BageMessageContent;

public class ChatWithFromUIDAdapter extends BaseQuickAdapter<BageMsg, BaseViewHolder> {
    String name;
    String avatarKey;

    public ChatWithFromUIDAdapter(String name, String avatarKey) {
        super(R.layout.item_chat_with_fromuid_layout);
        this.name = name;
        this.avatarKey = avatarKey;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, BageMsg msg) {
        BageMessageContent msgModel = msg.baseContentMsgModel;
        long msgTime = msg.timestamp;
        if (msgModel != null)
            baseViewHolder.setText(R.id.contentTv, msgModel.getDisplayContent());
        TextView msgTimeTv = baseViewHolder.getView(R.id.timeTv);
        String timeSpace = BageTimeUtils.getInstance().getTimeString(msgTime * 1000);
        msgTimeTv.setText(timeSpace);
        baseViewHolder.setText(R.id.nameTv, name);
        AvatarView avatarView = baseViewHolder.getView(R.id.avatarView);
        avatarView.showAvatar(msg.fromUID, BageChannelType.PERSONAL, avatarKey);
    }
}
