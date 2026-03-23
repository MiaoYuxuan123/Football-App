package com.example.football.utils;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Map;

public class HttpUtil {
    // 全局OkHttpClient实例（复用，避免重复创建）
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();

    /**
     * GET请求
     * @param url 请求地址（注意：模拟器中localhost替换为10.0.2.2）
     * @param callback 回调（处理成功/失败）
     */
    public static void sendGetRequest(String url, Callback callback) {
        // 构建GET请求
        Request request = new Request.Builder()
                .url(url)
                .get() // GET方法（可省略，默认就是GET）
                .build();
        // 异步执行请求
        OK_HTTP_CLIENT.newCall(request).enqueue(callback);
    }

    /**
     * POST请求（表单格式）
     * @param url 请求地址
     * @param params POST参数（键值对）
     * @param callback 回调
     */
    public static void sendPostRequest(String url, FormBody params, Callback callback) {
        // 构建POST请求
        Request request = new Request.Builder()
                .url(url)
                .post(params) // POST方法，传入参数体
                .build();
        // 异步执行请求
        OK_HTTP_CLIENT.newCall(request).enqueue(callback);
    }

    /**
     * 便捷方法：构建POST表单参数
     * @param keyValues 键值对，如 "name", "张三", "age", "20"
     * @return FormBody
     */
    public static FormBody buildPostParams(String... keyValues) {
        FormBody.Builder builder = new FormBody.Builder();
        if (keyValues != null && keyValues.length % 2 == 0) {
            for (int i = 0; i < keyValues.length; i += 2) {
                builder.add(keyValues[i], keyValues[i + 1]);
            }
        }
        return builder.build();
    }

    /**
     * POST multipart 文件上传请求
     * @param url 请求地址
     * @param fieldName multipart 字段名，例如 file
     * @param file 上传文件
     * @param callback 回调
     */
    public static void sendMultipartFileRequest(String url, String fieldName, File file, Callback callback) {
        sendMultipartFileRequest(url, fieldName, file, null, null, null, callback);
    }

    /**
     * POST multipart 文件上传请求（可选 MIME、额外表单字段、请求头）
     */
    public static void sendMultipartFileRequest(
            String url,
            String fieldName,
            File file,
            String mimeType,
            Map<String, String> formFields,
            Map<String, String> headers,
            Callback callback
    ) {
        if (file == null || !file.exists()) {
            if (callback != null) {
                callback.onFailure(OK_HTTP_CLIENT.newCall(new Request.Builder().url(url).build()),
                        new IOException("upload file does not exist"));
            }
            return;
        }

        String resolvedMime = mimeType;
        if (resolvedMime == null || resolvedMime.trim().isEmpty()) {
            resolvedMime = URLConnection.guessContentTypeFromName(file.getName());
        }
        if (resolvedMime == null || resolvedMime.trim().isEmpty()) {
            resolvedMime = "application/octet-stream";
        }

        MediaType mediaType = MediaType.parse(resolvedMime);
        RequestBody fileBody = RequestBody.create(file, mediaType);

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fieldName, file.getName(), fileBody);

        if (formFields != null) {
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(multipartBuilder.build());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        OK_HTTP_CLIENT.newCall(requestBuilder.build()).enqueue(callback);
    }
}
