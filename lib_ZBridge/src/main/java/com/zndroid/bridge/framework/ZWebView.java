package com.zndroid.bridge.framework;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;

import com.zndroid.bridge.framework.core.DWebView;

/**
 * Created by lazy on 2019-08-26
 */
public class ZWebView extends DWebView {
    public ZWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWebView();
    }

    public ZWebView(Context context) {
        super(context);
        initWebView();
    }

    private void initWebView() {
        getSettings().setAllowFileAccess(true);
        getSettings().setAllowContentAccess(true);
        getSettings().setAppCacheEnabled(false);//暂时关闭缓存  TODO by lazy
        getSettings().setDisplayZoomControls(false);//隐藏web view缩放按钮
        getSettings().setLoadWithOverviewMode(true);//自适应手机屏幕
        getSettings().setDomStorageEnabled(true);//开启本地DOM存储
        getSettings().setDefaultTextEncodingName("UTF-8");//设置编码格式
        getSettings().setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        getSettings().setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true
        // 是否允许通过file url加载的Javascript读取本地文件，默认值 false
        getSettings().setAllowFileAccessFromFileURLs(true);
        // 是否允许通过file url加载的Javascript读取全部资源(包括文件,http,https)，默认值 false
        getSettings().setAllowUniversalAccessFromFileURLs(true);
        getSettings().setSupportZoom(true);// 支持缩放
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }
}
