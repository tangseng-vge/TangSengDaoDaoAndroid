package com.chat.moments.activities;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.WKToastUtils;
import com.chat.moments.R;
import com.chat.moments.adapter.MyReportAdapter;
import com.chat.moments.databinding.ActMyReportsBinding;
import com.chat.moments.entity.MyReportRecord;
import com.chat.moments.service.MomentsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyReportsActivity extends WKBaseActivity<ActMyReportsBinding> {
    private static final int PAGE_SIZE = 20;
    private final List<MyReportRecord> records = new ArrayList<>();
    private MyReportAdapter adapter;
    private int pageIndex = 1;

    @Override
    protected ActMyReportsBinding getViewBinding() {
        return ActMyReportsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.my_reports);
    }

    @Override
    protected void initPresenter() {
    }

    @Override
    protected void initView() {
        adapter = new MyReportAdapter();
        initAdapter(wkVBinding.recyclerView, adapter);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
    }

    @Override
    protected void initListener() {
        wkVBinding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            pageIndex = 1;
            loadReports();
        });
        wkVBinding.refreshLayout.setOnLoadMoreListener(refreshLayout -> loadReports());
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> {
            MyReportRecord record = adapter.getItem(position);
            if (!TextUtils.isEmpty(record.momentNo)) {
                Intent intent = new Intent(this, MomentsDetailActivity.class);
                intent.putExtra("momentNo", record.momentNo);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();
        loadReports();
    }

    private void loadReports() {
        String language = Locale.getDefault().getLanguage();
        String lang = language != null && language.startsWith("en") ? "en" : "cn";
        final int requestedPage = pageIndex;
        MomentsModel.getInstance().myReports(lang, requestedPage, PAGE_SIZE, (code, msg, result) -> {
            wkVBinding.refreshLayout.finishRefresh();
            wkVBinding.refreshLayout.finishLoadMore();
            if (code != HttpResponseCode.success || result == null) {
                if (!TextUtils.isEmpty(msg)) WKToastUtils.getInstance().showToastFail(msg);
                updateEmptyView();
                return;
            }
            if (requestedPage == 1) records.clear();
            JSONArray list = result.getJSONArray("list");
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    if (item != null) records.add(parseRecord(item));
                }
            }
            adapter.setList(new ArrayList<>(records));
            boolean more = result.getIntValue("more") == 1;
            wkVBinding.refreshLayout.setEnableLoadMore(more);
            if (more) pageIndex = requestedPage + 1;
            updateEmptyView();
        });
    }

    private MyReportRecord parseRecord(JSONObject item) {
        MyReportRecord record = new MyReportRecord();
        record.targetType = item.getString("target_type");
        record.momentNo = item.getString("moment_no");
        record.categoryName = item.getString("category_name");
        record.remark = item.getString("remark");
        record.reportedAt = item.getString("reported_at");
        JSONObject moment = item.getJSONObject("moment");
        if (moment != null) {
            record.content = moment.getString("text");
            if ("comment".equals(record.targetType)) {
                String commentId = item.getString("comment_id");
                JSONArray comments = moment.getJSONArray("comments");
                if (comments != null) {
                    for (int i = 0; i < comments.size(); i++) {
                        JSONObject comment = comments.getJSONObject(i);
                        if (comment != null && commentId != null
                                && commentId.equals(comment.getString("sid"))) {
                            record.content = comment.getString("content");
                            break;
                        }
                    }
                }
            }
        }
        return record;
    }

    private void updateEmptyView() {
        wkVBinding.emptyTv.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
