package com.chat.uikit.fragment;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 网格等间距：列与列、行与行之间间距相同，左右/上下边缘与中间间距一致。
 */
public final class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spanCount;
    private final int spacingPx;

    public GridSpacingItemDecoration(int spanCount, int spacingPx) {
        this.spanCount = spanCount;
        this.spacingPx = spacingPx;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION || spanCount <= 0) {
            return;
        }
        int itemCount = state.getItemCount();
        if (itemCount <= 0) {
            return;
        }
        int column = position % spanCount;
        int row = position / spanCount;
        int rowCount = (itemCount + spanCount - 1) / spanCount;

        outRect.left = column == 0 ? spacingPx : spacingPx / 2;
        outRect.right = column == spanCount - 1 ? spacingPx : spacingPx / 2;
        outRect.top = row == 0 ? spacingPx : spacingPx / 2;
        outRect.bottom = row == rowCount - 1 ? spacingPx : spacingPx / 2;
    }
}
