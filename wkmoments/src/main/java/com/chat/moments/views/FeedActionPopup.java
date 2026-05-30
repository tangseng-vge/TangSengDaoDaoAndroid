package com.chat.moments.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.ui.Theme;
import com.chat.moments.R;

public class FeedActionPopup extends PopupWindow {

    public interface IClick {
        void onClick(int type);
    }

    public FeedActionPopup(final Context context, boolean isLiked, IClick clickListener) {
        final View view = LayoutInflater.from(context).inflate(R.layout.pop_feed_action_layout, null, false);
        setAnimationStyle(R.style.FeedActionPopup_anim_style);
        setFocusable(true);
        setOutsideTouchable(true);
        setClippingEnabled(false);
        ColorDrawable dw = new ColorDrawable(Color.TRANSPARENT);
        setBackgroundDrawable(dw);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        setContentView(view);
        TextView praiseTv = view.findViewById(R.id.praiseTv);
        ImageView praiseIv = view.findViewById(R.id.praiseIv);
        if (isLiked) {
            praiseTv.setText(R.string.cancel);
            Theme.setColorFilter(context, praiseIv, R.color.red);
        } else {
            praiseTv.setText(R.string.str_praise);
            Theme.setColorFilter(context, praiseIv, R.color.ps_color_grey);
        }

        view.findViewById(R.id.comment).setOnClickListener(v -> {
            dismiss();
            clickListener.onClick(1);
        });
        view.findViewById(R.id.like).setOnClickListener(v -> {
            dismiss();
            clickListener.onClick(0);
        });
    }

    /**
     * 在 tvMore 正下方显示：卡片右缘贴近按钮，并抵消阴影留白造成的偏移。
     */
    public void showBelowAnchor(@NonNull View anchor) {
        View content = getContentView();
        if (content == null) {
            return;
        }
        content.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = content.getMeasuredWidth();
        int popupHeight = content.getMeasuredHeight();
        setWidth(popupWidth);
        setHeight(popupHeight);

        int shadowInset = content.getResources().getDimensionPixelSize(R.dimen.feed_action_popup_shadow_inset);
        int offsetX = content.getResources().getDimensionPixelSize(R.dimen.feed_action_popup_anchor_offset_x);
        int gapY = content.getResources().getDimensionPixelSize(R.dimen.feed_action_popup_anchor_gap_y);

        View root = anchor.getRootView();
        int[] anchorLoc = new int[2];
        anchor.getLocationOnScreen(anchorLoc);
        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);
        // 外框含 shadow padding：右移/上移 shadowInset，使 MaterialCardView 贴在 tvMore 下方
        int x = anchorLoc[0] - rootLoc[0] + anchor.getWidth() - popupWidth + shadowInset + offsetX;
        int y = anchorLoc[1] - rootLoc[1] + anchor.getHeight() - shadowInset + gapY;
        showAtLocation(root, Gravity.NO_GRAVITY, Math.max(0, x), Math.max(0, y));
    }

}
