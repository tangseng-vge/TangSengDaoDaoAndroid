package com.chat.uikit.message.mass;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKSystemAccount;
import com.chat.base.net.HttpResponseCode;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActMassMessageSelectBinding;
import com.chat.uikit.group.GroupEntity;
import com.chat.uikit.group.service.GroupModel;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MassMessageSelectActivity extends WKBaseActivity<ActMassMessageSelectBinding> {
    private static final int MAX_RECIPIENTS = 100;

    private final List<MassMessageTarget> friends = new ArrayList<>();
    private final List<MassMessageTarget> groups = new ArrayList<>();
    private final Map<String, WKChannel> selected = new LinkedHashMap<>();
    private final MassMessageTargetAdapter adapter = new MassMessageTargetAdapter();
    private boolean showingGroups;

    @Override
    protected ActMassMessageSelectBinding getViewBinding() {
        return ActMassMessageSelectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.select_recipients);
    }

    @Override
    protected void initView() {
        initAdapter(wkVBinding.recyclerView, adapter);
        refreshTabs();
        refreshList();
    }

    @Override
    protected void initListener() {
        wkVBinding.friendsBtn.setOnClickListener(v -> {
            showingGroups = false;
            refreshTabs();
            refreshList();
        });
        wkVBinding.groupsBtn.setOnClickListener(v -> {
            showingGroups = true;
            refreshTabs();
            refreshList();
        });
        wkVBinding.selectAllTv.setOnClickListener(v -> toggleSelectAll());
        wkVBinding.nextBtn.setOnClickListener(v -> openEditor());
        adapter.setOnItemClickListener((a, view, position) -> toggleTarget(position));
    }

    @Override
    protected void initData() {
        super.initData();
        loadFriends();
        GroupModel.getInstance().getMyGroups((code, msg, list) -> {
            groups.clear();
            if (code == HttpResponseCode.success && list != null) {
                for (GroupEntity group : list) {
                    if (group == null || TextUtils.isEmpty(group.group_no)) continue;
                    WKChannel channel = new WKChannel(group.group_no, WKChannelType.GROUP);
                    channel.channelName = group.name;
                    channel.channelRemark = group.remark;
                    channel.receipt = group.receipt;
                    groups.add(new MassMessageTarget(channel, selected.containsKey(key(channel))));
                }
            } else if (!TextUtils.isEmpty(msg)) {
                showToast(msg);
            }
            if (showingGroups) refreshList();
        });
    }

    private void loadFriends() {
        friends.clear();
        List<WKChannel> channels = WKIM.getInstance().getChannelManager()
                .getWithFollowAndStatus(WKChannelType.PERSONAL, 1, 1);
        if (channels != null) {
            for (WKChannel channel : channels) {
                if (channel == null || TextUtils.isEmpty(channel.channelID)
                        || WKSystemAccount.isSystemAccount(channel.channelID)) {
                    continue;
                }
                friends.add(new MassMessageTarget(channel, selected.containsKey(key(channel))));
            }
        }
        if (!showingGroups) refreshList();
    }

    private void toggleTarget(int position) {
        MassMessageTarget target = adapter.getItem(position);
        if (target == null) return;
        if (!target.selected && selected.size() >= MAX_RECIPIENTS) {
            showToast(String.format(getString(R.string.max_mass_recipients), MAX_RECIPIENTS));
            return;
        }
        target.selected = !target.selected;
        if (target.selected) selected.put(target.key(), target.channel);
        else selected.remove(target.key());
        adapter.notifyItemChanged(position);
        refreshBottom();
    }

    private void toggleSelectAll() {
        List<MassMessageTarget> current = showingGroups ? groups : friends;
        boolean allSelected = !current.isEmpty();
        for (MassMessageTarget item : current) allSelected &= item.selected;
        if (allSelected) {
            for (MassMessageTarget item : current) {
                item.selected = false;
                selected.remove(item.key());
            }
        } else {
            for (MassMessageTarget item : current) {
                if (!item.selected && selected.size() >= MAX_RECIPIENTS) {
                    showToast(String.format(getString(R.string.max_mass_recipients), MAX_RECIPIENTS));
                    break;
                }
                item.selected = true;
                selected.put(item.key(), item.channel);
            }
        }
        adapter.notifyDataSetChanged();
        refreshBottom();
    }

    private void openEditor() {
        if (selected.isEmpty()) return;
        Intent intent = new Intent(this, MassMessageEditActivity.class);
        intent.putParcelableArrayListExtra(MassMessageEditActivity.EXTRA_TARGETS,
                new ArrayList<>(selected.values()));
        startActivity(intent);
    }

    private void refreshTabs() {
        wkVBinding.friendsBtn.setEnabled(showingGroups);
        wkVBinding.groupsBtn.setEnabled(!showingGroups);
    }

    private void refreshList() {
        List<MassMessageTarget> current = showingGroups ? groups : friends;
        adapter.setList(new ArrayList<>(current));
        wkVBinding.emptyTv.setText(showingGroups ? R.string.no_group_chats : R.string.no_contacts);
        wkVBinding.emptyTv.setVisibility(current.isEmpty() ? View.VISIBLE : View.GONE);
        wkVBinding.recyclerView.setVisibility(current.isEmpty() ? View.GONE : View.VISIBLE);
        refreshBottom();
    }

    private void refreshBottom() {
        List<MassMessageTarget> current = showingGroups ? groups : friends;
        boolean allSelected = !current.isEmpty();
        for (MassMessageTarget item : current) allSelected &= item.selected;
        wkVBinding.selectAllTv.setText(allSelected ? R.string.deselect_all : R.string.select_all);
        wkVBinding.nextBtn.setEnabled(!selected.isEmpty());
        wkVBinding.nextBtn.setAlpha(selected.isEmpty() ? 0.5f : 1f);
        wkVBinding.nextBtn.setText(selected.isEmpty()
                ? getString(R.string.next_step)
                : getString(R.string.selected_recipients, selected.size()));
    }

    private static String key(WKChannel channel) {
        return channel.channelType + ":" + channel.channelID;
    }
}
