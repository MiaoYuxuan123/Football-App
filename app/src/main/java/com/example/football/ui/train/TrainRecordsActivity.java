package com.example.football.ui.train;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.utils.SPUtils;

import java.io.File;

public class TrainRecordsActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT = "extra_account";
    private static final String SP_KEY_TRAIN_RECORDS = "train_records";

    private LinearLayout layoutRecordsContainer;
    private TextView tvEmptyRecords;
    private String currentAccount = "default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_records);

        currentAccount = getIntent().getStringExtra(EXTRA_ACCOUNT);
        if (TextUtils.isEmpty(currentAccount)) {
            String account = SPUtils.getString(this, "account", "default");
            currentAccount = TextUtils.isEmpty(account) ? "default" : account;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvEmptyRecords = findViewById(R.id.tv_empty_records);
        layoutRecordsContainer = findViewById(R.id.layout_records_container);

        findViewById(R.id.btn_clear_records).setOnClickListener(v -> {
            SPUtils.putString(this, getTrainRecordsKey(currentAccount), "");
            SPUtils.putString(this, SP_KEY_TRAIN_RECORDS, ""); // 兼容旧key
            MilestoneDbHelper.getInstance(this).resetTrainingProgress(currentAccount);
            loadRecords();
            Toast.makeText(this, "训练记录已清空", Toast.LENGTH_SHORT).show();
        });

        loadRecords();
    }

    private void loadRecords() {
        String raw = SPUtils.getString(this, getTrainRecordsKey(currentAccount), "");
        if (TextUtils.isEmpty(raw)) {
            raw = SPUtils.getString(this, SP_KEY_TRAIN_RECORDS, "");
            if (!TextUtils.isEmpty(raw)) {
                SPUtils.putString(this, getTrainRecordsKey(currentAccount), raw);
            }
        }

        layoutRecordsContainer.removeAllViews();
        if (TextUtils.isEmpty(raw)) {
            tvEmptyRecords.setVisibility(View.VISIBLE);
            return;
        }

        String[] lines = raw.split("\\n");
        int added = 0;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            TextView item = buildRecordItem(trimmed);
            layoutRecordsContainer.addView(item);
            added++;
        }
        tvEmptyRecords.setVisibility(added == 0 ? View.VISIBLE : View.GONE);
    }

    private TextView buildRecordItem(String record) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        tv.setLayoutParams(lp);
        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
        tv.setText(record);
        tv.setTextSize(14f);
        tv.setTextColor(0xFF1F2A44);
        tv.setBackgroundColor(0xFFF7F9FF);

        tv.setOnClickListener(v -> {
            String videoPath = parseVideoPath(record);
            if (TextUtils.isEmpty(videoPath) || "无".equals(videoPath)) {
                Toast.makeText(this, "该记录没有可播放视频", Toast.LENGTH_SHORT).show();
                return;
            }
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                Toast.makeText(this, "视频文件不存在或已删除", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, videoPath);
            startActivity(intent);
        });
        return tv;
    }

    private String parseVideoPath(String record) {
        int markerIndex = record.lastIndexOf("视频:");
        if (markerIndex < 0) {
            return "";
        }
        return record.substring(markerIndex + 3).trim();
    }

    private String getTrainRecordsKey(String account) {
        return SP_KEY_TRAIN_RECORDS + "_" + account;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

