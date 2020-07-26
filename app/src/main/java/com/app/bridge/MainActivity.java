package com.app.bridge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.zndroid.bridge.InvokeController;
import com.zndroid.bridge.framework.ZWebView;

public class MainActivity extends AppCompatActivity {

    private final static String URL = "file:///android_asset/dist/test.html";

    private ZWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);

        InvokeController.get().onCreate(this, webView);
        InvokeController.get().setPageLoadListener(new InvokeController.PageLoadListener() {
            @Override
            public void onPageFinished(String url) {

            }

            @Override
            public void onPageError(String reason) {

            }
        });
        InvokeController.get().load(URL);
    }

    @Override
    protected void onDestroy() {
        InvokeController.get().onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        InvokeController.get().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        InvokeController.get().onBackPressed();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        InvokeController.get().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }
}