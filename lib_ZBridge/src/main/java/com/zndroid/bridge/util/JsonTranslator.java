package com.zndroid.bridge.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by lazy on 2019-08-23
 *
 * JSON序列化（采用阿里FastJson）
 */
public class JsonTranslator {
    public <T> T jsonToObject(String jsonString, Class<T> cls) {
        return JSON.parseObject(jsonString, cls);
    }

    public JSONObject jsonStringToJsonObject(String jsonString) {
        return (JSONObject) JSONObject.parse(jsonString);
    }

    public String ObjectToJsonString(Object o) {
        return JSONObject.toJSONString(o);
    }


    //////////////////////// get instance ////////////////////////
    private JsonTranslator() {}
    private static class $ {
        private static JsonTranslator $$ = new JsonTranslator();
    }

    public static JsonTranslator instance() {
        return $.$$;
    }
    //////////////////////// get instance ////////////////////////

}
