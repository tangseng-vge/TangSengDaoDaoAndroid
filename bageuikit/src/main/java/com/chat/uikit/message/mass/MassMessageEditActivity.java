package com.chat.uikit.message.mass;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.utils.BageToastUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActMassMessageEditBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageSendOptions;
import com.bage.im.msgmodel.BageTextContent;

import java.util.ArrayList;
import java.util.List;

public class MassMessageEditActivity extends BageBaseActivity<ActMassMessageEditBinding> {
    public static final String EXTRA_TARGETS = "mass_message_targets";
    private final ArrayList<BageChannel> targets = new ArrayList<>();
    private boolean sending;

    @Override
    protected ActMassMessageEditBinding getViewBinding() {
        return ActMassMessageEditBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.mass_message_title);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void initPresenter() {
        ArrayList<BageChannel> values = getIntent().getParcelableArrayListExtra(EXTRA_TARGETS);
        if (values != null) targets.addAll(values);
    }

    @Override
    protected void initView() {
        int friendCount = 0;
        int groupCount = 0;
        List<String> names = new ArrayList<>();
        for (BageChannel channel : targets) {
            if (channel.channelType == BageChannelType.GROUP) groupCount++;
            else friendCount++;
            if (names.size() < 5) {
                String name = !TextUtils.isEmpty(channel.channelRemark)
                        ? channel.channelRemark : channel.channelName;
                if (!TextUtils.isEmpty(name)) names.add(name);
            }
        }
        if (friendCount > 0 && groupCount > 0) {
            bageVBinding.hintTv.setText(getString(R.string.mass_to_friends_groups, friendCount, groupCount));
        } else if (groupCount > 0) {
            bageVBinding.hintTv.setText(getString(R.string.mass_to_groups, groupCount));
        } else {
            bageVBinding.hintTv.setText(getString(R.string.mass_to_friends, friendCount));
        }
        String preview = TextUtils.join(getString(R.string.recipient_separator), names);
        if (targets.size() > names.size()) {
            preview += " " + getString(R.string.and_more_recipients, targets.size());
        }
        bageVBinding.namesTv.setText(preview);
        refreshSendButton();
    }

    @Override
    protected void initListener() {
        bageVBinding.contentEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshSendButton(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        bageVBinding.sendBtn.setOnClickListener(v -> send());
    }

    private void refreshSendButton() {
        boolean enabled = !sending && !TextUtils.isEmpty(bageVBinding.contentEt.getText().toString().trim())
                && !targets.isEmpty();
        bageVBinding.sendBtn.setEnabled(enabled);
        bageVBinding.sendBtn.setAlpha(enabled ? 1f : 0.5f);
    }

    @SuppressLint("SetTextI18n")
    private void send() {
        String content = bageVBinding.contentEt.getText().toString().trim();
        if (TextUtils.isEmpty(content) || sending) return;
        sending = true;
        bageVBinding.contentEt.setEnabled(false);
        bageVBinding.sendBtn.setText(R.string.sending);
        refreshSendButton();

        int success = 0;
        int fail = 0;
        for (BageChannel channel : targets) {
            try {
                BageSendOptions options = new BageSendOptions();
                options.setting.receipt = channel.receipt;
                BageIM.getInstance().getMsgManager().sendWithOptions(
                        new BageTextContent(content), channel, options);
                success++;
            } catch (RuntimeException e) {
                fail++;
            }
        }
        sending = false;
        bageVBinding.contentEt.setEnabled(true);
        bageVBinding.sendBtn.setText(R.string.send_msg);
        refreshSendButton();
        if (fail == 0 && success > 0) {
            BageToastUtils.getInstance().showToastNormal(getString(R.string.send_success));
            finish();
        } else if (success > 0) {
            showToast(getString(R.string.send_partial_result, success, fail));
        } else {
            showToast(R.string.send_failed_retry);
        }
    }
}
