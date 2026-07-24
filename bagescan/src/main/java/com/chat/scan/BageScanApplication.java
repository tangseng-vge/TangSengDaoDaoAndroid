package com.chat.scan;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.king.zxing.util.CodeUtils;
import com.chat.base.act.BageWebViewActivity;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ParseQrCodeMenu;
import com.chat.base.entity.PopupMenuItem;

import java.lang.ref.WeakReference;

/**
 * 2020-04-19 17:04
 */
public class BageScanApplication {
    private BageScanApplication() {

    }

    private static class ScanApplicationBinder {
        static final BageScanApplication application = new BageScanApplication();
    }

    public static BageScanApplication getInstance() {
        return ScanApplicationBinder.application;
    }

    public WeakReference<Context> mContext;

    public void init(Context context) {
        mContext = new WeakReference<>(context);
        EndpointManager.getInstance().setMethod("bage_scan_show", object -> {
            Intent intent = new Intent(context, BageScanActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return null;
        });
        //添加tab页扫一扫功能
        EndpointManager.getInstance().setMethod(EndpointCategory.tabMenus + "_scan", EndpointCategory.tabMenus, 99, object -> new PopupMenuItem( context.getString(R.string.bage_scan_module_scan), R.mipmap.ic_scan_qr,() -> {
            Intent intent = new Intent(context, BageScanActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }));

        EndpointManager.getInstance().setMethod("create_qrcode", object -> {
            String qrcode = (String) object;
            return CodeUtils.createQRCode(qrcode, 400, null);
        });
        // 识别bitmap是否为二维码
        EndpointManager.getInstance().setMethod("parse_qrcode", object -> {
            ParseQrCodeMenu menu = (ParseQrCodeMenu) object;
            final String result = CodeUtils.parseCode(menu.bitmap);
            Log.e("识别的内容是：","--->"+result);
            if (menu.isJump && !TextUtils.isEmpty(result)) {
                ScanUtils.getInstance().handleScanResult(menu.activity, result, new ScanUtils.IHandleScanResult() {
                    @Override
                    public void showOtherContent(String content) {
                        Intent intent = new Intent(menu.activity, BageScanOtherResultActivity.class);
                        intent.putExtra("result", content);
                        menu.activity.startActivity(intent);

                    }

                    @Override
                    public void showWebView(String url) {
                        Intent intent = new Intent(menu.activity, BageWebViewActivity.class);
                        intent.putExtra("url", url);
                        menu.activity.startActivity(intent);

                    }

                    @Override
                    public void dismissView() {
                    }
                });
            } else {
                if (menu.iResult != null) {
                    menu.iResult.onResult(result);
                }
            }
            return null;
        });
    }

}
