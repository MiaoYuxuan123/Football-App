package com.example.football.ui.main.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.football.R;
import com.example.football.ui.login.LoginActivity;
import com.example.football.utils.SPUtils;

public class MineFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        TextView tvAccount = view.findViewById(R.id.tv_account);
        String account = SPUtils.getString(getActivity(), "account", "");
        tvAccount.setText("账号：" + account);

        view.findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SPUtils.putBoolean(getActivity(), "isLogin", false);
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        view.findViewById(R.id.btn_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelpDialog();
            }
        });

        return view;
    }

    private void showHelpDialog() {
        String helpText = "使用指引：\n\n" +
                "1. 首页：查看足球识别和评分功能\n" +
                "2. 识别：点击开始识别按钮进行足球动作识别\n" +
                "3. 历史：查看历史识别记录和评分\n" +
                "4. 个人中心：查看个人信息和设置\n\n" +
                "如有问题请联系客服。";

        new AlertDialog.Builder(getActivity())
                .setTitle("帮助")
                .setMessage(helpText)
                .setPositiveButton("确定", null)
                .show();
    }
}