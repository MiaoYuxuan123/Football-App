package com.example.football.ui.main.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.football.R;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.ui.main.MainActivity;
import com.example.football.utils.SPUtils;

public class HomeFragment extends Fragment {

    private TextView tvHomeGreeting;
    private TextView tvHomeTodayStatus;
    private TextView tvHomeTotalCount;
    private TextView tvHomeModeBreakdown;
    private TextView tvHomeSuccessRate;
    private TextView tvHomeRecommendation;
    private TextView tvHomeFocusMode;
    private TextView tvHomeFocusVolume;
    private TextView tvHomeFocusGoal;
    private TextView tvHomeRecommend1;
    private TextView tvHomeRecommend2;
    private TextView tvHomeRecommend3;
    private TextView tvHomeRecommendMeta1;
    private TextView tvHomeRecommendMeta2;
    private TextView tvHomeRecommendMeta3;
    private ProgressBar pbHomeShoot;
    private ProgressBar pbHomeDribble;
    private ProgressBar pbHomePass;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvHomeGreeting = view.findViewById(R.id.tv_home_greeting);
        tvHomeTodayStatus = view.findViewById(R.id.tv_home_today_status);
        tvHomeTotalCount = view.findViewById(R.id.tv_home_total_count);
        tvHomeModeBreakdown = view.findViewById(R.id.tv_home_mode_breakdown);
        tvHomeSuccessRate = view.findViewById(R.id.tv_home_success_rate);
        tvHomeRecommendation = view.findViewById(R.id.tv_home_recommendation);
        tvHomeFocusMode = view.findViewById(R.id.tv_home_focus_mode);
        tvHomeFocusVolume = view.findViewById(R.id.tv_home_focus_volume);
        tvHomeFocusGoal = view.findViewById(R.id.tv_home_focus_goal);
        tvHomeRecommend1 = view.findViewById(R.id.tv_home_recommend_1);
        tvHomeRecommend2 = view.findViewById(R.id.tv_home_recommend_2);
        tvHomeRecommend3 = view.findViewById(R.id.tv_home_recommend_3);
        tvHomeRecommendMeta1 = view.findViewById(R.id.tv_home_recommend_meta_1);
        tvHomeRecommendMeta2 = view.findViewById(R.id.tv_home_recommend_meta_2);
        tvHomeRecommendMeta3 = view.findViewById(R.id.tv_home_recommend_meta_3);
        pbHomeShoot = view.findViewById(R.id.pb_home_shoot);
        pbHomeDribble = view.findViewById(R.id.pb_home_dribble);
        pbHomePass = view.findViewById(R.id.pb_home_pass);

        applyStatusBarInset(view);
        bindHomeSummary();

        View btnStart = view.findViewById(R.id.btn_start_train);
        View cardRecommend1 = view.findViewById(R.id.card_home_recommend_1);
        View cardRecommend2 = view.findViewById(R.id.card_home_recommend_2);
        View cardRecommend3 = view.findViewById(R.id.card_home_recommend_3);

        applyPressFeedback(btnStart);
        applyPressFeedback(cardRecommend1);
        applyPressFeedback(cardRecommend2);
        applyPressFeedback(cardRecommend3);

        btnStart.setOnClickListener(v -> navigateTo(new TrainFragment()));
        cardRecommend1.setOnClickListener(v ->
                navigateTo(TrainFragment.newInstance(TrainFragment.MODE_KEY_SHOOT)));
        cardRecommend2.setOnClickListener(v ->
                navigateTo(TrainFragment.newInstance(TrainFragment.MODE_KEY_DRIBBLE)));
        cardRecommend3.setOnClickListener(v ->
                navigateTo(TrainFragment.newInstance(TrainFragment.MODE_KEY_PASS)));

        return view;
    }

    private void applyStatusBarInset(View root) {
        View heroCard = root.findViewById(R.id.layout_home_hero);
        if (heroCard == null) {
            return;
        }
        final int baseTopPadding = heroCard.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(heroCard, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), baseTopPadding + topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(heroCard);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindHomeSummary();
    }

    private void bindHomeSummary() {
        if (!isAdded()) {
            return;
        }
        String account = SPUtils.getString(requireContext(), "account", "default");
        String name = TextUtils.isEmpty(account) || "default".equals(account) ? getString(R.string.home_player_default) : account;
        tvHomeGreeting.setText(getString(R.string.home_greeting_format, name));

        MilestoneDbHelper.TrainingSummary summary =
                MilestoneDbHelper.getInstance(requireContext()).getTrainingSummary(account);

        tvHomeTotalCount.setText(getString(R.string.home_total_count_format, summary.totalCount));
        tvHomeModeBreakdown.setText(getString(
                R.string.home_mode_breakdown_format,
                summary.shootCount,
                summary.dribbleCount,
                summary.passCount));
        tvHomeSuccessRate.setText(getString(
                R.string.home_success_rate_format,
                summary.successRate,
                summary.avgScore,
                summary.level));

        int remain = summary.totalCount > 0 ? 0 : 1;
        tvHomeTodayStatus.setText(getString(R.string.home_today_status_format, summary.totalCount, remain));

        String focusMode = resolveFocusMode(summary);
        tvHomeFocusMode.setText(getString(R.string.home_focus_mode_format, focusMode));
        tvHomeFocusVolume.setText(getString(R.string.home_focus_volume_format));
        tvHomeFocusGoal.setText(getString(R.string.home_focus_goal_format, Math.max(70, summary.avgScore + 3)));

        bindModeProgress(summary);
        bindRecommendSection(focusMode, summary.avgScore);

        if (summary.totalCount == 0) {
            tvHomeRecommendation.setText(getString(R.string.home_recommendation_beginner));
        } else if (summary.successRate >= 80) {
            tvHomeRecommendation.setText(getString(R.string.home_recommendation_advanced));
        } else {
            tvHomeRecommendation.setText(getString(R.string.home_recommendation_normal));
        }
    }

    private void bindModeProgress(MilestoneDbHelper.TrainingSummary summary) {
        int total = Math.max(1, summary.shootCount + summary.dribbleCount + summary.passCount);
        pbHomeShoot.setProgress(Math.round(summary.shootCount * 100f / total));
        pbHomeDribble.setProgress(Math.round(summary.dribbleCount * 100f / total));
        pbHomePass.setProgress(Math.round(summary.passCount * 100f / total));
    }

    private void bindRecommendSection(String focusMode, int avgScore) {
        tvHomeRecommend1.setText(getString(R.string.home_recommend_item_1_format, focusMode));
        tvHomeRecommend2.setText(getString(R.string.home_recommend_item_2));
        int boost = avgScore < 80 ? 8 : 5;
        tvHomeRecommend3.setText(getString(R.string.home_recommend_item_3_format, boost));

        tvHomeRecommendMeta1.setText(getString(R.string.home_recommend_meta_mode, focusMode));
        tvHomeRecommendMeta2.setText(getString(R.string.home_recommend_meta_core));
        tvHomeRecommendMeta3.setText(getString(R.string.home_recommend_meta_recovery));
    }

    private String resolveFocusMode(MilestoneDbHelper.TrainingSummary summary) {
        int minCount = Math.min(summary.shootCount, Math.min(summary.dribbleCount, summary.passCount));
        if (minCount == summary.shootCount) {
            return getString(R.string.train_mode_shoot);
        }
        if (minCount == summary.dribbleCount) {
            return getString(R.string.train_mode_dribble);
        }
        return getString(R.string.train_mode_pass);
    }

    private void navigateTo(Fragment fragment) {
        if (!isAdded()) {
            return;
        }
        ((MainActivity) requireActivity()).replaceFragment(fragment);
    }

    private void applyPressFeedback(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }
}
