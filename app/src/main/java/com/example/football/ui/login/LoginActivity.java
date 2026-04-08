package com.example.football.ui.login;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.entity.user;
import com.example.football.ui.main.MainActivity;
import com.google.gson.Gson;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    // 声明控件
    private EditText etAccount;   // 账号输入框
    private EditText etPassword;  // 密码输入框
    // true: 走后端校验登录; false: 本地直登
    private static final boolean USE_BACKEND_LOGIN = false;
    private static final String GET_USER_URL = "http://1.94.62.162:8088/findoneuser";
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局文件（关键！之前空白就是因为少了这行）
        setContentView(R.layout.activity_login);

        repository = RepositoryProvider.get(this);

        // 初始化控件（把代码和布局里的控件绑定）
        initView();

        // 设置登录按钮点击事件
        setLoginClick();

        // 设置注册入口点击事件
        setRegisterClick();
    }

    /**
     * 初始化控件：把代码里的变量和布局里的ID绑定
     */
    private void initView() {
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
    }

    /**
     * 登录按钮点击逻辑
     */
    private void setLoginClick() {
        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 1. 获取输入的账号和密码
                String account = etAccount.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // 2. 简单校验：不能为空
                if (account.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "账号不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 3. 一键开关：按常量切换登录模式
                if (USE_BACKEND_LOGIN) {
                    loginWithBackend(account, password);
                } else {
                    loginDirect(account);
                }
            }
        });
    }

    private void loginDirect(String account) {
        repository.setLoggedIn(true);
        repository.setCurrentAccount(account);
        navigateToMain();
    }

    private void loginWithBackend(String account, String password) {
        String url = GET_USER_URL + "?name=" + account;
        Request request = new Request.Builder()
                .url(url)
                .build();

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "网络请求失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            Gson gson = new Gson();
                            user user = gson.fromJson(responseBody, user.class);

                            if (user != null && password.equals(user.getPassword())) {
                                repository.setLoggedIn(true);
                                repository.setCurrentAccount(account);
                                navigateToMain();
                            } else {
                                Toast.makeText(LoginActivity.this, "账号或密码错误", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "登录失败：" + responseBody, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 注册入口点击逻辑：跳转到注册页
     */
    private void setRegisterClick() {
        TextView tvGoRegister = findViewById(R.id.tv_go_register);
        tvGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到注册页
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}