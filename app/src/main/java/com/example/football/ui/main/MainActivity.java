package com.example.football.ui.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import com.example.football.R;
import com.example.football.ui.main.fragments.HomeFragment;
import com.example.football.ui.main.fragments.MilestoneFragment;
import com.example.football.ui.main.fragments.MineFragment;
import com.example.football.ui.main.fragments.TrainFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 默认显示首页Fragment
        replaceFragment(new HomeFragment());

        // 底部导航点击事件
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_train) {
                replaceFragment(new TrainFragment());
                return true;
            } else if (itemId == R.id.nav_milestone) {
                replaceFragment(new MilestoneFragment());
                return true;
            } else if (itemId == R.id.nav_mine) {
                replaceFragment(new MineFragment());
                return true;
            }
            return false;
        });
    }

    // 替换Fragment的核心方法
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}