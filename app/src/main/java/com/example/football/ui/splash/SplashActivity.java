package com.example.football.ui.splash;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.example.football.R;
import com.example.football.ui.login.LoginActivity;
import com.example.football.ui.main.MainActivity;
import com.example.football.utils.SPUtils;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 延迟2秒模拟加载，跳转到登录/主页面
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查本地是否登录（SP工具类后续给代码）
                boolean isLogin = SPUtils.getBoolean(SplashActivity.this, "isLogin", false);
                Intent intent;
                if (isLogin) {
                    // 已登录 → 主页面
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // 未登录 → 登录页
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish(); // 关闭启动页，避免返回
            }
        }, 2000);
    }
}