package com.app.bridge;

import android.app.Activity;
import android.webkit.JavascriptInterface;

import com.zndroid.bridge.api.BaseAPI;

/**
 * @name:CustomAPI
 * @author:lazy
 * @email:luzhenyuxfcy@sina.com
 * @date : 2020/7/27 22:52
 * @version:
 * @description:TODO
 */
public class CustomAPI extends BaseAPI {
    public CustomAPI(Activity activity) {
        super(activity);
    }

    @Override
    protected String getTAG() {
        return "CustomAPI";
    }

    @JavascriptInterface
    public void custom(Object object) {
        showToast("im custom API call");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showLog("im on destroyed.....");
    }
}
