package com.zndroid.bridge.api;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by lazy on 2019-12-16
 */
public interface LifecycleCallBack {
    void onCreate(Bundle savedInstanceState);
    void onStart();
    void onResume();
    void onStop();
    void onPause();
    void onSaveInstanceState(Bundle outState);
    void onDestroy();

    void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);
}
