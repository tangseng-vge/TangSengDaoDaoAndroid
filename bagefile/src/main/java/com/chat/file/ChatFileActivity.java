package com.chat.file;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.net.ud.BageDownloader;
import com.chat.base.net.ud.BageProgressManager;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.StringUtils;
import com.chat.base.utils.BageFileUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.file.databinding.ActChatFileLayoutBinding;
import com.chat.file.msgitem.FileContent;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;

import java.io.File;
import java.util.Objects;

/**
 * 2020-05-06 17:38
 * 聊天文件查看
 */
public class ChatFileActivity extends BageBaseActivity<ActChatFileLayoutBinding> {
    FileContent fileContent;
    private BageMsg msg;

    @Override
    protected ActChatFileLayoutBinding getViewBinding() {
        return ActChatFileLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.str_file_file);
    }

    @Override
    protected void initView() {
        bageVBinding.openBtn.getBackground().setTint(Theme.colorAccount);
        String clientMsgNo = getIntent().getStringExtra("clientMsgNo");
        msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
        if (msg == null) {
            showToast(getString(R.string.file_is_deleted));
            new Handler(Objects.requireNonNull(Looper.myLooper())).postDelayed(this::finish, 1000);
            return;
        }

        fileContent = (FileContent) msg.baseContentMsgModel;
        if (fileContent != null && !TextUtils.isEmpty(fileContent.name)) {
            bageVBinding.nameTv.setText(fileContent.name);
            if (fileContent.name.contains(".")) {
                String type = fileContent.name.substring(fileContent.name.lastIndexOf(".") + 1);
                if (!TextUtils.isEmpty(type))
                    bageVBinding.typeTv.setText(type.toUpperCase());
                else bageVBinding.typeTv.setText(R.string.unknown_file);
            } else bageVBinding.typeTv.setText(R.string.unknown_file);
            bageVBinding.sizeTv.setText(StringUtils.sizeFormatNum2String(fileContent.size));
        }

    }

    @Override
    protected void initListener() {
        if (fileContent != null && !TextUtils.isEmpty(fileContent.localPath)) {
            bageVBinding.progressBar.setVisibility(View.GONE);
            bageVBinding.openBtn.setText(R.string.open_file);
        } else {
            bageVBinding.openBtn.setText(R.string.download_open);
            bageVBinding.progressBar.setVisibility(View.VISIBLE);
        }
        SingleClickUtil.onSingleClick(bageVBinding.openBtn, view1 -> openFile());
    }

    private void openFile() {
        if (fileContent != null && !TextUtils.isEmpty(fileContent.localPath)) {
            handleWriteExternalStorage(fileContent.localPath);
            // BageFileUtils.getInstance().openFileByPath(this, fileContent.localPath);
        } else {
            if (fileContent != null) {
                String showUrl = BageApiConfig.getShowUrl(fileContent.url);
                if (TextUtils.isEmpty(showUrl)) {
                    showToast(R.string.file_is_deleted);
                    finish();
                    return;
                }
                String fileDir = BageConstants.imageDir + msg.channelType + "/" + msg.channelID + "/";
                BageFileUtils.getInstance().createFileDir(fileDir);
                String filePath = fileDir + msg.clientSeq + "." + fileContent.name.substring(fileContent.name.lastIndexOf(".") + 1);
//                String taskTag = UUID.randomUUID().toString().replaceAll("-", "");
                BageDownloader.Companion.getInstance().download(showUrl, filePath, new BageProgressManager.IProgress() {
                    @Override
                    public void onProgress(@Nullable Object tag, int progress) {
                        bageVBinding.progressBar.setProgress(progress);
                        if (progress >= 100) {
                            bageVBinding.openBtn.setEnabled(true);
                            bageVBinding.openBtn.setAlpha(1f);
                            bageVBinding.openBtn.setText(R.string.open_file);
                            bageVBinding.progressBar.setVisibility(View.GONE);
                        } else {
                            bageVBinding.openBtn.setEnabled(false);
                            bageVBinding.openBtn.setAlpha(0.2f);
                            bageVBinding.progressBar.setVisibility(View.VISIBLE);
                        }

                    }

                    @Override
                    public void onSuccess(@Nullable Object tag, @Nullable String path) {
                        fileContent.localPath = filePath;
                        BageIM.getInstance().getMsgManager().updateContentAndRefresh(msg.clientMsgNO, fileContent, false);
                        handleWriteExternalStorage(fileContent.localPath);
                    }

                    @Override
                    public void onFail(@Nullable Object tag, @Nullable String msg) {

                    }
                });
            }
        }

    }


    public void handleWriteExternalStorage(String path) {
        File sandFile = new File(path);
        if (!sandFile.exists()) {
            showToast(R.string.file_is_deleted);
            finish();
            return;
        }
        String typeName = path.substring(path.lastIndexOf(".") + 1);
        String name = msg.clientMsgNO + "." + typeName;
        new Thread(() -> {
            Uri externalUri;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                File externalFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File destFile = new File(externalFile + File.separator + name);
                externalUri = Uri.fromFile(destFile);
            } else {
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = MediaStore.Files.getContentUri("external");
                externalUri = resolver.insert(uri, values);
            }

            boolean ret = BageFileUtils.getInstance().copyFileToExternalUri(ChatFileActivity.this, sandFile.getAbsolutePath(), externalUri);
            if (ret) {
                String sharePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                AndroidUtilities.runOnUIThread(() -> BageFileUtils.getInstance().openFileByPath(ChatFileActivity.this, sharePath + "/" + name));
            }
        }).start();
    }
}
