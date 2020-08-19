package com.zndroid.bridge.api.impl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.zndroid.bridge.InvokeController;
import com.zndroid.bridge.api.BaseAPI;
import com.zndroid.bridge.framework.ZWebView;
import com.zndroid.bridge.framework.core.CompletionHandler;
import com.zndroid.bridge.util.JsonTranslator;
import com.zndroid.bridge.util.SnackbarUtils;

import java.util.List;

import static com.zndroid.bridge.InvokeController.KEY_BUNDLE;

/**
 * Created by lazy on 2019-09-21
 */
public class CommonAPI extends BaseAPI {
    private String TAG = "CommonAPI";

    public final static int REQUEST_CODE = 207;
    public final static String REQUEST_DATA = "key_data";
    private String originUrl;
    private InvokeController invokeController;

    private CompletionHandler globalCompletionHandler;

    private ZWebView webView;

    public CommonAPI(Activity activity) {
        super(activity);
    }

    public void setWebView(ZWebView webView) {
        this.webView = webView;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public void setInvokeController(InvokeController invokeController) {
        this.invokeController = invokeController;
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    /** 判断指定包名是否安装
     *
     * @return boolean true = isInstalled, false not install
     * */
    @JavascriptInterface
    public boolean isInstall(Object object) {
        if (null == object || TextUtils.isEmpty(object.toString())) {
            showToast( "package name is null or empty");
            return false;
        }

        String packageName = object.toString();
        PackageManager manager = context.get().getPackageManager();
        List<PackageInfo> list = manager.getInstalledPackages(0);
        if (null != list) {
            for (PackageInfo info : list) {
                if (packageName.equals(info.packageName)) {
                    showLog("isInstall '" + packageName + "' = " + true);
                    return true;
                }
            }
        }

        showLog("isInstall '" + packageName + "' = " + false);
        return false;
    }

    /** 打开三方浏览器
     *
     * */
    @JavascriptInterface
    public void openBrowser(Object object) {
        showLog("openBrowser");

        if (null == object) {
            showToast("url is null");
            return;
        }

        try {
            String url = (String) object;

            showLog(url);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setData(uri);
            activity.get().startActivity(intent);

        } catch (Exception e) {
            showToast(e.getMessage());
        }

    }

    @JavascriptInterface
    public void showToast(Object object) {
        showLog("showToast");
        if (object instanceof String)
            showToast((String) object);
        else
            showToast(object.toString());
    }

    @JavascriptInterface
    public void showSnackBar(Object object, final CompletionHandler handler) {
        showLog("showSnackBar");
        if (null == object
                || TextUtils.isEmpty(object.toString())
                || object.toString().equals("null")) {
            showToast("content is null or empty");
            return;
        }

        if (null == webView) {
            showToast("webview is null, pls call function 'setWebView(xxx)' at CommonAPI");
            return;
        }

        int type = 0;
        String msg = "";
        String action = "";
        SnackbarUtils snackbar;

        try {
            JSONObject jsonObject = JsonTranslator.instance().jsonStringToJsonObject(object.toString());
            if (jsonObject.containsKey("type")) {
                type = jsonObject.getInteger("type");
            }

            if (jsonObject.containsKey("message")) {
                msg = jsonObject.getString("message");
            }

            if (jsonObject.containsKey("action")) {
                action = jsonObject.getString("action");
            }

            snackbar = SnackbarUtils.Short(webView, msg);
            if (type == 0) {
                snackbar = snackbar.info();
                snackbar.actionColor(context.get().getResources().getColor(android.R.color.white));//可能会动态化
            } else if (type == 1) {
                snackbar = snackbar.confirm();
                snackbar.actionColor(context.get().getResources().getColor(android.R.color.white));
            } else if (type == 2) {
                snackbar = snackbar.warning();
                snackbar.actionColor(context.get().getResources().getColor(android.R.color.white));
            } else if (type == 3) {
                snackbar = snackbar.danger();
                snackbar.actionColor(context.get().getResources().getColor(android.R.color.white));
            }
        } catch (Exception e) {
            msg = object.toString();
            snackbar = SnackbarUtils.Short(webView, msg).info();
        }

        if (null != handler) {
            snackbar.setAction(action, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.complete();
                }
            });
        }

        snackbar.show();
    }

    @JavascriptInterface
    public void activityFinish(Object object) {
        if (null != activity)
            activity.get().finish();
    }

    @JavascriptInterface
    public void openActivity(Object object) {
        showLog("openActivity");

        Intent intent = getIntent(object);
        if (null != intent)
            jumpToActivity(intent, false);
    }

    private Intent getIntent(Object object) {
        if (null == object) {
            showToast("object is null");
            return null;
        }

        showLog(object.toString());

        JSONObject jsonObject = JsonTranslator.instance().jsonStringToJsonObject(object.toString());

        String className = "";
        String value = "";
        if (jsonObject.containsKey("className"))
            className = jsonObject.getString("className");
        if (jsonObject.containsKey("value"))
            value = jsonObject.getString("value");

        try {
            Intent intent = new Intent(activity.get(), Class.forName(className));
            if (!TextUtils.isEmpty(value)) {
                Bundle bundle = new Bundle();
                bundle.putString(KEY_BUNDLE, value);

                intent.putExtras(bundle);
            }

            return intent;
        } catch (ClassNotFoundException e) {
            showToast(e.toString());
        }

        return null;
    }

    private void jumpToActivity(Intent intent, boolean hasBack) {
        if (hasBack)
            activity.get().startActivityForResult(intent, REQUEST_CODE);
        else
            activity.get().startActivity(intent);
    }

    @JavascriptInterface
    public void openActivityForResult(Object object, final CompletionHandler handler) {
        showLog("openActivityForResult");

        globalCompletionHandler = handler;

        Intent intent = getIntent(object);
        if (null != intent)
            jumpToActivity(intent, true);
    }

    @JavascriptInterface
    public void reload(Object object) {
        showLog("reload=" + originUrl);
        if (!TextUtils.isEmpty(originUrl) && null != invokeController)
            invokeController.load(originUrl);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        if (null == intent && null != globalCompletionHandler)
            globalCompletionHandler.complete("");

        if (requestCode == REQUEST_CODE
                && resultCode == Activity.RESULT_OK
                && globalCompletionHandler != null) {
            String data  = "";
            Bundle bundle = intent.getExtras();
            if (null != bundle)
                data = bundle.getString(KEY_BUNDLE);

            showLog(data);
            globalCompletionHandler.complete(data);
        }

    }
}
