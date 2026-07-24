package com.chat.base.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.chat.base.R;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.entity.ImagePopupBottomSheetItem;
import com.chat.base.ui.components.SecretDeleteTimer;
import com.chat.base.utils.ImageUtils;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageToastUtils;
import com.lxj.xpopup.core.ImageViewerPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 * Create by dance, at 2019/5/8
 */
@SuppressLint("ViewConstructor")
public class CustomImageViewerPopup extends ImageViewerPopupView {
    Context context;
    //唯一标记和图片数组长度必须一样
    private final IImgPopupMenu iImgPopupMenu;
    List<ImagePopupBottomSheetItem> list;
    private final BageMsg msg;
    private final int flame;
    private List<Object> originalUrls;
    private TextView originalButton;
    private CustomTarget<File> originalDownloadTarget;
    private int loadingOriginalPosition = -1;

    public CustomImageViewerPopup(@NonNull Context context, int flame, BageMsg msg, List<ImagePopupBottomSheetItem> list, IImgPopupMenu iImgPopupMenu) {
        super(context);
        this.context = context;
        this.list = list;
        this.msg = msg;
        this.flame = flame;
        this.iImgPopupMenu = iImgPopupMenu;
        bgColor = ContextCompat.getColor(context, R.color.black);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.custom_image_viewer_popup;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        ImageView imgMoreIv = findViewById(R.id.imgMoreIv);
        originalButton = findViewById(R.id.loadOriginalTv);
        originalButton.setOnClickListener(v -> loadOriginal());
        pager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateOriginalButton(position);
            }
        });
        updateOriginalButton(pager.getCurrentItem());
        imgMoreIv.setVisibility(flame == 0 ? VISIBLE : GONE);
        imgMoreIv.setOnClickListener(view -> showLongClickDialog(pager.getCurrentItem(), urls.get(pager.getCurrentItem())));
        if (list != null) {
            list.add(new ImagePopupBottomSheetItem(context.getString(R.string.save_img), R.mipmap.msg_download, new ImagePopupBottomSheetItem.IBottomSheetClick() {
                @Override
                public void onClick(int index) {
                    download(urls.get(pager.getCurrentItem()).toString());
                }
            }));
        }
        FrameLayout contentLayout = findViewById(R.id.contentLayout);
        SecretDeleteTimer deleteTimer = new SecretDeleteTimer(context);
        if (msg != null && msg.flame == 1) {
            ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            imgMoreIv.setVisibility(View.GONE);
            deleteTimer.setSize(25);
            deleteTimer.setDestroyTime(
                    msg.clientMsgNO,
                    msg.flameSecond,
                    msg.viewedAt,
                    false
            );
            contentLayout.addView(deleteTimer, LayoutHelper.createFrame(
                    25,
                    25,
                    Gravity.CENTER
            ));
        }
        BageIM.getInstance().getMsgManager().addOnDeleteMsgListener("view_img", deletedMsg -> {
            if (msg != null && deletedMsg != null && msg.clientMsgNO.equals(deletedMsg.clientMsgNO)) {
                dismiss();
            }
        });
    }

    public void setOriginalUrls(List<Object> originalUrls) {
        this.originalUrls = originalUrls;
        if (originalButton != null) updateOriginalButton(pager.getCurrentItem());
    }

    private void updateOriginalButton(int position) {
        if (originalButton == null) return;
        boolean show = flame == 0 && originalUrls != null && position >= 0 && position < originalUrls.size() && position < urls.size()
                && originalUrls.get(position) != null && !String.valueOf(originalUrls.get(position)).equals(String.valueOf(urls.get(position)));
        originalButton.setVisibility(show ? VISIBLE : GONE);
        boolean loading = show && position == loadingOriginalPosition;
        originalButton.setEnabled(!loading);
        originalButton.setText(loading ? R.string.loading_original_image : R.string.view_original_image);
    }

    private void loadOriginal() {
        int position = pager.getCurrentItem();
        if (originalUrls == null || position < 0 || position >= originalUrls.size()) return;
        Object original = originalUrls.get(position);
        if (original == null) return;
        if (originalDownloadTarget != null) {
            Glide.with(context).clear(originalDownloadTarget);
        }
        loadingOriginalPosition = position;
        originalButton.setEnabled(false);
        originalButton.setText(R.string.loading_original_image);
        originalDownloadTarget = new CustomTarget<File>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
            @Override
            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                if (position < 0 || position >= urls.size() || originalUrls == null || position >= originalUrls.size()) {
                    loadingOriginalPosition = -1;
                    updateOriginalButton(pager.getCurrentItem());
                    return;
                }
                // Point both lists at the fully downloaded local file. This
                // prevents a stale preview request from replacing the original
                // and lets the tiled image loader read the source at full size.
                urls.set(position, resource);
                originalUrls.set(position, resource);
                loadingOriginalPosition = -1;
                reloadImage(position);
                updateOriginalButton(pager.getCurrentItem());
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                loadingOriginalPosition = -1;
                updateOriginalButton(pager.getCurrentItem());
                BageToastUtils.getInstance().showToastNormal(context.getString(R.string.load_original_image_failed));
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                if (loadingOriginalPosition == position) {
                    loadingOriginalPosition = -1;
                    updateOriginalButton(pager.getCurrentItem());
                }
            }
        };
        Glide.with(context).downloadOnly().load(original).into(originalDownloadTarget);
    }

    private void reloadImage(int position) {
        androidx.viewpager.widget.PagerAdapter adapter = pager.getAdapter();
        if (adapter == null) return;
        pager.setAdapter(null);
        pager.setAdapter(adapter);
        pager.setCurrentItem(position, false);
    }
//    public class MyPhotoViewAdapter extends PagerAdapter {
//        @Override
//        public int getCount() {
//            return isInfinite ? Integer.MAX_VALUE / 2 : urls.size();
//        }
//
//        @Override
//        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
//            return o == view;
//        }
//
//        @NonNull
//        @Override
//        public Object instantiateItem(@NonNull ViewGroup container, int position) {
//            final PhotoView photoView = new PhotoView(container.getContext());
//            ProgressBar progressBar = buildProgressBar(container.getContext());
//            if (imageLoader != null)
//                imageLoader.loadImage(position, urls.get(isInfinite ? position % urls.size() : position), CustomImageViewerPopup.this,photoView,progressBar);
//            container.addView(photoView);
//            photoView.setOnClickListener(view -> dismiss());
//            photoView.setOnLongClickListener(v -> {
//                showLongClickDialog(position, urls.get(isInfinite ? position % urls.size() : position));
//                return true;
//            });
//            return photoView;
//        }
//
//        @Override
//        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//            container.removeView((View) object);
//        }
//    }

    public void showLongClickDialog(int position, Object url) {

        if (list != null) {
            List<BottomSheetItem> sheetItemList = new ArrayList<>();
            for (ImagePopupBottomSheetItem item : list) {
                sheetItemList.add(new BottomSheetItem(item.getText(), item.getIcon(), () -> item.getIClick().onClick(position)));
            }
            BageDialogUtils.getInstance().showBottomSheet(getContext(), context.getString(R.string.str_choose), false, sheetItemList);
        } else {

            BageDialogUtils.getInstance().showChatImageBottomViewDialog(context, String.valueOf(url), new BageDialogUtils.IImageBottomClick() {
                @Override
                public void onForward() {
                    if (iImgPopupMenu != null)
                        iImgPopupMenu.onForward(position);
                }

                @Override
                public void onFavorite() {
                    if (iImgPopupMenu != null)
                        iImgPopupMenu.onFavorite(position);
                }

                @Override
                public void onShowInChat() {
                    if (iImgPopupMenu != null)
                        iImgPopupMenu.onShowInChat(position);
                    dismiss();
                }

                @Override
                public void onDownload() {
                    download(String.valueOf(url));
                }
            });
        }


    }

    private ProgressBar buildProgressBar(Context context) {
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        int size = XPopupUtils.dp2px(container.getContext(), 40f);
        FrameLayout.LayoutParams params = new LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(params);
        progressBar.setVisibility(GONE);
        return progressBar;
    }

    private void download(String url) {
        if (url.startsWith("http") || url.startsWith("HTTP")) {
            Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .into(new CustomTarget<Bitmap>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            ImageUtils.getInstance().saveBitmap(context, resource, true, null);
                            BageToastUtils.getInstance().showToastNormal(context.getString(R.string.saved_album));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(url);
            ImageUtils.getInstance().saveBitmap(context, bitmap, true, null);
            BageToastUtils.getInstance().showToastNormal(context.getString(R.string.saved_album));
        }
    }

    @Override
    public void dismiss() {
        if (originalDownloadTarget != null) {
            Glide.with(context).clear(originalDownloadTarget);
            originalDownloadTarget = null;
        }
        super.dismiss();
        BageIM.getInstance().getMsgManager().removeDeleteMsgListener("view_img");
        if (msg != null && msg.flame == 1) {
            boolean disable_screenshot;
            String uid = BageConfig.getInstance().getUid();
            if (!TextUtils.isEmpty(uid)) {
                disable_screenshot = BageSharedPreferencesUtil.getInstance().getBoolean(uid + "_disable_screenshot");
            } else {
                disable_screenshot = BageSharedPreferencesUtil.getInstance().getBoolean("disable_screenshot");
            }
            if (disable_screenshot)
                ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            else {
                ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }
    }

    public interface IImgPopupMenu {
        void onForward(int position);

        void onFavorite(int position);

        void onShowInChat(int position);
    }
}
