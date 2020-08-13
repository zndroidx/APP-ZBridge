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
import android.widget.Toast;

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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by lazy on 2019-08-19
 *
 * Java <---> Js 调度中心
 */
public class InvokeController {
    private boolean isDebug = BuildConfig.DEBUG;
    private AtomicBoolean isLoadError;

    private static final String TAG = "InvokeController";
    private static final String _js_script_file = "dsbridge.js";
    private final int PERMISSION_REQUEST_CODE = 137;

    private String originUrl = "";
    private String errorUrl = "";

    /** 获取页面传值，当调用这两个方法时 {@link CommonAPI#openActivity(Object)} or {@link CommonAPI#openActivityForResult(Object, CompletionHandler)}
     * @deprecated 建议采用 {@link #addAPI(BaseAPI, String)} 自定义API的方式实现*/
    public static final String KEY_BUNDLE = "key_bundle";

    private PageLoadListener pageLoadListener;
    private InvokePermissionListener invokePermissionListener;//默认实现权限请求

    private WeakReference<Context> context;
    private WeakReference<Activity> activity;
    private Bundle savedInstanceState;
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
        this.savedInstanceState = savedInstanceState;
        this.webView = webView;
        this.isLoadError = new AtomicBoolean(false);

        this.webView.disableJavascriptDialogBlock(isDebug);
        DWebView.setWebContentsDebuggingEnabled(isDebug);
        MessageController.get().setDebug(isDebug);

        if (isHasScript()) {//检测初始化状态
            /** init web view*/
            initWebView();
            /** init api*/
            initAPI(this.activity.get());
            /** request permission*/
            PermissionHelper.requestPermission(this, PERMISSION_REQUEST_CODE, permissions);
        } else {
            Log.e(TAG, "The file 'dsbridge.js' not exist");
            Toast.makeText(activity, "The file 'dsbridge.js' not exist", Toast.LENGTH_LONG).show();
        }
    }

    /** 加载 web view*/
    public void load(String url) {
        this.isLoadError.set(false);
        this.originUrl = url;//save origin url for reload...
        if (null != webView && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url);
        }
    }

    public void load(String url, Map<String, String> additionalHttpHeaders) {
        this.isLoadError.set(false);
        this.originUrl = url;//save origin url for reload...
        if (null != webView && !TextUtils.isEmpty(url)) {
            webView.loadUrl(url, additionalHttpHeaders);webView.reload();
        }
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public Context getContext() {
        return context.get();
    }

    public Activity getActivity() {
        return activity.get();
    }

    /**
     * 设置加载失败显示的网页地址 暂不开放，逻辑处理较繁琐，
     * <b>墙裂建议使用静态网页</b>
     * 比如：用户的加载失败页面如果不是静态页面可能会加载失败，比较容易陷入死循环
     * @future
     * */
    private void setErrorUrl(String errorUrl) {
        this.errorUrl = errorUrl;
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

    private void showErrorPage(WebView view, String msg) {
        String errorLocalHtml = "file:///android_asset/zndroid/bridge_error/index.html";
        if (!isLoadError.getAndSet(true)) {
            view.loadUrl(TextUtils.isEmpty(errorUrl) ? errorLocalHtml : errorUrl);
            if (pageLoadListener != null) {
                pageLoadListener.onPageError(msg);
            }
        }
    }

    private boolean isAPI21() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private boolean isAPI23() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /** 初始化 web_view*/
    private void initWebView() {
        webView.setWebViewClient(new ZWebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (pageLoadListener != null && !isLoadError.get())//如果加载失败，不会透传该回调
                    pageLoadListener.onPageStart(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (pageLoadListener != null && !isLoadError.get())//如果加载失败，不会透传该回调
                    pageLoadListener.onPageFinished(url);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                showErrorPage(view, isAPI21() ? errorResponse.getReasonPhrase() : "onReceivedHttpError");
                if (isDebug)
                    Log.i(TAG, isAPI21() ? errorResponse.getReasonPhrase() : "onReceivedHttpError");
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showErrorPage(view, description);
                if (isDebug)
                    Log.i(TAG, description);

            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                showErrorPage(view, isAPI23() ? error.getDescription().toString() : "onReceivedError");

                if (isDebug)
                    Log.i(TAG, isAPI23() ? error.getDescription().toString() : "onReceivedError");
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
        onCreate(savedInstanceState);

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
        //reset all
        isLoadError.set(false);

        if (apiMaps != null) {
            for(BaseAPI api : apiMaps.keySet()) {
                api.onDestroy();
            }

            apiMaps.clear();
        }

        if (webView != null && webView.isActivated()) {
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }

        System.gc();
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
