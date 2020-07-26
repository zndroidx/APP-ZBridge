package com.zndroid.bridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zndroid.bridge.api.BaseAPI;
import com.zndroid.bridge.api.impl.CommonAPI;
import com.zndroid.bridge.api.impl.DebugAPI;
import com.zndroid.bridge.api.impl.NativeAPI;
import com.zndroid.bridge.framework.ZWebView;
import com.zndroid.bridge.framework.ZWebViewClient;
import com.zndroid.bridge.framework.core.DWebView;
import com.zndroid.bridge.params.Message;
import com.zndroid.bridge.util.SPUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Created by lazy on 2019-08-19
 *
 * Java <---> Js 调度中心
 */
public class InvokeController {
    private static final String TAG = "InvokeController";
    private static final String _js_script_file = "dsbridge.js";

    public static final String KEY_BUNDLE = "key_bundle";

    private boolean isDebug = BuildConfig.DEBUG;
    private PageLoadListener pageLoadListener;

    private WeakReference<Context> context;
    private WeakReference<Activity> activity;
    private ZWebView webView;

    private NativeAPI nativeAPI;
    private CommonAPI commonAPI;
    private DebugAPI debugAPI;

    public interface PageLoadListener {
        void onPageFinished(String url);
        void onPageError(String reason);
    }


    /**
     * 自定义API 请在 {@#link #setActivity(activity) 方法之前调用}
     * 使用方式参考</br>：
     * <code>
     *     public class TestAPI extends BaseAPI {
     *     public TestAPI(Activity activity) {
     *         super(activity);
     *     }
     *
     *     @Override
     *     protected String getTAG() {
     *         return "TestAPI";
     *     }
     *
     *     @JavascriptInterface
     *     public void testAPI(Object object) {
     *         Log.i(getTAG(), "test api");
     *     }
     *
     * }
     * </code>
     * */
    public void addAPI(BaseAPI api, String nameSpace) {
        if (null != webView) {
            webView.addJavascriptObject(api, nameSpace);
        }
    }

    public void setPageLoadListener(PageLoadListener pageLoadListener) {
        this.pageLoadListener = pageLoadListener;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    private void initAPI(Activity activity) {
        nativeAPI = new NativeAPI(activity);
        commonAPI = new CommonAPI(activity);
        commonAPI.setWebView(webView);
        debugAPI = new DebugAPI(activity);

        injectAPI();//注入API
    }

    public void onSaveInstanceState(Bundle outState) {
        nativeAPI.onSaveInstanceState(outState);
    }

    /** 初始化并注入交互API*/
    public void onCreate(Activity activity, @NonNull ZWebView webView) {
        this.context = new WeakReference<>(activity.getApplicationContext());
        this.activity = new WeakReference<>(activity);

        this.webView = webView;

        MessageController.get().setDebug(isDebug);

        if (isHasScript()) {//检测初始化状态
            if (isDebug)
                DWebView.setWebContentsDebuggingEnabled(true);

            initWebView();
            initAPI(this.activity.get());
        } else {
            Log.e(TAG, "The file 'dsbridge.js' not exist");
        }
    }

    private void initWebView() {
        webView.setWebViewClient(new ZWebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (pageLoadListener != null)
                    pageLoadListener.onPageFinished(url);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (pageLoadListener != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        pageLoadListener.onPageError(errorResponse.getReasonPhrase());
                    else
                        pageLoadListener.onPageError("onReceivedHttpError");
                }

            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (pageLoadListener != null)
                    pageLoadListener.onPageError(description);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (pageLoadListener != null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        pageLoadListener.onPageError(error.getDescription().toString());
                    else
                        pageLoadListener.onPageError("onReceivedError");
            }
        });
    }

    /** 注入JS调用native方法*/
    private void injectAPI() {
        if (null != webView) {
            webView.addJavascriptObject(nativeAPI, NameSpace.NATIVE.getValue());
            webView.addJavascriptObject(commonAPI, NameSpace.COMMON.getValue());
            webView.addJavascriptObject(debugAPI, NameSpace.DEBUG.getValue());
        }
    }

    /** 加载 web view*/
    public void load(String url) {
        if (null != webView && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url);
        }
    }

    public void load(String url, Map<String, String> additionalHttpHeaders) {
        if (null != webView && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url, additionalHttpHeaders);
        }
    }

    private void onError(String msg) {
        throw new UnsupportedOperationException(msg);
    }

    private boolean isHasScript() {
        try {
            String[] fileNames = context.get().getResources().getAssets().list("");
            if (fileNames != null) {
                for (String s: fileNames) {
                    if (_js_script_file.equals(s))
                        return true;
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }


    /** 注册USB状态监听广播*/
    private void USBMonitor() {
        //nothing

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//        try {
//            context.registerReceiver(mUsbStateReceiver, filter);
//        } catch (Exception e) {
//            LogPrinter.t(TAG).e(e, "");
//        }
    }

    /** 注册安装包安装是否成功广播*/
    private void APKInstallMonitor() {
        //nothing

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
//        try {
//            context.registerReceiver(mAppInstallReceiver, filter);
//        } catch (Exception e) {
//            LogPrinter.t(TAG).e(e, e.getMessage());
//        }
    }

    //////////////////////////////// API //////////////////////////////
    /** 相关销毁处理*/
    public void onDestroy() {
        if (webView != null && webView.isActivated()) {
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        nativeAPI.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        nativeAPI.onActivityResult(requestCode, resultCode, data);
        commonAPI.onActivityResult(requestCode, resultCode, data);
    }

    public void onBackPressed() {
        if (null != webView) {

            Message<Boolean> message = new Message();
            message.setKey(MessageController.KEY_EVENT_ON_BACK);
            message.setValue(true);
            MessageController.get().with(webView).sendMessage(message);
//            if (webView.canGoBack()) {
//                webView.goBack();
//            } else {
//                activity.finish();
//            }

            if (!webView.canGoBack() && null != activity)
                activity.get().finish();
        }
    }

    public void setSPFileName(String fileName) {
        SPUtil.setFileName(fileName);
    }

    //////////////////////// get instance ////////////////////////
    private InvokeController() {}

    private static class $ {
        private static InvokeController $$ = new InvokeController();
    }

    public static InvokeController get() {
        return $.$$;
    }
    //////////////////////// get instance ////////////////////////
}
