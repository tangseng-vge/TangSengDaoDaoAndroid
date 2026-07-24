package com.chat.uikit.user;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.chat.base.act.BageWebViewActivity;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.endpoint.entity.UserDetailViewMenu;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.NormalClickableContent;
import com.chat.base.ui.components.NormalClickableSpan;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.BageToastUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.contacts.service.FriendModel;
import com.chat.uikit.databinding.ActUserDetailLayoutBinding;
import com.chat.uikit.db.BageContactsDB;
import com.chat.uikit.message.MsgModel;
import com.chat.uikit.user.service.UserModel;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelMemberExtras;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 2020-03-19 22:06
 * 个人资料
 */
public class UserDetailActivity extends BageBaseActivity<ActUserDetailLayoutBinding> {
    String uid;
    String groupID;
    private String vercode;
    private BageChannel userChannel;

    @Override
    protected ActUserDetailLayoutBinding getViewBinding() {
        return ActUserDetailLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.user_card);
    }

    @Override
    protected void initPresenter() {
        initParams(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initParams(intent);
        initView();
        initListener();
        initData();
    }

    private void initParams(Intent mIntent) {
        uid = mIntent.getStringExtra("uid");
        if (TextUtils.isEmpty(uid)) finish();
        if (uid.equals(BageSystemAccount.system_file_helper)) {
            Intent intent = new Intent(this, BageFileHelperActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if (uid.equals(BageSystemAccount.system_team)) {
            Intent intent = new Intent(this, BageSystemTeamActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if (uid.equals(BageConfig.getInstance().getUid())) {
            Intent intent = new Intent(this, MyInfoActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if (mIntent.hasExtra("groupID")) {
            groupID = mIntent.getStringExtra("groupID");
        } else {
            groupID = "";
        }
        if (mIntent.hasExtra("vercode")) {
            vercode = mIntent.getStringExtra("vercode");
        } else {
            vercode = "";
        }
        userChannel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
        if (!TextUtils.isEmpty(groupID)) {
            BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(groupID, BageChannelType.GROUP, uid);
            if (member != null && member.extraMap != null && member.extraMap.containsKey(BageChannelMemberExtras.BageCode)) {
                vercode = (String) member.extraMap.get(BageChannelMemberExtras.BageCode);
            }
            if (member != null && !TextUtils.isEmpty(member.memberRemark)) {
                bageVBinding.inGroupNameLayout.setVisibility(View.VISIBLE);
                bageVBinding.inGroupNameTv.setText(member.memberRemark);
            }
            if (member != null && !TextUtils.isEmpty(member.memberInviteUID) && member.isDeleted == 0) {
                String name = "";
                BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(member.memberInviteUID, BageChannelType.PERSONAL);
                if (channel != null) {
                    name = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
                }
                if (TextUtils.isEmpty(name)) {
                    BageChannelMember member1 = BageIM.getInstance().getChannelMembersManager().getMember(groupID, BageChannelType.GROUP, member.memberInviteUID);
                    if (member1 != null) {
                        name = TextUtils.isEmpty(member1.memberRemark) ? member1.memberName : member1.memberRemark;
                    }
                }
                if (!TextUtils.isEmpty(name)) {
                    bageVBinding.joinGroupWayLayout.setVisibility(View.VISIBLE);
                    String showTime = "";
                    if (!TextUtils.isEmpty(member.createdAt) && member.createdAt.contains(" ")) {
                        showTime = member.createdAt.split(" ")[0];
                    }
                    String content = String.format("%s %s", showTime, String.format(getString(R.string.invite_join_group), name));
                    bageVBinding.joinGroupWayTv.setText(content);
                    int index = content.indexOf(name);
                    SpannableString span = new SpannableString(content);
                    span.setSpan(new NormalClickableSpan(false, Theme.colorAccount, new NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""), view -> {

                    }), index, index + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bageVBinding.joinGroupWayTv.setText(span);
                }
            }
        } else {
            bageVBinding.joinGroupWayLayout.setVisibility(View.GONE);
        }

    }

    @Override
    protected void initView() {
        bageVBinding.applyBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.sendMsgBtn.getBackground().setTint(Theme.colorAccount);
        bageVBinding.avatarView.setSize(50);
        bageVBinding.appIdNumLeftTv.setText(String.format(getString(R.string.app_idnum), getString(R.string.app_name)));
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.otherLayout.removeAllViews();
        List<View> list = EndpointManager.getInstance().invokes(EndpointCategory.bageUserDetailView, new UserDetailViewMenu(this, bageVBinding.otherLayout, uid, groupID));
        if (BageReader.isNotEmpty(list)) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null)
                    bageVBinding.otherLayout.addView(list.get(i));
            }
        }
        if (bageVBinding.otherLayout.getChildCount() > 0) {
            LinearLayout view = new LinearLayout(this);
            view.setBackgroundColor(ContextCompat.getColor(this, R.color.homeColor));
            view.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 15));
            bageVBinding.otherLayout.addView(view);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initListener() {
        if (!TextUtils.isEmpty(groupID) && !uid.equals(BageConfig.getInstance().getUid())) {
            BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo("user_detail_refresh_channel", (channel, isEnd) -> {
                if (channel != null && channel.channelID.equals(groupID) && channel.channelType == BageChannelType.GROUP) {
                    getUserInfo();
                    bageVBinding.avatarView.showAvatar(channel);
                }
            });
        }

        SingleClickUtil.onSingleClick(bageVBinding.complaintLayout,v->{
            Intent intent = new Intent(this, BageWebViewActivity.class);
            intent.putExtra("channelType", BageChannelType.PERSONAL);
            intent.putExtra("channelID", userChannel.channelID);
            intent.putExtra("url", BageApiConfig.baseWebUrl + "report.html");
            startActivity(intent);
        });


        bageVBinding.pushBlackLayout.setOnClickListener(v -> {

            if (userChannel == null) return;
            String title = getString(userChannel.status == 2 ? R.string.pull_out_black_list : R.string.push_black_list);
            String content = getString(userChannel.status == 2 ? R.string.pull_out_black_list_tips : R.string.join_black_list_tips);

            BageDialogUtils.getInstance().showDialog(this, title, content, true, "", "", 0, 0, index -> {
                if (index == 1) {
                    if (userChannel.status != 2)
                        UserModel.getInstance().addBlackList(uid, (code, msg) -> {
                            if (code == HttpResponseCode.success) {
                                finish();
                            } else showToast(msg);
                        });
                    else UserModel.getInstance().removeBlackList(uid, (code, msg) -> {
                        if (code == HttpResponseCode.success) {
                            finish();
                        } else showToast(msg);
                    });

                }
            });

        });
        setonLongClick(bageVBinding.nameTv, bageVBinding.nameTv);
        setonLongClick(bageVBinding.identityLayout, bageVBinding.appIdNumTv);
        setonLongClick(bageVBinding.nickNameLayout, bageVBinding.nickNameTv);

        //频道资料刷新
        BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo("user_detail_refresh_channel1", (channel, isEnd) -> {
            if (channel != null && channel.channelID.equals(uid) && channel.channelType == BageChannelType.PERSONAL) {
                userChannel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
                setData();
            }
        });
        SingleClickUtil.onSingleClick(bageVBinding.applyBtn, v -> BageDialogUtils.getInstance().showInputDialog(UserDetailActivity.this, getString(R.string.apply), getString(R.string.input_remark), "", getString(R.string.input_remark), 20, text -> FriendModel.getInstance().applyAddFriend(uid, vercode, text, (code, msg) -> {
            if (code == HttpResponseCode.success) {
                bageVBinding.applyBtn.setText(R.string.applyed);
                bageVBinding.applyBtn.setAlpha(0.2f);
                bageVBinding.applyBtn.setEnabled(false);
            } else showToast(msg);
        })));
        SingleClickUtil.onSingleClick(bageVBinding.sendMsgBtn, v -> {
            BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(this, uid, BageChannelType.PERSONAL, 0, true));
            finish();
        });
        bageVBinding.deleteLayout.setOnClickListener(v -> {
            String content = String.format(getString(R.string.delete_friends_tips), bageVBinding.nameTv.getText().toString());
            BageDialogUtils.getInstance().showDialog(this, getString(R.string.delete_friends), content, true, "", getString(R.string.delete), 0, ContextCompat.getColor(this, R.color.red), index -> {
                if (index == 1) {
                    UserModel.getInstance().deleteUser(uid, (code, msg) -> {
                        if (code == HttpResponseCode.success) {
                            BageIM.getInstance().getConversationManager().deleteWitchChannel(uid, BageChannelType.PERSONAL);
                            MsgModel.getInstance().offsetMsg(uid, BageChannelType.PERSONAL, null);
                            BageIM.getInstance().getMsgManager().clearWithChannel(uid, BageChannelType.PERSONAL);
                            BageContactsDB.getInstance().updateFriendStatus(uid, 0);
                            BageIM.getInstance().getChannelManager().updateFollow(uid, BageChannelType.PERSONAL, 0);
                            EndpointManager.getInstance().invoke(BageConstants.refreshContacts, null);
                            EndpointManager.getInstance().invokes(EndpointCategory.bageExitChat, new BageChannel(uid, BageChannelType.PERSONAL));
                            finish();
                        } else showToast(msg);
                    });
                }
            });
        });
        SingleClickUtil.onSingleClick(bageVBinding.remarkLayout, v -> {
            Intent intent = new Intent(this, SetUserRemarkActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("oldStr", userChannel == null ? "" : userChannel.channelRemark);
            chooseResultLac.launch(intent);
        });
        bageVBinding.avatarView.setOnClickListener(v -> showImg());
    }

    private void showCopy(View view, float[] coordinate, String content) {
        List<PopupMenuItem> list = new ArrayList<>();
        list.add(new PopupMenuItem(getString(R.string.copy), R.mipmap.msg_copy, () -> {
            view.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", content);
            assert cm != null;
            cm.setPrimaryClip(mClipData);
            BageToastUtils.getInstance().showToastNormal(getString(R.string.copyed));
        }));
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.color999));
        BageDialogUtils.getInstance().showScreenPopup(view, coordinate, list, () -> view.setBackgroundColor(ContextCompat.getColor(UserDetailActivity.this, R.color.transparent)));
    }

    @Override
    protected void initData() {
        super.initData();
        setData();
        getUserInfo();
    }

    private void setData() {
        bageVBinding.avatarView.showAvatar(uid, BageChannelType.PERSONAL);
        if (uid.equals(BageConfig.getInstance().getUid())) hideTitleRightView();
        if (userChannel != null) {
            if (!TextUtils.isEmpty(userChannel.channelRemark)) {
                bageVBinding.nickNameLayout.setVisibility(View.VISIBLE);
                bageVBinding.nickNameTv.setText(userChannel.channelName);
                bageVBinding.nameTv.setText(userChannel.channelRemark);
            } else {
                bageVBinding.nameTv.setText(userChannel.channelName);
                bageVBinding.nickNameLayout.setVisibility(View.GONE);
            }
        } else {
            bageVBinding.deleteLayout.setVisibility(View.GONE);
            bageVBinding.sendMsgBtn.setVisibility(View.GONE);
        }
    }

    private void getUserInfo() {
        BageIM.getInstance().getChannelManager().fetchChannelInfo(uid, BageChannelType.PERSONAL);
        UserModel.getInstance().getUserInfo(uid, groupID, (code, msg, userInfo) -> {
            if (code == HttpResponseCode.success) {
                if (userInfo != null) {
                    if (!TextUtils.isEmpty(userInfo.vercode)) {
                        vercode = userInfo.vercode;
                    }
                    bageVBinding.nameTv.setText(TextUtils.isEmpty(userInfo.remark) ? userInfo.name : userInfo.remark);
                    bageVBinding.nickNameTv.setText(userInfo.name);
                    bageVBinding.nickNameLayout.setVisibility(TextUtils.isEmpty(userInfo.remark) ? View.GONE : View.VISIBLE);
                    if (TextUtils.isEmpty(userInfo.short_no)) {
                        bageVBinding.identityLayout.setVisibility(View.GONE);
                    } else {
                        bageVBinding.identityLayout.setVisibility(View.VISIBLE);
                        bageVBinding.appIdNumTv.setText(userInfo.short_no);
                    }
                    if (!TextUtils.isEmpty(userInfo.source_desc)) {
                        bageVBinding.sourceFromTv.setText(userInfo.source_desc);
                        bageVBinding.fromLayout.setVisibility(View.VISIBLE);
                    } else {
                        bageVBinding.fromLayout.setVisibility(View.GONE);
                    }

                    if (userInfo.status == 2) {
                        bageVBinding.blacklistTv.setText(R.string.pull_out_black_list);
                    } else {
                        bageVBinding.blacklistTv.setText(R.string.push_black_list);
                    }
                    bageVBinding.sendMsgBtn.setVisibility(userInfo.follow == 1 ? View.VISIBLE : View.GONE);
                    bageVBinding.applyBtn.setVisibility(userInfo.follow == 1 ? View.GONE : View.VISIBLE);
                    bageVBinding.deleteLayout.setVisibility(userInfo.follow == 1 ? View.VISIBLE : View.GONE);
                    bageVBinding.blacklistDescTv.setVisibility(userInfo.status == 2 ? View.VISIBLE : View.GONE);
                    if (userInfo.follow == 0) {
                        bageVBinding.applyBtn.setVisibility(TextUtils.isEmpty(vercode) ? View.GONE : View.VISIBLE);
                    } else {
                        bageVBinding.applyBtn.setVisibility(View.GONE);
                    }

                    if (!TextUtils.isEmpty(userInfo.join_group_invite_uid)){
                        bageVBinding.joinGroupWayLayout.setVisibility(View.VISIBLE);
                        String content = String.format("%s %s", userInfo.join_group_time, String.format(getString(R.string.invite_join_group), userInfo.join_group_invite_name));
                        bageVBinding.joinGroupWayTv.setText(content);
                        int index = content.indexOf(userInfo.join_group_invite_name);
                        SpannableString span = new SpannableString(content);
                        span.setSpan(new NormalClickableSpan(false, Theme.colorAccount, new NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""), view -> {

                        }), index, index + userInfo.join_group_invite_name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        bageVBinding.joinGroupWayTv.setText(span);
                    }
                }
            } else {
                showToast(msg);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BageIM.getInstance().getChannelManager().removeRefreshChannelInfo("user_detail_refresh_channel");
        BageIM.getInstance().getChannelManager().removeRefreshChannelInfo("user_detail_refresh_channel1");
    }


    private void showImg() {
        String uri = BageApiConfig.getAvatarUrl(uid) + "?key=" + BageTimeUtils.getInstance().getCurrentMills();
        //查看大图
        List<Object> tempImgList = new ArrayList<>();
        List<ImageView> imageViewList = new ArrayList<>();
        imageViewList.add(bageVBinding.avatarView.imageView);
        tempImgList.add(BageApiConfig.getShowUrl(uri));
        int index = 0;
        BageDialogUtils.getInstance().showImagePopup(this, tempImgList, imageViewList, bageVBinding.avatarView.imageView, index, new ArrayList<>(), null, null);
        BageIM.getInstance().getChannelManager().updateAvatarCacheKey(uid, BageChannelType.PERSONAL, UUID.randomUUID().toString().replaceAll("-", ""));
    }

    ActivityResultLauncher<Intent> chooseResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            getUserInfo();
        }
    });

    @SuppressLint("ClickableViewAccessibility")
    private void setonLongClick(View view, TextView textView) {
        final float[][] location = {new float[2]};
        view.setOnTouchListener((var view12, var motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                location[0] = new float[]{motionEvent.getRawX(), motionEvent.getRawY()};
            }
            return false;
        });
        view.setOnLongClickListener(view1 -> {
            showCopy(textView, location[0], textView.getText().toString());
            return true;
        });
    }
}
