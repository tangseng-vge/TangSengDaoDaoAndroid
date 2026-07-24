package com.chat.base.utils;

import android.text.TextUtils;

import com.chat.base.config.BageSharedPreferencesUtil;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 2021/7/23 12:25
 * 获取html数据
 */
public class BageJsoupUtils {
    private BageJsoupUtils() {
    }

    private static class JsoupUtilsBinder {
        final static BageJsoupUtils jsoup = new BageJsoupUtils();
    }

    public static BageJsoupUtils getInstance() {
        return JsoupUtilsBinder.jsoup;
    }

    public void getURLContent(String url, String clientMsgNo) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(clientMsgNo)) return;

        Observable.create((ObservableOnSubscribe<BageURLContent>) emitter -> {
            String tempURL = url;
            if (url.startsWith("www") || url.startsWith("WWW")) {
                tempURL = "http://" + url;
            }
            Document document = Jsoup.connect(tempURL).get();
            if (document != null) {
                String title = document.head().getElementsByTag("title").text();
                Elements elements = document.head().getElementsByTag("meta");
                String htmlContent = "";
                String coverURL = "";
                for (Element element : elements) {
                    String name = element.attr("name");
                    String content = element.attr("content");
                    if (name.equals("description")) {
                        htmlContent = content;
                        if (!TextUtils.isEmpty(coverURL))
                            break;
                    }
                    String property = element.attr("property");
                    if (property.equals("og:image")) {
                        coverURL = content;
                        if (!TextUtils.isEmpty(htmlContent)) {
                            break;
                        }
                    }
                }
                BageURLContent bageurlContent = new BageURLContent();
                bageurlContent.content = htmlContent;
                bageurlContent.title = title;
                bageurlContent.url = url;
                bageurlContent.coverURL = coverURL;
                emitter.onNext(bageurlContent);
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Observer<BageURLContent>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(@NonNull BageJsoupUtils.BageURLContent bageUrlContent) {
                if (!TextUtils.isEmpty(bageUrlContent.title) && !TextUtils.isEmpty(bageUrlContent.content)) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("title", bageUrlContent.title);
                        jsonObject.put("content", bageUrlContent.content);
                        jsonObject.put("coverURL", bageUrlContent.coverURL);
                        jsonObject.put("logo", bageUrlContent.url + "/favicon.ico");
                        jsonObject.put("expirationTime", BageTimeUtils.getInstance().getCurrentSeconds());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    BageMsg bageMsg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(clientMsgNo);
                    if (bageMsg != null) {
//                        if (bageMsg.extraMap == null)
//                            bageMsg.extraMap = new HashMap<String, Object>();
//                        bageMsg.extraMap.put("link_url", url);
//                        bageMsg.extraMap.put("link_title", bageUrlContent.title);
//                        bageMsg.extraMap.put("link_content", bageUrlContent.content);
//                        bageMsg.extraMap.put("link_coverURL", bageUrlContent.coverURL);
//                        bageMsg.extraMap.put("link_logo", bageUrlContent.url + "/favicon.ico");
                        BageIM.getInstance().getMsgManager().setRefreshMsg(bageMsg, true);
                    }
                    BageSharedPreferencesUtil.getInstance().putSP(bageUrlContent.url, jsonObject.toString());
                }

            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    private static class BageURLContent {
        public String title;
        public String url;
        public String content;
        public String coverURL;
    }
}
