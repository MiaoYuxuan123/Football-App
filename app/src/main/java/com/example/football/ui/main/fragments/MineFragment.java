package com.example.football.ui.main.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.AppRepositoryImpl;
import com.example.football.data.RepositoryProvider;
import com.example.football.data.TrainingRefreshNotifier;
import com.example.football.ui.login.LoginActivity;
import com.example.football.ui.train.TrainRecordsActivity;

import java.io.File;
import java.util.Locale;

public class MineFragment extends Fragment {

    private ImageView ivAvatar;
    private String currentAccount = "default";
    private AppRepository repository;

    private TextView tvAvgScore;
    private TextView tvLastScore;
    private TextView tvTrainDays;
    private TextView tvAnalysisCount;

    private final ActivityResultLauncher<String> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveAvatar(uri);
                    loadAvatar();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        repository = RepositoryProvider.get(requireContext());

        TextView tvAccount = view.findViewById(R.id.tv_account);
        String account = repository.getCurrentAccount();
        currentAccount = TextUtils.isEmpty(account) ? "default" : account;
        tvAccount.setText(TextUtils.isEmpty(account) || "default".equals(account) ? "未登录用户" : account);

        ivAvatar = view.findViewById(R.id.iv_avatar);
        setupAvatarStyle();
        loadAvatar();

        View btnUploadAvatar = view.findViewById(R.id.btn_upload_avatar);
        if (btnUploadAvatar != null) {
            btnUploadAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        }
        ivAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));

        // 新增：显示平均分数
        tvAvgScore = view.findViewById(R.id.tv_avg_score);
        tvLastScore = view.findViewById(R.id.tv_last_score);
        tvTrainDays = view.findViewById(R.id.tv_train_days);
        tvAnalysisCount = view.findViewById(R.id.tv_analysis_count);
        updateScoreViews();

        view.findViewById(R.id.btn_train_records).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TrainRecordsActivity.class);
            intent.putExtra(TrainRecordsActivity.EXTRA_ACCOUNT, currentAccount);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            repository.logout();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        view.findViewById(R.id.btn_help).setOnClickListener(v -> showHelpDialog());

        return view;
    }

    private void updateScoreViews() {
        float avgScore = ((AppRepositoryImpl)repository).getAvgScore(currentAccount);
        if (tvAvgScore != null) {
            if (avgScore <= 0.01f) {
                tvAvgScore.setText("--");
            } else {
                tvAvgScore.setText(String.format(Locale.getDefault(), "%.1f", avgScore));
            }
        }
        if (tvLastScore != null) {
            if (avgScore <= 0.01f) {
                tvLastScore.setText("0");
            } else {
                tvLastScore.setText(String.format(Locale.getDefault(), "%.1f", avgScore));
            }
        }
        // 训练天数 = 当前日期 - 2026-03-01
        if (tvTrainDays != null) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date start = sdf.parse("2026-03-01");
                java.util.Date now = new java.util.Date();
                long days = (now.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
                days = Math.max(days, 1); // 至少为1天
                tvTrainDays.setText(String.valueOf(days));
            } catch (Exception e) {
                tvTrainDays.setText("--");
            }
        }
        // 分析数量 = 训练记录个数
        if (tvAnalysisCount != null) {
            int count = 0;
            try {
                java.util.List<com.example.football.database.entity.TrainRecord> records = ((AppRepositoryImpl)repository).getTrainRecordList(currentAccount);
                if (records != null) count = records.size();
            } catch (Exception ignored) {}
            tvAnalysisCount.setText(String.valueOf(count));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeTrainingRefresh();
    }

    private void showHelpDialog() {
        String helpText = "使用指引：\n\n"
                + "1. 首页：查看足球识别和评分功能\n"
                + "2. 识别：点击开始识别按钮进行足球动作识别\n"
                + "3. 历史：查看历史识别记录和评分\n"
                + "4. 个人中心：查看个人信息和设置\n\n"
                + "如有问题请联系客服。";

        new AlertDialog.Builder(requireActivity())
                .setTitle("帮助")
                .setMessage(helpText)
                .setPositiveButton("确定", null)
                .show();
    }

    private void saveAvatar(Uri uri) {
        String path = repository.saveAvatarFromUri(currentAccount, uri);
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(requireActivity(), "头像保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAvatarStyle() {
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(0xFFF6F7FA);
        avatarBg.setStroke(dp(3), 0xFFFFFFFF);
        ivAvatar.setBackground(avatarBg);
        ivAvatar.setClipToOutline(true);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private void loadAvatar() {
        String path = repository.getAvatarPath(currentAccount);
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            ivAvatar.setImageBitmap(BitmapFactory.decodeFile(path));
        } else {
            ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void observeTrainingRefresh() {
        TrainingRefreshNotifier.events().observe(getViewLifecycleOwner(), event -> {
            if (event == null || repository == null) {
                return;
            }
            if (event.matchesAccount(currentAccount) && (event.affectsRecords() || event.affectsOverview() || event.affectsMedia() || event.scope.equals("all"))) {
                updateScoreViews();
                loadAvatar();
            }
        });
    }
}
