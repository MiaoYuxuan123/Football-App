package com.example.football.ui.main.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.ui.login.LoginActivity;
import com.example.football.ui.train.VideoPlayerActivity;
import com.example.football.utils.SPUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MineFragment extends Fragment {

    private static final String SP_KEY_TRAIN_RECORDS = "train_records";

    private ListView lvTrainRecords;
    private TextView tvEmpty;
    private ArrayAdapter<String> recordsAdapter;
    private final List<String> records = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        lvTrainRecords = view.findViewById(R.id.lv_train_records);
        tvEmpty = view.findViewById(R.id.tv_empty_records);
        Button btnClearRecords = view.findViewById(R.id.btn_clear_records);
        Button btnLogout = view.findViewById(R.id.btn_logout);

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

        btnLogout.setOnClickListener(v -> {
            SPUtils.putBoolean(requireActivity(), "isLogin", false);
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

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
}