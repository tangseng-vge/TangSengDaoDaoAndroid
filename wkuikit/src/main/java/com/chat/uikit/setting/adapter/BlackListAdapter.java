package com.chat.uikit.setting.adapter;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.components.AvatarView;
import com.chat.uikit.R;
import com.chat.uikit.enity.BlackListEntity;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.List;

public class BlackListAdapter extends BaseQuickAdapter<BlackListEntity, BaseViewHolder> {

    public BlackListAdapter(@Nullable List<BlackListEntity> data) {
        super(R.layout.item_black_list_layout, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, BlackListEntity item) {
        String showName = !TextUtils.isEmpty(item.name) ? item.name : item.username;
        helper.setText(R.id.nameTv, showName);
        helper.setGone(R.id.usernameTv, TextUtils.isEmpty(item.username) || item.username.equals(showName));
        helper.setText(R.id.usernameTv, item.username);

        String pying = TextUtils.isEmpty(item.pying) ? "#" : item.pying;
        String letter = pying.substring(0, 1);
        int position = helper.getBindingAdapterPosition();
        int sectionFirstIndex = getPositionForSection(letter);
        helper.setText(R.id.pyTv, letter);
        helper.setGone(R.id.pyTv, position != sectionFirstIndex);

        AvatarView avatarView = helper.getView(R.id.avatarView);
        avatarView.showAvatar(item.uid, WKChannelType.PERSONAL);
    }

    private int getPositionForSection(String catalog) {
        for (int i = 0, size = getData().size(); i < size; i++) {
            String pying = getData().get(i).pying;
            if (TextUtils.isEmpty(pying)) {
                pying = "#";
            }
            if (catalog.equalsIgnoreCase(pying.substring(0, 1))) {
                return i;
            }
        }
        return -1;
    }
}
