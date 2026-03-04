package com.example.football.ui.login;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.football.R;
import com.example.football.ui.main.MainActivity;
import com.example.football.ui.login.RegisterActivity;
import com.example.football.utils.SPUtils;

public class LoginActivity extends AppCompatActivity {

    // 声明控件
    private EditText etAccount;   // 账号输入框
    private EditText etPassword;  // 密码输入框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局文件（关键！之前空白就是因为少了这行）
        setContentView(R.layout.activity_login);

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

                // 3. 模拟登录成功（实际项目可对接后端，这里先本地验证）
                // 临时规则：账号任意，密码只要是6位及以上就登录成功
                if (password.length() >= 6) {
                    // 保存登录状态（用SP工具类）
                    SPUtils.putBoolean(LoginActivity.this, "isLogin", true);
                    SPUtils.putString(LoginActivity.this, "account", account);

                    // 跳转到主页面
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    // 关闭登录页，避免返回
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "密码至少6位", Toast.LENGTH_SHORT).show();
                }
            }
        });
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