package com.chat.base.msgitem;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.chat.base.R;
import com.chat.base.act.BageWebViewActivity;
import com.chat.base.emoji.EmojiManager;
import com.chat.base.emoji.MoonUtil;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.msg.ChatContentSpanType;
import com.chat.base.msg.IConversationContext;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AlignImageSpan;
import com.chat.base.ui.components.NormalClickableContent;
import com.chat.base.ui.components.NormalClickableSpan;
import com.chat.base.utils.StringUtils;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageToastUtils;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMsg;
import com.bage.im.msgmodel.BageMsgEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 2020-08-05 18:00
 * 消息列表item
 */
public class BageUIChatMsgItemEntity {
    public BageMsg bageMsg; // 本条消息对象
    public boolean showNickName = true; // 是否显示消息昵称
    public boolean isPlaying; // 语音是否在播放
    public boolean isChoose; // 是否选择消息
    public boolean isChecked; // 是否选中消息
    public boolean isShowTips; // 是否显示背景提示
    public BageMsg previousMsg; // 上一条消息
    public BageMsg nextMsg; // 下一条消息
    public boolean isUpdateStatus;
    public boolean isRefreshReaction;
    public boolean isRefreshAvatarAndName;
    public boolean isShowPinnedMessage;
    public int isPinned = 0;
    //=========本地数据========
    public ILinkClick iLinkClick;
    public SpannableStringBuilder displaySpans;

    public BageUIChatMsgItemEntity(IConversationContext conversationContext, BageMsg bageMsg, ILinkClick iLinkClick) {
        this.bageMsg = bageMsg;
        this.iLinkClick = iLinkClick;
        if (bageMsg != null) {
            try {
                formatSpans(conversationContext, bageMsg);
            } catch (Exception ignored) {
            }
        }

    }

    private String getContent() {
        String showContent = bageMsg.baseContentMsgModel.getDisplayContent();
        if (bageMsg.remoteExtra.contentEditMsgModel != null && !TextUtils.isEmpty(bageMsg.remoteExtra.contentEditMsgModel.getDisplayContent())) {
            showContent = bageMsg.remoteExtra.contentEditMsgModel.getDisplayContent();
        }
        return showContent;
    }

    public void formatSpans(IConversationContext conversationContext, BageMsg bageMsg) {
        if (bageMsg.type != BageContentType.Bage_TEXT
                || bageMsg.baseContentMsgModel == null) {
            return;
        }

        displaySpans = new SpannableStringBuilder();
        displaySpans.append(getContent());
        Activity context = conversationContext.getChatActivity();
        if (BageReader.isNotEmpty(bageMsg.baseContentMsgModel.entities)) {
            for (BageMsgEntity entity : bageMsg.baseContentMsgModel.entities) {
                if ((entity.offset + entity.length) > displaySpans.length() || entity.offset > displaySpans.length())
                    continue;

                if (entity.type.equals(ChatContentSpanType.getLink())) {
                    String content = getContent().substring(entity.offset, (entity.offset + entity.length));
                    NormalClickableContent.NormalClickableTypes types;
                    if (StringUtils.isMobile(content) || StringUtils.isEmail(content)) {
                        types = NormalClickableContent.NormalClickableTypes.Other;
                    } else types = NormalClickableContent.NormalClickableTypes.URL;

                    NormalClickableSpan clickableSpan = new NormalClickableSpan(true, ContextCompat.getColor(context, R.color.blue), new NormalClickableContent(types, content), view -> {

                        if (StringUtils.isMobile(content)) {
                            conversationContext.hideSoftKeyboard();
                            List<BottomSheetItem> list = new ArrayList<>();
                            list.add(new
                                            BottomSheetItem(
                                            context.getString(R.string.copy),
                                            R.mipmap.msg_copy, () -> {
                                        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData mClipData = ClipData.newPlainText("Label", content);
                                        assert cm != null;
                                        cm.setPrimaryClip(mClipData);
                                        BageToastUtils.getInstance().showToastNormal(context.getString(R.string.copyed));
                                    })
                            );
                            list.add(new BottomSheetItem(context.getString(R.string.call), R.mipmap.msg_calls, () -> {
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + content));
                                context.startActivity(intent);
                            }));
                            list.add(new BottomSheetItem(context.getString(R.string.add_to_phone_book), R.mipmap.msg_contacts, () -> {
                                Intent addIntent = new Intent(Intent.ACTION_INSERT, Uri.withAppendedPath(Uri.parse("content://com.android.contacts"), "contacts"));
                                addIntent.setType("vnd.android.cursor.dir/person");
                                addIntent.setType("vnd.android.cursor.dir/contact");
                                addIntent.setType("vnd.android.cursor.dir/raw_contact");
                                addIntent.putExtra(ContactsContract.Intents.Insert.NAME, "");
                                addIntent.putExtra(ContactsContract.Intents.Insert.PHONE, content);
                                context.startActivity(addIntent);
                            }));
                            list.add(new BottomSheetItem(context.getString(R.string.str_search), R.mipmap.ic_ab_search, () -> {
                                if (iLinkClick != null)
                                    iLinkClick.onShowSearchUser(content);
                            }));

                            SpannableStringBuilder displaySpans = new SpannableStringBuilder();
                            displaySpans.append(content);
                            displaySpans.setSpan(new
                                            StyleSpan(Typeface.BOLD), 0,
                                    content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            displaySpans.setSpan(new
                                            ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)), 0,
                                    content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            BageDialogUtils.getInstance().showBottomSheet(context, displaySpans, false, list);
                            return;
                        }
                        if (StringUtils.isEmail(content)) {
                            conversationContext.hideSoftKeyboard();
                            List<BottomSheetItem> list = new ArrayList<>();
                            list.add(new BottomSheetItem(context.getString(R.string.copy), R.mipmap.msg_copy, () -> {

                                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData mClipData = ClipData.newPlainText("Label", content);
                                assert cm != null;
                                cm.setPrimaryClip(mClipData);
                                BageToastUtils.getInstance().showToastNormal(context.getString(R.string.copyed));

                            }));
                            list.add(new BottomSheetItem(context.getString(R.string.send_email), R.mipmap.msg2_email, () -> {

                                Uri uri = Uri.parse("mailto:" + content);
                                String[] email = {content};
                                Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
                                intent.putExtra(Intent.EXTRA_CC, email); // 抄送人
                                intent.putExtra(Intent.EXTRA_SUBJECT, ""); // 主题
                                intent.putExtra(Intent.EXTRA_TEXT, ""); // 正文
                                context.startActivity(Intent.createChooser(intent, ""));

                            }));
                            list.add(new BottomSheetItem(context.getString(R.string.str_search), R.mipmap.ic_ab_search, () -> {
                                if (iLinkClick != null)
                                    iLinkClick.onShowSearchUser(content);
                            }));
                            SpannableStringBuilder displaySpans = new SpannableStringBuilder();
                            displaySpans.append(content);
                            displaySpans.setSpan(new
                                            StyleSpan(Typeface.BOLD), 0,
                                    content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            displaySpans.setSpan(new
                                            ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)), 0,
                                    content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            BageDialogUtils.getInstance().showBottomSheet(context, displaySpans, false, list);
                            return;
                        }
                        Intent intent = new Intent(conversationContext.getChatActivity(), BageWebViewActivity.class);
                        intent.putExtra("url", content);
                        conversationContext.getChatActivity().startActivity(intent);
                    });
                    displaySpans.setSpan(new StyleSpan(Typeface.BOLD), entity.offset, (entity.offset + entity.length), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    displaySpans.setSpan(clickableSpan, entity.offset, (entity.offset + entity.length), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (entity.type.equals(ChatContentSpanType.getBotCommand())) {
                    displaySpans.setSpan(new UnderlineSpan(), entity.offset, (entity.offset + entity.length), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } else {
            if (bageMsg.baseContentMsgModel.mentionInfo != null) {
                for (String uid : bageMsg.baseContentMsgModel.mentionInfo.uids) {

                    String showName = "";
                    BageChannelMember member = BageIM.getInstance().getChannelMembersManager().getMember(conversationContext.getChatChannelInfo().channelID, conversationContext.getChatChannelInfo().channelType, uid);
                    if (member != null) {
                        showName = member.remark;
                        if (TextUtils.isEmpty(showName))
                            showName = TextUtils.isEmpty(member.memberRemark) ? member.memberName : member.memberRemark;
                    }
                    if (!TextUtils.isEmpty(showName)) {
                        showName = "@" + showName;
                        int index = getContent().indexOf(showName);
                        if (index >= 0) {
                            String groupNo = "";
                            if (bageMsg.channelType == BageChannelType.GROUP) {
                                groupNo = bageMsg.channelID;
                            }
                            showName = showName + " ";
                            SpannableStringBuilder nameSpan = new SpannableStringBuilder();
                            nameSpan.append(showName);
                            nameSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, showName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            String finalGroupNo = groupNo;
                            String content = uid;
                            if (!TextUtils.isEmpty(groupNo)) content = content + "|" + groupNo;
                            nameSpan.setSpan(new NormalClickableSpan(false, Theme.colorAccount, new NormalClickableContent(NormalClickableContent.NormalClickableTypes.Remind, content), view -> {
                                if (iLinkClick != null) {
                                    iLinkClick.onShowUserDetail(uid, finalGroupNo);
                                }
                            }), 0, showName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            displaySpans.replace(index, (index + showName.length()), nameSpan);
                        }
                    }
                }
            }
            // 默认数据
            String content = getContent();
            List<String> urls = StringUtils.getStrUrls(content);
            for (String url : urls) {
                int fromIndex = 0;
                while (fromIndex >= 0) {
                    fromIndex = content.indexOf(url, fromIndex);
                    if (fromIndex >= 0) {
                        NormalClickableSpan span = new NormalClickableSpan(false, ContextCompat.getColor(context, R.color.blue), new NormalClickableContent(NormalClickableContent.NormalClickableTypes.URL, url), view -> {
                            Intent intent = new Intent(conversationContext.getChatActivity(), BageWebViewActivity.class);
                            intent.putExtra("url", url);
                            conversationContext.getChatActivity().startActivity(intent);
                        });
                        displaySpans.setSpan(new StyleSpan(Typeface.BOLD), fromIndex, (fromIndex + url.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        displaySpans.setSpan(span, fromIndex, (fromIndex + url.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        fromIndex += url.length();
                    }
                }
            }
            if (bageMsg.baseContentMsgModel.mentionAll == 1) {
                String mentionAll = "@All";
                String mentionAll1 = "@所有人";
                int index = getContent().indexOf(mentionAll);
                if (index >= 0) {
                    displaySpans.setSpan(new ForegroundColorSpan(Theme.colorAccount), index, (index + mentionAll.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    displaySpans.setSpan(new StyleSpan(Typeface.BOLD), index, (index + mentionAll.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                int index1 = getContent().indexOf(mentionAll1);
                if (index1 >= 0) {
                    displaySpans.setSpan(new ForegroundColorSpan(Theme.colorAccount), index1, (index1 + mentionAll.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    displaySpans.setSpan(new StyleSpan(Typeface.BOLD), index1, (index1 + mentionAll.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        // emoji
        Matcher matcher = EmojiManager.getInstance().getPattern().matcher(getContent());
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String emoji = getContent().substring(start, end);
            Drawable d = MoonUtil.getEmotDrawable(context, emoji, MoonUtil.DEF_SCALE);
            if (d != null) {
                AlignImageSpan span = new AlignImageSpan(d, AlignImageSpan.ALIGN_CENTER) {
                    @Override
                    public void onClick(View view) {

                    }
                };
                displaySpans.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // 单独处理@效果
        if (BageReader.isNotEmpty(bageMsg.baseContentMsgModel.entities)) {
            int allOffset = 0;
            for (BageMsgEntity entity : bageMsg.baseContentMsgModel.entities) {
                if (entity.type.equals(ChatContentSpanType.getMention())) {
                    String uid = entity.value;
                    String showName = "";

                    int start = entity.offset + allOffset;
                    int end = entity.offset + entity.length + allOffset;

                    if (uid.equals("-1")) {
                        showName = displaySpans.subSequence(start, end).toString();
                    }
                    BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
                    if (channel != null) {
                        showName = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
                    }
                    boolean isUserDetail = !TextUtils.isEmpty(uid) && !uid.equals("-1");
                    String displayContent = getContent();
                    if (entity.offset > displayContent.length() || (entity.offset + entity.length) > displayContent.length()) {
                        continue;
                    }
                    String oldName = displayContent.substring(entity.offset, (entity.offset + entity.length));
                    if (!TextUtils.isEmpty(showName)) {
                        if (!showName.startsWith("@"))
                            showName = "@" + showName;
                    } else {
                        showName = displaySpans.subSequence(start, end).toString();
                    }
                    showName = showName + " ";
                    SpannableStringBuilder nameSpan = new SpannableStringBuilder();
                    nameSpan.append(showName);
                    nameSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, showName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (isUserDetail && iLinkClick != null) {
                        String groupNo = "";
                        if (bageMsg.channelType == BageChannelType.GROUP) {
                            groupNo = bageMsg.channelID;
                        }
                        String content = entity.value;
                        if (!TextUtils.isEmpty(groupNo)) content = content + "|" + groupNo;
                        String finalGroupNo = groupNo;
                        nameSpan.setSpan(new NormalClickableSpan(false, Theme.colorAccount, new NormalClickableContent(NormalClickableContent.NormalClickableTypes.Remind, content), view -> iLinkClick.onShowUserDetail(entity.value, finalGroupNo)), 0, showName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        nameSpan.setSpan(new ForegroundColorSpan(Theme.colorAccount), 0, showName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    displaySpans.replace(start, end, nameSpan);

                    if (!TextUtils.isEmpty(oldName)) {
                        int diff = showName.length() - oldName.length();
                        allOffset += diff;
                    }
                }
            }
        }

    }

    public interface ILinkClick {
        void onShowUserDetail(String uid, String groupNo);

        void onShowSearchUser(String phone);
    }
}
