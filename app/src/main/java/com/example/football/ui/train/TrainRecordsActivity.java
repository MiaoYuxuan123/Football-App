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
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.data.TrainingRefreshNotifier;
import com.example.football.database.entity.TrainRecord;
import com.example.football.utils.VideoUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class TrainRecordsActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT = "extra_account";

    private LinearLayout layoutRecordsContainer;
    private TextView tvEmptyRecords;
    private TextView tvOverallScore;
    private TextView tvOverallAssessment;
    private TextView tvActionSummary;
    private TextView tvReportEmpty;
    private GridLayout gridScoreBreakdown;
    private LinearLayout containerStrengths;
    private LinearLayout containerImprovements;
    private LinearLayout containerDrills;
    private TextView tvVideoCurrent;
    private TextView tvVideoTotal;
    private TextView tvVideoToggle;
    private View viewVideoProgress;
    private VideoView vvPreview;

    private String currentAccount = "default";
    private final ArrayList<TrainRecord> records = new ArrayList<>();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private int selectedIndex = -1;
    private AppRepository repository;
    private long lastHandledRefreshVersion = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_records);

        repository = RepositoryProvider.get(this);

        currentAccount = getIntent().getStringExtra(EXTRA_ACCOUNT);
        if (TextUtils.isEmpty(currentAccount)) {
            currentAccount = repository.getCurrentAccount();
        }
        lastHandledRefreshVersion = System.currentTimeMillis();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvEmptyRecords = findViewById(R.id.tv_empty_records);
        layoutRecordsContainer = findViewById(R.id.layout_records_container);
        tvOverallScore = findViewById(R.id.tv_overall_score);
        tvOverallAssessment = findViewById(R.id.tv_overall_assessment);
        tvActionSummary = findViewById(R.id.tv_action_summary);
        tvReportEmpty = findViewById(R.id.tv_report_empty_state);
        gridScoreBreakdown = findViewById(R.id.grid_score_breakdown);
        containerStrengths = findViewById(R.id.container_strengths);
        containerImprovements = findViewById(R.id.container_improvements);
        containerDrills = findViewById(R.id.container_drills);
        tvVideoCurrent = findViewById(R.id.tv_video_current);
        tvVideoTotal = findViewById(R.id.tv_video_total);
        tvVideoToggle = findViewById(R.id.tv_video_toggle);
        viewVideoProgress = findViewById(R.id.view_video_progress);
        vvPreview = findViewById(R.id.vv_preview);

        setupVideoToggle();
        resetPreviewPanel();

        findViewById(R.id.btn_clear_records).setOnClickListener(v -> showClearRecordsConfirmDialog());

        observeTrainingRefresh();
        loadRecords();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopProgressUpdates();
        if (vvPreview != null && vvPreview.isPlaying()) {
            vvPreview.pause();
            tvVideoToggle.setText(R.string.common_play);
        }
    }

    private void setupVideoToggle() {
        tvVideoToggle.setOnClickListener(v -> {
            if (vvPreview == null || vvPreview.getDuration() <= 0) {
                return;
            }
            if (vvPreview.isPlaying()) {
                vvPreview.pause();
                tvVideoToggle.setText(R.string.common_play);
            } else {
                vvPreview.start();
                tvVideoToggle.setText(R.string.common_pause);
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
        repository.saveTrainRecords(currentAccount, "");
        repository.resetTrainingProgress(currentAccount);
        selectedIndex = -1;
        resetPreviewPanel();
        stopVideo();
        loadRecords();
        Toast.makeText(this, getString(R.string.train_records_cleared), Toast.LENGTH_SHORT).show();
    }

    private void loadRecords() {
        List<TrainRecord> stored = repository.getTrainRecordList(currentAccount);

        records.clear();
        layoutRecordsContainer.removeAllViews();
        if (stored != null) {
            for (TrainRecord record : stored) {
                if (record != null && !TextUtils.isEmpty(resolveRawText(record))) {
                    records.add(record);
                }
            }
        }
        if (records.isEmpty()) {
            tvEmptyRecords.setVisibility(View.VISIBLE);
            resetPreviewPanel();
            stopVideo();
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= records.size()) {
            selectedIndex = 0;
        }

        for (int i = 0; i < records.size(); i++) {
            TrainRecord record = records.get(i);
            View item = buildRecordItem(record, i, i == selectedIndex);
            layoutRecordsContainer.addView(item);
        }
        tvEmptyRecords.setVisibility(View.GONE);
        bindRecordToPreview(records.get(selectedIndex));
    }

    private View buildRecordItem(TrainRecord record, int index, boolean selected) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_train_record, layoutRecordsContainer, false);
        TextView tv = row.findViewById(R.id.tv_record_content);
        TextView btnShare = row.findViewById(R.id.tv_record_share);
        TextView btnDelete = row.findViewById(R.id.tv_record_delete);
        TextView tvScore = row.findViewById(R.id.tv_record_score);

        tv.setText(buildRecordDisplayText(record));
        tvScore.setText(String.valueOf(resolveDisplayScore(record)));
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

    private String buildRecordDisplayText(TrainRecord record) {
        ParsedRecord parsed = toParsedRecord(record);
        TrainRecord fallback = TrainRecord.fromRawText(resolveRawText(record));

        String time = firstNonEmpty(parsed.time, "--");
        String mode = firstNonEmpty(parsed.mode, "训练");
        String video = firstNonEmpty(parsed.videoPath, fallback.videoPath, "无");

        int count = Math.max(0, record == null ? fallback.actionCount : record.actionCount);
        if (count == 0) {
            count = Math.max(0, fallback.actionCount);
        }
        int avgScore = Math.max(0, record == null ? fallback.avgScore : record.avgScore);
        if (avgScore == 0) {
            avgScore = Math.max(0, fallback.avgScore);
        }

        if (count == 0 && avgScore == 0) {
            return time + " | " + mode + " | 视频:" + video;
        }
        return time + " | " + mode + " | 次数:" + count + " | 均分:" + avgScore + " | 视频:" + video;
    }

    private void renderRecordSelection() {
        for (int i = 0; i < layoutRecordsContainer.getChildCount(); i++) {
            View child = layoutRecordsContainer.getChildAt(i);
            child.setAlpha(i == selectedIndex ? 1f : 0.92f);
        }
    }

    private void bindRecordToPreview(TrainRecord record) {
        bindFeedback(record == null ? "" : record.feedbackJson);
        ParsedRecord parsed = toParsedRecord(record);
        bindVideo(parsed.videoPath);
    }

    private ParsedRecord toParsedRecord(TrainRecord record) {
        TrainRecord fallback = TrainRecord.fromRawText(resolveRawText(record));
        ParsedRecord parsed = new ParsedRecord();
        parsed.raw = resolveRawText(record);
        parsed.mode = firstNonEmpty(record == null ? "" : record.mode, fallback.mode, "训练");
        parsed.count = record != null && record.actionCount > 0 ? record.actionCount : fallback.actionCount;
        parsed.avgScore = resolveDisplayScore(record);
        parsed.videoPath = firstNonEmpty(record == null ? "" : record.videoPath, fallback.videoPath, "");
        parsed.time = firstNonEmpty(record == null ? "" : record.createdAt, fallback.createdAt, "");
        return parsed;
    }

    private int resolveDisplayScore(TrainRecord record) {
        int fallback = 0;
        if (record != null) {
            fallback = Math.max(0, record.avgScore);
        }
        if (record == null || TextUtils.isEmpty(record.feedbackJson)) {
            return fallback;
        }
        try {
            JSONObject root = new JSONObject(record.feedbackJson);
            JSONObject feedback = root.optJSONObject("feedback");
            JSONObject source = feedback == null ? root : feedback;
            return readInt(source, "overall_score", fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String resolveRawText(TrainRecord record) {
        if (record == null) {
            return "";
        }
        if (!TextUtils.isEmpty(record.rawText)) {
            return record.rawText.trim();
        }
        String time = firstNonEmpty(record.createdAt, "--");
        String mode = firstNonEmpty(record.mode, "训练");
        String video = TextUtils.isEmpty(record.videoPath) ? "无" : record.videoPath;
        return time + " | " + mode + " | 次数:" + Math.max(0, record.actionCount)
                + " | 均分:" + Math.max(0, record.avgScore) + " | 视频:" + video;
    }


    private void bindFeedback(String feedbackJson) {
        clearReportViews();
        if (TextUtils.isEmpty(feedbackJson)) {
            showReportEmpty(getString(R.string.train_records_feedback_empty));
            return;
        }

        try {
            JSONObject root = new JSONObject(feedbackJson);
            JSONObject feedback = root.optJSONObject("feedback");
            if (feedback == null) {
                feedback = root;
            }
            feedback.remove("provider_model");
            feedback.remove("used_fallback");

            int overallScore = readInt(feedback, "overall_score", 0);
            tvOverallScore.setText(String.valueOf(overallScore));
            tvOverallAssessment.setText(readText(feedback, "overall_assessment", getString(R.string.result_assessment_fallback)));
            tvActionSummary.setText(readText(feedback, "action_summary", getString(R.string.result_action_summary_fallback)));

            bindScoreBreakdown(feedback.optJSONObject("score_breakdown"));
            bindStringList(containerStrengths, feedback.optJSONArray("strengths"), R.layout.item_result_strength);
            bindImprovements(feedback.optJSONArray("improvements"));
            bindDrills(feedback.optJSONArray("training_drills"));

            tvReportEmpty.setVisibility(View.GONE);
        } catch (Exception e) {
            showReportEmpty(getString(R.string.result_feedback_parse_failed));
        }
    }

    private void clearReportViews() {
        tvOverallScore.setText(getString(R.string.result_score_default));
        tvOverallAssessment.setText(getString(R.string.result_assessment_fallback));
        tvActionSummary.setText(getString(R.string.result_action_summary_fallback));
        gridScoreBreakdown.removeAllViews();
        containerStrengths.removeAllViews();
        containerImprovements.removeAllViews();
        containerDrills.removeAllViews();
    }

    private void showReportEmpty(String message) {
        tvReportEmpty.setVisibility(View.VISIBLE);
        tvReportEmpty.setText(message);
    }

    private void bindScoreBreakdown(JSONObject scoreBreakdown) {
        gridScoreBreakdown.removeAllViews();
        if (scoreBreakdown == null || scoreBreakdown.length() == 0) {
            return;
        }

        List<String> preferredOrder = Arrays.asList(
                "摆动腿速度与幅度",
                "触球质量",
                "支撑腿稳定性",
                "随摆完整度"
        );

        List<MetricItem> orderedItems = new ArrayList<>();
        for (String key : preferredOrder) {
            if (scoreBreakdown.has(key)) {
                orderedItems.add(new MetricItem(key, readInt(scoreBreakdown, key, 0)));
            }
        }

        Iterator<String> iterator = scoreBreakdown.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (containsMetric(orderedItems, key)) {
                continue;
            }
            orderedItems.add(new MetricItem(key, readInt(scoreBreakdown, key, 0)));
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (MetricItem item : orderedItems) {
            View card = inflater.inflate(R.layout.item_result_metric, gridScoreBreakdown, false);
            TextView tvName = card.findViewById(R.id.tv_metric_name);
            TextView tvScore = card.findViewById(R.id.tv_metric_score);

            tvName.setText(item.name);
            tvScore.setText(String.valueOf(item.score));
            card.setBackgroundResource(item.score < 60
                    ? R.drawable.bg_result_metric_card_warning
                    : R.drawable.bg_result_metric_card);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            card.setLayoutParams(params);
            gridScoreBreakdown.addView(card);
        }
    }

    private void bindImprovements(JSONArray improvements) {
        containerImprovements.removeAllViews();
        if (improvements == null || improvements.length() == 0) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < improvements.length(); i++) {
            JSONObject item = improvements.optJSONObject(i);
            if (item == null) {
                continue;
            }
            View card = inflater.inflate(R.layout.item_result_improvement, containerImprovements, false);
            TextView tvIssue = card.findViewById(R.id.tv_improvement_issue);
            TextView tvPriority = card.findViewById(R.id.tv_improvement_priority);
            TextView tvEvidence = card.findViewById(R.id.tv_improvement_evidence);
            TextView tvSuggestion = card.findViewById(R.id.tv_improvement_suggestion);

            tvIssue.setText(readText(item, "issue", getString(R.string.result_improvement_default_issue)));
            String evidence = readText(item, "evidence", "");
            String suggestion = readText(item, "suggestion", "");

            if (TextUtils.isEmpty(evidence)) {
                tvEvidence.setVisibility(View.GONE);
            } else {
                tvEvidence.setText(getString(R.string.result_bullet_content_format, evidence));
            }

            if (TextUtils.isEmpty(suggestion)) {
                tvSuggestion.setVisibility(View.GONE);
            } else {
                tvSuggestion.setText(getString(R.string.result_bullet_content_format, suggestion));
            }

            tvPriority.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            containerImprovements.addView(card);
        }
    }

    private void bindDrills(JSONArray drills) {
        containerDrills.removeAllViews();
        if (drills == null || drills.length() == 0) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < drills.length(); i++) {
            String drill = drills.optString(i, "");
            if (TextUtils.isEmpty(drill)) {
                continue;
            }
            View card = inflater.inflate(R.layout.item_result_drill, containerDrills, false);
            TextView tvTitle = card.findViewById(R.id.tv_drill_title);
            TextView tvMeta = card.findViewById(R.id.tv_drill_meta);

            DrillItem drillItem = splitDrillText(drill);
            tvTitle.setText(drillItem.title);
            tvMeta.setText(drillItem.meta);
            containerDrills.addView(card);
        }
    }

    private void bindStringList(@NonNull LinearLayout container, JSONArray array, int itemLayoutRes) {
        container.removeAllViews();
        if (array == null || array.length() == 0) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < array.length(); i++) {
            String content = array.optString(i, "");
            if (TextUtils.isEmpty(content)) {
                continue;
            }
            View itemView = inflater.inflate(itemLayoutRes, container, false);
            TextView textView = itemView.findViewById(R.id.tv_content);
            textView.setText(content);
            container.addView(itemView);
        }
    }

    private DrillItem splitDrillText(String drill) {
        int index = drill.indexOf("（");
        if (index > 0 && drill.endsWith("）")) {
            return new DrillItem(drill.substring(0, index), drill.substring(index + 1, drill.length() - 1));
        }
        return new DrillItem(drill, getString(R.string.result_drill_meta_default));
    }

    private boolean containsMetric(List<MetricItem> items, String key) {
        for (MetricItem item : items) {
            if (item.name.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private int readInt(JSONObject jsonObject, String key, int fallback) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return fallback;
        }
        try {
            Object value = jsonObject.get(key);
            if (value instanceof Number) {
                return clampScore(((Number) value).intValue());
            }
            String raw = String.valueOf(value).replaceAll("[^0-9-]", "");
            if (TextUtils.isEmpty(raw)) {
                return fallback;
            }
            return clampScore(Integer.parseInt(raw));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String readText(JSONObject jsonObject, String key, String fallback) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return fallback;
        }
        String value = jsonObject.optString(key, "").trim();
        return value.isEmpty() ? fallback : value;
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private void bindVideo(String videoPath) {
        stopVideo();
        tvVideoCurrent.setText("00:00");

        String resolvedVideoPath = resolveExistingVideoPath(videoPath);
        if (TextUtils.isEmpty(resolvedVideoPath)) {
            tvVideoTotal.setText("00:00");
            tvVideoToggle.setText(R.string.common_play);
            return;
        }

        File file = new File(resolvedVideoPath);
        vvPreview.setVideoURI(Uri.fromFile(file));
        vvPreview.setOnPreparedListener(mp -> {
            // Adjust VideoView to center-crop and respect rotation using metadata
            adjustVideoViewForMeta(file.getAbsolutePath(), vvPreview);

            int duration = vvPreview.getDuration();
            tvVideoTotal.setText(formatMs(duration));
            tvVideoToggle.setText(R.string.common_pause);
            vvPreview.start();
            startProgressUpdates();
        });
        vvPreview.setOnCompletionListener(mp -> {
            stopProgressUpdates();
            tvVideoCurrent.setText(tvVideoTotal.getText());
            tvVideoToggle.setText(R.string.common_play);
            updateProgressBar(1f);
        });
    }

    private String resolveExistingVideoPath(String videoPath) {
        if (TextUtils.isEmpty(videoPath) || "无".equals(videoPath)) {
            return "";
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(videoPath);

        if (videoPath.startsWith("file://")) {
            try {
                String fromUri = Uri.parse(videoPath).getPath();
                if (!TextUtils.isEmpty(fromUri)) {
                    candidates.add(fromUri);
                }
            } catch (Exception ignored) {
            }
        }

        File origin = new File(videoPath);
        String name = origin.getName();
        File parent = origin.getParentFile();
        if (parent != null && !TextUtils.isEmpty(name)) {
            if (name.startsWith("pose_")) {
                candidates.add(new File(parent, name.substring("pose_".length())).getAbsolutePath());
            } else {
                candidates.add(new File(parent, "pose_" + name).getAbsolutePath());
            }
        }

        for (String candidate : candidates) {
            if (TextUtils.isEmpty(candidate)) {
                continue;
            }
            File file = new File(candidate);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        Toast.makeText(this, getString(R.string.train_record_video_missing), Toast.LENGTH_SHORT).show();
        return "";
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
        clearReportViews();
        tvReportEmpty.setText(R.string.train_records_select_hint);
        tvReportEmpty.setVisibility(View.VISIBLE);
        tvVideoCurrent.setText("00:00");
        tvVideoTotal.setText("00:00");
        tvVideoToggle.setText(R.string.common_play);
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

    private void shareRecord(TrainRecord record) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareText(record));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.train_record_share_chooser)));
    }

    private String buildShareText(TrainRecord record) {
        ParsedRecord parsed = toParsedRecord(record);
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
        repository.replaceTrainRecordList(currentAccount, new ArrayList<>(records));
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

    private void observeTrainingRefresh() {
        TrainingRefreshNotifier.events().observe(this, event -> {
            if (event == null || repository == null) {
                return;
            }
            if (!event.matchesAccount(currentAccount) || !event.affectsRecords()) {
                return;
            }
            if (event.version <= lastHandledRefreshVersion) {
                return;
            }
            lastHandledRefreshVersion = event.version;
            loadRecords();
        });
    }

    private static class ParsedRecord {
        String raw;
        String mode;
        String time;
        int count;
        int avgScore;
        String videoPath;
    }

    private static class MetricItem {
        final String name;
        final int score;

        MetricItem(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    private static class DrillItem {
        final String title;
        final String meta;

        DrillItem(String title, String meta) {
            this.title = title;
            this.meta = meta;
        }
    }

    /**
     * Adjust the VideoView size and rotation to center-crop the video content and avoid clipping/offset.
     * Uses VideoUtil.extractMeta to get width/height/rotation. If parent is not measured yet, it will retry later.
     */
    private void adjustVideoViewForMeta(@NonNull String videoPath, @NonNull VideoView videoView) {
        VideoUtil.VideoMeta meta = VideoUtil.extractMeta(videoPath);
        if (meta == null || meta.width <= 0 || meta.height <= 0) {
            // fallback: make VideoView match parent
            ViewGroup parent = (ViewGroup) videoView.getParent();
            if (parent != null) {
                ViewGroup.LayoutParams lp = videoView.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                videoView.setLayoutParams(lp);
                parent.setClipToPadding(false);
                parent.setClipChildren(false);
            }
            videoView.setRotation(0f);
            return;
        }

        View parentView = (View) videoView.getParent();
        if (parentView == null) {
            return;
        }

        // If parent not laid out yet, post and retry
        if (parentView.getWidth() == 0 || parentView.getHeight() == 0) {
            parentView.post(() -> adjustVideoViewForMeta(videoPath, videoView));
            return;
        }

        int rotation = meta.rotation;
        boolean rotated = (rotation == 90 || rotation == 270);
        int naturalW = rotated ? meta.height : meta.width;
        int naturalH = rotated ? meta.width : meta.height;

        int containerW = parentView.getWidth();
        int containerH = parentView.getHeight();

        // center-crop: scale so video covers the container
        float scale = Math.max(containerW / (float) naturalW, containerH / (float) naturalH);
        int targetW = Math.round(naturalW * scale);
        int targetH = Math.round(naturalH * scale);

        // Apply layout params centered
        FrameLayout.LayoutParams flp;
        ViewGroup.LayoutParams oldLp = videoView.getLayoutParams();
        if (oldLp instanceof FrameLayout.LayoutParams) {
            flp = (FrameLayout.LayoutParams) oldLp;
            flp.width = targetW;
            flp.height = targetH;
            flp.gravity = android.view.Gravity.CENTER;
            videoView.setLayoutParams(flp);
        } else {
            ViewGroup.LayoutParams lp = videoView.getLayoutParams();
            lp.width = targetW;
            lp.height = targetH;
            videoView.setLayoutParams(lp);
        }

        // Ensure parent won't clip the rotated/scaled child
        if (parentView instanceof ViewGroup) {
            ((ViewGroup) parentView).setClipToPadding(false);
            ((ViewGroup) parentView).setClipChildren(false);
        }

        // Set pivot and rotation after layout pass
        videoView.post(() -> {
            try {
                videoView.setPivotX(videoView.getWidth() / 2f);
                videoView.setPivotY(videoView.getHeight() / 2f);
                videoView.setRotation(rotation);
            } catch (Exception ignored) {
            }
        });
    }
}
