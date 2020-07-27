package com.zndroid.bridge.util;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.zndroid.bridge.permission.PermissionUtils;

public class DeviceUtils {

    private static String mac;
    private static final String TAG = "DeviceUtils";

    public static String getMac(Context context) {
        if (mac == null) {
            mac = getMacAddress(context);
        }

        return mac;
    }

    @Nullable
    @SuppressLint({"HardwareIds", "WifiManagerPotentialLeak"})
    public static String getMacAddress(final Context context) {
        if (PermissionUtils.hasPermission(context, Manifest.permission.INTERNET)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    List<NetworkInterface> ifs = Collections.list(NetworkInterface.getNetworkInterfaces());
                    for (NetworkInterface nif : ifs) {
                        if (nif.getName().equalsIgnoreCase("wlan0")) {
                            byte[] macBytes = nif.getHardwareAddress();
                            if (macBytes == null) {
                                return null;
                            }

                            StringBuilder res1 = new StringBuilder();
                            for (byte b : macBytes) {
                                res1.append(String.format("%02x", b)).append(":");
                            }

                            if (res1.length() > 0) {
                                res1.deleteCharAt(res1.length() - 1);
                            }
                            return res1.toString().toLowerCase();
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "");
                }
            } else {
                if (PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                        && PermissionUtils.hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
                    try {
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        if (wifiManager == null) {
                            return null;
                        }
                        WifiInfo info = wifiManager.getConnectionInfo();
                        return info.getMacAddress().toLowerCase();
                    } catch (Throwable t) {
                        Log.e(TAG, "");
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    @SuppressLint({"HardwareIds", "MissingPermission"})
    public static String getDeviceId(final Context context) {
        if (PermissionUtils.hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (manager == null) {
                return null;
            }

            if (Build.VERSION.SDK_INT >= 29) {
                String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                return deviceId.toLowerCase();
            } else {
                try {
                    String deviceId = manager.getDeviceId();
                    if (deviceId != null && deviceId.length() > 0 && !deviceId.contains("*") && countZero(deviceId) != deviceId.length()) {
                        return deviceId.toLowerCase();
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "");
                }
            }
        }
        return null;
    }

    @Nullable
    @SuppressLint({"HardwareIds", "MissingPermission"})
    public static String getSubscriberId(final Context context) {
        if (PermissionUtils.hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (Build.VERSION.SDK_INT >= 29) {

                String temp_imsi = (String) SPUtil.get(context, "_android_Q_IMSI", "");
                if (TextUtils.isEmpty(temp_imsi)) {
                    String uuid = UUID.randomUUID().toString();
                    SPUtil.save(context, "_android_Q_IMSI", uuid);
                    return uuid;
                } else {
                    return temp_imsi;
                }

            } else {
                try {
                    if (manager == null || manager.getSubscriberId() == null) {
                        return null;
                    }
                    return manager.getSubscriberId().toLowerCase();
                } catch (Throwable t) {
                    Log.e(TAG, t.getMessage());
                }
            }

        }
        return null;
    }

    public static String[] getDeviceIds(final Context context) {
        Set<String> result = new LinkedHashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String id1 = getPhoneInfo(0, "getDeviceId", context, String.class);
            String id2 = getPhoneInfo(1, "getDeviceId", context, String.class);
            if (!TextUtils.isEmpty(id1)) {
                result.add(id1.toLowerCase());
            }
            if (!TextUtils.isEmpty(id2)) {
                result.add(id2.toLowerCase());
            }
            if (result.size() == 0) {
                String id = getDeviceId(context);
                if (!TextUtils.isEmpty(id)) {
                    result.add(id.toLowerCase());
                }
            }
            return result.toArray(new String[result.size()]);
        } else {
            String id = getDeviceId(context);
            if (!TextUtils.isEmpty(id)) {
                result.add(id.toLowerCase());
            }
            return result.toArray(new String[result.size()]);
        }
    }

    public static String[] getSubscriberIds(final Context context) {
        Set<String> result = new LinkedHashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < 29) {
            String imsi1 = getPhoneInfo(0, "getSubscriberId", context, String.class);
            String imsi2 = getPhoneInfo(1, "getSubscriberId", context, String.class);
            if (!TextUtils.isEmpty(imsi1)) {
                result.add(imsi1.toLowerCase());
            }
            if (!TextUtils.isEmpty(imsi2)) {
                result.add(imsi2.toLowerCase());
            }
            if (result.size() == 0) {
                String imsi = getSubscriberId(context);
                if (!TextUtils.isEmpty(imsi)) {
                    result.add(imsi.toLowerCase());
                }
            }
            return result.toArray(new String[result.size()]);
        } else {
            String imsi = getSubscriberId(context);
            if (!TextUtils.isEmpty(imsi)) {
                result.add(imsi.toLowerCase());
            }
            return result.toArray(new String[result.size()]);
        }
    }

    @Nullable
    @SuppressLint("HardwareIds")
    public static String getAndroidId(final Context context) {
        try {
            String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (!TextUtils.isEmpty(id)) {
                return id.toLowerCase();
            }
        } catch (Throwable t) {
            Log.e(TAG, "");
        }
        return "";
    }

    private static <T> T getPhoneInfo(int subId, String methodName, Context context, Class<T> clz) {
        Object value = null;
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= 21) {
                Method method = tm.getClass().getMethod(methodName, getMethodParamTypes(methodName));
                if (subId >= 0) {
                    value = method.invoke(tm, subId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        try {
            return clz.cast(value);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public static Class[] getMethodParamTypes(String methodName) {
        Class[] params = null;
        try {
            Method[] methods = TelephonyManager.class.getDeclaredMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    params = method.getParameterTypes();
                    if (params.length >= 1) {
                        Log.d(TAG, "length: " + params.length);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return params;
    }

    private static int countZero(String string) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == '0') {
                count++;
            }
        }
        return count;
    }
}
