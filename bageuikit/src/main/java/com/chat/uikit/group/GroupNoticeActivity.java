package com.chat.uikit.group;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.msgitem.BageChannelMemberRole;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.StringUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageToastUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActGroupNoticeLayoutBinding;
import com.chat.uikit.group.service.GroupModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2020-01-26 16:03
 * 群公告
 */
public class GroupNoticeActivity extends BageBaseActivity<ActGroupNoticeLayoutBinding> {

    private String groupNo, oldNotice;
    private TextView titleRightTv;

    @Override
    protected ActGroupNoticeLayoutBinding getViewBinding() {
        return ActGroupNoticeLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_announcement);
    }

    @Override
    protected String getRightTvText(TextView textView) {
        titleRightTv = textView;
        return getString(R.string.save);
    }

    @Override
    protected boolean hideStatusBar() {
        return true;
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        String content = Objects.requireNonNull(bageVBinding.contentEt.getText()).toString();
//        content = StringUtils.replaceBlank(content);
        if ((!TextUtils.isEmpty(oldNotice) && content.equals(oldNotice))) {
            return;
        }
        showTitleRightLoading();
        GroupModel.getInstance().updateGroupInfo(groupNo, "notice", content, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                finish();
            } else {
                hideTitleRightLoading();
                showToast(msg);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView() {
        List<PopupMenuItem> list = new ArrayList<>();
        list.add(new PopupMenuItem(getString(R.string.copy), R.mipmap.msg_copy, () -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", Objects.requireNonNull(bageVBinding.contentEt.getText()).toString());
            assert cm != null;
            cm.setPrimaryClip(mClipData);
            BageToastUtils.getInstance().showToastNormal(getString(R.string.copyed));
        }));
        BageDialogUtils.getInstance().setViewLongClickPopup(bageVBinding.contentEt,list);
    }

    @Override
    protected void initListener() {

    }

    @Override
    protected void initData() {
        super.initData();

        groupNo = getIntent().getStringExtra("groupNo");
        oldNotice = getIntent().getStringExtra("oldNotice");

        BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(groupNo, BageChannelType.GROUP, BageConfig.getInstance().getUid());
        bageVBinding.contentEt.setText(oldNotice);
        if (!TextUtils.isEmpty(oldNotice))
            bageVBinding.contentEt.setSelection(oldNotice.length());

        if (member != null && member.role != BageChannelMemberRole.normal) {
            bageVBinding.contentEt.setEnabled(true);
            titleRightTv.setVisibility(View.VISIBLE);
            bageVBinding.contentEt.requestFocus();
            SoftKeyboardUtils.getInstance().showSoftKeyBoard(this, bageVBinding.contentEt);
        } else {
            bageVBinding.contentEt.setFocusableInTouchMode(false);
            bageVBinding.contentEt.setEnabled(true);
            titleRightTv.setVisibility(View.GONE);
        }
        if (member != null) {
            bageVBinding.bottomView.setVisibility(member.role == BageChannelMemberRole.normal ? View.VISIBLE : View.GONE);
        }
        bageVBinding.contentEt.setFilters(new InputFilter[]{StringUtils.getInputFilter(300)});
    }

    @Override
    public void finish() {
        super.finish();
        SoftKeyboardUtils.getInstance().hideSoftKeyboard(this);
    }
}
