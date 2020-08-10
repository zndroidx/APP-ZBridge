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
import com.zndroid.bridge.api.NameSpace;
import com.zndroid.bridge.api.impl.CommonAPI;
import com.zndroid.bridge.api.impl.DebugAPI;
import com.zndroid.bridge.api.impl.NativeAPI;
import com.zndroid.bridge.framework.ZWebView;
import com.zndroid.bridge.framework.ZWebViewClient;
import com.zndroid.bridge.framework.core.CompletionHandler;
import com.zndroid.bridge.framework.core.DWebView;
import com.zndroid.bridge.params.Message;
import com.zndroid.bridge.permission.PermissionFail;
import com.zndroid.bridge.permission.PermissionHelper;
import com.zndroid.bridge.permission.PermissionSucceed;
import com.zndroid.bridge.permission.PermissionUtils;
import com.zndroid.bridge.util.SPUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
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

    /** 获取页面传值，当调用这两个方法时 {@link CommonAPI#openActivity(Object)} or {@link CommonAPI#openActivityForResult(Object, CompletionHandler)}
     * @deprecated 建议采用 {@link #addAPI(BaseAPI, String)} 自定义API的方式实现*/
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

    private LinkedHashMap<BaseAPI, String> apiMaps = new LinkedHashMap<>();
    
    public interface PageLoadListener {
        void onPageStart(String url);
        void onPageFinished(String url);
        void onPageError(String reason);
    }

    public interface InvokePermissionListener {
        void onPermissionGranted();
        void onPermissionRefused(List<String> deniedPermissions);
    }

    //////////////////////////////// API //////////////////////////////
    /** 设置页面加载监听*/
    public void setPageLoadListener(PageLoadListener pageLoadListener) {
        this.pageLoadListener = pageLoadListener;
    }

    /** 设置权限监听*/
    public void setInvokePermissionListener(InvokePermissionListener invokeInitListener) {
        this.invokePermissionListener = invokeInitListener;
    }

    /** 设置调试模式 发布版默认 false*/
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    /** 开发环境？*/
    public boolean isDebug() {
        return isDebug;
    }

    /** 添加自定义 API，用于宿主APP业务逻辑，请在 {@link #onCreate(Activity, Bundle, ZWebView)} 方法之前调用*/
    public void addAPI(BaseAPI api, String nameSpace) {
        apiMaps.put(api, nameSpace);
    }

    /** 初始化并注入API*/
    public void onCreate(Activity activity, Bundle savedInstanceState, @NonNull ZWebView webView) {
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
            onCreate(savedInstanceState);
        } else {
            Log.e(TAG, "The file 'dsbridge.js' not exist");
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

    public Context getContext() {
        return context.get();
    }

    public Activity getActivity() {
        return activity.get();
    }

    /**
     * 提供可以设置SharedPreferences为宿主APPSharedPreferences文件的方法
     *
     * @deprecated {建议使用自定义API处理上层业务逻辑 @link #addAPI(BaseAPI, String)}
     * */
    public void setSharedPreferencesFileName(String fileName) {
        SPUtil.setFileName(fileName);
    }
    //////////////////////////////// API //////////////////////////////

    /** 初始化API*/
    private void initAPI(Activity activity) {
        nativeAPI = new NativeAPI(activity);
        commonAPI = new CommonAPI(activity);
        commonAPI.setWebView(webView);
        debugAPI = new DebugAPI(activity);

        apiMaps.put(nativeAPI, NameSpace.NATIVE.getValue());
        apiMaps.put(commonAPI, NameSpace.COMMON.getValue());
        apiMaps.put(debugAPI, NameSpace.DEBUG.getValue());

        injectAPI();//注入API
    }

    /** 初始化 web_view*/
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
            for (Map.Entry<BaseAPI, String> api : apiMaps.entrySet()) {
                webView.addJavascriptObject(api.getKey(), api.getValue());
            }
        }
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

    /** 检查脚本文件是否存在*/
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

    //////////////////////// get instance ////////////////////////
    private InvokeController() {}

    private static class $ {
        private static InvokeController $$ = new InvokeController();
    }

    public static InvokeController get() {
        return $.$$;
    }
    //////////////////////// get instance ////////////////////////


    //////////////////////// 生命周期处理 ////////////////////////
    private void onCreate(Bundle savedInstanceState) {
        if (apiMaps != null) {
            for (BaseAPI baseAPI : apiMaps.keySet()) {
                baseAPI.onCreate(savedInstanceState);
            }
        }
    }

    public void onStart() {
        if (apiMaps != null) {
            for (BaseAPI baseAPI : apiMaps.keySet()) {
                baseAPI.onStart();
            }
        }
    }

    public void onResume() {
        if (apiMaps != null) {
            for (BaseAPI baseAPI : apiMaps.keySet()) {
                baseAPI.onResume();
            }
        }
    }

    public void onStop() {
        if (apiMaps != null) {
            for (BaseAPI baseAPI : apiMaps.keySet()) {
                baseAPI.onStop();
            }
        }
    }

    public void onPause() {
        if (apiMaps != null) {
            for (BaseAPI baseAPI : apiMaps.keySet()) {
                baseAPI.onPause();
            }
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (apiMaps != null) {
            for (BaseAPI baseAPI : apiMaps.keySet()) {
                baseAPI.onSaveInstanceState(outState);
            }
        }
    }

    /** 相关销毁处理*/
    public void onDestroy() {
        if (apiMaps != null) {
            for(BaseAPI api : apiMaps.keySet()) {
                api.onDestroy();
            }
        }

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

        if (apiMaps != null) {
            for(BaseAPI api : apiMaps.keySet()) {
                api.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (apiMaps != null) {
            for(BaseAPI api : apiMaps.keySet()) {
                api.onActivityResult(requestCode, resultCode, data);
            }
        }
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
}
