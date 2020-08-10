package com.zndroid.bridge.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.zndroid.bridge.BuildConfig;
import com.zndroid.bridge.InvokeController;

import java.lang.ref.WeakReference;

/**
 * Created by lazy on 2019-09-21
 */
public abstract class BaseAPI implements LifecycleCallBack {
    private boolean isDebug = InvokeController.get().isDebug();
    protected WeakReference<Context> context;
    protected WeakReference<Activity> activity;

    public BaseAPI(Activity activity) {
        this.activity = new WeakReference<>(activity);
        this.context = new WeakReference<>(activity.getApplicationContext());
    }

    protected void showLog(String msg) {
        if (isDebug)
            Log.i(getTAG(), "==> " + msg);
    }

    protected void showToast(final String msg) {
        //不考虑debug，将异常抛出上层
        activity.get().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.get(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onPause() {

    }

    protected abstract String getTAG();
}
