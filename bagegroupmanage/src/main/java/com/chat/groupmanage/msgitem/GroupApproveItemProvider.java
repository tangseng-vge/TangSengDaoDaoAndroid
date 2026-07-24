package com.chat.groupmanage.msgitem;

import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.act.BageWebViewActivity;
import com.chat.base.msgitem.BageChatBaseProvider;
import com.chat.base.msgitem.BageChatIteMsgFromType;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.msgitem.BageUIChatMsgItemEntity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.components.NormalClickableContent;
import com.chat.base.ui.components.NormalClickableSpan;
import com.chat.base.ui.components.SystemMsgBackgroundColorSpan;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageToastUtils;
import com.chat.groupmanage.R;
import com.chat.groupmanage.service.GroupManageModel;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-08-07 09:52
 * 进群审批消息
 */
public class GroupApproveItemProvider extends BageChatBaseProvider {

    @Override
    public int getLayoutId() {
        return R.layout.chat_system_layout;
    }

    @Override
    protected View getChatViewItem(@NonNull ViewGroup parentView, @NonNull BageChatIteMsgFromType from) {
        return null;
    }

    @Override
    public void convert(@NotNull BaseViewHolder baseViewHolder, @NonNull BageUIChatMsgItemEntity chatMsgItemEntity) {
        super.convert(baseViewHolder, chatMsgItemEntity);
        TextView textView = baseViewHolder.getView(R.id.contentTv);
        String content = getShowContent(chatMsgItemEntity.bageMsg.content);
        SpannableStringBuilder displaySpans = new SpannableStringBuilder();
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setShadowLayer(AndroidUtilities.dp(5f), 0f, 0f, 0);
        String str = context.getString(R.string.group_approve);
        displaySpans.append(content).append(str);
        NormalClickableSpan span = new NormalClickableSpan(false, ContextCompat.getColor(context, R.color.blue), new NormalClickableContent(NormalClickableContent.NormalClickableTypes.Other, ""), view -> {
            try {
                JSONObject jsonObject = new JSONObject(chatMsgItemEntity.bageMsg.content);
                String invite_no = jsonObject.optString("invite_no");
                GroupManageModel.getInstance().getH5confirmUrl(chatMsgItemEntity.bageMsg.channelID, invite_no, (code, msg) -> {
                    if (code == HttpResponseCode.success && !TextUtils.isEmpty(msg)) {
                        Intent intent = new Intent(getContext(), BageWebViewActivity.class);
                        intent.putExtra("url", msg);
                        getContext().startActivity(intent);
                    } else BageToastUtils.getInstance().showToastNormal(msg);
                });
            } catch (JSONException e) {
                BageLogUtils.e("解析群邀请数据错误");
            }

        });
        assert content != null;
        displaySpans.setSpan(
                new SystemMsgBackgroundColorSpan(
                        ContextCompat.getColor(
                                context,
                                R.color.colorSystemBg
                        ), AndroidUtilities.dp(5f), AndroidUtilities.dp((2 * 5))
                ), 0, content.length() + str.length(), 0
        );
        displaySpans.setSpan(new StyleSpan(Typeface.BOLD), content.length(), (content.length() + str.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        displaySpans.setSpan(span, content.length(), content.length() + str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        displaySpans.setSpan(new AbsoluteSizeSpan(17, true),
                content.length(), (content.length() + str.length()),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(displaySpans);
    }

    @Override
    protected void setData(int adapterPosition, @NonNull View parentView, @NonNull BageUIChatMsgItemEntity uiChatMsgItemEntity, @NonNull BageChatIteMsgFromType from) {

    }

    @Override
    public int getItemViewType() {
        return BageContentType.approveGroupMember;
    }

}
