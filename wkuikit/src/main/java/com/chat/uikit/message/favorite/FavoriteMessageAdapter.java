package com.chat.uikit.message.favorite;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.WKTimeUtils;
import com.chat.uikit.R;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;

import org.jetbrains.annotations.NotNull;

public class FavoriteMessageAdapter extends BaseQuickAdapter<FavoriteMessageRecord, BaseViewHolder> {
    public FavoriteMessageAdapter() {
        super(R.layout.item_favorite_message);
        addChildClickViewIds(R.id.removeTv);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder holder, FavoriteMessageRecord item) {
        WKChannel channel = WKIM.getInstance().getChannelManager()
                .getChannel(item.channelID, item.channelType);
        String name = item.channelID;
        if (channel != null) {
            name = !TextUtils.isEmpty(channel.channelRemark)
                    ? channel.channelRemark : channel.channelName;
        }
        holder.setText(R.id.nameTv, name);
        holder.setText(R.id.contentTv, TextUtils.isEmpty(item.content)
                ? getContext().getString(R.string.message_unavailable) : item.content);
        holder.setText(R.id.timeTv, item.favoritedAt > 0
                ? WKTimeUtils.getInstance().getTimeString(item.favoritedAt * 1000) : "");
        AvatarView avatarView = holder.getView(R.id.avatarView);
        avatarView.setSize(48);
        avatarView.showAvatar(item.channelID, item.channelType);
    }
}
