package com.app.bridge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.zndroid.bridge.InvokeController;
import com.zndroid.bridge.framework.ZWebView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String URL = "file:///android_asset/zndroid/test.html";
//    private final static String URL = "http://www.baidu.com";

    private ZWebView webView;
    private InvokeController invokeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);

        invokeController = new InvokeController();

        //可选
        invokeController.setDebug(true);
        //可选 根据宿主业务逻辑处理自定义API
        CustomAPI customAPI = new CustomAPI(this);
        customAPI.setWebView(webView);
        invokeController.addAPI(customAPI, "test");

        //可选
        invokeController.setPageLoadListener(new InvokeController.PageLoadListener() {
            @Override
            public void onPageStart(String url) {
                Log.i("bridge", "start=" + url);
            }

            @Override
            public void onPageFinished(String url) {
                Log.i("bridge", "finish=" + url);
            }

            @Override
            public void onPageError(String reason) {
                Log.i("bridge", "error=" + reason);
            }
        });

        //可选（先设置回调，再请求权限，否则因为多线程的原因导致回调时机错乱从而loadURL无效）
        invokeController.setInvokePermissionListener(new InvokeController.InvokePermissionListener() {
            @Override
            public void onPermissionGranted() {
                //must
                invokeController.load(URL);
            }

            @Override
            public void onPermissionRefused(List<String> deniedPermissions) {
                PermissionDialog dialog = new PermissionDialog();
                dialog.show(getSupportFragmentManager(), "permission");
            }
        });

        //must
        invokeController.onCreate(this, savedInstanceState, webView);
    }

    @Override
    protected void onDestroy() {
        //must
        invokeController.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //must
        invokeController.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        //must
        invokeController.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        //must
        invokeController.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //must
        invokeController.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        invokeController.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        invokeController.onPause();
    }
}