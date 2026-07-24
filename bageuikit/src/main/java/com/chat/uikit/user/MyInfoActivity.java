package com.chat.uikit.user;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageConfig;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.entity.UserInfoEntity;
import com.chat.base.entity.BageAPPConfig;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActMyInfoLayoutBinding;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-06-29 22:28
 * 登录用户个人信息
 */
public class MyInfoActivity extends BageBaseActivity<ActMyInfoLayoutBinding> {

    @Override
    protected ActMyInfoLayoutBinding getViewBinding() {
        return ActMyInfoLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.personal_info);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        bageVBinding.idLeftTv.setText(String.format(getString(R.string.identity), getString(R.string.app_name)));
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        UserInfoEntity userInfoEntity = BageConfig.getInstance().getUserInfo();
        BageAPPConfig appConfig = BageConfig.getInstance().getAppConfig();
        if (userInfoEntity.short_status == 1 || appConfig.shortno_edit_off == 1) {
            bageVBinding.identityLayout.setEnabled(false);
            bageVBinding.identityIv.setVisibility(View.GONE);
        }

        bageVBinding.phoneRightTV.setText(userInfoEntity.phone);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
        if (channel != null && !TextUtils.isEmpty(channel.channelID)) {
            bageVBinding.avatarView.showAvatar(channel);
        } else
            bageVBinding.avatarView.showAvatar(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
    }

    @Override
    protected void initListener() {
        BageCommonModel.getInstance().getChannel(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL, (code, msg, entity) -> {
            if (entity != null && entity.extra != null) {
                Object sexObject = entity.extra.get("sex");
                if (sexObject != null) {
                    int sex = (int) sexObject;
                    bageVBinding.sexTv.setText(sex == 1 ? R.string.male : R.string.female);
                }
                Object shortNoObject = entity.extra.get("short_no");
                if (shortNoObject != null) {
                    String shortNo = (String) shortNoObject;
                    bageVBinding.identityTv.setText(shortNo);
                    bageVBinding.nameTv.setText(entity.name);
                }
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.headLayout, view -> startActivity(new Intent(MyInfoActivity.this, MyHeadPortraitActivity.class)));
        SingleClickUtil.onSingleClick(bageVBinding.nameLayout, view1 -> {
            Intent intent = new Intent(this, UpdateUserInfoActivity.class);
            intent.putExtra("oldStr", bageVBinding.nameTv.getText().toString());
            intent.putExtra("updateType", 1);
            chooseResultLac.launch(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.identityLayout, view1 -> {
            if (BageConfig.getInstance().getAppConfig().shortno_edit_off == 0) {
                Intent intent = new Intent(this, UpdateUserInfoActivity.class);
                intent.putExtra("oldStr", bageVBinding.identityTv.getText().toString());
                intent.putExtra("updateType", 2);
                chooseResultLac.launch(intent);
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.qrLayout, view1 -> startActivity(new Intent(this, UserQrActivity.class)));
        bageVBinding.sexLayout.setOnClickListener(v -> {
            List<BottomSheetItem> list = new ArrayList<>();
            list.add(new BottomSheetItem(getString(R.string.male), 0, () -> updateSex(1)));
            list.add(new BottomSheetItem(getString(R.string.female), 0, () -> updateSex(0)));
            BageDialogUtils.getInstance().showBottomSheet(this,getString(R.string.sex),false,list);
        });
    }
    private void updateSex(int value){
        UserModel.getInstance().updateUserInfo("sex", String.valueOf(value), (code, msg) -> {
            if (code == HttpResponseCode.success)
                bageVBinding.sexTv.setText(value == 1 ? R.string.male : R.string.female);
            else showToast(msg);
        });
    }
    ActivityResultLauncher<Intent> chooseResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            String resultStr = result.getData().getStringExtra("result");
            int updateType = result.getData().getIntExtra("updateType", 1);
            if (updateType == 1) {
                bageVBinding.nameTv.setText(resultStr);
                BageConfig.getInstance().setUserName(resultStr);
            } else if (updateType == 2) {
                bageVBinding.identityTv.setText(resultStr);
                bageVBinding.identityIv.setVisibility(View.GONE);
            }
        }
    });
}
