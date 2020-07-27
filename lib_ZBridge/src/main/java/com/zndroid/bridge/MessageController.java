package com.zndroid.bridge;

import android.util.Log;

import com.zndroid.bridge.framework.ZWebView;
import com.zndroid.bridge.framework.core.OnReturnValue;
import com.zndroid.bridge.params.Message;
import com.zndroid.bridge.util.JsonTranslator;

/**
 * Created by lazy on 2019-09-21
 */
public class MessageController {
    private final String TAG = "MessageController";

    private boolean isDebug = BuildConfig.DEBUG;
    private final String METHOD = "listener";

    public static final String KEY_USB_STATUE = "usb_statue";
    public static final String KEY_DRIVER_STATUE = "driver_statue";
    public static final String KEY_SNIFFER_MONITOR = "sniffer_listener";
    public static final String KEY_APK_INSTALL_STATUE = "apk_install_statue";
    public static final String KEY_DOWNLOAD_APK_STATUE = "download_apk_statue";
    public static final String KEY_EVENT_ON_BACK = "key_event_on_back";

    private ZWebView webView;
    public MessageController with(ZWebView webView) {
        this.webView = webView;
        return this;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public void sendMessage(Message message) {
        sendMessage(message, null);
    }

    public void sendMessage(Message message, OnReturnValue<String> callBackValue) {
        if (null != webView && null != message) {
            String arg = JsonTranslator.instance().ObjectToJsonString(message);

            if (isDebug && null != arg)
                Log.i(TAG, arg);

            webView.callHandler(METHOD, new String[]{arg}, callBackValue);
        }
    }

    //////////////////////// get instance ////////////////////////
    private MessageController() {}

    private static class $ {
        private static MessageController $$ = new MessageController();
    }

    public static MessageController get() {
        return $.$$;
    }
    //////////////////////// get instance ////////////////////////
}
