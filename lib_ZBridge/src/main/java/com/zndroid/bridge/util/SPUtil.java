package com.zndroid.bridge.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lazy on 2019-09-23
 */
public class SPUtil {
    private static String FILE_NAME = "com_zndroid_sp";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    private static void save(Context context, String key, Object value, boolean rightNow) {
        if (value != null) {
            String type = value.getClass().getSimpleName();
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            switch (type) {
                case "String":
                    editor.putString(key, (String) value);
                    break;
                case "Integer":
                    editor.putInt(key, (Integer) value);
                    break;
                case "Boolean":
                    editor.putBoolean(key, (Boolean) value);
                    break;
                case "Float":
                    editor.putFloat(key, (Float) value);
                    break;
                case "Long":
                    editor.putLong(key, (Long) value);
                    break;
            }

            if (rightNow) {
                editor.commit();
            } else {
                editor.apply();
            }
        }
    }

    public static void save(Context context, String key, Object value) {
        save(context, key, value, false);
    }

    public static void saveNow(Context context, String key, Object value) {
        save(context, key, value, true);
    }

    public static Object get(Context context, String key, Object defaultObject) {
        String type = defaultObject.getClass().getSimpleName();
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        switch (type) {
            case "String":
                return sp.getString(key, (String) defaultObject);
            case "Integer":
                return sp.getInt(key, (Integer) defaultObject);
            case "Boolean":
                return sp.getBoolean(key, (Boolean) defaultObject);
            case "Float":
                return sp.getFloat(key, (Float) defaultObject);
            case "Long":
                return sp.getLong(key, (Long) defaultObject);
        }

        return null;
    }
}
