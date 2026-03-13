package com.example.football.ui.train;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.utils.SPUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class TrainRecordsActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT = "extra_account";
    private static final String SP_KEY_TRAIN_RECORDS = "train_records";

    private LinearLayout layoutRecordsContainer;
    private TextView tvEmptyRecords;
    private TextView tvKneeAngle;
    private TextView tvHipRotation;
    private TextView tvPowerOutput;
    private TextView tvImpactForceValue;
    private TextView tvSwingSpeedValue;
    private TextView tvInsightBody;
    private TextView tvChainLine1;
    private TextView tvChainLine2;
    private TextView tvChainLine3;
    private TextView tvVideoCurrent;
    private TextView tvVideoTotal;
    private TextView tvVideoToggle;
    private View viewVideoProgress;
    private VideoView vvPreview;

    private String currentAccount = "default";
    private final ArrayList<String> records = new ArrayList<>();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private int selectedIndex = -1;

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

        tvKneeAngle = findViewById(R.id.tv_knee_angle);
        tvHipRotation = findViewById(R.id.tv_hip_rotation);
        tvPowerOutput = findViewById(R.id.tv_power_output);
        tvImpactForceValue = findViewById(R.id.tv_impact_force_value);
        tvSwingSpeedValue = findViewById(R.id.tv_swing_speed_value);
        tvInsightBody = findViewById(R.id.tv_insight_body);
        tvChainLine1 = findViewById(R.id.tv_chain_line_1);
        tvChainLine2 = findViewById(R.id.tv_chain_line_2);
        tvChainLine3 = findViewById(R.id.tv_chain_line_3);
        tvVideoCurrent = findViewById(R.id.tv_video_current);
        tvVideoTotal = findViewById(R.id.tv_video_total);
        tvVideoToggle = findViewById(R.id.tv_video_toggle);
        viewVideoProgress = findViewById(R.id.view_video_progress);
        vvPreview = findViewById(R.id.vv_preview);

        setupVideoToggle();
        resetPreviewPanel();

        findViewById(R.id.btn_clear_records).setOnClickListener(v -> showClearRecordsConfirmDialog());

        loadRecords();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopProgressUpdates();
        if (vvPreview != null && vvPreview.isPlaying()) {
            vvPreview.pause();
            tvVideoToggle.setText("play");
        }
    }

    private void setupVideoToggle() {
        tvVideoToggle.setOnClickListener(v -> {
            if (vvPreview == null || vvPreview.getDuration() <= 0) {
                return;
            }
            if (vvPreview.isPlaying()) {
                vvPreview.pause();
                tvVideoToggle.setText("play");
            } else {
                vvPreview.start();
                tvVideoToggle.setText("pause");
                startProgressUpdates();
            }
        });
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
        SPUtils.putString(this, SP_KEY_TRAIN_RECORDS, ""); // backward compatibility
        MilestoneDbHelper.getInstance(this).resetTrainingProgress(currentAccount);
        selectedIndex = -1;
        resetPreviewPanel();
        stopVideo();
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
            resetPreviewPanel();
            stopVideo();
            return;
        }

        String[] lines = raw.split("\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.isEmpty()) {
                records.add(trimmed);
            }
        }

        if (selectedIndex < 0 || selectedIndex >= records.size()) {
            selectedIndex = 0;
        }

        for (int i = 0; i < records.size(); i++) {
            View item = buildRecordItem(records.get(i), i, i == selectedIndex);
            layoutRecordsContainer.addView(item);
        }
        tvEmptyRecords.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);

        if (!records.isEmpty()) {
            bindRecordToPreview(records.get(selectedIndex));
        }
    }

    private View buildRecordItem(String record, int index, boolean selected) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_train_record, layoutRecordsContainer, false);
        TextView tv = row.findViewById(R.id.tv_record_content);
        TextView btnShare = row.findViewById(R.id.tv_record_share);
        TextView btnDelete = row.findViewById(R.id.tv_record_delete);

        tv.setText(record);
        row.setAlpha(selected ? 1f : 0.92f);

        View.OnClickListener previewClick = v -> {
            selectedIndex = index;
            bindRecordToPreview(record);
            renderRecordSelection();
        };

        tv.setOnClickListener(previewClick);
        row.setOnClickListener(previewClick);
        btnShare.setOnClickListener(v -> shareRecord(record));
        btnDelete.setOnClickListener(v -> deleteRecordAt(index));
        return row;
    }

    private void renderRecordSelection() {
        for (int i = 0; i < layoutRecordsContainer.getChildCount(); i++) {
            View child = layoutRecordsContainer.getChildAt(i);
            child.setAlpha(i == selectedIndex ? 1f : 0.92f);
        }
    }

    private void bindRecordToPreview(String record) {
        ParsedRecord parsed = parseRecord(record);
        updateMetrics(parsed);
        bindVideo(parsed.videoPath);
    }

    private ParsedRecord parseRecord(String record) {
        ParsedRecord parsed = new ParsedRecord();
        parsed.raw = record;
        parsed.mode = "训练";
        parsed.count = 0;
        parsed.avgScore = 0;
        parsed.videoPath = "";
        parsed.time = "";

        String[] parts = record.split("\\|");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (i == 0 && part.contains("-") && part.contains(":")) {
                parsed.time = part;
            }
            if (part.startsWith("次数:")) {
                parsed.count = safeParseInt(part.substring(3));
                continue;
            }
            if (part.startsWith("次数：")) {
                parsed.count = safeParseInt(part.substring(3));
                continue;
            }
            if (part.startsWith("均分:")) {
                parsed.avgScore = safeParseInt(part.substring(3));
                continue;
            }
            if (part.startsWith("均分：")) {
                parsed.avgScore = safeParseInt(part.substring(3));
                continue;
            }
            if (part.startsWith("视频:")) {
                parsed.videoPath = part.substring(3).trim();
                continue;
            }
            if (part.startsWith("视频：")) {
                parsed.videoPath = part.substring(3).trim();
                continue;
            }

            if (part.contains(getString(R.string.train_mode_shoot))
                    || part.contains(getString(R.string.train_mode_dribble))
                    || part.contains(getString(R.string.train_mode_pass))) {
                parsed.mode = part;
            }
        }

        return parsed;
    }

    private void updateMetrics(ParsedRecord parsed) {
        int kneeAngle = 95 + Math.min(30, parsed.avgScore / 3);
        String hipState = parsed.avgScore >= 70 ? "ACTIVE" : "BUILDING";
        String powerState = parsed.avgScore >= 85 ? "HIGH" : parsed.avgScore >= 60 ? "MEDIUM" : "LOW";

        int impactForce = 620 + parsed.avgScore * 3 + parsed.count * 4;
        double swingSpeed = 9.8 + (parsed.avgScore / 20.0) + (parsed.count / 15.0);

        tvKneeAngle.setText("KNEE ANGLE\n" + kneeAngle + " deg");
        tvHipRotation.setText("HIP ROTATION\n" + hipState);
        tvPowerOutput.setText("POWER OUTPUT\n" + powerState);

        tvImpactForceValue.setText(impactForce + " N");
        tvSwingSpeedValue.setText(String.format(Locale.getDefault(), "%.1f m/s", swingSpeed));

        tvInsightBody.setText(buildInsightText(parsed, kneeAngle));

        int stability = Math.max(72, Math.min(99, 70 + parsed.avgScore / 2));
        int torsoLean = 8 + Math.min(8, parsed.count % 9);
        int extensionCm = parsed.avgScore >= 80 ? 3 : parsed.avgScore >= 60 ? 7 : 12;

        tvChainLine1.setText("- Plant foot stability: Excellent (" + stability + "%)");
        tvChainLine2.setText("- Torso lean: " + torsoLean + " deg (Within optimal range)");
        tvChainLine3.setText("- Follow-through could be extended by " + extensionCm + " cm.");
        tvEmptyRecords.setVisibility(View.GONE);
    }

    private String buildInsightText(ParsedRecord parsed, int kneeAngle) {
        String modeText = TextUtils.isEmpty(parsed.mode) ? "本次训练" : parsed.mode;
        String timeText = TextUtils.isEmpty(parsed.time) ? "最近一次" : parsed.time;
        return timeText + " 的 " + modeText + " 训练中，检测到膝关节角度约 " + kneeAngle
                + " deg，平均分 " + parsed.avgScore + "，动作次数 " + parsed.count
                + "。建议保持核心稳定并延长随摆，以提升发力效率与动作连贯性。";
    }

    private void bindVideo(String videoPath) {
        stopVideo();
        tvVideoCurrent.setText("00:00");

        if (TextUtils.isEmpty(videoPath) || "无".equals(videoPath)) {
            tvVideoTotal.setText("00:00");
            tvVideoToggle.setText("play");
            Toast.makeText(this, getString(R.string.train_record_no_video), Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(videoPath);
        if (!file.exists()) {
            tvVideoTotal.setText("00:00");
            tvVideoToggle.setText("play");
            Toast.makeText(this, getString(R.string.train_record_video_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        vvPreview.setVideoURI(Uri.fromFile(file));
        vvPreview.setOnPreparedListener(mp -> {
            int duration = vvPreview.getDuration();
            tvVideoTotal.setText(formatMs(duration));
            tvVideoToggle.setText("pause");
            vvPreview.start();
            startProgressUpdates();
        });
        vvPreview.setOnCompletionListener(mp -> {
            stopProgressUpdates();
            tvVideoCurrent.setText(tvVideoTotal.getText());
            tvVideoToggle.setText("play");
            updateProgressBar(1f);
        });
    }

    private void startProgressUpdates() {
        stopProgressUpdates();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (vvPreview == null || vvPreview.getDuration() <= 0) {
                    return;
                }
                int position = vvPreview.getCurrentPosition();
                int duration = vvPreview.getDuration();
                tvVideoCurrent.setText(formatMs(position));
                updateProgressBar(duration == 0 ? 0f : (position * 1f / duration));
                if (vvPreview.isPlaying()) {
                    progressHandler.postDelayed(this, 300L);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    private void updateProgressBar(float ratio) {
        if (viewVideoProgress == null) {
            return;
        }
        View parent = (View) viewVideoProgress.getParent();
        if (parent == null) {
            return;
        }
        int maxWidth = parent.getWidth() - dp(32);
        if (maxWidth <= 0) {
            return;
        }
        int target = Math.max(dp(12), Math.round(maxWidth * Math.max(0f, Math.min(1f, ratio))));
        ViewGroup.LayoutParams lp = viewVideoProgress.getLayoutParams();
        if (lp.width != target) {
            lp.width = target;
            viewVideoProgress.setLayoutParams(lp);
        }
    }

    private void stopVideo() {
        stopProgressUpdates();
        if (vvPreview != null) {
            vvPreview.stopPlayback();
        }
        updateProgressBar(0f);
    }

    private void resetPreviewPanel() {
        tvKneeAngle.setText("KNEE ANGLE\n--");
        tvHipRotation.setText("HIP ROTATION\n--");
        tvPowerOutput.setText("POWER OUTPUT\n--");
        tvImpactForceValue.setText("-- N");
        tvSwingSpeedValue.setText("-- m/s");
        tvInsightBody.setText("Select one record below to preview video and technical analysis.");
        tvChainLine1.setText("- Plant foot stability: --");
        tvChainLine2.setText("- Torso lean: --");
        tvChainLine3.setText("- Follow-through: --");
        tvVideoCurrent.setText("00:00");
        tvVideoTotal.setText("00:00");
        tvVideoToggle.setText("play");
    }

    private void deleteRecordAt(int index) {
        if (index < 0 || index >= records.size()) {
            return;
        }
        records.remove(index);
        if (selectedIndex >= records.size()) {
            selectedIndex = records.isEmpty() ? -1 : records.size() - 1;
        }
        persistRecords();
        loadRecords();
        Toast.makeText(this, getString(R.string.train_record_deleted), Toast.LENGTH_SHORT).show();
    }

    private void shareRecord(String record) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareText(record));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.train_record_share_chooser)));
    }

    private String buildShareText(String record) {
        ParsedRecord parsed = parseRecord(record);
        String timeText = TextUtils.isEmpty(parsed.time) ? getString(R.string.train_record_share_default_time) : parsed.time;
        String modeText = TextUtils.isEmpty(parsed.mode) ? getString(R.string.train_record_share_default_mode) : parsed.mode;
        return getString(
                R.string.train_record_share_text,
                timeText,
                modeText,
                parsed.count,
                parsed.avgScore,
                parsed.raw
        );
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
        SPUtils.putString(this, SP_KEY_TRAIN_RECORDS, updated); // backward compatibility
    }

    private String getTrainRecordsKey(String account) {
        return SP_KEY_TRAIN_RECORDS + "_" + account;
    }

    private int safeParseInt(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return 0;
        }
        String digits = raw.replaceAll("[^0-9-]", "").trim();
        if (TextUtils.isEmpty(digits)) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatMs(int ms) {
        int totalSec = Math.max(0, ms / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class ParsedRecord {
        String raw;
        String time;
        String mode;
        String videoPath;
        int count;
        int avgScore;
    }
}
