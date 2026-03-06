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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.ui.login.LoginActivity;
import com.example.football.ui.train.VideoPlayerActivity;
import com.example.football.utils.SPUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MineFragment extends Fragment {

    private static final String SP_KEY_TRAIN_RECORDS = "train_records";

    private ListView lvTrainRecords;
    private TextView tvEmpty;
    private ArrayAdapter<String> recordsAdapter;
    private final List<String> records = new ArrayList<>();
    private ImageView ivAvatar;
    private String avatarSpKey = "avatar_path_default";

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

        TextView tvAccount = view.findViewById(R.id.tv_account);
        String account = SPUtils.getString(requireActivity(), "account", "");
        tvAccount.setText("账号：" + (TextUtils.isEmpty(account) ? "未登录用户" : account));

        ivAvatar = view.findViewById(R.id.iv_avatar);
        setupAvatarStyle();
        avatarSpKey = "avatar_path_" + account;
        ensureAvatarDir();
        loadAvatar();
        View btnUploadAvatar = view.findViewById(R.id.btn_upload_avatar);
        if (btnUploadAvatar != null) {
            btnUploadAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        }
        ivAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));

        lvTrainRecords = view.findViewById(R.id.lv_train_records);
        tvEmpty = view.findViewById(R.id.tv_empty_records);
        Button btnClearRecords = view.findViewById(R.id.btn_clear_records);
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnHelp = view.findViewById(R.id.btn_help);
        // 下方列表区域只保留清空按钮，避免重复的退出登录

        recordsAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, records);
        lvTrainRecords.setAdapter(recordsAdapter);
        loadTrainRecords();

        lvTrainRecords.setOnItemClickListener((parent, itemView, position, id) -> {
            String record = records.get(position);
            String videoPath = parseVideoPath(record);
            if (TextUtils.isEmpty(videoPath) || "无".equals(videoPath)) {
                Toast.makeText(requireContext(), "该记录没有可播放视频", Toast.LENGTH_SHORT).show();
                return;
            }

            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                Toast.makeText(requireContext(), "视频文件不存在或已删除", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, videoPath);
            startActivity(intent);
        });

        btnClearRecords.setOnClickListener(v -> {
            SPUtils.putString(requireContext(), SP_KEY_TRAIN_RECORDS, "");
            loadTrainRecords();
            Toast.makeText(requireContext(), "训练记录已清空", Toast.LENGTH_SHORT).show();
        });

        View.OnClickListener logoutAction = v -> {
            SPUtils.putBoolean(requireActivity(), "isLogin", false);
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        };
        btnLogout.setOnClickListener(logoutAction);
        // 仅保留头部退出按钮

        btnHelp.setOnClickListener(v -> showHelpDialog());

        return view;
    }

    private void loadTrainRecords() {
        String raw = SPUtils.getString(requireContext(), SP_KEY_TRAIN_RECORDS, "");
        records.clear();
        if (!TextUtils.isEmpty(raw)) {
            String[] lines = raw.split("\\n");
            for (String line : lines) {
                if (!TextUtils.isEmpty(line.trim())) {
                    records.add(line.trim());
                }
            }
        }

        recordsAdapter.notifyDataSetChanged();
        boolean hasData = !records.isEmpty();
        tvEmpty.setVisibility(hasData ? View.GONE : View.VISIBLE);
        lvTrainRecords.setVisibility(hasData ? View.VISIBLE : View.GONE);
    }

    private String parseVideoPath(String record) {
        int markerIndex = record.lastIndexOf("视频:");
        if (markerIndex < 0) {
            return "";
        }
        return record.substring(markerIndex + 3).trim();
    }

    private void showHelpDialog() {
        String helpText = "使用指引：\n\n" +
                "1. 首页：查看足球识别和评分功能\n" +
                "2. 识别：点击开始识别按钮进行足球动作识别\n" +
                "3. 历史：查看历史识别记录和评分\n" +
                "4. 个人中心：查看个人信息和设置\n\n" +
                "如有问题请联系客服。";

        new AlertDialog.Builder(requireActivity())
                .setTitle("帮助")
                .setMessage(helpText)
                .setPositiveButton("确定", null)
                .show();
    }

    private File ensureAvatarDir() {
        File dir = new File(requireContext().getFilesDir(), "avatars");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void saveAvatar(Uri uri) {
        File target = new File(ensureAvatarDir(), avatarSpKey + ".jpg");
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) return;
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            SPUtils.putString(requireContext(), avatarSpKey, target.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(requireActivity(), "头像保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAvatarStyle() {
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(0xFFF1F4FB);
        avatarBg.setStroke(dp(1), 0xFFD7DEEE);
        ivAvatar.setBackground(avatarBg);
        ivAvatar.setClipToOutline(true);
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private void loadAvatar() {
        String path = SPUtils.getString(requireContext(), avatarSpKey, "");
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            ivAvatar.setImageBitmap(BitmapFactory.decodeFile(path));
        } else {
            ivAvatar.setImageDrawable(null);
        }
    }
}
