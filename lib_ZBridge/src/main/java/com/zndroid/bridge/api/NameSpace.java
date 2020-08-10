package com.zndroid.bridge.api;

/**
 * Created by lazy on 2019-09-21
 */
public enum NameSpace {
    //静态方法
    NATIVE("native"),
    //通用模块
    COMMON("common"),
    //测试接口
    DEBUG("debug");

    ////////////////////////////////////////////////
    private String value;

    NameSpace(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static NameSpace valueFor(String s) {
        for (NameSpace e: NameSpace.values()) {
            if (e.getValue().equals(s) )
                return e;
        }

        return null;
    }
    ////////////////////////////////////////////////
}
