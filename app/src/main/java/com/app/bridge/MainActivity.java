package com.app.bridge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.zndroid.bridge.InvokeController;
import com.zndroid.bridge.framework.ZWebView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String URL = "file:///android_asset/zndroid/test.html";

    private ZWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);

        //可选
        InvokeController.get().setDebug(true);
        //可选 根据宿主业务逻辑处理自定义API
        CustomAPI customAPI = new CustomAPI(this);
        InvokeController.get().addAPI(customAPI, "test");

        //可选
        InvokeController.get().setPageLoadListener(new InvokeController.PageLoadListener() {
            @Override
            public void onPageStart(String url) {

            }

            @Override
            public void onPageFinished(String url) {

            }

            @Override
            public void onPageError(String reason) {

            }
        });

        //可选（先设置回调，再请求权限，否则因为多线程的原因导致回调时机错乱从而loadURL无效）
        InvokeController.get().setInvokePermissionListener(new InvokeController.InvokePermissionListener() {
            @Override
            public void onPermissionGranted() {
                //must
                InvokeController.get().load(URL);
            }

            @Override
            public void onPermissionRefused(List<String> deniedPermissions) {
                PermissionDialog dialog = new PermissionDialog();
                dialog.show(getSupportFragmentManager(), "permission");
            }
        });

        //must
        InvokeController.get().onCreate(this, savedInstanceState, webView);
    }

    @Override
    protected void onDestroy() {
        //must
        InvokeController.get().onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //must
        InvokeController.get().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        //must
        InvokeController.get().onBackPressed();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        //must
        InvokeController.get().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //must
        InvokeController.get().onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        InvokeController.get().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        InvokeController.get().onPause();
    }
}