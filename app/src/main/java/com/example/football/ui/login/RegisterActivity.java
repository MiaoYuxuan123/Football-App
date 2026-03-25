package com.example.football.ui.login;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.football.R;
import com.example.football.entity.user;
import com.google.gson.Gson;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class RegisterActivity extends AppCompatActivity {
//    private static final String POST_URL = "http://10.0.2.2:8088/postuser";
    // 替换成你的真实公网 IP
    private static final String POST_URL = "http://1.94.62.162:8088/postuser";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    // OkHttpClient实例（复用，避免重复创建）
    private final OkHttpClient okHttpClient = new OkHttpClient();


    private EditText etUsername;    // 用户名
    private EditText etPwd;         // 密码
    private EditText etPwdConfirm;  // 确认密码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定注册页布局
        setContentView(R.layout.activity_register);

        // 初始化控件
        initView();

        // 设置注册按钮点击事件
        setRegisterClick();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        etUsername = findViewById(R.id.et_username);
        etPwd = findViewById(R.id.et_pwd);
        etPwdConfirm = findViewById(R.id.et_pwd_confirm);
    }

    /**
     * 注册按钮逻辑
     */
    private void setRegisterClick() {
        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. 获取输入内容
                String username = etUsername.getText().toString().trim();
                String pwd = etPwd.getText().toString().trim();
                String pwdConfirm = etPwdConfirm.getText().toString().trim();

                // 2. 校验输入
                if (username.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pwd.isEmpty() || pwd.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "密码至少6位", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!pwd.equals(pwdConfirm)) {
                    Toast.makeText(RegisterActivity.this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                user user = new user(username, pwd);
                Gson gson = new Gson();
                String jsonStr = gson.toJson(user);
                RequestBody requestBody = RequestBody.create(jsonStr, JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url(POST_URL)
                        .post(requestBody) // POST方法，传入JSON请求体
                        .build();

                // 3. 发送网络请求
                okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        // 网络请求失败
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this, "网络请求失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                        // 网络请求成功
                        final String responseBody = response.body().string();
                        final boolean isSuccessful = response.isSuccessful();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isSuccessful) {
                                    // 注册成功
                                    Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                                    // 跳回登录页
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // 注册失败
                                    Toast.makeText(RegisterActivity.this, "注册失败：" + responseBody, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}