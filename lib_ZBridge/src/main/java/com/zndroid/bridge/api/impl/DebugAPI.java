package com.zndroid.bridge.api.impl;

import android.app.Activity;
import android.webkit.JavascriptInterface;

import com.zndroid.bridge.api.BaseAPI;

/**
 * Created by lazy on 2019-09-24
 */
public class DebugAPI extends BaseAPI {
    private final String TAG = "DebugAPI";

    public DebugAPI(Activity activity) {
        super(activity);
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    @JavascriptInterface
    public void call(Object object) {

    }

    @JavascriptInterface
    public Object back(Object object) {

        return "";
    }
}
