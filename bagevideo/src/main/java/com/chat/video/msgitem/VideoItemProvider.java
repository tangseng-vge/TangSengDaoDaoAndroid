package com.chat.video.msgitem;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import com.chat.base.config.BageApiConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.glide.GlideUtils;
import com.chat.base.msgitem.BageChatBaseProvider;
import com.chat.base.msgitem.BageChatIteMsgFromType;
import com.chat.base.msgitem.BageMsgBgType;
import com.chat.base.msgitem.BageUIChatMsgItemEntity;
import com.chat.base.net.ud.BageProgressManager;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.FilterImageView;
import com.chat.base.ui.components.SecretDeleteTimer;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.views.CircularProgressView;
import com.chat.base.views.blurview.ShapeBlurView;
import com.chat.base.act.PlayVideoActivity;
import com.chat.video.R;
import com.bage.im.message.type.BageMsgContentType;
import com.bage.im.msgmodel.BageVideoContent;

import java.io.File;
import java.util.Objects;

/**
 * 2020-08-06 14:27
 * 视频消息
 */
public class VideoItemProvider extends BageChatBaseProvider {
    @NonNull
    @Override
    protected View getChatViewItem(@NonNull ViewGroup parentView, @NonNull BageChatIteMsgFromType from) {
        return LayoutInflater.from(getContext()).inflate(R.layout.chat_item_video, parentView, false);
    }

    @Override
    protected void setData(int adapterPosition, View parentView, @NonNull BageUIChatMsgItemEntity uiChatMsgItemEntity, @NonNull BageChatIteMsgFromType from) {
        LinearLayout contentLayout = parentView.findViewById(R.id.contentLayout);
        CircularProgressView progressView = parentView.findViewById(R.id.progressView);
        progressView.setProgColor(Theme.colorAccount);
        TextView durationTv = parentView.findViewById(R.id.durationTv);
        FilterImageView coverIv = parentView.findViewById(R.id.imageView);
        FrameLayout otherLayout = parentView.findViewById(R.id.otherLayout);
        ShapeBlurView blurView = parentView.findViewById(R.id.blurView);
        SecretDeleteTimer deleteTimer = new SecretDeleteTimer(context);
        otherLayout.removeAllViews();
        otherLayout.addView(deleteTimer, LayoutHelper.createFrame(35, 35, Gravity.CENTER));

        if (from == BageChatIteMsgFromType.RECEIVED) {
            contentLayout.setGravity(Gravity.START);
        } else {
            contentLayout.setGravity(Gravity.END);
        }

        View videoLayout = parentView.findViewById(R.id.videoLayout);
        if (uiChatMsgItemEntity.bageMsg != null) {
            BageVideoContent videoMsgModel = (BageVideoContent) uiChatMsgItemEntity.bageMsg.baseContentMsgModel;
            if (videoMsgModel == null) return;
            if (videoMsgModel.second > 0) {
                //分
                int minute = (int) (videoMsgModel.second / (60));
                //秒
                int second = (int) (videoMsgModel.second % 60);
                String showMinute = minute < 10 ? ("0" + minute) : minute + "";
                String showSecond = second < 10 ? ("0" + second) : second + "";
                durationTv.setText(String.format("%s:%s", showMinute, showSecond));

//                double size = (videoMsgModel.size / 1024) / 1024;
            } else {
                durationTv.setText("");
            }
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) coverIv.getLayoutParams();
            int[] ints = ImageUtils.getInstance().getImageWidthAndHeightToTalk(videoMsgModel.width, videoMsgModel.height);
            LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) videoLayout.getLayoutParams();
            FrameLayout.LayoutParams blurViewLayoutParams = (FrameLayout.LayoutParams) blurView.getLayoutParams();
            if (uiChatMsgItemEntity.bageMsg.flame == 1) {
                layoutParams.height = AndroidUtilities.dp(150f);
                layoutParams.width = AndroidUtilities.dp(150f);
                layoutParams1.height = AndroidUtilities.dp(150f);
                layoutParams1.width = AndroidUtilities.dp(150f);
                blurViewLayoutParams.height = AndroidUtilities.dp(150f);
                blurViewLayoutParams.width = AndroidUtilities.dp(150f);
                blurView.setVisibility(View.VISIBLE);
                progressView.setVisibility(View.GONE);
                otherLayout.setVisibility(View.VISIBLE);
                deleteTimer.setSize(35);
                if (uiChatMsgItemEntity.bageMsg.viewedAt > 0) {
                    deleteTimer.setDestroyTime(
                            uiChatMsgItemEntity.bageMsg.clientMsgNO,
                            uiChatMsgItemEntity.bageMsg.flameSecond,
                            uiChatMsgItemEntity.bageMsg.viewedAt,
                            false
                    );
                }
            } else {
                progressView.setVisibility(View.VISIBLE);
                otherLayout.setVisibility(View.GONE);
                layoutParams.height = ints[1];
                layoutParams.width = ints[0];
                layoutParams1.height = ints[1];
                layoutParams1.width = ints[0];
                blurView.setVisibility(View.GONE);
            }

            coverIv.setLayoutParams(layoutParams);
//            coverIv.setAllCorners(10);
            blurView.setLayoutParams(blurViewLayoutParams);
//            if (uiChatMsgItemEntity.bageMsg.channelType != BageChannelType.PERSONAL && from != BageChatIteMsgFromType.SEND) {
//                layoutParams1.leftMargin = AndroidUtilities.dp(10f);
//                layoutParams1.rightMargin = AndroidUtilities.dp(10f);
//            }
            videoLayout.setLayoutParams(layoutParams1);

            String showUrl = getCoverURL(videoMsgModel);
            GlideUtils.getInstance().showImg(context, showUrl, coverIv);
            String videoUrl;
            if (!TextUtils.isEmpty(videoMsgModel.localPath)) {
                File file = new File(videoMsgModel.localPath);
                if (!file.exists()) {
                    videoUrl = BageApiConfig.getShowUrl(videoMsgModel.url);
                } else videoUrl = videoMsgModel.localPath;
            } else
                videoUrl = BageApiConfig.getShowUrl(videoMsgModel.url);

            String finalVideoUrl = videoUrl;
            final String coverImg = showUrl;
            coverIv.setOnClickListener(view -> {
                //网络视频先下载在播放
                if (finalVideoUrl.startsWith("http") || finalVideoUrl.startsWith("HTTP")) {
                    @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) Objects.requireNonNull(getContext()), new Pair<>(coverIv, "coverIv"));

                    Intent intent = new Intent(context, PlayVideoActivity.class);
                    intent.putExtra("url", finalVideoUrl);
                    intent.putExtra("coverImg", coverImg);
                    intent.putExtra("clientMsgNo", uiChatMsgItemEntity.bageMsg.clientMsgNO);
                    Activity activity = (Activity) context;
                    context.startActivity(intent, activityOptions.toBundle());
                    activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                } else {
                    @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) Objects.requireNonNull(getContext()), new Pair<>(coverIv, "coverIv"));
                    Intent intent = new Intent(context, PlayVideoActivity.class);
                    intent.putExtra("url", finalVideoUrl);
                    intent.putExtra("coverImg", coverImg);
                    intent.putExtra("clientMsgNo", uiChatMsgItemEntity.bageMsg.clientMsgNO);
//                    Activity activity = (Activity) context;
//                    CyTransition.startActivity(intent, activity, coverIv);
                    context.startActivity(intent, activityOptions.toBundle());

                    //  activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                }
            });
            BageProgressManager.Companion.getInstance().registerProgress(uiChatMsgItemEntity.bageMsg.clientSeq, new BageProgressManager.IProgress() {
                @Override
                public void onProgress(@Nullable Object tag, int progress) {
                    if (tag instanceof Long) {
                        long index = (long) tag;
                        if (index == uiChatMsgItemEntity.bageMsg.clientSeq) {
                            if (progress >= 100) {
                                progressView.setProgress(0);
                                if (uiChatMsgItemEntity.bageMsg.flame == 1) {
                                    progressView.setVisibility(View.GONE);
                                }
                            } else progressView.setProgress(progress);
                        }
                    }

                }

                @Override
                public void onSuccess(@Nullable Object tag, @Nullable String path) {
                    if (uiChatMsgItemEntity.bageMsg.flame == 1) {
                        progressView.setVisibility(View.GONE);
                    }
                    if (tag != null) {
                        BageProgressManager.Companion.getInstance().unregisterProgress(tag);
                    }
                }

                @Override
                public void onFail(@Nullable Object tag, @Nullable String msg) {

                }
            });
        }

        assert uiChatMsgItemEntity.bageMsg != null;
        addLongClick(coverIv, uiChatMsgItemEntity);
        EndpointManager.getInstance().setMethod("video_viewed", object -> {
            String clientMsgNo = (String) object;
            if (uiChatMsgItemEntity.bageMsg.clientMsgNO.equals(clientMsgNo)) {
                uiChatMsgItemEntity.bageMsg.viewed = 1;
                uiChatMsgItemEntity.bageMsg.viewedAt = BageTimeUtils.getInstance().getCurrentMills();
                deleteTimer.setDestroyTime(
                        uiChatMsgItemEntity.bageMsg.clientMsgNO,
                        uiChatMsgItemEntity.bageMsg.flameSecond,
                        uiChatMsgItemEntity.bageMsg.viewedAt,
                        false
                );
                deleteTimer.invalidate();
            }
            return null;
        });
    }


    @Override
    public int getItemViewType() {
        return BageMsgContentType.Bage_VIDEO;
    }

    @Override
    public void resetCellListener(int position, @NonNull View parentView, @NonNull BageUIChatMsgItemEntity uiChatMsgItemEntity, @NonNull BageChatIteMsgFromType from) {
        super.resetCellListener(position, parentView, uiChatMsgItemEntity, from);
        FilterImageView coverIv = parentView.findViewById(R.id.imageView);
        addLongClick(coverIv, uiChatMsgItemEntity);
    }

    @Override
    public void resetCellBackground(@NonNull View parentView, @NonNull BageUIChatMsgItemEntity uiChatMsgItemEntity, @NonNull BageChatIteMsgFromType from) {
        super.resetCellBackground(parentView, uiChatMsgItemEntity, from);
        FilterImageView coverIv = parentView.findViewById(R.id.imageView);
        ShapeBlurView blurView = parentView.findViewById(R.id.blurView);
        setCorners(from, uiChatMsgItemEntity, coverIv, blurView);
    }

    public void setCorners(BageChatIteMsgFromType from, BageUIChatMsgItemEntity uiChatMsgItemEntity, FilterImageView coverIv, ShapeBlurView blurView) {
        BageMsgBgType bgType = getMsgBgType(
                uiChatMsgItemEntity.previousMsg,
                uiChatMsgItemEntity.bageMsg,
                uiChatMsgItemEntity.nextMsg
        );
        if (bgType == BageMsgBgType.center) {
            if (from == BageChatIteMsgFromType.SEND) {
                coverIv.setCorners(10, 5, 10, 5);
                blurView.setCornerRadius(
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(5f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(5f)
                );
            } else {
                coverIv.setCorners(5, 10, 5, 10);
                blurView.setCornerRadius(
                        AndroidUtilities.dp(5f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(5f),
                        AndroidUtilities.dp(10f)
                );
            }
        } else if (bgType == BageMsgBgType.top) {
            if (from == BageChatIteMsgFromType.SEND) {
                coverIv.setCorners(10, 10, 10, 5);
                blurView.setCornerRadius(
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(5f)
                );
            } else {
                coverIv.setCorners(10, 10, 5, 10);
                blurView.setCornerRadius(
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(5f),
                        AndroidUtilities.dp(10f)
                );
            }
        } else if (bgType == BageMsgBgType.bottom) {
            if (from == BageChatIteMsgFromType.SEND) {
                coverIv.setCorners(10, 5, 10, 10);
                blurView.setCornerRadius(
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(5f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(10f)
                );
            } else {
                coverIv.setCorners(5, 10, 10, 10);
                blurView.setCornerRadius(
                        AndroidUtilities.dp(5f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(10f),
                        AndroidUtilities.dp(10f)
                );
            }
        } else {
            coverIv.setAllCorners(10);
            blurView.setCornerRadius(
                    AndroidUtilities.dp(10f),
                    AndroidUtilities.dp(10f),
                    AndroidUtilities.dp(10f),
                    AndroidUtilities.dp(10f)
            );
        }
    }

    private String getCoverURL(BageVideoContent videoMsgModel) {
        if (!TextUtils.isEmpty(videoMsgModel.coverLocalPath)) {
            File file = new File(videoMsgModel.coverLocalPath);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        if (!TextUtils.isEmpty(videoMsgModel.cover)) {
            return BageApiConfig.getShowUrl(videoMsgModel.cover);
        }
        return "";
    }
}
