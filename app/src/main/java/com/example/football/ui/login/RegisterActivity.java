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
    private static final String POST_URL = "http://10.0.2.2:8088/postuser";
    private static final String CHECK_USER_URL = "http://10.0.2.2:8088/findoneuser";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
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

        // 设置去登录点击事件
        setGoLoginClick();
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
     * 去登录点击事件
     */
    private void setGoLoginClick() {
        findViewById(R.id.tv_go_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * 注册按钮逻辑
     */
    private void setRegisterClick() {
        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String pwd = etPwd.getText().toString().trim();
                String pwdConfirm = etPwdConfirm.getText().toString().trim();

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

                checkUserAndRegister(username, pwd);
            }
        });
    }

    private void checkUserAndRegister(String username, String pwd) {
        String url = CHECK_USER_URL + "?name=" + username;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "网络请求失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (responseBody != null && !responseBody.isEmpty() && !responseBody.equals("null")) {
                            Toast.makeText(RegisterActivity.this, "用户名已存在，请换一个", Toast.LENGTH_SHORT).show();
                        } else {
                            registerUser(username, pwd);
                        }
                    }
                });
            }
        });
    }

    private void registerUser(String username, String pwd) {
        user user = new user(username, pwd);
        Gson gson = new Gson();
        String jsonStr = gson.toJson(user);
        RequestBody requestBody = RequestBody.create(jsonStr, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(POST_URL)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RegisterActivity.this, "网络请求失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                final String responseBody = response.body().string();
                final boolean isSuccessful = response.isSuccessful();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isSuccessful) {
                            Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "注册失败：" + responseBody, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}