package com.example.football.ui.login;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.football.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;    // 用户名
    private EditText etPhone;       // 手机号
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
        etPhone = findViewById(R.id.et_phone);
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
                String phone = etPhone.getText().toString().trim();
                String pwd = etPwd.getText().toString().trim();
                String pwdConfirm = etPwdConfirm.getText().toString().trim();

                // 2. 校验输入
                if (username.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phone.isEmpty() || phone.length() != 11) {
                    Toast.makeText(RegisterActivity.this, "请输入11位手机号", Toast.LENGTH_SHORT).show();
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

                // 3. 模拟注册成功（实际项目可存数据库/对接后端）
                Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();

                // 4. 跳回登录页
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}