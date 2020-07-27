package com.zndroid.bridge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.zndroid.bridge.permission.PermissionFail;
import com.zndroid.bridge.permission.PermissionHelper;
import com.zndroid.bridge.permission.PermissionSucceed;
import com.zndroid.bridge.permission.PermissionUtils;
import com.zndroid.bridge.util.SPUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * Created by lazy on 2019-08-19
 *
 * Java <---> Js 调度中心
 */
public class InvokeController {
    private boolean isDebug = BuildConfig.DEBUG;

    private static final String TAG = "InvokeController";
    private static final String _js_script_file = "dsbridge.js";
    private final int PERMISSION_REQUEST_CODE = 137;

    public static final String KEY_BUNDLE = "key_bundle";

    private PageLoadListener pageLoadListener;
    private InvokePermissionListener invokePermissionListener;//默认实现权限请求

    private WeakReference<Context> context;
    private WeakReference<Activity> activity;
    private ZWebView webView;

    private NativeAPI nativeAPI;
    private CommonAPI commonAPI;
    private DebugAPI debugAPI;

    private String[] permissions = new String[] {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public interface PageLoadListener {
        void onPageStart(String url);
        void onPageFinished(String url);
        void onPageError(String reason);
    }

    public interface InvokePermissionListener {
        void onPermissionGranted();
        void onPermissionRefused(List<String> deniedPermissions);
    }

    public void addAPI(BaseAPI api, String nameSpace) {
        if (null != webView) {
            webView.addJavascriptObject(api, nameSpace);
        }
    }

    public void setPageLoadListener(PageLoadListener pageLoadListener) {
        this.pageLoadListener = pageLoadListener;
    }

    public void setInvokePermissionListener(InvokePermissionListener invokeInitListener) {
        this.invokePermissionListener = invokeInitListener;
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

        PermissionHelper.requestPermission(this, PERMISSION_REQUEST_CODE, permissions);

        MessageController.get().setDebug(isDebug);
        this.webView.disableJavascriptDialogBlock(isDebug);

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
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (pageLoadListener != null)
                    pageLoadListener.onPageStart(url);
            }

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

    @PermissionSucceed(requestCode = PERMISSION_REQUEST_CODE)
    public void permissionOK() {
        if (isDebug)
            Log.i(TAG, "permission all is ok...");
        if (null != invokePermissionListener)
            invokePermissionListener.onPermissionGranted();
    }

    @PermissionFail(requestCode = PERMISSION_REQUEST_CODE)
    public void permissionFailed() {
        if (isDebug)
            Log.i(TAG, "permission has deny...");
        if (null != invokePermissionListener)
            invokePermissionListener.onPermissionRefused(PermissionUtils.getDeniedPermissions(this, permissions));
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

    private boolean isHasScript() {
        try {
            String[] fileNames = context.get().getResources().getAssets().list("zndroid");
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

    public Context getContext() {
        return context.get();
    }

    public Activity getActivity() {
        return activity.get();
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
        PermissionHelper.requestPermissionsResult(this, requestCode, permissions);
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

            if (!webView.canGoBack() && null != activity)
                activity.get().finish();
        }
    }

    /**
     * 提供可以设置SharedPreferences为宿主APPSharedPreferences文件的方法
     * */
    public void setSharedPreferencesFileName(String fileName) {
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
