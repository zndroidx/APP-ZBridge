package com.zndroid.bridge.params;

/**
 * Created by lazy on 2019-09-21
 */
public class Message<T> {
    private String key;

    private T value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
