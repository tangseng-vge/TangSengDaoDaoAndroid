package com.chat.uikit.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 文本与时间同行：时间在最后一行右侧，正文仅在末尾为时间预留空间（行内不截断）
 */
public class ChatTextTimeLayout extends FrameLayout {
    public ChatTextTimeLayout(@NonNull Context context) {
        super(context);
    }

    public ChatTextTimeLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatTextTimeLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private TextView textView;
    private View containerView;
    private int containerWidth = 0;
    private int containerHeight = 0;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        textView = (TextView) getChildAt(0);
        containerView = getChildAt(getChildCount() - 1);
        LayoutParams viewPartMainLayoutParams = (LayoutParams) textView.getLayoutParams();
        LayoutParams viewPartSlaveLayoutParams = (LayoutParams) containerView.getLayoutParams();
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        if (widthSize <= 0) {
            return;
        }

        int availableWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int textViewWidth = textView.getMeasuredWidth()
                + viewPartMainLayoutParams.leftMargin + viewPartMainLayoutParams.rightMargin;
        int textViewHeight =
                textView.getMeasuredHeight() + viewPartMainLayoutParams.topMargin + viewPartMainLayoutParams.bottomMargin;

        containerWidth =
                containerView.getMeasuredWidth() + viewPartSlaveLayoutParams.leftMargin + viewPartSlaveLayoutParams.rightMargin;

        containerHeight =
                containerView.getMeasuredHeight() + viewPartSlaveLayoutParams.topMargin + viewPartSlaveLayoutParams.bottomMargin;

        int viewPartMainLineCount = textView.getLineCount();
        float viewPartMainLastLineWidth;
        if (viewPartMainLineCount > 0 && textView.getLayout() != null) {
            viewPartMainLastLineWidth = textView.getLayout().getLineWidth(viewPartMainLineCount - 1);
        } else {
            viewPartMainLastLineWidth = 0.0f;
        }

        widthSize = getPaddingLeft() + getPaddingRight();
        int heightSize = getPaddingTop() + getPaddingBottom();

        if (viewPartMainLineCount > 1 && viewPartMainLastLineWidth + containerWidth < textView.getMeasuredWidth()) {
            widthSize += textViewWidth;
            heightSize += textViewHeight;
        } else if (viewPartMainLineCount > 1 && viewPartMainLastLineWidth + containerWidth >= availableWidth) {
            widthSize += textViewWidth;
            heightSize += textViewHeight + containerHeight;
        } else if (viewPartMainLineCount == 1 && textViewWidth + containerWidth >= availableWidth) {
            widthSize += textView.getMeasuredWidth();
            heightSize += textViewHeight + containerHeight;
        } else {
            widthSize += textViewWidth + containerWidth;
            heightSize += textViewHeight;
        }

        int usernameWidth = 0;
        if (getParent() instanceof LinearLayout) {
            LinearLayout parentLayout = (LinearLayout) getParent();
            if (parentLayout.getChildCount() > 0) {
                usernameWidth = parentLayout.getChildAt(0).getMeasuredWidth();
            }
        }
        if (usernameWidth > textViewWidth + containerWidth) {
            widthSize = usernameWidth + getPaddingLeft() + getPaddingRight();
        }
        if (widthSize < Math.max(textViewWidth, containerWidth)) {
            widthSize = Math.max(textViewWidth, containerWidth) + getPaddingLeft() + getPaddingRight();
        }
        setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (textView == null) {
            textView = (TextView) getChildAt(0);
        }
        if (containerView == null) {
            containerView = getChildAt(getChildCount() - 1);
        }

        textView.layout(
                getPaddingLeft(),
                getPaddingTop(),
                textView.getWidth() + getPaddingLeft(),
                textView.getHeight() + getPaddingTop()
        );

        containerView.layout(
                right - left - containerWidth - getPaddingRight(),
                bottom - top - getPaddingBottom() - containerHeight,
                right - left - getPaddingRight(),
                bottom - top - getPaddingBottom()
        );
    }
}
