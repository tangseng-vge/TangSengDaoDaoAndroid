package com.chat.uikit.chat;

import android.content.Intent;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.msgitem.BageChannelMemberRole;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.BageReader;
import com.chat.uikit.BageUIKitApplication;
import com.chat.uikit.R;
import com.chat.uikit.chat.adapter.ChooseChatAdapter;
import com.chat.uikit.contacts.ChooseContactsActivity;
import com.chat.uikit.databinding.ActChooseChatLayoutBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelStatus;
import com.bage.im.entity.BageUIConversationMsg;
import com.bage.im.msgmodel.BageMessageContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 2019-12-08 13:47
 * 选择会话页面
 */
public class ChooseChatActivity extends BageBaseActivity<ActChooseChatLayoutBinding> {
    ChooseChatAdapter chooseChatAdapter;
    Button rightBtn;
    private boolean isChoose;
    List<ChooseChatEntity> allList;

    @Override
    protected ActChooseChatLayoutBinding getViewBinding() {
        return ActChooseChatLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.choose_chat);
    }

    @Override
    protected String getRightBtnText(Button titleRightBtn) {
        rightBtn = titleRightBtn;
        return getString(R.string.sure);
    }

    @Override
    protected void rightButtonClick() {
        super.rightButtonClick();

        List<BageUIConversationMsg> selectedList = new ArrayList<>();
        for (int i = 0, size = chooseChatAdapter.getData().size(); i < size; i++) {
            if (chooseChatAdapter.getData().get(i).isCheck)
                selectedList.add(chooseChatAdapter.getData().get(i).uiConveursationMsg);
        }
        List<BageChannel> list = new ArrayList<>();
        if (BageReader.isNotEmpty(selectedList)) {
            for (int i = 0; i < selectedList.size(); i++) {
                list.add(selectedList.get(i).getBageChannel());
            }
            if (isChoose) {
                if (BageUIKitApplication.getInstance().getMessageContentList() != null) {
                    BageUIKitApplication.getInstance().showChatConfirmDialog(this, list, BageUIKitApplication.getInstance().getMessageContentList(), new BageUIKitApplication.IShowChatConfirm() {
                        @Override
                        public void onBack(@NonNull List<BageChannel> list, @NonNull List<BageMessageContent> messageContentList) {
                            BageUIKitApplication.getInstance().sendChooseChatBack(list);
                            finish();
                        }
                    });
                } else {
                    BageUIKitApplication.getInstance().sendChooseChatBack(list);
                    finish();
                }
            } else {
                Intent intent = new Intent();
                intent.putParcelableArrayListExtra("list", (ArrayList<? extends Parcelable>) list);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    @Override
    protected void initPresenter() {
        isChoose = getIntent().getBooleanExtra("isChoose", false);
    }

    @Override
    protected void initView() {
        chooseChatAdapter = new ChooseChatAdapter(new ArrayList<>());
        initAdapter(bageVBinding.recyclerView, chooseChatAdapter);
        chooseChatAdapter.addHeaderView(getHeader());
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
    }

    @Override
    protected void initListener() {
        chooseChatAdapter.setOnItemClickListener((adapter, view1, position) -> {
            ChooseChatEntity chooseChatEntity = (ChooseChatEntity) adapter.getItem(position);
            if (chooseChatEntity != null) {
                boolean isSelect = !chooseChatEntity.isBan && !chooseChatEntity.isForbidden;
                if (isSelect) {
                    chooseChatEntity.isCheck = !chooseChatEntity.isCheck;
                    int selectCount = 0;
                    for (int i = 0, size = allList.size(); i < size; i++) {
                        if (allList.get(i).isCheck)
                            selectCount++;
                    }
                    if (chooseChatEntity.isCheck && selectCount == 10) {
                        chooseChatEntity.isCheck = false;
                        showSingleBtnDialog(String.format(getString(R.string.max_select_count_chat), 9));
                        adapter.notifyItemChanged(position + adapter.getHeaderLayoutCount());
                        return;
                    }
                    adapter.notifyItemChanged(position + adapter.getHeaderLayoutCount(),chooseChatEntity);


                    int count = 0;
                    for (int i = 0, size = allList.size(); i < size; i++) {
                        if (allList.get(i).isCheck)
                            count++;
                    }
                    if (count > 0) {
                        rightBtn.setVisibility(View.VISIBLE);
                        rightBtn.setText(String.format("%s(%s)", getString(R.string.sure), count));
                    } else {
                        rightBtn.setText(R.string.sure);
                        rightBtn.setVisibility(View.INVISIBLE);
                    }
                }
            }

        });

        bageVBinding.searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        bageVBinding.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                SoftKeyboardUtils.getInstance().hideSoftKeyboard(ChooseChatActivity.this);
                return true;
            }
            return false;
        });
        bageVBinding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchUser(editable.toString());
            }
        });
    }

    private void searchUser(String content) {
        if (TextUtils.isEmpty(content)) {
            chooseChatAdapter.setList(allList);
            return;
        }
        List<ChooseChatEntity> tempList = new ArrayList<>();
        for (int i = 0, size = allList.size(); i < size; i++) {
            if ((!TextUtils.isEmpty(allList.get(i).uiConveursationMsg.getBageChannel().channelName) && allList.get(i).uiConveursationMsg.getBageChannel().channelName.toLowerCase(Locale.getDefault())
                    .contains(content.toLowerCase(Locale.getDefault())))
                    || (!TextUtils.isEmpty(allList.get(i).uiConveursationMsg.getBageChannel().channelRemark) && allList.get(i).uiConveursationMsg.getBageChannel().channelRemark.toLowerCase(Locale.getDefault())
                    .contains(content.toLowerCase(Locale.getDefault())))) {
                tempList.add(allList.get(i));
            }
        }
        chooseChatAdapter.setList(tempList);
    }

    @Override
    protected void initData() {
        super.initData();
        List<BageUIConversationMsg> list = BageIM.getInstance().getConversationManager().getAll();
        allList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            ChooseChatEntity chooseChatEntity = new ChooseChatEntity(list.get(i));
            if (list.get(i).getBageChannel() != null) {
                BageChannelMember mChannelMember = BageIM.getInstance().getChannelMembersManager().getMember(list.get(i).getBageChannel().channelID, list.get(i).getBageChannel().channelType, BageConfig.getInstance().getUid());
                if (list.get(i).getBageChannel().forbidden == 1) {
                    // 禁言中
                    if (mChannelMember != null) {
                        chooseChatEntity.isForbidden = mChannelMember.role == BageChannelMemberRole.normal;
                    }
                } else {
                    if (mChannelMember != null)
                        chooseChatEntity.isForbidden = mChannelMember.forbiddenExpirationTime > 0;
                    else chooseChatEntity.isForbidden = false;
                }
                chooseChatEntity.isBan = list.get(i).getBageChannel().status == BageChannelStatus.statusDisabled;
            }
            allList.add(chooseChatEntity);
        }

        chooseChatAdapter.setList(allList);
        rightBtn.setVisibility(View.GONE);
    }

    public static class ChooseChatEntity {
        ChooseChatEntity(BageUIConversationMsg uiConveursationMsg) {
            this.uiConveursationMsg = uiConveursationMsg;
        }

        public BageUIConversationMsg uiConveursationMsg;
        public boolean isCheck;
        // 禁言中
        public boolean isForbidden;
        // 禁用中
        public boolean isBan;
    }

    private View getHeader() {
        View view = LayoutInflater.from(this).inflate(R.layout.choose_chat_header_layout, bageVBinding.recyclerView, false);
        View headerView = view.findViewById(R.id.createTv);
        headerView.setOnClickListener(view1 -> {
            Intent intent = new Intent(this, ChooseContactsActivity.class);
            if (BageUIKitApplication.getInstance().getMessageContentList() != null)
                intent.putParcelableArrayListExtra("msgContentList", (ArrayList<? extends Parcelable>) BageUIKitApplication.getInstance().getMessageContentList());
            startActivity(intent);
        });
        return view;
    }
}
