package com.zndroid.bridge.framework;

import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Created by lazy on 2019-09-18
 */
public class ZWebChromeClient extends WebChromeClient {
    private FileChooserCallBack callBack;

    public interface FileChooserCallBack {
        void onValue_4(ValueCallback<Uri> callback);
        void onValue(ValueCallback<Uri[]> callback);
    }

    public void setCallBack(FileChooserCallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        if (null != callBack)
            callBack.onValue(filePathCallback);
        return true;
    }

    //3.0++版本
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        if (null != callBack)
            callBack.onValue_4(uploadMsg);
    }

    //3.0--版本
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        if (null != callBack)
            callBack.onValue_4(uploadMsg);
    }

    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        if (null != callBack)
            callBack.onValue_4(uploadMsg);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
    }


}
