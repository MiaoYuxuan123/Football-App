package com.example.football.ui.main.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.utils.SPUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Milestone page: level, XP, growth trend, star comparison and badges.
 */
public class MilestoneFragment extends Fragment {

    private MilestoneData data;
    private MilestoneDbHelper dbHelper;
    private View rootView;

    private TextView tvLevel;
    private TextView tvTechnicalTitle;
    private TextView tvExperience;
    private TextView tvTrainingInfo;
    private TextView tvTrainCount;
    private TextView tvBadges;
    private TextView tvTrendEmpty;
    private LineChart chartTrend;
    private RadarChart chartRadar;
    private Spinner spinnerStar;
    private LinearLayout layoutLevelDots;

    // Star data: [shoot, pass, dribble, defend, fitness, awareness] - values elevated so users rarely match
    private static final String[][] STARS = {
            {"梅西", "98,96,98,52,92,97"},
            {"C罗", "97,94,95,55,96,95"},
            {"内马尔", "94,93,99,50,90,93"}
    };

    private static final String[] RADAR_LABELS = {"射门", "传球", "盘带", "防守", "体能", "意识"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_milestone, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        dbHelper = MilestoneDbHelper.getInstance(requireContext());
        initViews(view);
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
        tvExperience = view.findViewById(R.id.tvExperience);
        tvTrainingInfo = view.findViewById(R.id.tvTrainingInfo);
        tvTrainCount = view.findViewById(R.id.tvTrainCount);
        tvBadges = view.findViewById(R.id.tvBadges);
        tvTrendEmpty = view.findViewById(R.id.tvTrendEmpty);
        chartTrend = view.findViewById(R.id.chartTrend);
        chartRadar = view.findViewById(R.id.chartRadar);
        spinnerStar = view.findViewById(R.id.spinnerStar);
        layoutLevelDots = view.findViewById(R.id.layoutLevelDots);
    }

    private void loadAndShowData() {
        String account = SPUtils.getString(requireContext(), "account", "default");
        data = dbHelper.getOrCreate(account);
        bindData();
        setupCharts();
        setupLevelDots();
        setupStarSpinner();
    }

    private void bindData() {
        tvLevel.setText(getString(R.string.milestone_level_format, data.level));
        tvTechnicalTitle.setText(data.technicalTitle);
        tvExperience.setText(getString(R.string.milestone_experience_format, data.experience, data.experienceToNext));

        int remain = Math.max(0, data.experienceToNext - data.experience);
        int times = data.xpPerTraining > 0 ? (remain + data.xpPerTraining - 1) / data.xpPerTraining : 0;
        tvTrainingInfo.setText(getString(R.string.milestone_training_info_format, times, data.xpPerTraining));
        tvTrainCount.setText(getString(R.string.milestone_train_count_format, data.trainCount));

        android.widget.ProgressBar pb = rootView.findViewById(R.id.progressBarExperience);
        pb.setMax(Math.max(1, data.experienceToNext));
        pb.setProgress(Math.min(data.experience, data.experienceToNext));

        tvBadges.setText(formatBadges(data.badgesJson));
    }

    private String formatBadges(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        try {
            Type type = new TypeToken<List<BadgeItem>>() { } .getType();
            List<BadgeItem> list = new Gson().fromJson(json, type);
            if (list == null || list.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                BadgeItem b = list.get(i);
                if (i > 0) {
                    sb.append("   ");
                }
                String status = getString(b.unlocked
                        ? R.string.milestone_badge_status_unlocked
                        : R.string.milestone_badge_status_locked);
                sb.append(getString(R.string.milestone_badge_format, status, b.name));
            }
            return sb.toString();
        } catch (Exception ignored) {
            return getString(R.string.milestone_badge_default);
        }
    }

    private static class BadgeItem {
        String name;
        boolean unlocked;
    }

    private void setupCharts() {
        float[] trendScores = parseScores(data.technicalScoresJson);
        boolean hasTrendData = data.trainCount > 0 && trendScores != null && trendScores.length > 0;
        if (hasTrendData) {
            tvTrendEmpty.setVisibility(View.GONE);
            chartTrend.setVisibility(View.VISIBLE);
            setupLineChart(trendScores);
        } else {
            tvTrendEmpty.setVisibility(View.VISIBLE);
            chartTrend.setVisibility(View.GONE);
        }

        float[] myRadar = parseScores(data.radarScoresJson);
        if (myRadar == null || myRadar.length != 6) {
            float myAvg = trendScores != null && trendScores.length > 0 ? avg(trendScores) : 75f;
            myRadar = new float[]{myAvg, myAvg - 2, myAvg + 3, myAvg - 5, myAvg + 2, myAvg};
        }

        int starIdx = Math.max(0, Math.min(data.selectedStarId, STARS.length - 1));
        float[] starScores = parseStarScores(STARS[starIdx][1]);
        setupRadarChart(myRadar, starScores);
    }

    private float[] parseScores(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type type = new TypeToken<List<Float>>() { } .getType();
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
            return new float[]{70, 72, 75, 78, 80};
        }
    }

    private float avg(float[] a) {
        float sum = 0f;
        for (float v : a) {
            sum += v;
        }
        return a.length == 0 ? 0f : sum / a.length;
    }

    private float[] parseStarScores(String csv) {
        String[] parts = csv.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private void setupLineChart(float[] scores) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            entries.add(new Entry(i, scores[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "技术评分");
        dataSet.setColor(Color.parseColor("#008000"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#008000"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        chartTrend.setData(new LineData(dataSet));
        chartTrend.getDescription().setEnabled(false);
        chartTrend.getLegend().setEnabled(false);
        chartTrend.setTouchEnabled(false);

        XAxis xAxis = chartTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis yAxis = chartTrend.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        chartTrend.getAxisRight().setEnabled(false);
        chartTrend.invalidate();
    }

    private void setupRadarChart(float[] myScores, float[] starScores) {
        ArrayList<RadarEntry> meEntries = new ArrayList<>();
        for (float score : myScores) {
            meEntries.add(new RadarEntry(score));
        }
        ArrayList<RadarEntry> starEntries = new ArrayList<>();
        for (float score : starScores) {
            starEntries.add(new RadarEntry(score));
        }

        RadarDataSet meSet = new RadarDataSet(meEntries, "我");
        meSet.setColor(Color.parseColor("#008000"));
        meSet.setFillColor(Color.parseColor("#40008000"));
        meSet.setDrawFilled(true);
        meSet.setLineWidth(2f);

        RadarDataSet starSet = new RadarDataSet(starEntries, "球星");
        starSet.setColor(Color.parseColor("#FF9800"));
        starSet.setFillColor(Color.parseColor("#40FF9800"));
        starSet.setDrawFilled(true);
        starSet.setLineWidth(2f);

        chartRadar.setData(new RadarData(meSet, starSet));
        chartRadar.getDescription().setEnabled(false);
        chartRadar.getYAxis().setAxisMinimum(0f);
        chartRadar.getYAxis().setAxisMaximum(100f);
        chartRadar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(RADAR_LABELS));
        chartRadar.setTouchEnabled(false);
        chartRadar.invalidate();
    }

    private void setupLevelDots() {
        layoutLevelDots.removeAllViews();
        int currentLevel = data.level;
        for (int i = 1; i <= 5; i++) {
            TextView dot = new TextView(requireContext());
            String marker = i == currentLevel ? "●" : "○";
            dot.setText(getString(R.string.milestone_dot_format, marker, i));
            dot.setTextSize(14f);
            dot.setTextColor(i == currentLevel ? Color.parseColor("#008000") : Color.GRAY);
            layoutLevelDots.addView(dot);
        }
    }

    private void setupStarSpinner() {
        String[] names = new String[STARS.length];
        for (int i = 0; i < STARS.length; i++) {
            names[i] = STARS[i][0];
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                names
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStar.setAdapter(adapter);
        spinnerStar.setSelection(Math.max(0, Math.min(data.selectedStarId, STARS.length - 1)));

        spinnerStar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                data.selectedStarId = position;
                dbHelper.update(data);

                float[] myRadar = parseScores(data.radarScoresJson);
                if (myRadar == null || myRadar.length != 6) {
                    float[] trend = parseScores(data.technicalScoresJson);
                    float myAvg = trend != null && trend.length > 0 ? avg(trend) : 75f;
                    myRadar = new float[]{myAvg, myAvg - 2, myAvg + 3, myAvg - 5, myAvg + 2, myAvg};
                }

                float[] starScores = parseStarScores(STARS[position][1]);
                setupRadarChart(myRadar, starScores);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }
}
