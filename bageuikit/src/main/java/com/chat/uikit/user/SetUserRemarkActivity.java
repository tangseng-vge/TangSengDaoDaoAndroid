package com.chat.uikit.user;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActUpdateUserRemarkLayoutBinding;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.Objects;

/**
 * 2020-07-01 22:41
 * 设置用户备注
 */
public class SetUserRemarkActivity extends BageBaseActivity<ActUpdateUserRemarkLayoutBinding> {
    private String oldStr;
    private String uid;

    @Override
    protected ActUpdateUserRemarkLayoutBinding getViewBinding() {
        return ActUpdateUserRemarkLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.set_remark);
    }

    @Override
    protected String getRightTvText(TextView textView) {
        return getString(R.string.sure);
    }

    @Override
    protected void initPresenter() {
        oldStr = getIntent().getStringExtra("oldStr");
        uid = getIntent().getStringExtra("uid");

        if (!TextUtils.isEmpty(oldStr)) {
            if (oldStr.length() > 10) {
                oldStr = oldStr.substring(0, 10);
            }
            bageVBinding.contentEt.setText(oldStr);
            bageVBinding.contentEt.setSelection(oldStr.length());
        } else {
            BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
            if (channel != null) {
                String showName = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
                if (showName.length() > 10) {
                    showName = showName.substring(0, 10);
                }
                bageVBinding.contentEt.setText(showName);
                bageVBinding.contentEt.setSelection(showName.length());
            }
        }
        bageVBinding.contentEt.setFilters(new InputFilter[]{filter});
        SoftKeyboardUtils.getInstance().showSoftKeyBoard(SetUserRemarkActivity.this, bageVBinding.contentEt);
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        String name = Objects.requireNonNull(bageVBinding.contentEt.getText()).toString();
        UserModel.getInstance().updateUserRemark(uid, name, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                BageIM.getInstance().getChannelManager().updateRemark(uid, BageChannelType.PERSONAL, name);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void initView() {
        bageVBinding.contentEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals(oldStr)) {
                    hideTitleRightView();
                } else showTitleRightView();
            }
        });
    }

    @Override
    protected void initListener() {

    }

    @Override
    public void finish() {
        super.finish();
        SoftKeyboardUtils.getInstance().hideInput(this, bageVBinding.contentEt);
    }

    int maxLength = 10;
    private final InputFilter filter = (src, start, end, dest, dstart, dend) -> {
        int dindex = 0;
        int count = 0;

        while (count <= maxLength * 2 && dindex < dest.length()) {
            char c = dest.charAt(dindex++);
            if (c < 128) {
                count = count + 1;
            } else {
                count = count + 2;
            }
        }

        if (count > maxLength * 2) {
            return dest.subSequence(0, dindex - 1);
        }

        int sindex = 0;
        while (count <= maxLength * 2 && sindex < src.length()) {
            char c = src.charAt(sindex++);
            if (c < 128) {
                count = count + 1;
            } else {
                count = count + 2;
            }
        }

        if (count > maxLength * 2) {
            sindex--;
        }

        return src.subSequence(0, sindex);
    };

}
