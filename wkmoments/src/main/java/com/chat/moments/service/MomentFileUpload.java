package com.chat.moments.service;

import android.graphics.BitmapFactory;

import com.chat.base.base.WKBaseModel;
import com.chat.base.config.WKApiConfig;
import com.chat.base.config.WKConfig;
import com.chat.base.net.ApiService;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.UploadFileUrl;
import com.chat.base.utils.WKMediaFileUtils;
import com.chat.base.utils.WKTimeUtils;

import java.io.File;

/**
 * 2020-12-15 10:59
 * 动态文件上传
 */
public class MomentFileUpload extends WKBaseModel {

    private MomentFileUpload() {

    }

    private static class MomentFileUploadBinder {
        private final static MomentFileUpload fileUpload = new MomentFileUpload();
    }

    public static MomentFileUpload getInstance() {
        return MomentFileUploadBinder.fileUpload;
    }

    public void getMomentFileUploadUrl(String localPath, final IGetUploadFileUrl iGetUploadFileUrl) {
        getMomentUploadUrl(localPath, false, iGetUploadFileUrl);
    }

    public void getMomentFileUploadUrl(String localPath, boolean imageVariants, final IGetUploadFileUrl iGetUploadFileUrl) {
        getMomentUploadUrl(localPath, imageVariants, iGetUploadFileUrl);
    }

    private void getMomentUploadUrl(String localPath, boolean imageVariants, final IGetUploadFileUrl iGetUploadFileUrl) {
        File f = new File(localPath);
        String tempFileName = f.getName();
        String prefix = tempFileName.substring(tempFileName.lastIndexOf(".") + 1);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(localPath, options);
        if (!WKMediaFileUtils.getInstance().isVideoFileType(localPath)) {
            prefix = "png";
        }
        int w = Math.max(options.outWidth, 0);
        int h = Math.max(options.outHeight, 0);
        String path = "/" + WKConfig.getInstance().getUid() + "/" + WKTimeUtils.getInstance().getCurrentMills() + "." + prefix + "@" + w + "x" + h;
        String variants = imageVariants ? "&image_variants=1" : "";
        request(createService(ApiService.class).getUploadFileUrl(WKApiConfig.baseUrl + "file/upload?type=moment&path=" + path + variants), new IRequestResultListener<UploadFileUrl>() {
            @Override
            public void onSuccess(UploadFileUrl result) {
                iGetUploadFileUrl.onResult(result.url, path);
            }

            @Override
            public void onFail(int code, String msg) {
                iGetUploadFileUrl.onResult(null, path);
            }
        });

    }

    public interface IGetUploadFileUrl {
        void onResult(String url, String path);
    }

    public void getMomentCoverUploadUrl(final IGetUploadFileUrl iGetUploadFileUrl) {
        request(createService(ApiService.class).getUploadFileUrl(WKApiConfig.baseUrl + "file/upload?type=momentcover"), new IRequestResultListener<UploadFileUrl>() {

            @Override
            public void onSuccess(UploadFileUrl result) {
                iGetUploadFileUrl.onResult(result.url, "");
            }

            @Override
            public void onFail(int code, String msg) {
                iGetUploadFileUrl.onResult(null, "");
            }
        });
    }
}
