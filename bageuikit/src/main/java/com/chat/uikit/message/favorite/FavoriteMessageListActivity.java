package com.chat.uikit.message.favorite;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActPersonalFeatureListBinding;
import com.chat.uikit.message.MsgModel;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteMessageListActivity extends BageBaseActivity<ActPersonalFeatureListBinding> {
    private static final int PAGE_SIZE = 20;
    private final FavoriteMessageAdapter adapter = new FavoriteMessageAdapter();
    private int pageIndex = 1;
    private boolean hasMore;

    @Override
    protected ActPersonalFeatureListBinding getViewBinding() {
        return ActPersonalFeatureListBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.message_favorite);
    }

    @Override
    protected void initView() {
        initAdapter(bageVBinding.recyclerView, adapter);
        bageVBinding.emptyTv.setText(R.string.no_favorites);
        bageVBinding.refreshLayout.setEnableRefresh(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
    }

    @Override
    protected void initListener() {
        bageVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                load(pageIndex + 1, false);
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                load(1, true);
            }
        });
        adapter.setOnItemChildClickListener((a, view, position) -> {
            if (view.getId() != R.id.removeTv) return;
            FavoriteMessageRecord record = adapter.getItem(position);
            if (record == null) return;
            MsgModel.getInstance().favoriteMessage(record.messageID, record.messageSeq,
                    record.channelID, record.channelType, false, (code, msg) -> {
                        if (code == HttpResponseCode.success) {
                            adapter.removeAt(position);
                            updateEmpty();
                            showToast(R.string.unfavorited);
                        } else {
                            showToast(TextUtils.isEmpty(msg) ? getString(R.string.favorite_failed) : msg);
                        }
                    });
        });
        adapter.setOnItemClickListener((a, view, position) ->
                showFavoriteInChat(adapter.getItem(position)));
    }

    private void showFavoriteInChat(FavoriteMessageRecord record) {
        if (record == null || TextUtils.isEmpty(record.channelID)) {
            return;
        }
        long orderSeq = 0;
        if (record.messageSeq > 0) {
            orderSeq = BageIM.getInstance().getMsgManager().getMessageOrderSeq(
                    record.messageSeq, record.channelID, record.channelType);
        }
        if (orderSeq <= 0 && !TextUtils.isEmpty(record.messageID)) {
            BageMsg message = BageIM.getInstance().getMsgManager()
                    .getWithMessageID(record.messageID);
            if (message != null) {
                orderSeq = message.orderSeq;
            }
        }
        if (orderSeq <= 0) {
            showToast(R.string.message_unavailable);
            return;
        }
        EndpointManager.getInstance().invoke(EndpointSID.chatView,
                new ChatViewMenu(this, record.channelID, record.channelType,
                        orderSeq, false));
    }

    @Override
    protected void initData() {
        super.initData();
        load(1, true);
    }

    private void load(int page, boolean replace) {
        MsgModel.getInstance().getFavoriteMessages(page, PAGE_SIZE, (code, msg, result) -> {
            bageVBinding.refreshLayout.finishRefresh();
            bageVBinding.refreshLayout.finishLoadMore();
            if (code != HttpResponseCode.success || result == null) {
                if (!TextUtils.isEmpty(msg)) showToast(msg);
                updateEmpty();
                return;
            }
            List<FavoriteMessageRecord> records = parse(result);
            if (replace) adapter.setList(records);
            else adapter.addData(records);
            pageIndex = page;
            hasMore = result.getIntValue("more") == 1;
            bageVBinding.refreshLayout.setEnableLoadMore(hasMore);
            updateEmpty();
        });
    }

    private List<FavoriteMessageRecord> parse(JSONObject result) {
        Map<String, JSONObject> messageMap = new HashMap<>();
        JSONArray messages = result.getJSONArray("messages");
        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                JSONObject message = messages.getJSONObject(i);
                if (message == null) continue;
                String id = message.getString("message_idstr");
                if (TextUtils.isEmpty(id)) id = message.getString("message_id");
                if (!TextUtils.isEmpty(id)) messageMap.put(id, message);
            }
        }
        List<FavoriteMessageRecord> records = new ArrayList<>();
        JSONArray favorites = result.getJSONArray("favorites");
        if (favorites == null) return records;
        for (int i = 0; i < favorites.size(); i++) {
            JSONObject value = favorites.getJSONObject(i);
            if (value == null) continue;
            FavoriteMessageRecord record = new FavoriteMessageRecord();
            record.messageID = value.getString("message_id");
            record.messageSeq = value.getIntValue("message_seq");
            record.channelID = value.getString("channel_id");
            record.channelType = value.getByteValue("channel_type");
            record.favoritedAt = value.getLongValue("favorited_at");
            JSONObject message = messageMap.get(record.messageID);
            if (message != null) {
                JSONObject payload = message.getJSONObject("payload");
                if (payload != null) record.content = payload.getString("content");
            }
            if (!TextUtils.isEmpty(record.messageID) && !TextUtils.isEmpty(record.channelID)) {
                records.add(record);
            }
        }
        return records;
    }

    private void updateEmpty() {
        boolean empty = adapter.getData().isEmpty();
        bageVBinding.emptyTv.setVisibility(empty ? View.VISIBLE : View.GONE);
        bageVBinding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
