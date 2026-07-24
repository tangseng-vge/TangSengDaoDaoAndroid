package com.chat.uikit.search;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.core.view.ViewCompat;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.ui.Theme;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.chat.ChatActivity;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.databinding.ActSearchAllLayoutBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelSearchResult;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMessageSearchResult;
import com.bage.im.entity.BageMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2020-05-04 16:35
 * 搜索所有内容
 */
public class SearchAllActivity extends BageBaseActivity<ActSearchAllLayoutBinding> {
    private SearchChannelAdapter userAdapter;
    private SearchChannelAdapter groupAdapter;
    private SearchMsgAdapter msgAdapter;

    @Override
    protected ActSearchAllLayoutBinding getViewBinding() {
        return ActSearchAllLayoutBinding.inflate(getLayoutInflater());
    }
    

    @Override
    protected void initPresenter() {
        Theme.setColorFilter(this, bageVBinding.searchIv, R.color.popupTextColor);
        ViewCompat.setTransitionName(bageVBinding.searchIv, "searchView");
    }

    @Override
    protected void initView() {
        // 设置状态栏占位高度
        View statusBarView = findViewById(R.id.statusBarView);
        if (statusBarView != null) {
            android.view.ViewGroup.LayoutParams params = statusBarView.getLayoutParams();
            params.height = com.chat.base.utils.systembar.BageStatusBarUtils.getStatusBarHeight(this);
            statusBarView.setLayoutParams(params);
        }
        
        bageVBinding.searchKeyTv.setTextColor(Theme.colorAccount);
        Theme.setPressedBackground(bageVBinding.cancelTv);
        userAdapter = new SearchChannelAdapter();
        groupAdapter = new SearchChannelAdapter();
        msgAdapter = new SearchMsgAdapter();
        initAdapter(bageVBinding.userRecyclerView, userAdapter);
        initAdapter(bageVBinding.groupRecyclerView, groupAdapter);
        initAdapter(bageVBinding.msgRecyclerView, msgAdapter);
        bageVBinding.userRecyclerView.setNestedScrollingEnabled(false);
        bageVBinding.groupRecyclerView.setNestedScrollingEnabled(false);
        bageVBinding.msgRecyclerView.setNestedScrollingEnabled(false);
        SoftKeyboardUtils.getInstance().showSoftKeyBoard(SearchAllActivity.this, bageVBinding.searchEt);
    }

    @Override
    protected void initListener() {
        bageVBinding.searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        bageVBinding.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                SoftKeyboardUtils.getInstance().hideSoftKeyboard(SearchAllActivity.this);
                return true;
            }

            return false;
        });
        msgAdapter.setOnItemClickListener((adapter, view1, position) -> {
            BageMessageSearchResult result = (BageMessageSearchResult) adapter.getItem(position);
            if (result != null) {
                if (result.messageCount > 1) {
                    Intent intent = new Intent(this, SearchMsgResultActivity.class);
                    intent.putExtra("result", result);
                    intent.putExtra("searchKey", Objects.requireNonNull(bageVBinding.searchEt.getText()).toString());
                    startActivity(intent);
                } else {
                    List<BageMsg> msgList = BageIM.getInstance().getMsgManager().searchWithChannel(Objects.requireNonNull(bageVBinding.searchEt.getText()).toString(),result.bageChannel.channelID, result.bageChannel.channelType);
                    if (BageReader.isNotEmpty(msgList)) {
                        BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(this, msgList.get(0).channelID, msgList.get(0).channelType, msgList.get(0).orderSeq, false));
                    }

                }
                SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.searchEt);
            }
        });
        userAdapter.setOnItemClickListener((adapter, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view2 -> {
            BageChannelSearchResult result = (BageChannelSearchResult) adapter.getItem(position);
            if (result != null) {
                Intent intent = new Intent(SearchAllActivity.this, ChatActivity.class);
                intent.putExtra("channelId", result.bageChannel.channelID);
                intent.putExtra("channelType", result.bageChannel.channelType);
                startActivity(intent);
                SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.searchEt);
            }
        }));
        groupAdapter.setOnItemClickListener((adapter, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view2 -> {
            BageChannelSearchResult result = (BageChannelSearchResult) adapter.getItem(position);
            if (result != null) {
                SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.searchEt);
                BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(this, result.bageChannel.channelID, result.bageChannel.channelType, 0, false));
            }
        }));
        bageVBinding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String key = editable.toString();
                if (TextUtils.isEmpty(key)) {
                    bageVBinding.resultView.setVisibility(View.GONE);
                } else {
                    searchChannel(key);
                    bageVBinding.resultView.setVisibility(View.VISIBLE);
                }
            }
        });
        bageVBinding.cancelTv.setOnClickListener(v -> finish());
        SingleClickUtil.onSingleClick(bageVBinding.findUserLayout, v -> {
            SoftKeyboardUtils.getInstance().hideSoftKeyboard(this);
            String searchKey = Objects.requireNonNull(bageVBinding.searchEt.getText()).toString();
            Intent intent = new Intent(this, SearchUserActivity.class);
            intent.putExtra("searchKey", searchKey);
            startActivity(intent);
        });
    }

    private void searchChannel(String key) {
        List<BageChannelSearchResult> groupList = BageIM.getInstance().getChannelManager().search(key);
        List<BageChannel> tempList = BageIM.getInstance().getChannelManager().searchWithChannelTypeAndFollow(key, BageChannelType.PERSONAL, 1);
        List<BageChannelSearchResult> userList = new ArrayList<>();
        if (BageReader.isNotEmpty(tempList)) {
            for (int i = 0, size = tempList.size(); i < size; i++) {
                BageChannelSearchResult result = new BageChannelSearchResult();
                result.bageChannel = tempList.get(i);
                userList.add(result);
            }
        }
        List<BageMessageSearchResult> msgList = BageIM.getInstance().getMsgManager().search(key);
//        List<BageChannelSearchResult> groupList = new ArrayList<>();
//        if (list != null && list.size() > 0) {
//            for (int i = 0, size = list.size(); i < size; i++) {
//                if (list.get(i).bageChannel.channel_type == BageChannelType.PERSONAL) {
//                    userList.add(list.get(i));
//                } else if (list.get(i).bageChannel.channel_type == BageChannelType.GROUP) {
//                    groupList.add(list.get(i));
//                }
//            }
//        }
        userAdapter.setSearchKey(key);
        groupAdapter.setSearchKey(key);
        msgAdapter.setSearchKey(key);
        if (BageReader.isNotEmpty(userList)) {
            bageVBinding.userLayout.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.userLayout.setVisibility(View.GONE);
        }
        userAdapter.setList(userList);
        if (BageReader.isNotEmpty(groupList)) {
            bageVBinding.groupLayout.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.groupLayout.setVisibility(View.GONE);
        }
        groupAdapter.setList(groupList);
        if (BageReader.isNotEmpty(msgList)) {
            bageVBinding.msgLayout.setVisibility(View.VISIBLE);
        } else {
            bageVBinding.msgLayout.setVisibility(View.GONE);
        }
        msgAdapter.setList(msgList);
        bageVBinding.searchKeyTv.setText(key);
    }

    @Override
    public void finish() {
        super.finish();
        SoftKeyboardUtils.getInstance().hideSoftKeyboard(this);
    }
}
