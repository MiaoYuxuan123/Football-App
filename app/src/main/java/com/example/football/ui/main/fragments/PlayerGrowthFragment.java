package com.example.football.ui.main.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.database.entity.MilestoneData;
import com.example.football.utils.SPUtils;

import java.util.Locale;

public class PlayerGrowthFragment extends Fragment {

    private static final String LEGACY_RECORDS_KEY = "train_records";
    private static final String RECORDS_KEY_PREFIX = "train_records_";

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_growth, container, false);
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
            wvLiveModel.setWebChromeClient(null);
            wvLiveModel.setWebViewClient(null);
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

    private void bindGrowthData() {
        if (!isAdded()) {
            return;
        }
        String account = SPUtils.getString(requireContext(), "account", "default");
        MilestoneDbHelper db = MilestoneDbHelper.getInstance(requireContext());
        MilestoneData milestone = db.getOrCreate(account);
        MilestoneDbHelper.TrainingSummary summary = db.getTrainingSummary(account);
        ParsedTrainingRecord latestRecord = parseLatestRecord(account);
        AttributeSnapshot snapshot = buildSnapshot(summary, latestRecord);

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

        wvLiveModel.setVerticalScrollBarEnabled(false);
        wvLiveModel.setHorizontalScrollBarEnabled(false);
        wvLiveModel.setBackgroundColor(0x00000000);
        wvLiveModel.setWebViewClient(new WebViewClient());
        wvLiveModel.setWebChromeClient(new WebChromeClient());
        wvLiveModel.loadUrl("file:///android_asset/player_growth_live3d.html");
    }

    private void bindAttr(@NonNull ProgressBar progressBar, @NonNull TextView valueText, int value) {
        progressBar.setMax(99);
        progressBar.setProgress(value);
        valueText.setText(String.valueOf(value));
    }


    private ParsedTrainingRecord parseLatestRecord(@NonNull String account) {
        String raw = SPUtils.getString(requireContext(), RECORDS_KEY_PREFIX + account, "");
        if (TextUtils.isEmpty(raw)) {
            raw = SPUtils.getString(requireContext(), LEGACY_RECORDS_KEY, "");
        }
        if (TextUtils.isEmpty(raw)) {
            return new ParsedTrainingRecord();
        }

        String firstLine = raw.split("\\n")[0];
        ParsedTrainingRecord parsed = new ParsedTrainingRecord();
        String[] parts = firstLine.split("\\|");
        for (String rawPart : parts) {
            String part = rawPart == null ? "" : rawPart.trim();
            if (part.startsWith("次数:")) {
                parsed.count = safeParseInt(part.substring(3));
            } else if (part.startsWith("均分:")) {
                parsed.avgScore = safeParseInt(part.substring(3));
            } else if (part.contains(getString(R.string.train_mode_shoot))
                    || part.contains(getString(R.string.train_mode_dribble))
                    || part.contains(getString(R.string.train_mode_pass))) {
                parsed.mode = part;
            }
        }
        parsed.valid = parsed.avgScore > 0 || parsed.count > 0;
        return parsed;
    }

    private int safeParseInt(@Nullable String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private AttributeSnapshot buildSnapshot(@NonNull MilestoneDbHelper.TrainingSummary summary,
                                            @NonNull ParsedTrainingRecord latestRecord) {
        int total = Math.max(1, summary.shootCount + summary.dribbleCount + summary.passCount);
        int shootRatio = Math.round(summary.shootCount * 100f / total);
        int passRatio = Math.round(summary.passCount * 100f / total);
        int dribbleRatio = Math.round(summary.dribbleCount * 100f / total);

        AttributeSnapshot s = new AttributeSnapshot();
        s.jointMobility = clamp(Math.round(42 + dribbleRatio * 0.42f + summary.avgScore * 0.34f), 35, 99);
        s.sprintAngle = clamp(Math.round(30 + shootRatio * 0.48f + summary.avgScore * 0.30f), 28, 95);

        s.power = clamp(Math.round(44 + shootRatio * 0.40f + summary.avgScore * 0.36f), 40, 99);
        s.accuracy = clamp(Math.round(40 + passRatio * 0.42f + summary.successRate * 0.35f), 38, 99);
        s.technique = clamp(Math.round(42 + dribbleRatio * 0.43f + summary.avgScore * 0.35f), 40, 99);
        s.agility = clamp(Math.round(40 + s.jointMobility * 0.34f + s.sprintAngle * 0.32f), 38, 99);

        if (latestRecord.valid) {
            int baseGain = Math.max(1, latestRecord.avgScore / 28 + latestRecord.count / 12);
            if (latestRecord.mode.contains(getString(R.string.train_mode_shoot))) {
                s.gainPower = baseGain + 2;
                s.gainAccuracy = baseGain;
                s.gainTechnique = baseGain;
                s.gainAgility = Math.max(1, baseGain - 1);
            } else if (latestRecord.mode.contains(getString(R.string.train_mode_dribble))) {
                s.gainPower = Math.max(1, baseGain - 1);
                s.gainAccuracy = baseGain;
                s.gainTechnique = baseGain + 2;
                s.gainAgility = baseGain + 1;
            } else {
                s.gainPower = baseGain;
                s.gainAccuracy = baseGain + 2;
                s.gainTechnique = baseGain + 1;
                s.gainAgility = baseGain;
            }
        }

        s.power = clamp(s.power + s.gainPower, 40, 99);
        s.accuracy = clamp(s.accuracy + s.gainAccuracy, 38, 99);
        s.technique = clamp(s.technique + s.gainTechnique, 40, 99);
        s.agility = clamp(s.agility + s.gainAgility, 38, 99);
        s.overall = Math.round((s.power + s.accuracy + s.technique + s.agility) / 4f);

        return s;
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class ParsedTrainingRecord {
        String mode = "";
        int count = 0;
        int avgScore = 0;
        boolean valid = false;
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
