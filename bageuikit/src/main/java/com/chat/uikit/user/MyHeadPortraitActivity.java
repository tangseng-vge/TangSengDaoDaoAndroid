package com.chat.uikit.user;

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
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BagePermissions;
import com.chat.base.utils.BageReader;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActMyHeadPortraitLayoutBinding;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 2020-06-29 23:35
 * 我的头像
 */
public class MyHeadPortraitActivity extends BageBaseActivity<ActMyHeadPortraitLayoutBinding> {
    @Override
    protected ActMyHeadPortraitLayoutBinding getViewBinding() {
        return ActMyHeadPortraitLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.head_portrait);
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
    protected void onResume() {
        super.onResume();
        BageBaseApplication.getInstance().disconnect = true;
    }

    @Override
    protected void initView() {
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
        String url = BageApiConfig.getAvatarUrl(BageConfig.getInstance().getUid());

        if (channel != null && !TextUtils.isEmpty(channel.channelID)) {
            GlideUtils.getInstance().showAvatarImg(this, channel.channelID, channel.channelType, channel.avatarCacheKey, bageVBinding.avatarIv);
        } else {
            GlideUtils.getInstance().showImg(this, url + "?width=500&height=500", bageVBinding.avatarIv);
        }
    }

    @Override
    protected void initListener() {
        bageVBinding.avatarIv.setOnLongClickListener(view1 -> {
            showBottomDialog();
            return true;
        });
    }

    private void showBottomDialog() {
        List<PopupMenuItem> list = new ArrayList<>();
        list.add(new PopupMenuItem(getString(R.string.update_avatar), R.mipmap.msg_edit, () -> {
            BageBaseApplication.getInstance().disconnect = false;
            chooseIMG();
        }));

        list.add(new PopupMenuItem(getString(R.string.save_img), R.mipmap.msg_download, () -> {
            String avatarURL = BageApiConfig.getAvatarUrl(BageConfig.getInstance().getUid());
            avatarURL = avatarURL + "?key=" + UUID.randomUUID().toString().replaceAll("-","");
            ImageUtils.getInstance().downloadImg(this, avatarURL, bitmap -> {
                if (bitmap != null) {
                    ImageUtils.getInstance().saveBitmap(MyHeadPortraitActivity.this, bitmap, true, path -> showToast(R.string.saved_album));
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
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE);
        }


    }

    private void success() {

        GlideUtils.getInstance().chooseIMG(MyHeadPortraitActivity.this, 1, true, ChooseMimeType.img, false,false, new GlideUtils.ISelectBack() {
            @Override
            public void onBack(List<ChooseResult> paths) {
                if (BageReader.isNotEmpty(paths)) {
                    String path = paths.get(0).path;
                    if (!TextUtils.isEmpty(path)) {
                        Intent intent = new Intent(MyHeadPortraitActivity.this, BageCropImageActivity.class);
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
            UserModel.getInstance().uploadAvatar(path, code -> {
                if (code == HttpResponseCode.success) {
                    BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
                    if (channel == null || TextUtils.isEmpty(channel.channelID)) {
                        channel = new BageChannel();
                        channel.channelType = BageChannelType.PERSONAL;
                        channel.channelID = BageConfig.getInstance().getUid();
                        BageIM.getInstance().getChannelManager().saveOrUpdateChannel(channel);
                    }
                    channel.avatarCacheKey = UUID.randomUUID().toString().replace("-", "");
                    BageIM.getInstance().getChannelManager().updateAvatarCacheKey(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL, channel.avatarCacheKey);
                    GlideUtils.getInstance().showAvatarImg(this, channel.channelID, BageChannelType.PERSONAL, channel.avatarCacheKey, bageVBinding.avatarIv);
                    String avatarURL = BageApiConfig.getAvatarUrl(BageConfig.getInstance().getUid());
                    avatarURL = avatarURL + "?key=" + channel.avatarCacheKey;
                    EndpointManager.getInstance().invoke("updateRtcAvatarUrl", avatarURL);
                }
            });
        }
    });
}
