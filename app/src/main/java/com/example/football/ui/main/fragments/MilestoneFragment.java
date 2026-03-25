package com.example.football.ui.main.fragments;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.data.TrainingRefreshNotifier;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.ui.main.MainActivity;
import com.example.football.ui.main.views.DonutProgressView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MilestoneFragment extends Fragment {

    private MilestoneData data;
    private AppRepository repository;

    private TextView tvLevel;
    private TextView tvTechnicalTitle;
    private TextView tvLevelBadge;
    private DonutProgressView progressMilestone;
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
            {"里奥·梅西", "99,99,99,99,99,99"},
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
        repository = RepositoryProvider.get(requireContext());
        initViews(view);
        bindListeners();
        observeTrainingRefresh();
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

        tvBadges.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }
            ((MainActivity) requireActivity()).openBadgeHall();
        });
    }

    private void loadAndShowData() {
        String account = repository.getCurrentAccount();
        data = repository.getMilestone(account);
        bindHeaderAndProgress();
        bindBadges();
        bindStarComparison();
        bindFuturePrediction();
        bindGoal();
    }

    private void observeTrainingRefresh() {
        TrainingRefreshNotifier.events().observe(getViewLifecycleOwner(), event -> {
            if (event == null || repository == null) {
                return;
            }
            if (event.matchesAccount(repository.getCurrentAccount()) && event.affectsOverview()) {
                loadAndShowData();
            }
        });
    }

    private void bindHeaderAndProgress() {
        tvLevel.setText(getString(R.string.milestone_header_level_format, data.level));
        tvTechnicalTitle.setText(data.technicalTitle);
        tvLevelBadge.setText(String.valueOf(data.level));

        int progress = Math.max(0, Math.min(100,
                Math.round(data.experience * 100f / Math.max(1, data.experienceToNext))));
        progressMilestone.setMax(100);
        progressMilestone.setProgress(progress);
        tvProgressPercent.setText(buildStyledPercent(progress));
        tvProgressLabel.setText(getString(R.string.milestone_progress_label));

        // 训练时长=训练记录条数*1小时
        int monthHours = Math.max(1, getRealTrainCount());
        tvTrainingInfo.setText(getString(R.string.milestone_month_hours_format, monthHours));
        tvExperience.setText(getString(R.string.milestone_experience_detail_format,
                data.experience, data.experienceToNext, data.xpPerTraining));

        // 用真实训练记录分数
        float avgScore = getRealAvgScore();
        int totalCount = getRealTrainScores().length;
        tvTrainCount.setText(getString(R.string.milestone_star_compare_subtitle_format,
                totalCount, Math.round(avgScore)));
    }

    private CharSequence buildStyledPercent(int progress) {
        String text = getString(R.string.milestone_percent_format, progress);
        SpannableString styled = new SpannableString(text);
        int percentIndex = text.indexOf('%');
        if (percentIndex >= 0) {
            styled.setSpan(new ForegroundColorSpan(0xFFFF6A13), percentIndex, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new RelativeSizeSpan(0.58f), percentIndex, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
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

        // 用真实训练记录分数填充雷达图（射门/传球/带球）
        float[] myRadar = getRealTrainScores();
        float base = avg(myRadar);
        if (myRadar.length < 3) {
            myRadar = new float[]{base, base, base, base, base, base};
        }
        float realAvgScore = getRealAvgScore();
        int myShoot = clampScore(Math.round(realAvgScore));
        int myPass = clampScore(Math.round(myRadar.length > 1 ? myRadar[1] : base));
        int myDribble = clampScore(Math.round(myRadar.length > 2 ? myRadar[2] : base));

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
        MilestoneDbHelper.TrainingSummary summary = repository.getTrainingSummary(data.account);
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
        MilestoneDbHelper.TrainingSummary summary = repository.getTrainingSummary(data.account);
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

    private float[] getRealTrainScores() {
        // 获取所有训练记录，提取每条的后端反馈分数
        List<com.example.football.database.entity.TrainRecord> records = ((com.example.football.data.AppRepositoryImpl)repository).getTrainRecordList(data.account);
        if (records == null || records.isEmpty()) return new float[0];
        List<Float> scores = new ArrayList<>();
        for (com.example.football.database.entity.TrainRecord record : records) {
            int score = 0;
            if (record != null) {
                score = Math.max(0, record.avgScore);
                if (record.feedbackJson != null && !record.feedbackJson.isEmpty()) {
                    try {
                        org.json.JSONObject root = new org.json.JSONObject(record.feedbackJson);
                        org.json.JSONObject feedback = root.optJSONObject("feedback");
                        org.json.JSONObject source = feedback == null ? root : feedback;
                        score = source.optInt("overall_score", score);
                    } catch (Exception ignored) {}
                }
            }
            scores.add((float)score);
        }
        float[] arr = new float[scores.size()];
        for (int i = 0; i < scores.size(); i++) arr[i] = scores.get(i);
        return arr;
    }

    private float getRealAvgScore() {
        float[] arr = getRealTrainScores();
        if (arr.length == 0) return 0f;
        float sum = 0f;
        for (float v : arr) sum += v;
        return sum / arr.length;
    }

    private int getRealTrainCount() {
        // 获取所有训练记录条数
        List<com.example.football.database.entity.TrainRecord> records = ((com.example.football.data.AppRepositoryImpl)repository).getTrainRecordList(data.account);
        return records == null ? 0 : records.size();
    }

    private static class BadgeItem {
        String name;
        boolean unlocked;
    }
}
