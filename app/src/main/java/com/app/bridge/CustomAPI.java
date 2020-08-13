package com.app.bridge;

import android.app.Activity;
import android.webkit.JavascriptInterface;

import com.zndroid.bridge.MessageController;
import com.zndroid.bridge.api.BaseAPI;
import com.zndroid.bridge.framework.ZWebView;
import com.zndroid.bridge.params.Message;

/**
 * @name:CustomAPI
 * @author:lazy
 * @email:luzhenyuxfcy@sina.com
 * @date : 2020/7/27 22:52
 * @version:
 * @description:TODO
 */
public class CustomAPI extends BaseAPI {
    private ZWebView webView;
    public CustomAPI(Activity activity) {
        super(activity);
    }

    public void setWebView(ZWebView webView) {
        this.webView = webView;
    }

    @Override
    protected String getTAG() {
        return "CustomAPI";
    }

    @JavascriptInterface
    public void custom(Object object) {
        showToast("im custom API call");

        Message<String> message = new Message<>();
        message.setKey("haha");
        message.setValue("xxxxxxxxx");
        MessageController.get().with(webView).sendMessage(message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showLog("im on destroyed....." + this.toString());
    }
}
