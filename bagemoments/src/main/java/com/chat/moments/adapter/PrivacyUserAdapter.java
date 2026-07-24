package com.chat.moments.adapter;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.components.AvatarView;
import com.chat.moments.R;
import com.bage.im.entity.BageChannel;

import org.jetbrains.annotations.NotNull;

/**
 * 2020-12-10 15:46
 */
public class PrivacyUserAdapter extends BaseQuickAdapter<BageChannel, BaseViewHolder> {
    public PrivacyUserAdapter() {
        super(R.layout.item_privacy_user_layout);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, BageChannel channel) {
        AvatarView avatarView = baseViewHolder.getView(R.id.avatarView);
        avatarView.showAvatar(channel);
        baseViewHolder.setText(R.id.nameTv, TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark);
    }
}
