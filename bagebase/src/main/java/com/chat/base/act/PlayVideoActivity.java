package com.chat.base.act;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.chat.base.R;
import com.chat.base.BageBaseApplication;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatChooseContacts;
import com.chat.base.endpoint.entity.ChooseChatMenu;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.glide.GlideUtils;
import com.chat.base.net.ud.BageDownloader;
import com.chat.base.net.ud.BageProgressManager;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageFileUtils;
import com.chat.base.utils.BagePermissions;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.BageToastUtils;
import com.chat.base.utils.systembar.BageStatusBarUtils;
import com.google.android.material.snackbar.Snackbar;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageSendOptions;
import com.bage.im.msgmodel.BageMessageContent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager;

/**
 * 2020-03-11 11:54
 * 播放视频
 */
public class PlayVideoActivity extends GSYBaseActivityDetail<VideoPlayer> {

    VideoPlayer detailPlayer;
    String playUrl;
    String coverImg;
    private String clientMsgNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.act_play_video_layout);

        detailPlayer = findViewById(R.id.player);
        //增加title
        detailPlayer.getTitleTextView().setVisibility(View.GONE);
        detailPlayer.getBackButton().setVisibility(View.GONE);
        initView();
        initVideoBuilderMode();
        detailPlayer.startPlayLogic();

        PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
    }

    private void initView() {
        if (getIntent().hasExtra("clientMsgNo"))
            clientMsgNo = getIntent().getStringExtra("clientMsgNo");
        coverImg = getIntent().getStringExtra("coverImg");
        String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            BageToastUtils.getInstance().showToast(getString(R.string.video_deleted));
            finish();
            return;
        }
        playUrl = url;
        if (!url.startsWith("HTTP") && !url.startsWith("http")) {
            playUrl = "file:///" + url;
        }
        detailPlayer.setLongClick(() -> {
            if (!TextUtils.isEmpty(clientMsgNo)) {
                BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                if (msg.flame == 1) return;
            }
            showSaveDialog(playUrl);
        });


        Window window = getWindow();
        if (window == null) return;
        BageStatusBarUtils.transparentStatusBar(window);
//        BageStatusBarUtils.setDarkMode(window);
        BageStatusBarUtils.setStatusBarColor(window, ContextCompat.getColor(this, R.color.black), 0);
        BageStatusBarUtils.setLightMode(window);

        if (!TextUtils.isEmpty(clientMsgNo)) {
            BageIM.getInstance().getMsgManager().addOnRefreshMsgListener("play_video", (msg, b) -> {
                if (msg != null && !TextUtils.isEmpty(msg.clientMsgNO) && msg.clientMsgNO.equals(clientMsgNo)) {
                    if (msg.remoteExtra.revoke == 1) {
                        BageToastUtils.getInstance().showToast(getString(R.string.can_not_play_video_with_revoke));
                        finish();
                    }
                }
            });
        }
    }

    @Override
    public VideoPlayer getGSYVideoPlayer() {
        return detailPlayer;
    }

    @Override
    public GSYVideoOptionBuilder getGSYVideoOptionBuilder() {
        //内置封面可参考SampleCoverVideo
        ImageView imageView = new ImageView(this);
        ViewCompat.setTransitionName(detailPlayer, "coverIv");
        GlideUtils.getInstance().showImg(this, coverImg, imageView);
        return new GSYVideoOptionBuilder()
                .setThumbImageView(imageView)
                .setUrl(playUrl)
                .setCacheWithPlay(false)
                .setVideoTitle("")
                .setIsTouchWiget(true)
                //.setAutoFullWithSize(true)
                .setRotateViewAuto(false)
                .setLockLand(false)
                .setShowFullAnimation(false)//打开动画
                .setNeedLockFull(true)
                .setSeekRatio(1);
    }

    @Override
    public void clickForFullScreen() {

    }


    /**
     * 是否启动旋转横屏，true表示启动
     */
    @Override
    public boolean getDetailOrientationRotateAuto() {
        return true;
    }

    private void showSaveDialog(String url) {
        List<BottomSheetItem> list = new ArrayList<>();
        list.add(new BottomSheetItem(getString(R.string.save_img), R.mipmap.msg_download, () -> {
            checkPermissions(url);
        }));
        if (!TextUtils.isEmpty(clientMsgNo)) {
            list.add(new BottomSheetItem(getString(R.string.forward), R.mipmap.msg_forward, () -> {

                if (!TextUtils.isEmpty(clientMsgNo)) {
                    BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                    if (msg != null && msg.baseContentMsgModel != null) {
                        EndpointManager.getInstance().invoke(EndpointSID.showChooseChatView, new ChooseChatMenu(new ChatChooseContacts(list1 -> {
                            BageMessageContent msgContent = msg.baseContentMsgModel;
                            if (BageReader.isNotEmpty(list1)) {
                                for (BageChannel channel : list1) {
                                    msgContent.mentionAll = 0;
                                    msgContent.mentionInfo = null;
                                    BageSendOptions options = new BageSendOptions();
                                    options.setting.receipt = channel.receipt;
//                                    setting.signal = 0;
                                    BageIM.getInstance().getMsgManager().sendWithOptions(
                                            msgContent,
                                            channel, options
                                    );
                                }
                                View viewGroup = findViewById(android.R.id.content);
                                Snackbar.make(viewGroup, getString(R.string.str_forward), 1000).setAction("", view -> {
                                }).show();
                            }
                        }), msg.baseContentMsgModel));
                    }
                }

            }));
        }
        BageDialogUtils.getInstance().showBottomSheet(this, getString(R.string.bage_video), false, list);
    }

    @Override
    public void finish() {
        super.finish();
        if (!TextUtils.isEmpty(clientMsgNo)) {
            BageIM.getInstance().getMsgManager().removeRefreshMsgListener("play_video");
            BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
            if (msg != null && msg.flame == 1 && msg.viewed == 0) {
                BageIM.getInstance().getMsgManager().updateViewedAt(1, BageTimeUtils.getInstance().getCurrentMills(), clientMsgNo);
                EndpointManager.getInstance().invoke("video_viewed", clientMsgNo);
            }
        }
    }

    private void saveToAlbum(String url) {

        // 保存视频
        if (!url.startsWith("http") && !url.startsWith("HTTP")) {
            File file = new File(url.replaceAll("file:///", ""));
            save(file);
        } else {
            String fileDir = Objects.requireNonNull(getExternalFilesDir("video")).getAbsolutePath() + BageBaseApplication.getInstance().getFileDir() + "/";
            BageFileUtils.getInstance().createFileDir(fileDir);
            String filePath = fileDir + BageTimeUtils.getInstance().getCurrentMills() + ".mp4";
            BageDownloader.Companion.getInstance().download(url, filePath, new BageProgressManager.IProgress() {
                @Override
                public void onProgress(@Nullable Object tag, int progress) {

                }

                @Override
                public void onSuccess(@Nullable Object tag, @Nullable String path) {
                    File file = new File(filePath.replaceAll("file:///", ""));
                    save(file);
                }

                @Override
                public void onFail(@Nullable Object tag, @Nullable String msg) {
                    BageToastUtils.getInstance().showToastNormal(getString((R.string.download_err)));
                }
            });
        }

    }

    private void save(File file) {
        boolean result = BageFileUtils.getInstance().saveVideoToAlbum(PlayVideoActivity.this, file.getAbsolutePath());
        if (result) {
            BageToastUtils.getInstance().showToastNormal(getString(R.string.saved_album));
        }
    }

    private void checkPermissions(String url) {
        String desc = String.format(
                getString(R.string.file_permissions_des),
                getString(R.string.app_name)
        );
        if (Build.VERSION.SDK_INT < 33) {
            BagePermissions.getInstance().checkPermissions(new BagePermissions.IPermissionResult() {
                                                             @Override
                                                             public void onResult(boolean result) {
                                                                 if (result) {
                                                                     saveToAlbum(url);
                                                                 }
                                                             }

                                                             @Override
                                                             public void clickResult(boolean isCancel) {

                                                             }
                                                         },
                    this,
                    desc,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        } else {
            BagePermissions.getInstance().checkPermissions(
                    new BagePermissions.IPermissionResult() {
                        @Override
                        public void onResult(boolean result) {
                            if (result) {
                                saveToAlbum(url);
                            }
                        }

                        @Override
                        public void clickResult(boolean isCancel) {

                        }
                    },
                    this,
                    desc,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES
            );
        }
    }
}
