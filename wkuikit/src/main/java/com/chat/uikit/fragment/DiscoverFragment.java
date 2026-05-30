package com.chat.uikit.fragment;

import android.graphics.Typeface;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.base.WKBaseFragment;
import com.chat.base.config.WKSystemAccount;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.endpoint.entity.ContactsMenu;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.chat.manager.WKIMUtils;
import com.chat.uikit.databinding.FragDiscoverLayoutBinding;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 「发现」Tab 对应页面。
 */
public class DiscoverFragment extends WKBaseFragment<FragDiscoverLayoutBinding> {

    private ContactsHeaderAdapter contactsHeaderAdapter;

    @Override
    protected boolean isShowBackLayout() {
        return false;
    }

    @Override
    protected FragDiscoverLayoutBinding getViewBinding() {
        return FragDiscoverLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        Typeface face = Typeface.createFromAsset(getResources().getAssets(), "fonts/mw_bold.ttf");
        wkVBinding.textView.setTypeface(face);

        wkVBinding.refreshLayout.setEnableOverScrollDrag(true);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        wkVBinding.refreshLayout.setEnableRefresh(false);

        contactsHeaderAdapter = new ContactsHeaderAdapter(true);
        initHorizontalAdapter(wkVBinding.headerRecyclerView, contactsHeaderAdapter);
        setupBageAiChatItem();
    }

    private void setupBageAiChatItem() {
        wkVBinding.bageAiChatItem.bageAiNameTv.setText("Bage AI");
        wkVBinding.bageAiChatItem.bageAiSubtitleTv.setText(R.string.wk_system_notice);
        wkVBinding.bageAiChatItem.bageAiAvatarIv.setImageResource(R.mipmap.ic_bage_rounded);
        SingleClickUtil.onSingleClick(wkVBinding.bageAiChatItem.bageAiChatItemRoot, v -> {
            String uid = WKSystemAccount.system_team;
            WKChannel channel = WKIM.getInstance().getChannelManager()
                    .getChannel(uid, WKChannelType.PERSONAL);
            if (channel != null) {
                uid = channel.channelID;
            }
            WKIMUtils.getInstance().startChatActivity(
                    new ChatViewMenu(requireActivity(), uid, WKChannelType.PERSONAL, 0, true));
        });
    }

    @Override
    protected void initListener() {
        contactsHeaderAdapter.setOnItemClickListener((adapter, view, position) ->
                SingleClickUtil.determineTriggerSingleClick(view, view1 -> {
                    ContactsMenu item = (ContactsMenu) adapter.getItem(position);
                    if (item != null && item.iMenuClick != null) {
                        item.iMenuClick.onClick();
                    }
                }));

        EndpointManager.getInstance().setMethod("discover_fragment", EndpointCategory.wkRefreshMailList, object -> {
            loadMomentsMenu();
            return null;
        });
    }

    @Override
    protected void initData() {
        loadMomentsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMomentsMenu();
    }

    private void loadMomentsMenu() {
        if (!isAdded()) {
            return;
        }
        ContactsMenu menu = (ContactsMenu) EndpointManager.getInstance().invoke(
                EndpointCategory.mailList + "_moments", getActivity());
        List<ContactsMenu> list = new ArrayList<>();
        if (menu != null) {
            list.add(menu);
        }
        contactsHeaderAdapter.setList(list);
    }

    private void initHorizontalAdapter(RecyclerView recyclerView, ContactsHeaderAdapter adapter) {
        if (recyclerView == null || adapter == null) {
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);
        adapter.setAnimationFirstOnly(true);
    }
}
