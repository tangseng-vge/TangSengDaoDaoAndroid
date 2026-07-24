package com.chat.uikit.user;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActUserQrLayoutBinding;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-06-29 23:21
 * 个人二维码
 */
public class UserQrActivity extends BageBaseActivity<ActUserQrLayoutBinding> {

    @Override
    protected ActUserQrLayoutBinding getViewBinding() {
        return ActUserQrLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.my_qr);
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
            ImageUtils.getInstance().saveBitmap(UserQrActivity.this, bitmap, true, path -> showToast(R.string.saved_album));
        }));
        ImageView rightIV = findViewById(R.id.titleRightIv);
        BageDialogUtils.getInstance().showScreenPopup(rightIV,  list);

    }


    @Override
    protected void initView() {
        bageVBinding.qrDescTv.setText(String.format(getString(R.string.qr_desc), getString(R.string.app_name)));
        bageVBinding.nameTv.setText(BageConfig.getInstance().getUserName());
        bageVBinding.avatarView.showAvatar(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
        UserModel.getInstance().userQr((code, msg, userQr) -> {
            if (code == HttpResponseCode.success) {
                String qrCode = userQr.data;
                Bitmap mBitmap = (Bitmap) EndpointManager.getInstance().invoke("create_qrcode", qrCode);
                bageVBinding.qrIv.setImageBitmap(mBitmap);
            } else showToast(msg);
        });
    }

    @Override
    protected void initListener() {

    }
}
