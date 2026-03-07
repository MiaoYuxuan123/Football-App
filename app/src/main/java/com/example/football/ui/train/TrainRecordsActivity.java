package com.example.football.ui.train;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private final java.util.ArrayList<String> records = new java.util.ArrayList<>();

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

        findViewById(R.id.btn_clear_records).setOnClickListener(v -> showClearRecordsConfirmDialog());

        loadRecords();
    }

    private void showClearRecordsConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.train_records_clear_confirm_title))
                .setMessage(getString(R.string.train_records_clear_confirm_message))
                .setPositiveButton(getString(R.string.common_yes), (dialog, which) -> clearAllRecords())
                .setNegativeButton(getString(R.string.common_no), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void clearAllRecords() {
        SPUtils.putString(this, getTrainRecordsKey(currentAccount), "");
        SPUtils.putString(this, SP_KEY_TRAIN_RECORDS, ""); // 兼容旧key
        MilestoneDbHelper.getInstance(this).resetTrainingProgress(currentAccount);
        loadRecords();
        Toast.makeText(this, getString(R.string.train_records_cleared), Toast.LENGTH_SHORT).show();
    }

    private void loadRecords() {
        String raw = SPUtils.getString(this, getTrainRecordsKey(currentAccount), "");
        if (TextUtils.isEmpty(raw)) {
            raw = SPUtils.getString(this, SP_KEY_TRAIN_RECORDS, "");
            if (!TextUtils.isEmpty(raw)) {
                SPUtils.putString(this, getTrainRecordsKey(currentAccount), raw);
            }
        }

        records.clear();
        layoutRecordsContainer.removeAllViews();
        if (TextUtils.isEmpty(raw)) {
            tvEmptyRecords.setVisibility(View.VISIBLE);
            return;
        }

        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.isEmpty()) {
                records.add(trimmed);
            }
        }

        for (int i = 0; i < records.size(); i++) {
            View item = buildRecordItem(records.get(i), i);
            layoutRecordsContainer.addView(item);
        }
        tvEmptyRecords.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View buildRecordItem(String record, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(10);
        row.setLayoutParams(rowLp);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackgroundColor(0xFFF7F9FF);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvLp);
        tv.setText(record);
        tv.setTextSize(14f);
        tv.setTextColor(0xFF1F2A44);
        tv.setPadding(0, 0, dp(8), 0);

        TextView btnDelete = new TextView(this);
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnDelete.setLayoutParams(delLp);
        btnDelete.setText(getString(R.string.train_record_delete));
        btnDelete.setTextSize(13f);
        btnDelete.setTextColor(0xFFD32F2F);
        btnDelete.setPadding(dp(8), dp(4), dp(8), dp(4));

        tv.setOnClickListener(v -> {
            String videoPath = parseVideoPath(record);
            if (TextUtils.isEmpty(videoPath) || "无".equals(videoPath)) {
                Toast.makeText(this, getString(R.string.train_record_no_video), Toast.LENGTH_SHORT).show();
                return;
            }
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                Toast.makeText(this, getString(R.string.train_record_video_missing), Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_PATH, videoPath);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> deleteRecordAt(index));

        row.addView(tv);
        row.addView(btnDelete);
        return row;
    }

    private void deleteRecordAt(int index) {
        if (index < 0 || index >= records.size()) {
            return;
        }
        records.remove(index);
        persistRecords();
        loadRecords();
        Toast.makeText(this, getString(R.string.train_record_deleted), Toast.LENGTH_SHORT).show();
    }

    private void persistRecords() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(records.get(i));
        }
        String updated = sb.toString();
        SPUtils.putString(this, getTrainRecordsKey(currentAccount), updated);
        SPUtils.putString(this, SP_KEY_TRAIN_RECORDS, updated); // 兼容旧key
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
