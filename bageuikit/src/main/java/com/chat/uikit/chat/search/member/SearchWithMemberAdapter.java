package com.chat.uikit.chat.search.member;

import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.entity.GlobalMessage;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.BageTimeUtils;
import com.chat.uikit.R;
import com.bage.im.entity.BageChannelType;
import com.bage.im.msgmodel.BageMessageContent;

public class SearchWithMemberAdapter extends BaseQuickAdapter<GlobalMessage, BaseViewHolder> {
    String name;
    String avatarKey;

    public SearchWithMemberAdapter(String name, String avatarKey) {
        super(R.layout.item_search_with_member_layout);
        this.name = name;
        this.avatarKey = avatarKey;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, GlobalMessage msg) {
        BageMessageContent msgModel = msg.getMessageModel();
        long msgTime = msg.getTimestamp();
        if (msgModel != null)
            baseViewHolder.setText(R.id.contentTv, msgModel.getDisplayContent());
        TextView msgTimeTv = baseViewHolder.getView(R.id.timeTv);
        String timeSpace = BageTimeUtils.getInstance().getTimeString(msgTime * 1000);
        msgTimeTv.setText(timeSpace);
        baseViewHolder.setText(R.id.nameTv, name);
        AvatarView avatarView = baseViewHolder.getView(R.id.avatarView);
        avatarView.showAvatar(msg.from_uid, BageChannelType.PERSONAL, avatarKey);
    }
}
