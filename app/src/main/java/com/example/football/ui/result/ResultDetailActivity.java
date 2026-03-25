package com.example.football.ui.result;

import android.net.Uri;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.example.football.utils.VideoUtil;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.football.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ResultDetailActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    public static final String EXTRA_FEEDBACK_JSON = "extra_feedback_json";

    private TextView tvBack;
    private TextView tvPageTitle;
    private TextView tvOverallScore;
    private TextView tvOverallAssessment;
    private TextView tvActionSummary;
    private TextView tvEmptyState;
    private GridLayout gridScoreBreakdown;
    private LinearLayout containerStrengths;
    private LinearLayout containerImprovements;
    private LinearLayout containerDrills;
    private LinearLayout sectionVideo;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_detail);
        bindViews();

        tvBack.setOnClickListener(v -> finish());

        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        String feedbackJson = getIntent().getStringExtra(EXTRA_FEEDBACK_JSON);

        bindVideo(videoPath);
        bindFeedback(feedbackJson);
    }

    private void bindViews() {
        tvBack = findViewById(R.id.tv_back);
        tvPageTitle = findViewById(R.id.tv_page_title);
        tvOverallScore = findViewById(R.id.tv_overall_score);
        tvOverallAssessment = findViewById(R.id.tv_overall_assessment);
        tvActionSummary = findViewById(R.id.tv_action_summary);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        gridScoreBreakdown = findViewById(R.id.grid_score_breakdown);
        containerStrengths = findViewById(R.id.container_strengths);
        containerImprovements = findViewById(R.id.container_improvements);
        containerDrills = findViewById(R.id.container_drills);
        sectionVideo = findViewById(R.id.section_video);
        playerView = findViewById(R.id.video_result);
    }

    private void bindVideo(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            sectionVideo.setVisibility(View.GONE);
            releasePlayer();
            return;
        }
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            sectionVideo.setVisibility(View.GONE);
            releasePlayer();
            return;
        }

        releasePlayer();
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        // Adjust PlayerView to center-crop and respect rotation using metadata
        adjustPlayerViewForMeta(videoFile.getAbsolutePath(), playerView);

        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)));
        exoPlayer.prepare();
        exoPlayer.play();
        sectionVideo.setVisibility(View.VISIBLE);
    }

    /**
     * Adjust PlayerView size and rotation to center-crop video according to metadata.
     */
    private void adjustPlayerViewForMeta(@NonNull String videoPath, @NonNull PlayerView playerView) {
        VideoUtil.VideoMeta meta = VideoUtil.extractMeta(videoPath);
        ViewGroup parent = (ViewGroup) playerView.getParent();
        if (meta == null || meta.width <= 0 || meta.height <= 0 || parent == null) {
            // fallback: match parent
            if (parent != null) {
                ViewGroup.LayoutParams lp = playerView.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                playerView.setLayoutParams(lp);
            }
            playerView.setRotation(0f);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            return;
        }

        if (parent.getWidth() == 0 || parent.getHeight() == 0) {
            parent.post(() -> adjustPlayerViewForMeta(videoPath, playerView));
            return;
        }

        int rotation = meta.rotation;
        boolean rotated = (rotation == 90 || rotation == 270);
        int naturalW = rotated ? meta.height : meta.width;
        int naturalH = rotated ? meta.width : meta.height;

        int containerW = parent.getWidth();
        int containerH = parent.getHeight();

        float scale = Math.max(containerW / (float) naturalW, containerH / (float) naturalH);
        int targetW = Math.round(naturalW * scale);
        int targetH = Math.round(naturalH * scale);

        // Apply layout params (FrameLayout preferred)
        ViewGroup.LayoutParams oldLp = playerView.getLayoutParams();
        if (oldLp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) oldLp;
            flp.width = targetW;
            flp.height = targetH;
            flp.gravity = android.view.Gravity.CENTER;
            playerView.setLayoutParams(flp);
        } else {
            oldLp.width = targetW;
            oldLp.height = targetH;
            playerView.setLayoutParams(oldLp);
        }

        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(false);
            ((ViewGroup) parent).setClipToPadding(false);
        }

        playerView.post(() -> {
            try {
                playerView.setPivotX(playerView.getWidth() / 2f);
                playerView.setPivotY(playerView.getHeight() / 2f);
                playerView.setRotation(rotation);
                // center-crop using ZOOM mode
                playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            } catch (Exception ignored) {
            }
        });
    }

    private void bindFeedback(String feedbackJson) {
        if (TextUtils.isEmpty(feedbackJson)) {
            showEmpty(getString(R.string.result_feedback_empty));
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

            tvPageTitle.setText(readText(feedback, "title", getString(R.string.result_page_title)));
            int overallScore = readInt(feedback, "overall_score", 0);
            tvOverallScore.setText(String.valueOf(overallScore));
            tvOverallAssessment.setText(readText(feedback, "overall_assessment", getString(R.string.result_assessment_fallback)));
            tvActionSummary.setText(readText(feedback, "action_summary", getString(R.string.result_action_summary_fallback)));

            bindScoreBreakdown(feedback.optJSONObject("score_breakdown"));
            bindStringList(containerStrengths, feedback.optJSONArray("strengths"), R.layout.item_result_strength);
            bindImprovements(feedback.optJSONArray("improvements"));
            bindDrills(feedback.optJSONArray("training_drills"));

            tvEmptyState.setVisibility(View.GONE);
        } catch (Exception e) {
            showEmpty(getString(R.string.result_feedback_parse_failed));
        }
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

    private void showEmpty(String message) {
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText(message);
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

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
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
}
