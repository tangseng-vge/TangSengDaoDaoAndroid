package com.chat.uikit.user;

import android.widget.ImageView;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.databinding.ActFileHelperLayoutBinding;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 1/29/21 11:30 AM
 * 系统文件助手详情
 */
public class BageFileHelperActivity extends BageBaseActivity<ActFileHelperLayoutBinding> {
    @Override
    protected ActFileHelperLayoutBinding getViewBinding() {
        return ActFileHelperLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText("");
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        bageVBinding.appIdNumLeftTv.setText(String.format(getString(R.string.app_idnum), getString(R.string.app_name)));
        bageVBinding.appIdNumTv.setText(BageSystemAccount.system_file_helper_short_no);
        bageVBinding.avatarView.setSize(70);
        bageVBinding.avatarView.showAvatar(BageSystemAccount.system_file_helper, BageChannelType.PERSONAL);
        SingleClickUtil.onSingleClick(bageVBinding.sendMsgBtn, v -> BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(this, BageSystemAccount.system_file_helper, BageChannelType.PERSONAL, 0, true)));
    }

    @Override
    protected void initListener() {
        bageVBinding.avatarView.setOnClickListener(v -> showImg());
    }

    private void showImg() {
        String uri = BageApiConfig.getAvatarUrl(BageSystemAccount.system_file_helper) + "?key=" + BageTimeUtils.getInstance().getCurrentMills();
        List<Object> tempImgList = new ArrayList<>();
        List<ImageView> imageViewList = new ArrayList<>();
        imageViewList.add(bageVBinding.avatarView.imageView);
        tempImgList.add(BageApiConfig.getShowUrl(uri));
        int index = 0;
        BageDialogUtils.getInstance().showImagePopup(this, tempImgList, imageViewList, bageVBinding.avatarView.imageView, index, new ArrayList<>(), null, null);

    }
}
