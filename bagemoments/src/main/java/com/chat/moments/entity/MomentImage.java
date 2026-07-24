package com.chat.moments.entity;

/** Three persisted qualities for one Moments image. */
public class MomentImage {
    public String url;
    public String preview_url;
    public String original_url;

    public MomentImage() {
    }

    public MomentImage(String url, String previewUrl, String originalUrl) {
        this.url = url;
        this.preview_url = previewUrl;
        this.original_url = originalUrl;
    }
}
