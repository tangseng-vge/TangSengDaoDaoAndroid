package com.chat.groupmanage.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.chat.base.BageBaseApplication;
import com.chat.base.act.BageCropImageActivity;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.GlideUtils;
import com.chat.base.msgitem.BageChannelMemberRole;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BagePermissions;
import com.chat.base.utils.BageReader;
import com.chat.groupmanage.R;
import com.chat.groupmanage.databinding.ActGroupHeaderLayoutBinding;
import com.chat.groupmanage.service.GroupManageModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupHeaderActivity extends BageBaseActivity<ActGroupHeaderLayoutBinding> {
    private String groupNO;
    BageChannelMember member = null;

    @Override
    protected ActGroupHeaderLayoutBinding getViewBinding() {
        return ActGroupHeaderLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_avatar);
    }

    @Override
    protected void initPresenter() {
        groupNO = getIntent().getStringExtra("groupNo");
    }

    @Override
    protected int getRightIvResourceId(ImageView imageView) {
        return R.mipmap.ic_ab_other;
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
        showBottomDialog();
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initListener() {

    }

    @Override
    protected void initData() {
        super.initData();
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(groupNO, BageChannelType.GROUP);
        String key = "";
        if (channel != null) {
            key = channel.avatarCacheKey;
        }
        member = BageIM.getInstance().getChannelMembersManager().getMember(groupNO, BageChannelType.GROUP, BageConfig.getInstance().getUid());
        if (member == null) {
            hideTitleRightView();
        } else {
            showTitleRightView();
        }
        GlideUtils.getInstance().showAvatarImg(this, groupNO, BageChannelType.GROUP, key, bageVBinding.avatarIv);

    }

    private void showBottomDialog() {
        List<PopupMenuItem> list = new ArrayList<>();
        if (member != null && member.role != BageChannelMemberRole.normal)
            list.add(new PopupMenuItem(getString(R.string.update_avatar), R.mipmap.group_edit, () -> {
                BageBaseApplication.getInstance().disconnect = false;
                chooseIMG();
            }));
        list.add(new PopupMenuItem(getString(R.string.save_img), R.mipmap.msg_download, () -> {
            String url = String.format("%s?key=%s", BageApiConfig.getGroupUrl(groupNO), UUID.randomUUID().toString().replaceAll("-", ""));
            ImageUtils.getInstance().downloadImg(this, url, bitmap -> {
                if (bitmap != null) {
                    ImageUtils.getInstance().saveBitmap(GroupHeaderActivity.this, bitmap, true, path -> showToast(R.string.saved_album));
                }
            });
        }));
        ImageView rightIV = findViewById(R.id.titleRightIv);
        BageDialogUtils.getInstance().showScreenPopup(rightIV, list);
    }


    private void chooseIMG() {
        String desc = String.format(getString(R.string.file_permissions_des), getString(R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        success();
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA);
        } else {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        success();
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void success() {

        GlideUtils.getInstance().chooseIMG(GroupHeaderActivity.this, 1, true, ChooseMimeType.img, false,false, new GlideUtils.ISelectBack() {
            @Override
            public void onBack(List<ChooseResult> paths) {
                if (BageReader.isNotEmpty(paths)) {
                    String path = paths.get(0).path;
                    if (!TextUtils.isEmpty(path)) {
                        Intent intent = new Intent(GroupHeaderActivity.this, BageCropImageActivity.class);
                        intent.putExtra("path", path);
                        chooseResultLac.launch(intent);
                    }
                }
            }

            @Override
            public void onCancel() {

            }
        });

    }

    ActivityResultLauncher<Intent> chooseResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            String path = result.getData().getStringExtra("path");
            GroupManageModel.getInstance().uploadAvatar(groupNO, path, code -> {
                if (code == HttpResponseCode.success) {
                    BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(groupNO, BageChannelType.GROUP);
                    channel.avatarCacheKey = UUID.randomUUID().toString().replace("-", "");
                    BageIM.getInstance().getChannelManager().updateAvatarCacheKey(groupNO, BageChannelType.GROUP, channel.avatarCacheKey);
                    GlideUtils.getInstance().showAvatarImg(this, channel.channelID, BageChannelType.GROUP, channel.avatarCacheKey, bageVBinding.avatarIv);
                }
            });
        }
    });
}
