package com.chat.moments.adapter;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.moments.R;
import com.chat.moments.entity.MyReportRecord;

public class MyReportAdapter extends BaseQuickAdapter<MyReportRecord, BaseViewHolder> {
    public MyReportAdapter() {
        super(R.layout.item_my_report);
    }

    @Override
    protected void convert(BaseViewHolder holder, MyReportRecord item) {
        holder.setText(R.id.typeTv, "comment".equals(item.targetType)
                ? R.string.report_comment : R.string.report_moment);
        StringBuilder detail = new StringBuilder();
        appendLine(detail, getContext().getString(R.string.report_category), item.categoryName);
        appendLine(detail, getContext().getString(R.string.report_content), item.content);
        appendLine(detail, getContext().getString(R.string.report_remark), item.remark);
        appendLine(detail, getContext().getString(R.string.report_time), item.reportedAt);
        holder.setText(R.id.detailTv, detail.toString());
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (TextUtils.isEmpty(value)) return;
        if (builder.length() > 0) builder.append('\n');
        builder.append(label).append("：").append(value);
    }
}
