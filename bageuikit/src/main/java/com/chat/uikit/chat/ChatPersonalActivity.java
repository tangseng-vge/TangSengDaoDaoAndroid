package com.chat.uikit.chat;

import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chat.base.act.BageWebViewActivity;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatSettingCellMenu;
import com.chat.base.endpoint.entity.PrivacyMessageMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.NormalClickableContent;
import com.chat.base.ui.components.NormalClickableSpan;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.chat.search.MessageRecordActivity;
import com.chat.uikit.contacts.ChooseContactsActivity;
import com.chat.uikit.contacts.service.FriendModel;
import com.chat.uikit.databinding.ActChatPersonalLayoutBinding;
import com.chat.uikit.message.MsgModel;
import com.chat.uikit.user.MyInfoActivity;
import com.chat.uikit.user.SetUserRemarkActivity;
import com.chat.uikit.user.UserDetailActivity;
import com.chat.uikit.user.BageFileHelperActivity;
import com.chat.uikit.user.BageSystemTeamActivity;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelMemberExtras;
import com.bage.im.entity.BageChannelType;

/**
 * 2019-12-08 12:26
 * 个人会话资料页面
 */
public class ChatPersonalActivity extends BageBaseActivity<ActChatPersonalLayoutBinding> {
    private String channelId;
    private BageChannel channel;

    private BageChannel userChannel;

    ActivityResultLauncher<Intent> chooseResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
//            getUserInfo();
            channel = BageIM.getInstance().getChannelManager().getChannel(channelId, BageChannelType.PERSONAL);
            if (channel != null) {
                bageVBinding.avatarView.showAvatar(channel.channelID, channel.channelType, false);
                bageVBinding.nameTv.setText(TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark);
                bageVBinding.muteSwitchView.setChecked(channel.mute == 1);
                bageVBinding.stickSwitchView.setChecked(channel.top == 1);

            }
        }
    });

    @Override
    protected ActChatPersonalLayoutBinding getViewBinding() {
        return ActChatPersonalLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.chat_info);
    }

    @Override
    protected void initPresenter() {
        channelId = getIntent().getStringExtra("channelId");
        userChannel = BageIM.getInstance().getChannelManager().getChannel(channelId, BageChannelType.PERSONAL);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void initView() {
//        int w = AndroidUtilities.getScreenWidth() - AndroidUtilities.dp(10);
        Theme.applyAccentSwitchStyle(this, bageVBinding.muteSwitchView);
        Theme.applyAccentSwitchStyle(this, bageVBinding.stickSwitchView);
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        View view = (View) EndpointManager.getInstance().invoke("msg_remind_view", new ChatSettingCellMenu(channelId, BageChannelType.PERSONAL, bageVBinding.msgRemindLayout));
        if (view != null) {
            bageVBinding.msgRemindLayout.removeAllViews();
            bageVBinding.msgRemindLayout.addView(view);
        }
//        View findMsgView = (View) EndpointManager.getInstance().invoke("find_msg_view", new ChatSettingCellMenu(channelId, BageChannelType.PERSONAL, bageVBinding.findContentLayout));
//        if (findMsgView != null) {
//            bageVBinding.findContentLayout.removeAllViews();
//            bageVBinding.findContentLayout.addView(findMsgView);
//        }

        View msgReceiptView = (View) EndpointManager.getInstance().invoke("msg_receipt_view", new ChatSettingCellMenu(channelId, BageChannelType.PERSONAL, bageVBinding.msgSettingLayout));
        if (msgReceiptView != null) {
            bageVBinding.msgSettingLayout.removeAllViews();
            bageVBinding.msgSettingLayout.addView(msgReceiptView);
        }
        View msgPrivacyLayout = (View) EndpointManager.getInstance().invoke("chat_setting_msg_privacy", new ChatSettingCellMenu(channelId, BageChannelType.PERSONAL, bageVBinding.msgSettingLayout));
        if (msgPrivacyLayout != null) {
            bageVBinding.msgSettingLayout.addView(msgPrivacyLayout);
        }

        View chatPwdView = (View) EndpointManager.getInstance().invoke("chat_pwd_view", new ChatSettingCellMenu(channelId, BageChannelType.PERSONAL, bageVBinding.chatPwdView));
        if (chatPwdView != null) {
            bageVBinding.chatPwdView.addView(chatPwdView);
        }

    }

    @Override
    protected void initListener() {
        EndpointManager.getInstance().setMethod("chat_personal_activity", EndpointCategory.bageExitChat, object -> {
            if (object != null) {
                BageChannel channel = (BageChannel) object;
                if (channelId.equals(channel.channelID) && channel.channelType == BageChannelType.PERSONAL) {
                    finish();
                }
            }
            return null;
        });
        SingleClickUtil.onSingleClick(bageVBinding.findContentLayout, v -> {
            Intent intent = new Intent(this, MessageRecordActivity.class);
            intent.putExtra("channel_id", channelId);
            intent.putExtra("channel_type", BageChannelType.PERSONAL);
            startActivity(intent);
        });
        //remark
        SingleClickUtil.onSingleClick(bageVBinding.remarkLayout,v-> {
            Intent intent = new Intent(this, SetUserRemarkActivity.class);
            intent.putExtra("uid", channelId);
            intent.putExtra("oldStr", userChannel == null ? "" : userChannel.channelRemark);
            chooseResultLac.launch(intent);
        });
        //免打扰
        bageVBinding.muteSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                FriendModel.getInstance().updateUserSetting(channelId, "mute", b ? 1 : 0, (code, msg) -> {
                    if (code != HttpResponseCode.success) {
                        bageVBinding.muteSwitchView.setChecked(!b);
                        showToast(msg);
                    }
                });
            }
        });
        //置顶
        bageVBinding.stickSwitchView.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed())
                FriendModel.getInstance().updateUserSetting(channelId, "top", b ? 1 : 0, (code, msg) -> {
                    if (code != HttpResponseCode.success) {
                        bageVBinding.stickSwitchView.setChecked(!b);
                        showToast(msg);
                    }
                });
        });
        bageVBinding.clearChatMsgLayout.setOnClickListener(v -> {
            String content = String.format(getString(R.string.clear_history_tip), channel == null ? "" : channel.channelName);

            Object object = EndpointManager.getInstance().invoke("is_register_msg_privacy_module", null);
            if (object instanceof PrivacyMessageMenu) {
                String showName = "";
                if (channel != null) {
                    if (TextUtils.isEmpty(channel.channelRemark)) {
                        showName = channel.channelName;
                    } else {
                        showName = channel.channelRemark;
                    }
                }
                String checkBoxText = String.format(getString(R.string.str_delete_message_also_to), showName);
                BageDialogUtils.getInstance().showCheckBoxDialog(this, getString(R.string.clear_history), content, checkBoxText, true, "", getString(R.string.base_delete), 0, ContextCompat.getColor(this, R.color.red), (index, isChecked) -> {
                    if (index == 1) {
                        if (isChecked) {
                            ((PrivacyMessageMenu) object).getIClick().clearChannelMsg(channelId, BageChannelType.PERSONAL);
                        } else {
                            MsgModel.getInstance().offsetMsg(channelId, BageChannelType.PERSONAL, null);
                            BageIM.getInstance().getMsgManager().clearWithChannel(channelId, BageChannelType.PERSONAL);
                            showToast(R.string.cleared);
                        }
                    }
                });
                return;
            }
            BageDialogUtils.getInstance().showDialog(this, getString(R.string.clear_history), content, true, "", getString(R.string.base_delete), 0, ContextCompat.getColor(this, R.color.red), new BageDialogUtils.IClickListener() {
                @Override
                public void onClick(int index) {
                    if (index == 1) {
                        MsgModel.getInstance().offsetMsg(channelId, BageChannelType.PERSONAL, null);
                        BageIM.getInstance().getMsgManager().clearWithChannel(channelId, BageChannelType.PERSONAL);
                        showToast(R.string.cleared);
                    }
                }
            });
        });
        SingleClickUtil.onSingleClick(bageVBinding.addIv, view1 -> {
            Intent intent = new Intent(ChatPersonalActivity.this, ChooseContactsActivity.class);
            intent.putExtra("unSelectUids", channelId);
            intent.putExtra("isIncludeUids", true);
            chooseCardResultLac.launch(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.avatarView, view1 -> {
            Intent intent = new Intent(ChatPersonalActivity.this, UserDetailActivity.class);
            intent.putExtra("uid", channelId);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(bageVBinding.reportLayout, view1 -> {
            Intent intent = new Intent(this, BageWebViewActivity.class);
            intent.putExtra("channelType", BageChannelType.PERSONAL);
            intent.putExtra("channelID", channelId);
            intent.putExtra("url", BageApiConfig.baseWebUrl + "report.html");
            startActivity(intent);
        });
    }

    @Override
    protected void initData() {
        super.initData();

        if (BageSystemAccount.isSystemAccount(channelId)) {
            Intent intent = new Intent(this, UserDetailActivity.class);
            intent.putExtra("uid", channelId);
            startActivity(intent);
            finish();
            return;
        }
        channel = BageIM.getInstance().getChannelManager().getChannel(channelId, BageChannelType.PERSONAL);
        if (channel != null) {
            bageVBinding.avatarView.showAvatar(channel.channelID, channel.channelType, false);
            bageVBinding.nameTv.setText(TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark);
            bageVBinding.muteSwitchView.setChecked(channel.mute == 1);
            bageVBinding.stickSwitchView.setChecked(channel.top == 1);

        }
    }

    ActivityResultLauncher<Intent> chooseCardResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            finish();
        }
    });

    @Override
    public void finish() {
        super.finish();
        EndpointManager.getInstance().remove("chat_personal_activity");
    }

}
