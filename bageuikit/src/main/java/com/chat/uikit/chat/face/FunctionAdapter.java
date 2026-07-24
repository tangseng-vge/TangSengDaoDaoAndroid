package com.chat.uikit.chat.face;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.endpoint.entity.ChatFunctionMenu;
import com.chat.uikit.R;

import java.util.List;

/**
 * 2019-11-14 13:27
 * 功能模块
 */
public class FunctionAdapter extends BaseQuickAdapter<ChatFunctionMenu, BaseViewHolder> {
    int h;
    private final boolean useFullItemHeight;

    public FunctionAdapter(@Nullable List<ChatFunctionMenu> data, int h) {
        this(data, h, false);
    }

    public FunctionAdapter(@Nullable List<ChatFunctionMenu> data, int h, boolean useFullItemHeight) {
        super(R.layout.item_function_layout, data);
        this.h = h;
        this.useFullItemHeight = useFullItemHeight;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, ChatFunctionMenu item) {
        helper.setImageResource(R.id.functionIv, item.imgResourceID);
        helper.setText(R.id.functionNameTv, item.text);
        LinearLayout contentLayout = helper.getView(R.id.contentLayout);
        contentLayout.getLayoutParams().height = useFullItemHeight ? h : h / 2;
    }
}
