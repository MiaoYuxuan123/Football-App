package com.example.football.ui.main.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.database.entity.TrainRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerGrowthFragment extends Fragment {

    private WebView wvLiveModel;
    private TextView tvName;
    private TextView tvLevelRole;
    private TextView tvRank;
    private TextView tvXpLeft;
    private TextView tvXpRight;
    private TextView tvMilestone2;
    private TextView tvMilestone3;
    private TextView tvJointMobility;
    private TextView tvSprintAngle;
    private TextView tvPowerValue;
    private TextView tvAccuracyValue;
    private TextView tvTechniqueValue;
    private TextView tvAgilityValue;
    private TextView tvGainOverview;
    private TextView tvMappingNote;
    private ProgressBar pbXp;
    private ProgressBar pbPower;
    private ProgressBar pbAccuracy;
    private ProgressBar pbTechnique;
    private ProgressBar pbAgility;
    private AppRepository repository;
    private WebViewAssetLoader assetLoader;

    private static final int BASE_POWER_VALUE = 71;
    private static final int BASE_ACCURACY_VALUE = 63;
    private static final int BASE_TECHNIQUE_VALUE = 65;
    private static final int BASE_AGILITY_VALUE = 68;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_growth, container, false);
        repository = RepositoryProvider.get(requireContext());
        bindViews(view);
        bindActions(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindGrowthData();
    }

    @Override
    public void onDestroyView() {
        if (wvLiveModel != null) {
            wvLiveModel.loadUrl("about:blank");
            wvLiveModel.stopLoading();
            wvLiveModel.destroy();
            wvLiveModel = null;
        }
        super.onDestroyView();
    }

    private void bindViews(@NonNull View root) {
        wvLiveModel = root.findViewById(R.id.wv_pg_live_model);
        tvName = root.findViewById(R.id.tv_pg_name);
        tvLevelRole = root.findViewById(R.id.tv_pg_level_role);
        tvRank = root.findViewById(R.id.tv_pg_rank);
        tvXpLeft = root.findViewById(R.id.tv_pg_xp_left);
        tvXpRight = root.findViewById(R.id.tv_pg_xp_right);
        tvMilestone2 = root.findViewById(R.id.tv_pg_milestone_2);
        tvMilestone3 = root.findViewById(R.id.tv_pg_milestone_3);
        tvJointMobility = root.findViewById(R.id.tv_pg_joint_proxy);
        tvSprintAngle = root.findViewById(R.id.tv_pg_sprint_proxy);
        tvPowerValue = root.findViewById(R.id.tv_pg_power_value);
        tvAccuracyValue = root.findViewById(R.id.tv_pg_accuracy_value);
        tvTechniqueValue = root.findViewById(R.id.tv_pg_technique_value);
        tvAgilityValue = root.findViewById(R.id.tv_pg_agility_value);
        tvGainOverview = root.findViewById(R.id.tv_pg_gain_overview);
        tvMappingNote = root.findViewById(R.id.tv_pg_mapping_note);
        pbXp = root.findViewById(R.id.pb_pg_xp);
        pbPower = root.findViewById(R.id.pb_pg_power);
        pbAccuracy = root.findViewById(R.id.pb_pg_accuracy);
        pbTechnique = root.findViewById(R.id.pb_pg_technique);
        pbAgility = root.findViewById(R.id.pb_pg_agility);
    }

    private void bindActions(@NonNull View root) {
        View btnReplay = root.findViewById(R.id.btn_pg_replay);
        View btnCapture = root.findViewById(R.id.btn_pg_capture);
        btnReplay.setOnClickListener(v -> Toast.makeText(requireContext(), getString(R.string.pg_replay_hint), Toast.LENGTH_SHORT).show());
        btnCapture.setOnClickListener(v -> Toast.makeText(requireContext(), getString(R.string.pg_capture_hint), Toast.LENGTH_SHORT).show());
    }

    /**
     * 获取所有训练记录的真实分数（后端反馈）
     */
    private float[] getRealTrainScores(String account) {
        List<com.example.football.database.entity.TrainRecord> records = repository.getTrainRecordList(account);
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

    private float getRealAvgScore(String account) {
        float[] arr = getRealTrainScores(account);
        if (arr.length == 0) return 0f;
        float sum = 0f;
        for (float v : arr) sum += v;
        return sum / arr.length;
    }

    private void bindGrowthData() {
        if (!isAdded()) {
            return;
        }
        String account = repository.getCurrentAccount();
        MilestoneData milestone = repository.getMilestone(account);
        MilestoneDbHelper.TrainingSummary summary = repository.getTrainingSummary(account);
        AttributeSnapshot snapshot = buildSnapshot(account);

        // 用真实训练记录分数覆盖 summary.avgScore
        float realAvg = getRealAvgScore(account);
        summary.avgScore = Math.round(realAvg);

        String displayName = TextUtils.isEmpty(account) || "default".equals(account)
                ? getString(R.string.home_player_default)
                : account;
        tvName.setText(displayName.toUpperCase(Locale.getDefault()));
        tvLevelRole.setText(getString(R.string.pg_level_role_format, milestone.level, resolveRole(snapshot.overall)));
        tvRank.setText(getString(R.string.pg_rank_format, estimateRank(snapshot.overall, summary.totalCount)));

        int xpToNext = Math.max(1, milestone.experienceToNext);
        int xpNow = Math.max(0, Math.min(xpToNext, milestone.experience));
        pbXp.setMax(xpToNext);
        pbXp.setProgress(xpNow);
        tvXpLeft.setText(getString(R.string.pg_xp_left_format, xpNow));
        tvXpRight.setText(getString(R.string.pg_xp_right_format, xpToNext));

        tvMilestone2.setText(summary.totalCount >= 6
                ? getString(R.string.pg_milestone_2_unlocked)
                : getString(R.string.pg_milestone_2_locked));
        tvMilestone3.setText(summary.totalCount >= 12
                ? getString(R.string.pg_milestone_3_unlocked)
                : getString(R.string.pg_milestone_3_locked));

        tvJointMobility.setText(getString(R.string.pg_joint_value_format, snapshot.jointMobility));
        tvSprintAngle.setText(getString(R.string.pg_sprint_value_format, snapshot.sprintAngle));

        bindAttr(pbPower, tvPowerValue, snapshot.power);
        bindAttr(pbAccuracy, tvAccuracyValue, snapshot.accuracy);
        bindAttr(pbTechnique, tvTechniqueValue, snapshot.technique);
        bindAttr(pbAgility, tvAgilityValue, snapshot.agility);

        tvGainOverview.setText(getString(
                R.string.pg_gain_overview_format,
                snapshot.gainPower,
                snapshot.gainAccuracy,
                snapshot.gainTechnique,
                snapshot.gainAgility));

        tvMappingNote.setText(getString(
                R.string.pg_mapping_note_format,
                snapshot.jointMobility,
                snapshot.sprintAngle));

        setupLive3DView();
    }

    private void setupLive3DView() {
        if (!isAdded() || wvLiveModel == null) {
            return;
        }
        WebSettings settings = wvLiveModel.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(requireContext()))
                .build();

        wvLiveModel.setVerticalScrollBarEnabled(false);
        wvLiveModel.setHorizontalScrollBarEnabled(false);
        wvLiveModel.setBackgroundColor(0x00000000);
        wvLiveModel.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });
        wvLiveModel.setWebChromeClient(new WebChromeClient());
        wvLiveModel.loadUrl("https://appassets.androidplatform.net/assets/player_growth_live3d.html");
    }

    private void bindAttr(@NonNull ProgressBar progressBar, @NonNull TextView valueText, int value) {
        progressBar.setMax(99);
        progressBar.setProgress(value);
        valueText.setText(String.valueOf(value));
    }

    private AttributeSnapshot buildSnapshot(@NonNull String account) {
        List<TrainRecord> records = repository.getTrainRecordList(account);

        AttributeSnapshot s = new AttributeSnapshot();
        s.power = BASE_POWER_VALUE;
        s.accuracy = BASE_ACCURACY_VALUE;
        s.technique = BASE_TECHNIQUE_VALUE;
        s.agility = BASE_AGILITY_VALUE;

        if (records != null && !records.isEmpty()) {
            for (int i = records.size() - 1; i >= 0; i--) {
                int[] gain = resolveGainByMode(resolveRecordMode(records.get(i)));
                s.power = clamp(s.power + gain[0], 40, 99);
                s.accuracy = clamp(s.accuracy + gain[1], 38, 99);
                s.technique = clamp(s.technique + gain[2], 40, 99);
                s.agility = clamp(s.agility + gain[3], 38, 99);
            }

            int[] latestGain = resolveGainByMode(resolveRecordMode(records.get(0)));
            s.gainPower = latestGain[0];
            s.gainAccuracy = latestGain[1];
            s.gainTechnique = latestGain[2];
            s.gainAgility = latestGain[3];
        }

        s.jointMobility = clamp(Math.round((s.technique + s.agility) / 2f), 35, 99);
        s.sprintAngle = clamp(Math.round(30 + s.agility * 0.6f), 28, 95);
        s.overall = Math.round((s.power + s.accuracy + s.technique + s.agility) / 4f);
        return s;
    }

    private String resolveRecordMode(@Nullable TrainRecord record) {
        if (record == null) {
            return "";
        }
        String mode = safeString(record.mode);
        if (!TextUtils.isEmpty(mode)) {
            return mode;
        }
        TrainRecord parsed = TrainRecord.fromRawText(record.rawText);
        return safeString(parsed.mode);
    }

    // 每次训练从四项里固定选择两项 +1，保证文案与实际属性一致。
    private int[] resolveGainByMode(@NonNull String mode) {
        int[] gain = new int[]{0, 0, 0, 0}; // power, accuracy, technique, agility
        if (mode.contains(getString(R.string.train_mode_shoot))) {
            gain[0] = 1;
            gain[1] = 1;
            return gain;
        }
        if (mode.contains(getString(R.string.train_mode_dribble)) || mode.contains("盘带")) {
            gain[2] = 1;
            gain[3] = 1;
            return gain;
        }
        if (mode.contains(getString(R.string.train_mode_pass))) {
            gain[1] = 1;
            gain[2] = 1;
            return gain;
        }
        gain[0] = 1;
        gain[3] = 1;
        return gain;
    }

    private int estimateRank(int overall, int trainCount) {
        int seed = Math.max(1, overall * 6 + trainCount * 8);
        return Math.max(68, 1600 - seed);
    }

    @NonNull
    private String resolveRole(int overall) {
        if (overall >= 88) {
            return getString(R.string.pg_role_elite);
        }
        if (overall >= 78) {
            return getString(R.string.pg_role_core);
        }
        return getString(R.string.pg_role_rookie);
    }

    private String safeString(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class AttributeSnapshot {
        int jointMobility;
        int sprintAngle;
        int power;
        int accuracy;
        int technique;
        int agility;
        int overall;
        int gainPower;
        int gainAccuracy;
        int gainTechnique;
        int gainAgility;
    }
}
