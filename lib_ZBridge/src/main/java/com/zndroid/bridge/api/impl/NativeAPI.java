package com.zndroid.bridge.api.impl;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.alibaba.fastjson.JSONObject;
import com.zndroid.bridge.api.BaseAPI;
import com.zndroid.bridge.framework.core.CompletionHandler;
import com.zndroid.bridge.permission.PermissionUtils;
import com.zndroid.bridge.util.Cleaner;
import com.zndroid.bridge.util.DeviceUtils;
import com.zndroid.bridge.util.JsonTranslator;
import com.zndroid.bridge.util.SPUtil;
import com.zndroid.bridge.util.StringUtils;

import java.util.List;


/**
 * Created by lazy on 2019-09-21
 */
public class NativeAPI extends BaseAPI {
    private static final String TAG = "NativeAPI";

    private CompletionHandler<String> callBackHandler;

    public NativeAPI(Activity activity) {
        super(activity);
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    private String parseString(String string) {
        return "\"" + string + "\"";
    }

    @JavascriptInterface
    public String getMac(Object object) {
        try {
            String mac = parseString(DeviceUtils.getMac(context));
            showLog("getMac = " + mac);
            return mac;
        } catch (Exception e) {
            showToast(e.getMessage());
        }

        return "";
    }

    @JavascriptInterface
    public String getIMEI(Object object) {
        try {
            String[] imeis = DeviceUtils.getDeviceIds(context);
            if (imeis.length == 0) {
                return "";
            }
            String imei = parseString(StringUtils.join(",", imeis));
            showLog("getIMEI = " + imei);
            return imei;
        } catch (Exception e) {
            showToast(e.getMessage());
        }

        return "";
    }

    @JavascriptInterface
    public String getIMSI(Object object) {
        try {
            String[] imsis = DeviceUtils.getSubscriberIds(context);
            if (imsis.length == 0) {
                return "";
            }
            String imsi = parseString(StringUtils.join(",", imsis));
            showLog("getIMSI = " + imsi);
            return imsi;
        } catch (Exception e) {
            showToast(e.getMessage());
        }

        return "";
    }

    private PackageInfo getPackageInfo() {
        PackageManager pm = context.getPackageManager();

        try {
            return pm.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    @JavascriptInterface
    public int getVersionCode(Object object) {
        try {
            int versionCode = null == getPackageInfo() ? -1 : getPackageInfo().versionCode;
            showLog("getVersionCode = " + versionCode);
            return versionCode;
        } catch (Exception e) {
            showToast(e.getMessage());
        }

        return -1;
    }

    @JavascriptInterface
    public String getPackageName(Object object) {
        try {
            String packageName = parseString(context.getPackageName());
            showLog("getPackageName = " + packageName);
            return packageName;
        } catch (Exception e) {
            showToast(e.getMessage());
        }

        return null;
    }

    @JavascriptInterface
    public String getVersionName(Object object) {
        try {
            String versionName = null == getPackageInfo() ? parseString("") : parseString(getPackageInfo().versionName);
            showLog("getVersionName = " + versionName);
            versionName = "1.0";
            return versionName;
        } catch (Exception e) {
            showToast(e.getMessage());
        }

        return null;
    }

    @JavascriptInterface
    public boolean isGPSOpen(Object object) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSOpen = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        showLog("isGPSOpened = " + isGPSOpen);
        return isGPSOpen;
    }

    @JavascriptInterface
    public void openOrCloseGPS(Object object) {
        showLog("openOrCloseGPS");

        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (null != activity)
            activity.startActivity(settingsIntent);
    }

    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public void callPhone(Object object) {
        if (null == object
                || TextUtils.isEmpty(object.toString())
                || object.toString().equals("null")) {
            showToast("phone number is null or empty");
            return;
        }

        if (PermissionUtils.hasPermission(context, Manifest.permission.CALL_PHONE)) {
            String phoneNum = object.toString();
            showLog("callPhone = " + phoneNum);

            Intent intent = new Intent(Intent.ACTION_CALL);
            Uri data = Uri.parse("tel:" + phoneNum);
            intent.setData(data);

            if (null != activity)
                activity.startActivity(intent);
        } else {
            showToast("please access permission of 'call_phone'");
        }
    }

    @JavascriptInterface
    public void sendSMS(Object object) {
        if (null == object
                || TextUtils.isEmpty(object.toString())
                || object.toString().equals("null")) {
            showToast("content is null or empty");
            return;
        }

        JSONObject jsonObject = JsonTranslator.instance().jsonStringToJsonObject(object.toString());

        showLog("sendSMS = " + object.toString());

        if (PermissionUtils.hasPermission(context, Manifest.permission.SEND_SMS)) {
            if (jsonObject.containsKey("pn") && jsonObject.containsKey("message")) {
                String phoneNumber = jsonObject.getString("pn");
                String message = jsonObject.getString("message");

                SmsManager smsManager = SmsManager.getDefault();
                List<String> divideContents = smsManager.divideMessage(message);
                for (String text : divideContents) {
                    smsManager.sendTextMessage(phoneNumber, null, text, null, null);
                }
            } else {
                showToast("params pn or message not exists");
            }
        } else {
            showToast("please access permission of 'send_SMS'");
        }
    }


    @JavascriptInterface
    public String getAppCacheSize(Object object) {
        showLog("getAppCacheSize");
        return parseString(Cleaner.getAppCacheSizeFormat(context));
    }

    @JavascriptInterface
    public void clearAppCache(Object object) {
        showLog("clearAppCache");
        Cleaner.cleanAppData(context);
    }

    @JavascriptInterface
    public void restartApp(Object object) {
        showLog("restartApp");
        Intent intent = context.getApplicationContext().getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (null != intent) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent restartIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC, 500, restartIntent);

            System.exit(0);
        }
    }

    @JavascriptInterface
    public void saveLocalData(Object object) {
        if (null == object) {
            showToast("object is null");
            return;
        }

        showLog("saveLocalData = " + object.toString());

        JSONObject jsonObject = JsonTranslator.instance().jsonStringToJsonObject(object.toString());

        if (jsonObject.containsKey("key") && jsonObject.containsKey("value")) {
            String key = jsonObject.getString("key");
            Object value = jsonObject.get("value");

            SPUtil.save(context, key, value);
        } else {
            showToast("params key or value not exists");
        }
    }

    @JavascriptInterface
    public Object getLocalData(Object object) {
        showLog("getLocalData");

        if (null == object) {
            showToast("object is null");
            return null;
        }

        showLog(object.toString());

        JSONObject jsonObject = JsonTranslator.instance().jsonStringToJsonObject(object.toString());

        if (jsonObject.containsKey("key") && jsonObject.containsKey("default_value")) {
            String key = jsonObject.getString("key");
            Object def_value = jsonObject.get("default_value");

            if (null == def_value) {
                showToast("default_value is null");
                return null;
            }

            return SPUtil.get(context, key, def_value);
        }

        return null;
    }

    /**
     * [当前时间，一年内]
     * */
    @JavascriptInterface
    public void getDatePick(Object object, final CompletionHandler callBackHandler) {
        showLog("getDatePick");
    }

    /**
     * [过去一年内，当前时间]
     * */
    @JavascriptInterface
    public void getDatePickPast(Object object, final CompletionHandler callBackHandler) {
        showLog("getDatePickPast");
    }

    @JavascriptInterface
    public void getTimePick(Object object, final CompletionHandler callBackHandler) {
        showLog("getDatePick");
    }
}
