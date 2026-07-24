package com.chat.uikit.search;

import android.text.SpannableString;
import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.StringUtils;
import com.chat.uikit.R;
import com.bage.im.entity.BageMessageSearchResult;

import org.jetbrains.annotations.NotNull;

/**
 * 2020-05-10 22:33
 * 搜索消息
 */
public class SearchMsgAdapter extends BaseQuickAdapter<BageMessageSearchResult, BaseViewHolder> {
    private String searchKey;

    public SearchMsgAdapter() {
        super(R.layout.item_search_msg_layout);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, BageMessageSearchResult result) {
        AvatarView avatarView = baseViewHolder.getView(R.id.avatarView);
        avatarView.showAvatar(result.bageChannel);
        baseViewHolder.setText(R.id.nameTv, TextUtils.isEmpty(result.bageChannel.channelRemark) ? result.bageChannel.channelName : result.bageChannel.channelRemark);
        if (result.messageCount > 1) {
            baseViewHolder.setText(R.id.contentTv, String.format(getContext().getString(R.string.total_search_msg_count), result.messageCount));
        } else {
            SpannableString key = StringUtils.findSearch(Theme.colorAccount, result.searchableWord, searchKey);
            baseViewHolder.setText(R.id.contentTv, key);
        }
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
        notifyItemRangeChanged(0, getItemCount());
    }
}
