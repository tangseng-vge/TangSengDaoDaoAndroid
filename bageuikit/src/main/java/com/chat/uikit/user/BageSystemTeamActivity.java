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
import com.chat.uikit.databinding.ActSystemTeamLayoutBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 1/29/21 12:00 PM
 * 系统团队
 */
public class BageSystemTeamActivity extends BageBaseActivity<ActSystemTeamLayoutBinding> {
    @Override
    protected ActSystemTeamLayoutBinding getViewBinding() {
        return ActSystemTeamLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText("");
    }


    @Override
    protected void initView() {
        bageVBinding.appIdNumLeftTv.setText(String.format(getString(R.string.app_idnum), getString(R.string.app_name)));
        bageVBinding.functionNameTv.setText(String.format(getString(R.string.function_system_team_tips), getString(R.string.app_name)));
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(BageSystemAccount.system_team, BageChannelType.PERSONAL);
        if (channel != null) {
            bageVBinding.nameTv.setText(channel.channelName);
            bageVBinding.appIdNumTv.setText(BageSystemAccount.system_team_short_no);
        }
        bageVBinding.nameTv.setText(R.string.bage_system_notice);
        bageVBinding.avatarView.setSize(70);
        bageVBinding.avatarView.showAvatar(channel);
        SingleClickUtil.onSingleClick(bageVBinding.sendMsgBtn, v -> BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(this, BageSystemAccount.system_team, BageChannelType.PERSONAL, 0, true)));
    }

    @Override
    protected void initListener() {
        bageVBinding.avatarView.setOnClickListener(v -> showImg());
    }

    private void showImg() {
        String uri = BageApiConfig.getAvatarUrl(BageSystemAccount.system_team) + "?key=" + BageTimeUtils.getInstance().getCurrentMills();
        //查看大图
        List<Object> tempImgList = new ArrayList<>();
        List<ImageView> imageViewList = new ArrayList<>();
        imageViewList.add(bageVBinding.avatarView.imageView);
        tempImgList.add(BageApiConfig.getShowUrl(uri));
        int index = 0;
        BageDialogUtils.getInstance().showImagePopup(this, tempImgList, imageViewList, bageVBinding.avatarView.imageView, index, new ArrayList<>(), null, null);

    }
}
