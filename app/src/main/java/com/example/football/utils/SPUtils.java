package com.example.football.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 本地存储工具类：保存登录状态、用户信息
 */
public class SPUtils {
    // 存储文件名
    private static final String SP_NAME = "football_train";

    // 存布尔值（如isLogin）
    public static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, value).apply();
    }

    // 取布尔值
    public static boolean getBoolean(Context context, String key, boolean defValue) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defValue);
    }

    // 存字符串（如用户名）
    public static void putString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).apply();
    }

    // 取字符串
    public static String getString(Context context, String key, String defValue) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }
}