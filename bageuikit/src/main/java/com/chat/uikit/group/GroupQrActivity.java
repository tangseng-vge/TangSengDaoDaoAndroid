package com.chat.uikit.group;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.ChannelInfoEntity;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActGroupQrLayoutBinding;
import com.chat.uikit.group.service.GroupContract;
import com.chat.uikit.group.service.GroupPresenter;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-04-06 22:30
 * 群聊二维码名片
 */
public class GroupQrActivity extends BageBaseActivity<ActGroupQrLayoutBinding> implements GroupContract.GroupView {

    private GroupPresenter presenter;
    String groupId;

    @Override
    protected ActGroupQrLayoutBinding getViewBinding() {
        return ActGroupQrLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_qr);
    }

    @Override
    protected void initPresenter() {
        presenter = new GroupPresenter(this);
    }

    @Override
    protected int getRightIvResourceId(ImageView imageView) {
        return R.mipmap.ic_ab_other;
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        List<PopupMenuItem> list = new ArrayList<>();
        list.add(new PopupMenuItem(getString(R.string.save_img), R.mipmap.msg_download, () -> {
            Bitmap bitmap = ImageUtils.getInstance().loadBitmapFromView(bageVBinding.shadowLayout);
            ImageUtils.getInstance().saveBitmap(this, bitmap, true, path -> showToast(R.string.saved_album));
        }));
        ImageView rightIV = findViewById(R.id.titleRightIv);
        BageDialogUtils.getInstance().showScreenPopup(rightIV,  list);
    }

    @Override
    protected void initView() {
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.avatarView.setSize(45);
    }

    @Override
    protected void initListener() {
        BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo("group_qr_channel_refresh", (channel, isEnd) -> getGroupInfo());
    }

    @Override
    protected void initData() {
        super.initData();
        groupId = getIntent().getStringExtra("groupId");
        getGroupInfo();
    }

    private void getGroupInfo() {
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(groupId, BageChannelType.GROUP);
        if (channel != null) {
            bageVBinding.nameTv.setText(channel.channelName);
            bageVBinding.avatarView.showAvatar(groupId, channel.channelType, channel.avatarCacheKey);
            if (channel.invite == 1) {
                bageVBinding.qrTv.setVisibility(View.INVISIBLE);
                bageVBinding.unusedTv.setVisibility(View.VISIBLE);
                bageVBinding.qrIv.setImageResource(R.mipmap.icon_no_qr);
            } else {
                bageVBinding.unusedTv.setVisibility(View.INVISIBLE);
                bageVBinding.qrTv.setVisibility(View.VISIBLE);
                presenter.getQrData(groupId);
            }
        } else {
            presenter.getQrData(groupId);
        }
    }

    @Override
    public void onGroupInfo(ChannelInfoEntity groupEntity) {

    }

    @Override
    public void onRefreshGroupSetting(String key, int value) {

    }

    @Override
    public void setQrData(int day, String qrcode, String expire) {
        if (TextUtils.isEmpty(qrcode)) {
            bageVBinding.qrIv.setImageResource(R.mipmap.icon_no_qr);
        } else {
            Bitmap mBitmap = (Bitmap) EndpointManager.getInstance().invoke("create_qrcode", qrcode);
//            Bitmap mBitmap = CodeUtils.createQRCode(qrcode, 400, null);
            bageVBinding.qrIv.setImageBitmap(mBitmap);
            String content = String.format(getString(R.string.group_qr_desc), day, expire);
            bageVBinding.qrTv.setText(content);
        }
    }

    @Override
    public void setMyGroups(List<GroupEntity> list) {

    }

    @Override
    public void showError(String msg) {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BageIM.getInstance().getChannelManager().removeRefreshChannelInfo("group_qr_channel_refresh");
    }
}
