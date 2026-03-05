package com.example.football.ui.main.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.football.R;
import com.example.football.ui.login.LoginActivity;
import com.example.football.utils.SPUtils;

public class MineFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        // 设置退出登录按钮点击事件
        view.findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 将登录状态设置为 false
                SPUtils.putBoolean(getActivity(), "isLogin", false);
                // 跳转到登录页面
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                // 关闭当前活动
                getActivity().finish();
            }
        });

        return view;
    }
}