package com.chat.uikit.message.mass;

import android.widget.CheckBox;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.components.AvatarView;
import com.chat.uikit.R;

import org.jetbrains.annotations.NotNull;

public class MassMessageTargetAdapter extends BaseQuickAdapter<MassMessageTarget, BaseViewHolder> {
    public MassMessageTargetAdapter() {
        super(R.layout.item_mass_message_target);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder holder, MassMessageTarget item) {
        holder.setText(R.id.nameTv, item.displayName());
        CheckBox checkBox = holder.getView(R.id.checkBox);
        checkBox.setChecked(item.selected);
        AvatarView avatarView = holder.getView(R.id.avatarView);
        avatarView.setSize(44);
        avatarView.showAvatar(item.channel.channelID, item.channel.channelType);
    }
}
