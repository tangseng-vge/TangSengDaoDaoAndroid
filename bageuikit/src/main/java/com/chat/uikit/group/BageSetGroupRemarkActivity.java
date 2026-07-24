package com.chat.uikit.group;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.StringUtils;
import com.chat.uikit.databinding.ActUpdateGroupRemarkLayoutBinding;
import com.chat.uikit.group.service.GroupModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

/**
 * 4/2/21 2:47 PM
 * 修改群备注
 */
public class BageSetGroupRemarkActivity extends BageBaseActivity<ActUpdateGroupRemarkLayoutBinding> {
    private String groupNo;
    BageChannel channel;

    @Override
    protected ActUpdateGroupRemarkLayoutBinding getViewBinding() {
        return ActUpdateGroupRemarkLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText("");
    }

    @Override
    protected void initPresenter() {
        groupNo = getIntent().getStringExtra("groupNo");
    }

    @Override
    protected void initView() {
        bageVBinding.saveBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.enter.setTextColor(Theme.colorAccount);
        bageVBinding.remarkEt.setFilters(new InputFilter[]{StringUtils.getInputFilter(40)});
        channel = BageIM.getInstance().getChannelManager().getChannel(groupNo, BageChannelType.GROUP);
        if (channel != null) {
            if (!TextUtils.isEmpty(channel.channelRemark)) {
                bageVBinding.remarkEt.setText(channel.channelRemark);
                bageVBinding.remarkEt.setSelection(channel.channelRemark.length());
            }
            bageVBinding.avatarView.showAvatar(channel);
            bageVBinding.groupNameTv.setText(channel.channelName);
        }
        SoftKeyboardUtils.getInstance().showSoftKeyBoard(BageSetGroupRemarkActivity.this, bageVBinding.remarkEt);
    }

    @Override
    protected void initListener() {
        bageVBinding.enter.setOnClickListener(v -> {
            bageVBinding.remarkEt.setText(channel.channelName);
            bageVBinding.remarkEt.setSelection(channel.channelName.length());
        });
        bageVBinding.groupNameTv.setMaxWidth(AndroidUtilities.getScreenWidth() - AndroidUtilities.dp(135));
        bageVBinding.remarkEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if ((TextUtils.isEmpty(s.toString()) && TextUtils.isEmpty(channel.channelRemark)) || channel.channelRemark.equals(s.toString())) {
                    bageVBinding.saveBtn.setAlpha(0.2f);
                    bageVBinding.saveBtn.setEnabled(false);
                } else {
                    bageVBinding.saveBtn.setAlpha(1f);
                    bageVBinding.saveBtn.setEnabled(true);
                }
            }
        });
        bageVBinding.saveBtn.setOnClickListener(v -> {
            String remark = bageVBinding.remarkEt.getText().toString();
            GroupModel.getInstance().updateGroupSetting(groupNo, "remark", remark, (code, msg) -> {
                if (code != HttpResponseCode.success) {
                    showToast(msg);
                } else {
                    finish();
                }
            });
        });
    }

    @Override
    public void finish() {
        super.finish();
        SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.remarkEt);
    }
}
