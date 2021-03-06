package com.zndroid.bridge.permission;

import android.app.Activity;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.zndroid.bridge.InvokeController;

import java.util.List;

/**
 * Created by lazy on 2019/9/16 权限管理工具类(关键)
 */
public class PermissionHelper {

    // 需要反射的类
    private Object mObject;
    // 请求码
    private int mRequestCode;
    // 请求权限数组
    private String[] mRequestPermission;

    private PermissionHelper(Object object) {
        this.mObject = object;
    }

    // 传递参数如下：
    // Object or Fragment or Activity
    // int 请求码
    // 需要请求的权限 String[]

    /**
     * 请求权限
     *
     * @param activity
     * @param requestCode
     * @param permissions
     */
    public static void requestPermission(Activity activity, int requestCode, String[] permissions) {
        PermissionHelper.with(activity).requestCode(requestCode).requestPermission(permissions).request();
    }

    /**
     * 请求权限
     *
     * @param mObject
     * @param requestCode
     * @param permissions
     */
    public static void requestPermission(Object mObject, int requestCode, String[] permissions) {
        PermissionHelper.with(mObject).requestCode(requestCode).requestPermission(permissions).request();
    }

    /**
     * 请求权限
     *
     * @param fragment
     * @param requestCode
     * @param permissions
     */
    public static void requestPermission(Fragment fragment, int requestCode, String[] permissions) {
        PermissionHelper.with(fragment).requestCode(requestCode).requestPermission(permissions).request();
    }

    // 链式传参
    /**
     * 兼容
     *
     * @param object
     * @return
     */
    public static PermissionHelper with(Object object) {
        if (object instanceof InvokeController)
            return new PermissionHelper(object);
        if (object instanceof Activity)
            return with((Activity)object);
        if (object instanceof Fragment)
            return with((Fragment)object);
        return null;
    }

    /**
     * 兼容Activity
     *
     * @param activity
     * @return
     */
    public static PermissionHelper with(Activity activity) {
        return new PermissionHelper(activity);
    }

    /**
     * 兼容 Fragment
     *
     * @param fragment
     * @return
     */
    public static PermissionHelper with(Fragment fragment) {
        return new PermissionHelper(fragment);
    }

    /**
     * 添加请求码
     *
     * @param requestCode
     * @return
     */
    public PermissionHelper requestCode(int requestCode) {
        this.mRequestCode = requestCode;
        return this;
    }

    /**
     * 添加请求权限数组
     *
     * @param permissions
     * @return
     */
    public PermissionHelper requestPermission(String... permissions) {
        this.mRequestPermission = permissions;
        return this;
    }

    /**
     * 判断权限以及请求权限
     */
    public void request() {
        // 判断当前系统版本是否大于等于6.0
        if (!PermissionUtils.isOverMarshmallow()) {
            // 小于6.0 直接执行方法 通过反射去获取
            // 对执行的方法不确定 只能采用注解方式给方法设置Tag 通过反射去执行
            PermissionUtils.executeSucceedMethod(mObject, mRequestCode);
            return;
        }
        // 大于等于6.0 判断权限是否授予
        // 获取用户拒绝的权限 检测权限
        List<String> deniedPermissions = PermissionUtils.getDeniedPermissions(mObject, mRequestPermission);
        // 权限被授予 反射获取执行方法
        if (deniedPermissions.size() == 0) {
            // 用户授予请求权限
            PermissionUtils.executeSucceedMethod(mObject, mRequestCode);
        } else {
            // 权限被拒绝 申请权限
            ActivityCompat.requestPermissions(PermissionUtils.getActivity(mObject), deniedPermissions.toArray(new String[deniedPermissions.size()]), mRequestCode);
        }
    }

    /**
     * 处理权限回调
     */
    public static void requestPermissionsResult(Object object, int requestCode, String[] permissions) {
        // 再次获取用户拒绝的权限
        List<String> deniedPermissions = PermissionUtils.getDeniedPermissions(object, permissions);
        if (deniedPermissions.size() == 0) {
            // 用户已授权申请的权限
            PermissionUtils.executeSucceedMethod(object, requestCode);
        } else {
            // 申请的权限中 有用户不同意的
            PermissionUtils.executeFailMethod(object, requestCode);
        }
    }

}
