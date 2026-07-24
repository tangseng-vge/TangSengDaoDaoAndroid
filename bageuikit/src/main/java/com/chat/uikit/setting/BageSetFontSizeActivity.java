package com.chat.uikit.setting;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.msgitem.BageChatIteMsgFromType;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageMsgBgType;
import com.chat.base.utils.AndroidUtilities;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActSetFontSizeLayoutBinding;
import com.bage.im.entity.BageChannelType;

/**
 * 4/7/21 10:17 AM
 * 设置字体大小
 */
public class BageSetFontSizeActivity extends BageBaseActivity<ActSetFontSizeLayoutBinding> {
    private float fontSizeScale;
    private boolean isChange;//用于监听字体大小是否有改动
    private int defaultPos;

    @Override
    protected ActSetFontSizeLayoutBinding getViewBinding() {
        return ActSetFontSizeLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.font_size);
    }

    @Override
    protected String getRightBtnText(Button titleRightBtn) {
        return getString(R.string.complete);
    }

    @Override
    protected void rightButtonClick() {
        super.rightButtonClick();
        if (isChange) {
            showDialog(String.format(getString(R.string.dark_save_tips), getString(R.string.app_name)), index -> {
                if (index == 1) {
                    BageConstants.setFontScale(fontSizeScale);
                    //重启应用
                    EndpointManager.getInstance().invoke("main_show_home_view", 0);
                }
            });
        }
    }

    @Override
    protected void initView() {
        bageVBinding.sendLayout.setAll(BageMsgBgType.single, BageChatIteMsgFromType.SEND, BageContentType.Bage_TEXT);
        bageVBinding.recvLayout.setAll(BageMsgBgType.top, BageChatIteMsgFromType.RECEIVED, BageContentType.Bage_TEXT);
        bageVBinding.recvLayout1.setAll(BageMsgBgType.bottom, BageChatIteMsgFromType.RECEIVED, BageContentType.Bage_TEXT);
        String appName = getString(R.string.app_name);
        bageVBinding.textSizeTv3.setText(String.format(getString(R.string.set_text_size_feedback), appName));
        bageVBinding.avatarIv.showAvatar(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
        bageVBinding.leftAvatarIv1.showAvatar(BageSystemAccount.system_team, BageChannelType.PERSONAL);
    }

    @Override
    protected void initListener() {
        bageVBinding.sendLayout.setOnLongClickListener(v -> true);
        bageVBinding.recvLayout.setOnLongClickListener(v -> true);
        bageVBinding.recvLayout1.setOnLongClickListener(v -> true);
        bageVBinding.fontSizeView.setChangeCallbackListener(position -> {
            // 字体大小
            int dimension = getResources().getDimensionPixelSize(R.dimen.font_size_16);
            //根据position 获取字体倍数
            fontSizeScale = (float) (0.875 + 0.125 * position);
            //放大后的sp单位
            double v = fontSizeScale * (int) AndroidUtilities.px2sp(dimension);
            //改变当前页面大小
            changeTextSize((int) v);
            isChange = !(position == defaultPos);
        });

        float scale = BageConstants.getFontScale();
        if (scale > 0.5) {
            defaultPos = (int) ((scale - 0.875) / 0.125);
        } else {
            defaultPos = 1;
        }
        //注意： 写在改变监听下面 —— 否则初始字体不会改变
        bageVBinding.fontSizeView.setDefaultPosition(defaultPos);
    }

    private void changeTextSize(int dimension) {
        bageVBinding.textSizeTv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimension); //22SP
        bageVBinding.textSizeTv2.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimension); //22SP
        bageVBinding.textSizeTv3.setTextSize(TypedValue.COMPLEX_UNIT_SP, dimension); //22SP
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = res.getConfiguration();
        config.fontScale = 1;//1 设置正常字体大小的倍数
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }

}
