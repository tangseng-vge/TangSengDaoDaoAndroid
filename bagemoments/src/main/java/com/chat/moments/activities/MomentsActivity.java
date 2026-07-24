package com.chat.moments.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.BageBaseApplication;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.glide.ChooseMimeType;
import com.chat.base.glide.ChooseResult;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.ud.BageDownloader;
import com.chat.base.net.ud.BageProgressManager;
import com.chat.base.net.ud.BageUploader;
import com.chat.base.ui.Theme;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.ui.components.FilterImageView;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageFileUtils;
import com.chat.base.utils.BagePermissions;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.base.utils.systembar.BageStatusBarUtils;
import com.chat.moments.R;
import com.chat.moments.BageMomentsApplication;
import com.chat.moments.adapter.MomentsAdapter;
import com.chat.moments.databinding.ActMomentsLayoutBinding;
import com.chat.moments.entity.MomentSetting;
import com.chat.moments.entity.Moments;
import com.chat.moments.entity.MomentsPraise;
import com.chat.moments.entity.MomentsReply;
import com.chat.moments.entity.MomentsType;
import com.chat.moments.service.MomentFileUpload;
import com.chat.moments.service.MomentsContact;
import com.chat.moments.service.MomentsModel;
import com.chat.moments.service.MomentsPresenter;
import com.chat.moments.utils.MomentSpanUtils;
import com.chat.moments.utils.MomentModerationHelper;
import com.chat.moments.views.FeedActionPopup;
import com.chat.moments.views.FeedCommentDialog;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2020-11-05 10:43
 * 朋友圈列表
 */
public class MomentsActivity extends BageBaseActivity<ActMomentsLayoutBinding> implements MomentsContact.MomentsView {

    RecyclerView recyclerView;
    private View newMomentsLayout;
    private AvatarView newMomentsAvatarIv;//新消息头像
    private TextView newMomentsCountTv; //新消息数量
    private MomentsAdapter adapter;
    private MomentsPresenter presenter;
    private int page = 1;
    //被回复的动态ID
    private String replyMomentNo;
    private MomentsReply replyMomentsReply;
    private String uid;//查看某人的朋友圈
    /** 仅 Discover/通讯录主入口（无 uid）时展示滚动折叠标题 */
    private boolean showTitleCenterTv;

    private static final float HEADER_COVER_HEIGHT_SCREEN_RATIO = 0.4f;
    private static final int HEADER_PROFILE_TOP_OVERLAP_DP = 50;
    private int headerCoverHeightPx;
    private int titleOverlayHeightPx;
    private int collapseStartPx;
    private int collapseRangePx;
    private int coverBackIconColor = Color.WHITE;

    @Override
    protected ActMomentsLayoutBinding getViewBinding() {
        return ActMomentsLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        if (getIntent().hasExtra("uid"))
            uid = getIntent().getStringExtra("uid");
        showTitleCenterTv = TextUtils.isEmpty(uid);
        presenter = new MomentsPresenter(this);

    }

    @Override
    protected void backListener(int type) {
        super.backListener(type);
        finish();
    }

    @Override
    protected void initView() {
        BageSharedPreferencesUtil.getInstance().putSP(BageConfig.getInstance().getUid() + "_moments_msg_uid", "");
        EndpointManager.getInstance().invokes(EndpointCategory.bageRefreshMailList, null);
//        bageVBinding.emojiPanelView.initEmojiPanel(this);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.wrapview.setTitleViews(
                bageVBinding.titleCenterTv,
                bageVBinding.backIv,
                bageVBinding.cameraIv,
                bageVBinding.msgIv,
                bageVBinding.titleView);
        bageVBinding.wrapview.setUid(uid);
        setupTitleOverlay();
        setupTitleBarActions();
        bageVBinding.titleView.bringToFront();
        adapter = new MomentsAdapter(false, new ArrayList<>());
        recyclerView = bageVBinding.wrapview.getmContentView();
        ((DefaultItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        initAdapter(recyclerView, adapter);
        recyclerView.setLayoutManager(mLinearLayoutManager);
        recyclerView.setNestedScrollingEnabled(true);
        View headerView = bageVBinding.wrapview.getmHeadViw();
        applyHeaderCoverSize(headerView);
        adapter.addHeaderView(headerView);
        newMomentsLayout = headerView.findViewById(R.id.newMomentsLayout);
        FilterImageView avatarIv = headerView.findViewById(R.id.avatarIv);
        avatarIv.setAllCorners(8);
        TextView nameTv = headerView.findViewById(R.id.nameTv);
        newMomentsAvatarIv = headerView.findViewById(R.id.avatarView);
        newMomentsAvatarIv.setSize(25);
        newMomentsAvatarIv.setStrokeWidth(1);
        newMomentsCountTv = headerView.findViewById(R.id.newMomentsCountTv);
        if (!TextUtils.isEmpty(uid) && !uid.equals(BageConfig.getInstance().getUid())) {
            BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
            if (channel != null) {
                nameTv.setText(TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark);
                GlideUtils.getInstance().showAvatarImg(this, uid, BageChannelType.PERSONAL, channel.avatarCacheKey, avatarIv);
            } else {
                GlideUtils.getInstance().showImg(this, BageApiConfig.getShowAvatar(uid, BageChannelType.PERSONAL), avatarIv);
            }
        } else {
            BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
            if (channel != null) {
                GlideUtils.getInstance().showAvatarImg(this, BageConfig.getInstance().getUid(), BageChannelType.PERSONAL, channel.avatarCacheKey, avatarIv);
            } else {
                GlideUtils.getInstance().showImg(this, BageApiConfig.getShowAvatar(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL), avatarIv);
            }
            nameTv.setText(BageConfig.getInstance().getUserName());
            if (TextUtils.isEmpty(uid)) {
                SingleClickUtil.onSingleClick(avatarIv, view1 -> {
                    Intent intent = new Intent(this, MomentsActivity.class);
                    intent.putExtra("uid", BageConfig.getInstance().getUid());
                    startActivity(intent);
                });
            }
        }
        getUserMomentBg();
        getMomentMsg();
        bageVBinding.wrapview.startRefresh();
        if (TextUtils.isEmpty(uid)) {
            presenter.list(page);
        } else {
            presenter.listByUid(page, uid);
        }
    }

    private void setupTitleBarActions() {
        boolean isOtherUser = !TextUtils.isEmpty(uid)
                && !TextUtils.equals(uid, BageConfig.getInstance().getUid());
        boolean isOwnMoments = TextUtils.isEmpty(uid)
                || TextUtils.equals(uid, BageConfig.getInstance().getUid());
        bageVBinding.wrapview.setTitleActionVisibility(isOwnMoments, isOtherUser);
    }

    private void openChatWithUser() {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        EndpointManager.getInstance().invoke(
                EndpointSID.chatView,
                new ChatViewMenu(this, uid, BageChannelType.PERSONAL, 0, true));
    }

    LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this) {
        @Override
        public boolean canScrollVertically() {
            return !bageVBinding.wrapview.isRefreshing();
        }
    };

    private void setupTitleOverlay() {
        int statusBar = BageStatusBarUtils.getStatusBarHeight(this);
        bageVBinding.titleView.setBackgroundColor(Color.TRANSPARENT);
//        bageVBinding.titleView.setPadding(
//                bageVBinding.titleView.getPaddingLeft(),
//                statusBar + AndroidUtilities.dp(8),
//                bageVBinding.titleView.getPaddingRight(),
//                AndroidUtilities.dp(10));
        bageVBinding.titleCenterTv.setVisibility(View.GONE);
        bageVBinding.titleCenterTv.setAlpha(0f);
        bageVBinding.titleView.post(this::measureTitleCollapseThresholds);
    }

    private void measureTitleCollapseThresholds() {
        titleOverlayHeightPx = bageVBinding.titleView.getHeight();
        if (titleOverlayHeightPx <= 0) {
            titleOverlayHeightPx = BageStatusBarUtils.getStatusBarHeight(this) + AndroidUtilities.dp(52);
        }
        int coverHeightPx = getHeaderCoverHeightPx();
        collapseStartPx = Math.max(AndroidUtilities.dp(16),
                coverHeightPx - titleOverlayHeightPx + AndroidUtilities.dp(8));
        collapseRangePx = AndroidUtilities.dp(72);
    }

    private int getHeaderCoverHeightPx() {
        if (headerCoverHeightPx <= 0) {
            int screenHeight = AndroidUtilities.getScreenHeight();
            headerCoverHeightPx = screenHeight > 0
                    ? Math.round(screenHeight * HEADER_COVER_HEIGHT_SCREEN_RATIO)
                    : AndroidUtilities.dp(240);
        }
        return headerCoverHeightPx;
    }

    /** 与 iOS 一致使用屏幕高度的 40%，并保持头像区域相对封面底部的位置。 */
    private void applyHeaderCoverSize(@NonNull View headerView) {
        int coverHeightPx = getHeaderCoverHeightPx();
        updateViewHeight(headerView.findViewById(R.id.topLayout), coverHeightPx);
        updateViewHeight(headerView.findViewById(R.id.momentBgIv), coverHeightPx);

        View centerView = headerView.findViewById(R.id.centerView);
        ViewGroup.LayoutParams layoutParams = centerView.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams =
                    (ViewGroup.MarginLayoutParams) layoutParams;
            marginLayoutParams.topMargin = Math.max(0,
                    coverHeightPx - AndroidUtilities.dp(HEADER_PROFILE_TOP_OVERLAP_DP));
            centerView.setLayoutParams(marginLayoutParams);
        }
    }

    private void updateViewHeight(@NonNull View view, int heightPx) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = heightPx;
        view.setLayoutParams(layoutParams);
    }

    private void initMomentsScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                applyTitleBarForScroll(computeTitleCollapseRatio());
            }
        });
        bageVBinding.titleView.post(() -> applyTitleBarForScroll(computeTitleCollapseRatio()));
    }

    private int getHeaderScrollOffset() {
        if (recyclerView == null || mLinearLayoutManager == null) {
            return 0;
        }
        int firstPos = mLinearLayoutManager.findFirstVisibleItemPosition();
        if (firstPos > 0) {
            return collapseStartPx + collapseRangePx;
        }
        if (firstPos < 0 || recyclerView.getChildCount() == 0) {
            return 0;
        }
        View headerChild = null;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            if (recyclerView.getChildAdapterPosition(child) == 0) {
                headerChild = child;
                break;
            }
        }
        if (headerChild == null) {
            headerChild = recyclerView.getChildAt(0);
        }
        return Math.max(0, -headerChild.getTop());
    }

    private float computeTitleCollapseRatio() {
        if (collapseRangePx <= 0) {
            measureTitleCollapseThresholds();
        }
        int scrollOffset = getHeaderScrollOffset();
        if (scrollOffset <= collapseStartPx) {
            return 0f;
        }
        float raw = (scrollOffset - collapseStartPx) / (float) collapseRangePx;
        return Math.min(1f, Math.max(0f, raw));
    }

    /** Smoothstep: natural ease-in-out while scrolling. */
    private static float smoothStep(float t) {
        return t * t * (3f - 2f * t);
    }

    private void applyTitleBarForScroll(float collapseRatio) {
        float ratio = smoothStep(collapseRatio);
        boolean isDarkMode = Theme.getDarkModeStatus(this);
        int iconTo = ContextCompat.getColor(this, isDarkMode ? R.color.white : R.color.black);
        int iconColor = ColorUtils.blendARGB(coverBackIconColor, iconTo, ratio);

        bageVBinding.titleView.setBackgroundColor(Color.TRANSPARENT);
        if (ratio <= 0.001f) {
            bageVBinding.titleBgIv.setVisibility(View.GONE);
            bageVBinding.titleBgIv.setAlpha(0f);
            bageVBinding.titleCenterTv.setVisibility(View.GONE);
            bageVBinding.titleCenterTv.setAlpha(0f);
        } else {
            bageVBinding.titleBgIv.setVisibility(View.VISIBLE);
            float bgAlpha = ratio >= 0.98f ? 1f : ratio;
            bageVBinding.titleBgIv.setAlpha(bgAlpha);
            if (showTitleCenterTv) {
                bageVBinding.titleCenterTv.setVisibility(View.VISIBLE);
                bageVBinding.titleCenterTv.setAlpha(ratio);
            } else {
                bageVBinding.titleCenterTv.setVisibility(View.GONE);
                bageVBinding.titleCenterTv.setAlpha(0f);
            }
        }

        applyTitleIconColor(bageVBinding.backIv, iconColor);
        clearTitleIconTint(bageVBinding.cameraIv);
        clearTitleIconTint(bageVBinding.msgIv);
    }

    private void showMomentCover(@Nullable Bitmap bitmap) {
        ImageView coverView = bageVBinding.wrapview.getMomentBgIv();
        coverView.setImageBitmap(bitmap);
        if (bitmap == null) {
            coverBackIconColor = Color.WHITE;
            applyTitleBarForScroll(computeTitleCollapseRatio());
            return;
        }
        coverView.post(() -> {
            coverBackIconColor = resolveCoverBackIconColor(bitmap, coverView);
            applyTitleBarForScroll(computeTitleCollapseRatio());
        });
    }

    /** 根据返回按钮下方的封面亮度，自动选择黑色或白色图标。 */
    private int resolveCoverBackIconColor(@NonNull Bitmap bitmap,
                                          @NonNull ImageView coverView) {
        int viewWidth = coverView.getWidth();
        int viewHeight = coverView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0
                || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            return Color.WHITE;
        }

        int[] coverLocation = new int[2];
        int[] backLocation = new int[2];
        coverView.getLocationOnScreen(coverLocation);
        bageVBinding.backIv.getLocationOnScreen(backLocation);
        float targetX = backLocation[0] - coverLocation[0]
                + bageVBinding.backIv.getWidth() / 2f;
        float targetY = backLocation[1] - coverLocation[1]
                + bageVBinding.backIv.getHeight() / 2f;

        float scale = Math.max(viewWidth / (float) bitmap.getWidth(),
                viewHeight / (float) bitmap.getHeight());
        float offsetX = (viewWidth - bitmap.getWidth() * scale) / 2f;
        float offsetY = (viewHeight - bitmap.getHeight() * scale) / 2f;
        int sourceX = Math.round((targetX - offsetX) / scale);
        int sourceY = Math.round((targetY - offsetY) / scale);
        int radius = Math.max(2, Math.round(AndroidUtilities.dp(18) / scale));
        int step = Math.max(1, radius / 6);

        double luminance = 0d;
        int sampleCount = 0;
        for (int y = sourceY - radius; y <= sourceY + radius; y += step) {
            if (y < 0 || y >= bitmap.getHeight()) continue;
            for (int x = sourceX - radius; x <= sourceX + radius; x += step) {
                if (x < 0 || x >= bitmap.getWidth()) continue;
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) < 128) continue;
                luminance += ColorUtils.calculateLuminance(pixel);
                sampleCount++;
            }
        }
        if (sampleCount == 0) {
            return Color.WHITE;
        }
        return luminance / sampleCount >= 0.55d ? Color.BLACK : Color.WHITE;
    }

    private void applyTitleIconColor(ImageView icon, int color) {
        if (icon == null) {
            return;
        }
        icon.setColorFilter(color);
    }

    private void clearTitleIconTint(ImageView icon) {
        if (icon == null) {
            return;
        }
        icon.clearColorFilter();
    }

    @Override
    protected void initListener() {
        initMomentsScrollListener();

        if (newMomentsLayout != null) {
            SingleClickUtil.onSingleClick(newMomentsLayout, view1 -> {
                Intent intent = new Intent(this, MomentsMsgListActivity.class);
                startActivity(intent);
            });
        }
        bageVBinding.wrapview.setOnRefreshListener(() -> {
            page = 1;
            bageVBinding.refreshLayout.setEnableLoadMore(true);
            if (TextUtils.isEmpty(uid))
                presenter.list(page);
            else presenter.listByUid(page, uid);
        });
        bageVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                page++;
                if (TextUtils.isEmpty(uid))
                    presenter.list(page);
                else presenter.listByUid(page, uid);
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {

            }
        });
        bageVBinding.wrapview.setOnConverClick(this::chooseIMG);

        SingleClickUtil.onSingleClick(bageVBinding.msgIv, v -> openChatWithUser());

        bageVBinding.cameraIv.setOnLongClickListener(view1 -> {
            if (!TextUtils.isEmpty(uid) && uid.equals(BageConfig.getInstance().getUid()))
                return true;
            Intent intent = new Intent(MomentsActivity.this, PublishMomentsActivity.class);
            intent.putExtra("publishType", MomentsType.single_text);
            chooseResultLac.launch(intent);
            return true;
        });
        adapter.setReplyClick(((momentNo, reply, locationY) -> {
            recyclerView.smoothScrollBy(0, locationY + BageConstants.getKeyboardHeight() + AndroidUtilities.dp(48) - AndroidUtilities.getScreenHeight());
            replyMomentNo = momentNo;
            replyMomentsReply = reply;
            showCommentDialog(String.format(getString(R.string.str_moments_reply_user), reply.name));
//            bageVBinding.emojiPanelView.showEmojiPanel(String.format(getString(R.string.str_moments_reply_user), reply.name));
        }));
        adapter.setCommentReportClick((momentNo, reply) ->
                MomentModerationHelper.reportComment(this, momentNo, reply, result -> {
                    if (result != null && result.shouldRemoveContent()) {
                        String commentId = TextUtils.isEmpty(result.comment_id) ? reply.sid : result.comment_id;
                        adapter.removeReportedComment(momentNo, commentId);
                    }
                }));
        adapter.addChildClickViewIds(R.id.contentStatusTv, R.id.moreIv, R.id.deleteTv, R.id.avatarIv,R.id.tvMore);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> {
            Moments moments = (Moments) adapter1.getItem(position);
            if (moments != null) {
                if (view1.getId() == R.id.contentStatusTv) {
                    moments.isExpand = !moments.isExpand;
                    adapter1.notifyItemChanged(position + adapter1.getHeaderLayoutCount());
                } else if (view1.getId() == R.id.avatarIv) {
                    BageMomentsApplication.getInstance().gotoUserDetail(MomentsActivity.this, moments.publisher);
                } else if (view1.getId() == R.id.moreIv || view1.getId() == R.id.tvMore) {
                    boolean isLiked = false;
                    if (BageReader.isNotEmpty(moments.likes)) {
                        for (int i = 0, size = moments.likes.size(); i < size; i++) {
                            if (moments.likes.get(i).uid.equals(BageConfig.getInstance().getUid())) {
                                isLiked = true;
                                break;
                            }
                        }
                    }

                    boolean showModerationActions = !TextUtils.equals(moments.publisher, BageConfig.getInstance().getUid());
                    FeedActionPopup feedActionPopup = new FeedActionPopup(this, isLiked, showModerationActions, type -> {
                        if (type == 1) {
                            int[] location = new int[2];
                            view1.getLocationOnScreen(location);
                            recyclerView.smoothScrollBy(0, location[1] + BageConstants.getKeyboardHeight() + AndroidUtilities.dp(88) - AndroidUtilities.getScreenHeight());
                            replyMomentNo = moments.moment_no;
                            replyMomentsReply = null;
                            showCommentDialog(getString(R.string.moments_reply));
                            //  bageVBinding.emojiPanelView.showEmojiPanel(getString(R.string.moments_reply));
                        } else if (type == 2) {
                            MomentModerationHelper.blockPublisher(this, moments,
                                    adapter::removeMomentsByPublisher);
                        } else if (type == 3) {
                            MomentModerationHelper.reportMoment(this, moments, result -> {
                                if (result != null && result.shouldRemoveContent()) {
                                    String removeMomentNo = TextUtils.isEmpty(result.moment_no)
                                            ? moments.moment_no : result.moment_no;
                                    adapter.removeMoment(removeMomentNo);
                                }
                            });
                        } else {
                            boolean isLike = true;
                            if (BageReader.isNotEmpty(moments.likes)) {
                                for (MomentsPraise praise : moments.likes) {
                                    if (praise.uid.equals(BageConfig.getInstance().getUid())) {
                                        isLike = false;
                                        break;
                                    }
                                }
                            }
                            if (isLike) {
                                MomentsModel.getInstance().like(moments.moment_no, (code, msg) -> {
                                    if (code == HttpResponseCode.success) {
                                        if (moments.likes == null)
                                            moments.likes = new ArrayList<>();
                                        moments.likes.add(new MomentsPraise(BageConfig.getInstance().getUid(), BageConfig.getInstance().getUserName()));
                                        moments.praiseSpan = MomentSpanUtils.getInstance().makePraiseSpan(BageMomentsApplication.getInstance().getContext(), moments.likes);
                                        adapter1.notifyItemChanged(position + adapter1.getHeaderLayoutCount());
                                    } else showToast(msg);
                                });
                            } else {
                                MomentsModel.getInstance().unlike(moments.moment_no, (code, msg) -> {
                                    if (code == HttpResponseCode.success) {
                                        for (int i = 0, size = moments.likes.size(); i < size; i++) {
                                            if (moments.likes.get(i).uid.equals(BageConfig.getInstance().getUid())) {
                                                moments.likes.remove(i);
                                                break;
                                            }
                                        }
                                        moments.praiseSpan = MomentSpanUtils.getInstance().makePraiseSpan(BageMomentsApplication.getInstance().getContext(), moments.likes);
                                        adapter1.notifyItemChanged(position + adapter1.getHeaderLayoutCount());
                                    } else showToast(msg);
                                });
                            }
                        }
                    });
                    if (feedActionPopup.isShowing()) {
                        feedActionPopup.dismiss();
                    } else {
                        feedActionPopup.showBelowAnchor(view1);
                    }

                } else if (view1.getId() == R.id.deleteTv) {
                    BageDialogUtils.getInstance().showDialog(this, getString(R.string.base_delete), getString(R.string.delete_moments_tips), true, "", getString(R.string.base_delete), 0, ContextCompat.getColor(this, R.color.red), index -> {
                        if (index == 1) {
                            MomentsModel.getInstance().delete(moments.moment_no, (code, msg) -> {
                                if (code == HttpResponseCode.success) {
                                    adapter1.removeAt(position);
                                } else showToast(msg);
                            });

                        }
                    });
                }
            }
        });
        bageVBinding.cameraIv.setOnClickListener(v -> {
            if (TextUtils.isEmpty(uid)) {
                choose();
            } else {
                if (TextUtils.equals(uid, BageConfig.getInstance().getUid())) {
                    startActivity(new Intent(this, MomentsMsgListActivity.class));
                }
            }
        });
        bageVBinding.backIv.setOnClickListener(v -> finish());
        //监听刷新通讯录
        EndpointManager.getInstance().setMethod("moment_activity", EndpointCategory.bageRefreshMailList, object -> {
            getMomentMsg();
            return null;
        });
    }

    private void choose() {
        Object isRegisterVideo = EndpointManager.getInstance().invoke("is_register_video", null);
        if (isRegisterVideo instanceof Boolean) {
            boolean isRegister = (boolean) isRegisterVideo;
            if (isRegister) {
                List<BottomSheetItem> list = new ArrayList<>();
                String content = String.format("%s/%s", getString(R.string.photograph), getString(R.string.video_img));
                list.add(new BottomSheetItem(content, R.mipmap.msg_camera, () -> publish(0)));
                list.add(new BottomSheetItem(getString(R.string.choose_by_album), R.mipmap.ic_gallery, () -> publish(1)));
                BageDialogUtils.getInstance().showBottomSheet(this, "", false, list);
            }
            return;
        }
        publish(1);
    }

    private void publish(int position) {
        int publishType;
        if (position == 0) {
            publishType = MomentsType.video_text;
            checkVideoPublishPermissions(publishType);
        } else {
            publishType = MomentsType.image_text;
            checkImagePublishPermissions(publishType);
        }
    }

    private void checkVideoPublishPermissions(int publishType) {
        String desc = String.format(getString(R.string.camera_permissions_desc), getString(R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        launchPublishActivity(publishType);
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);
        } else {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        launchPublishActivity(publishType);
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void checkImagePublishPermissions(int publishType) {
        String desc = String.format(getString(R.string.file_permissions_des), getString(R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        launchPublishActivity(publishType);
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                @Override
                public void onResult(boolean result) {
                    if (result) {
                        launchPublishActivity(publishType);
                    }
                }

                @Override
                public void clickResult(boolean isCancel) {
                }
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void launchPublishActivity(int publishType) {
        Intent intent = new Intent(MomentsActivity.this, PublishMomentsActivity.class);
        intent.putExtra("publishType", publishType);
        chooseResultLac.launch(intent);
    }

    @Override
    public void setList(List<Moments> list) {
        bageVBinding.wrapview.stopRefresh();
        bageVBinding.refreshLayout.finishLoadMore();
        if (page == 1) {
            if (BageReader.isEmpty(list)) {
                list = new ArrayList<>();
                list.add(new Moments());
            }
            adapter.setList(list);
        } else {
            if (BageReader.isEmpty(list)) {
                bageVBinding.refreshLayout.setEnableLoadMore(false);
//                refreshLayout.finishLoadMoreWithNoMoreData();
            } else {
                adapter.addData(list);
            }
        }
    }

    @Override
    public void setMomentSetting(MomentSetting momentSetting) {

    }

    @Override
    public void showError(String msg) {

    }

    @Override
    public void hideLoading() {

    }

    private void getMomentMsg() {
        if (newMomentsLayout == null) {
            return;
        }
        //不是查看自己的朋友圈不显示动态消息
        if (!TextUtils.isEmpty(uid) && !TextUtils.equals(uid, BageConfig.getInstance().getUid()))
            return;
        int num = BageSharedPreferencesUtil.getInstance().getInt(BageConfig.getInstance().getUid() + "_moments_msg_count");
        String uid = BageSharedPreferencesUtil.getInstance().getSP(BageConfig.getInstance().getUid() + "_moments_msg_action_uid");
        if (num > 0 && !TextUtils.isEmpty(uid)) {
            BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(uid, BageChannelType.PERSONAL);
            String key = "";
            if (channel != null) {
                key = channel.avatarCacheKey;
            }
            GlideUtils.getInstance().showAvatarImg(this, uid, BageChannelType.PERSONAL, key, newMomentsAvatarIv.imageView);
            newMomentsCountTv.setText(String.format(getString(R.string.str_moments_new_msg_count), num));
            newMomentsLayout.setVisibility(View.VISIBLE);
        } else newMomentsLayout.setVisibility(View.GONE);
    }

    private void getUserMomentBg() {
        String tempUid;
        if (!TextUtils.isEmpty(uid)) tempUid = uid;
        else tempUid = BageConfig.getInstance().getUid();
        String showUrl = BageSharedPreferencesUtil.getInstance().getSP(String.format("moment_bg_url_%s", tempUid));
        if (!TextUtils.isEmpty(showUrl)) {
            Bitmap bitmap = BitmapFactory.decodeFile(showUrl);
            showMomentCover(bitmap);
            //避免重复下载
            if (tempUid.equals(BageConfig.getInstance().getUid())) return;
        }
        String finalTempUid = tempUid;
        String url = String.format("%s%s%s", BageApiConfig.baseUrl, "moment/cover?uid=", tempUid);
        String filePath = BageFileUtils.getInstance().getNormalFileSavePath("moment") + "/" + BageConfig.getInstance().getUid() + "_" + BageTimeUtils.getInstance().getCurrentMills() + ".bage_png";
        BageDownloader.Companion.getInstance().download(url, filePath, new BageProgressManager.IProgress() {
            @Override
            public void onProgress(@Nullable Object tag, int progress) {

            }

            @Override
            public void onSuccess(@Nullable Object tag, @Nullable String path) {
                bageVBinding.wrapview.hideOrShowUpdateBgTv(false);
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                showMomentCover(bitmap);
                BageSharedPreferencesUtil.getInstance().putSP(String.format("moment_bg_url_%s", finalTempUid), filePath);

            }

            @Override
            public void onFail(@Nullable Object tag, @Nullable String msg) {
                if (finalTempUid.equals(BageConfig.getInstance().getUid()))
                    bageVBinding.wrapview.hideOrShowUpdateBgTv(true);
                BageSharedPreferencesUtil.getInstance().putSP(String.format("moment_bg_url_%s", finalTempUid), "");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        BageBaseApplication.getInstance().disconnect = true;
    }

    private void chooseIMG() {
        BageBaseApplication.getInstance().disconnect = false;
        String desc = String.format(getString(R.string.album_permissions_desc), getString(R.string.app_name));
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
                                                         }, this, desc, Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO);
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
            }, this, desc, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }


    }

    private void success() {

        GlideUtils.getInstance().chooseIMG(MomentsActivity.this, 1, true, ChooseMimeType.img, false, new GlideUtils.ISelectBack() {
            @Override
            public void onBack(List<ChooseResult> paths) {
                if (BageReader.isNotEmpty(paths)) {
                    MomentFileUpload.getInstance().getMomentCoverUploadUrl((url, path) -> {
                        if (!TextUtils.isEmpty(url)) {
                            BageUploader.getInstance().upload(url, paths.get(0).path, new BageUploader.IUploadBack() {
                                @Override
                                public void onSuccess(String url) {
                                    String filePath = BageFileUtils.getInstance().getNormalFileSavePath("moment") + "/" + BageConfig.getInstance().getUid() + "_" + BageTimeUtils.getInstance().getCurrentMills() + ".bage_png";
                                    BageFileUtils.getInstance().fileCopy(paths.get(0).path, filePath);
                                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                                    showMomentCover(bitmap);

                                    BageSharedPreferencesUtil.getInstance().putSP(String.format("moment_bg_url_%s", BageConfig.getInstance().getUid()), filePath);
                                    getUserMomentBg();
                                    bageVBinding.wrapview.hideOrShowUpdateBgTv(false);
                                }

                                @Override
                                public void onError() {
                                    showToast(R.string.moment_upload_bg_err);
                                }
                            });

                        }
                    });

                }
            }

            @Override
            public void onCancel() {

            }
        });

    }

    ActivityResultLauncher<Intent> chooseResultLac = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            page = 1;
            presenter.list(page);
            recyclerView.scrollToPosition(0);
        }
    });

    @Override
    public void finish() {
        super.finish();
        EndpointManager.getInstance().remove("moment_activity");
    }

    private void showCommentDialog(String hintText) {
        new FeedCommentDialog(MomentsActivity.this, hintText, (visible, currentTop) -> {

        }, new FeedCommentDialog.IEmojiClick() {
            @Override
            public void onEmojiClick(String emojiName) {

            }

            @Override
            public void onSendClick(String content) {
                MomentsModel.getInstance().comments(replyMomentNo, content, replyMomentsReply == null ? "" : replyMomentsReply.sid, replyMomentsReply == null ? "" : replyMomentsReply.uid, replyMomentsReply == null ? "" : replyMomentsReply.name, (code, msg, commentID) -> {
                    if (code == HttpResponseCode.success) {

                        for (int i = 0, size = adapter.getData().size(); i < size; i++) {
                            if (adapter.getData().get(i).moment_no.equals(replyMomentNo)) {
                                if (adapter.getData().get(i).comments == null) {
                                    adapter.getData().get(i).comments = new ArrayList<>();
                                }
                                //添加一条评论
                                MomentsReply reply = new MomentsReply();
                                reply.sid = commentID;
                                reply.uid = BageConfig.getInstance().getUid();
                                reply.name = BageConfig.getInstance().getUserName();
                                reply.content = content;
                                if (replyMomentsReply != null) {
                                    reply.reply_uid = replyMomentsReply.uid;
                                    reply.reply_name = replyMomentsReply.name;
                                }
                                adapter.getData().get(i).comments.add(reply);
                                adapter.notifyItemChanged(i + adapter.getHeaderLayoutCount());
                                break;
                            }
                        }
                    } else showToast(msg);
                });
            }
        }).show();
    }
}
