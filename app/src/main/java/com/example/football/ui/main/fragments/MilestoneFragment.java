package com.example.football.ui.main.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.ui.main.MainActivity;
import com.example.football.utils.SPUtils;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MilestoneFragment extends Fragment {

    private MilestoneData data;
    private MilestoneDbHelper dbHelper;

    private TextView tvLevel;
    private TextView tvTechnicalTitle;
    private TextView tvLevelBadge;
    private CircularProgressIndicator progressMilestone;
    private TextView tvProgressPercent;
    private TextView tvProgressLabel;
    private TextView tvExperience;
    private TextView tvTrainingInfo;
    private TextView tvTrainCount;

    private TextView tvBadges;
    private TextView tvBadge1;
    private TextView tvBadge2;
    private TextView tvBadge3;

    private TextView tvStarName;
    private TextView tvGap;
    private TextView tvStarHint;
    private TextView tvMetricShoot;
    private TextView tvMetricPass;
    private TextView tvMetricDribble;
    private ProgressBar pbShoot;
    private ProgressBar pbPass;
    private ProgressBar pbDribble;

    private TextView tvTimelineTitle1;
    private TextView tvTimelineTitle2;
    private TextView tvTimelineTitle3;
    private TextView tvTimelineBody1;
    private TextView tvTimelineBody2;
    private TextView tvTimelineBody3;

    private TextView tvGoalText;
    private View btnStartGoal;

    // Star data: [shoot, pass, dribble, defend, fitness, awareness]
    private static final String[][] STARS = {
            {"里奥·梅西", "95,92,95,35,75,94"},
            {"克里斯蒂亚诺", "94,82,88,38,90,88"},
            {"内马尔", "88,85,96,35,78,86"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_milestone, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = MilestoneDbHelper.getInstance(requireContext());
        initViews(view);
        bindListeners();
        loadAndShowData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndShowData();
    }

    private void initViews(View view) {
        tvLevel = view.findViewById(R.id.tvLevel);
        tvTechnicalTitle = view.findViewById(R.id.tvTechnicalTitle);
        tvLevelBadge = view.findViewById(R.id.tvLevelBadge);
        progressMilestone = view.findViewById(R.id.progressMilestone);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        tvProgressLabel = view.findViewById(R.id.tvProgressLabel);
        tvExperience = view.findViewById(R.id.tvExperience);
        tvTrainingInfo = view.findViewById(R.id.tvTrainingInfo);
        tvTrainCount = view.findViewById(R.id.tvTrainCount);

        tvBadges = view.findViewById(R.id.tvBadges);
        tvBadge1 = view.findViewById(R.id.tvBadge1);
        tvBadge2 = view.findViewById(R.id.tvBadge2);
        tvBadge3 = view.findViewById(R.id.tvBadge3);

        tvStarName = view.findViewById(R.id.tvStarName);
        tvGap = view.findViewById(R.id.tvGap);
        tvStarHint = view.findViewById(R.id.tvStarHint);
        tvMetricShoot = view.findViewById(R.id.tvMetricShoot);
        tvMetricPass = view.findViewById(R.id.tvMetricPass);
        tvMetricDribble = view.findViewById(R.id.tvMetricDribble);
        pbShoot = view.findViewById(R.id.pbShoot);
        pbPass = view.findViewById(R.id.pbPass);
        pbDribble = view.findViewById(R.id.pbDribble);

        tvTimelineTitle1 = view.findViewById(R.id.tvTimelineTitle1);
        tvTimelineTitle2 = view.findViewById(R.id.tvTimelineTitle2);
        tvTimelineTitle3 = view.findViewById(R.id.tvTimelineTitle3);
        tvTimelineBody1 = view.findViewById(R.id.tvTimelineBody1);
        tvTimelineBody2 = view.findViewById(R.id.tvTimelineBody2);
        tvTimelineBody3 = view.findViewById(R.id.tvTimelineBody3);

        tvGoalText = view.findViewById(R.id.tvGoalText);
        btnStartGoal = view.findViewById(R.id.btnStartGoal);
    }

    private void bindListeners() {
        btnStartGoal.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }
            String modeKey = resolveWeakModeKey();
            ((MainActivity) requireActivity()).openTrainTab(modeKey);
        });
    }

    private void loadAndShowData() {
        String account = SPUtils.getString(requireContext(), "account", "default");
        data = dbHelper.getOrCreate(account);
        bindHeaderAndProgress();
        bindBadges();
        bindStarComparison();
        bindFuturePrediction();
        bindGoal();
    }

    private void bindHeaderAndProgress() {
        tvLevel.setText(getString(R.string.milestone_header_level_format, data.level));
        tvTechnicalTitle.setText(data.technicalTitle);
        tvLevelBadge.setText(String.valueOf(data.level));

        int progress = Math.max(0, Math.min(100,
                Math.round(data.experience * 100f / Math.max(1, data.experienceToNext))));
        progressMilestone.setProgress(progress);
        tvProgressPercent.setText(getString(R.string.milestone_percent_format, progress));
        tvProgressLabel.setText(getString(R.string.milestone_progress_label));

        int monthHours = Math.max(1, data.trainCount * 2);
        tvTrainingInfo.setText(getString(R.string.milestone_month_hours_format, monthHours));
        tvExperience.setText(getString(R.string.milestone_experience_detail_format,
                data.experience, data.experienceToNext, data.xpPerTraining));

        MilestoneDbHelper.TrainingSummary summary = dbHelper.getTrainingSummary(data.account);
        tvTrainCount.setText(getString(R.string.milestone_star_compare_subtitle_format,
                summary.totalCount, summary.avgScore));
    }

    private void bindBadges() {
        tvBadges.setText(getString(R.string.milestone_badge_preview));

        List<BadgeItem> badges = parseBadges(data.badgesJson);
        setBadgeCard(tvBadge1, badges, 0, getString(R.string.milestone_badge_card_1));
        setBadgeCard(tvBadge2, badges, 1, getString(R.string.milestone_badge_card_2));
        setBadgeCard(tvBadge3, badges, 2, getString(R.string.milestone_badge_card_3));
    }

    private void setBadgeCard(TextView view, List<BadgeItem> list, int index, String fallback) {
        if (list == null || index >= list.size() || list.get(index) == null) {
            view.setText(fallback);
            return;
        }
        BadgeItem badge = list.get(index);
        String prefix = badge.unlocked ? getString(R.string.milestone_badge_unlocked_prefix)
                : getString(R.string.milestone_badge_locked_prefix);
        view.setText(getString(R.string.milestone_badge_line_format, prefix, badge.name));
    }

    private void bindStarComparison() {
        int starIdx = Math.max(0, Math.min(data.selectedStarId, STARS.length - 1));
        String starName = STARS[starIdx][0];
        float[] starScores = parseStarScores(STARS[starIdx][1]);

        float[] myRadar = parseScores(data.radarScoresJson);
        if (myRadar == null || myRadar.length < 3) {
            float[] trend = parseScores(data.technicalScoresJson);
            float base = avg(trend);
            myRadar = new float[]{base, base - 2f, base + 3f, base, base, base};
        }

        int myShoot = clampScore(Math.round(myRadar[0]));
        int myPass = clampScore(Math.round(myRadar[1]));
        int myDribble = clampScore(Math.round(myRadar[2]));

        int starShoot = clampScore(Math.round(starScores[0]));
        int starPass = clampScore(Math.round(starScores[1]));
        int starDribble = clampScore(Math.round(starScores[2]));

        tvStarName.setText(starName);
        tvStarHint.setText(getString(R.string.milestone_star_hint));

        setMetric(tvMetricShoot, pbShoot, getString(R.string.milestone_metric_shoot), myShoot, starShoot, starName);
        setMetric(tvMetricPass, pbPass, getString(R.string.milestone_metric_pass), myPass, starPass, starName);
        setMetric(tvMetricDribble, pbDribble, getString(R.string.milestone_metric_dribble), myDribble, starDribble, starName);

        int myAvg = Math.round((myShoot + myPass + myDribble) / 3f);
        int starAvg = Math.round((starShoot + starPass + starDribble) / 3f);
        int gap = myAvg - starAvg;
        tvGap.setText(getString(R.string.milestone_gap_format, gap));
    }

    private void setMetric(TextView metricView, ProgressBar bar,
                           String metricName, int myScore, int starScore, String starName) {
        metricView.setText(getString(R.string.milestone_metric_line_format,
                metricName, myScore, starName, starScore));
        bar.setProgress(myScore);
        bar.setSecondaryProgress(starScore);
    }

    private void bindFuturePrediction() {
        MilestoneDbHelper.TrainingSummary summary = dbHelper.getTrainingSummary(data.account);
        int avgScore = Math.max(50, summary.avgScore == 0 ? 70 : summary.avgScore);

        int boost1 = Math.max(3, (80 - avgScore) / 6 + 4);
        int boost3 = boost1 + 8;
        int boost12 = boost3 + 12;

        tvTimelineTitle1.setText(getString(R.string.milestone_timeline_title_1));
        tvTimelineTitle2.setText(getString(R.string.milestone_timeline_title_2));
        tvTimelineTitle3.setText(getString(R.string.milestone_timeline_title_3));

        tvTimelineBody1.setText(getString(R.string.milestone_timeline_body_format_1, boost1, boost1 - 1, boost1));
        tvTimelineBody2.setText(getString(R.string.milestone_timeline_body_format_2, boost3, boost3 + 2, boost3 + 1));
        tvTimelineBody3.setText(getString(R.string.milestone_timeline_body_format_3, boost12));
    }

    private void bindGoal() {
        String weakMode = resolveWeakModeLabel();
        MilestoneDbHelper.TrainingSummary summary = dbHelper.getTrainingSummary(data.account);
        int remain = Math.max(1, (data.experienceToNext - data.experience + Math.max(1, data.xpPerTraining) - 1)
                / Math.max(1, data.xpPerTraining));
        tvGoalText.setText(getString(R.string.milestone_goal_format, weakMode, remain, summary.totalCount));
    }

    private String resolveWeakModeKey() {
        int min = Math.min(data.shootCount, Math.min(data.dribbleCount, data.passCount));
        if (min == data.shootCount) {
            return TrainFragment.MODE_KEY_SHOOT;
        }
        if (min == data.dribbleCount) {
            return TrainFragment.MODE_KEY_DRIBBLE;
        }
        return TrainFragment.MODE_KEY_PASS;
    }

    private String resolveWeakModeLabel() {
        String modeKey = resolveWeakModeKey();
        if (TrainFragment.MODE_KEY_SHOOT.equals(modeKey)) {
            return getString(R.string.train_mode_shoot);
        }
        if (TrainFragment.MODE_KEY_DRIBBLE.equals(modeKey)) {
            return getString(R.string.train_mode_dribble);
        }
        return getString(R.string.train_mode_pass);
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private float[] parseScores(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            Type type = new TypeToken<List<Float>>() { }.getType();
            List<Float> list = new Gson().fromJson(json, type);
            if (list == null || list.isEmpty()) {
                return null;
            }
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = list.get(i) == null ? 0f : list.get(i);
            }
            return arr;
        } catch (Exception ignored) {
            return null;
        }
    }

    private float avg(float[] values) {
        if (values == null || values.length == 0) {
            return 75f;
        }
        float sum = 0f;
        for (float value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private float[] parseStarScores(String csv) {
        String[] parts = csv.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (Exception e) {
                result[i] = 70f;
            }
        }
        return result;
    }

    private List<BadgeItem> parseBadges(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<BadgeItem>>() { }.getType();
            List<BadgeItem> list = new Gson().fromJson(json, type);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private static class BadgeItem {
        String name;
        boolean unlocked;
    }
}
