package com.chat.base.net.ud;


import com.chat.base.base.BageBaseModel;
import com.chat.base.config.BageApiConfig;
import com.chat.base.net.ApiService;
import com.chat.base.net.IRequestResultListener;
import com.chat.base.net.entity.UploadFileUrl;
import com.chat.base.net.entity.UploadResultEntity;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageTimeUtils;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class BageUploader extends BageBaseModel {
    private BageUploader() {
    }

    private static class UploadBinder {
        final static BageUploader upload = new BageUploader();
    }

    public static BageUploader getInstance() {
        return UploadBinder.upload;
    }

    public void upload(String uploadUrl, String filePath, final IUploadBack iUploadBack) {
        upload(uploadUrl, filePath, filePath, iUploadBack);
    }

    public void upload(String uploadUrl, String filePath, Object tag, final IUploadBack iUploadBack) {
        MediaType mediaType = MediaType.Companion.parse("multipart/form-data");
        File file = new File(filePath);
        RequestBody fileBody = RequestBody.Companion.create(file, mediaType);
        FileRequestBody fileRequestBody = new FileRequestBody(fileBody, tag);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), fileRequestBody);
        request(createService(UploadService.class).upload(uploadUrl, part), new IRequestResultListener<>() {
            @Override
            public void onSuccess(UploadResultEntity result) {
                if (iUploadBack != null ) {
                    iUploadBack.onSuccess(result);
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (iUploadBack != null) {
                    iUploadBack.onError();
                }
            }
        });
    }

    public void getUploadFileUrl(String channelID, byte channelType, String localPath, final IGetUploadFileUrl iGetUploadFileUrl) {
        getUploadFileUrl(channelID, channelType, localPath, false, iGetUploadFileUrl);
    }

    public void getUploadFileUrl(String channelID, byte channelType, String localPath, boolean imageVariants, final IGetUploadFileUrl iGetUploadFileUrl) {
        File f = new File(localPath);
        String tempFileName = f.getName();
        String prefix = tempFileName.substring(tempFileName.lastIndexOf(".") + 1);
        String path = "/" + channelType + "/" + channelID + "/" + BageTimeUtils.getInstance().getCurrentMills() + "." + prefix;
        String variants = imageVariants ? "&image_variants=1" : "";
        request(createService(ApiService.class).getUploadFileUrl(BageApiConfig.baseUrl + "file/upload?type=chat&path=" + path + variants), new IRequestResultListener<UploadFileUrl>() {
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
        void onResult(String url, String fileUrl);
    }

    public interface IUploadBack {
        void onSuccess(String url);

        default void onSuccess(UploadResultEntity result) {
            onSuccess(result.path);
        }

        void onError();
    }
}
