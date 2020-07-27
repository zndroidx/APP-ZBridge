package com.zndroid.bridge.framework;

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by lazy on 2019-09-18
 */
public class ZWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (shouldLoadingUrl()) {
            view.loadUrl(url);
            return false;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
    }

    private boolean shouldLoadingUrl() {
        /**
         * 低于android 8.0的需要手动loadURL，大于等于android 8.0直接返回false，否则无法重定向
         */
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
        handler.proceed();
    }
}
