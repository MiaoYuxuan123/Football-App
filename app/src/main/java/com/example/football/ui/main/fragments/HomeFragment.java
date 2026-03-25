package com.example.football.ui.main.fragments;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.data.TrainingRefreshNotifier;
import com.example.football.database.MilestoneDbHelper;
import com.example.football.ui.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final long STAR_BANNER_AUTO_SCROLL_DELAY_MS = 3200L;
    private static final String STAR_PHOTO_ASSET_DIR = "star_photos";

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
    private ViewPager2 vpHomeStar;
    private LinearLayout layoutHomeStarIndicator;
    private View btnHomeUploadStarPhotos;
    private HomeStarBannerAdapter homeStarBannerAdapter;
    private AppRepository repository;

    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoScrollRunnable = () -> {
        if (vpHomeStar == null || homeStarBannerAdapter == null || homeStarBannerAdapter.getItemCount() <= 1) {
            return;
        }
        int next = (vpHomeStar.getCurrentItem() + 1) % homeStarBannerAdapter.getItemCount();
        vpHomeStar.setCurrentItem(next, true);
        scheduleStarBannerAutoScroll();
    };

    private final ViewPager2.OnPageChangeCallback starPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            updateStarIndicators(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                stopStarBannerAutoScroll();
            } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                scheduleStarBannerAutoScroll();
            }
        }
    };

    private final ActivityResultLauncher<String> starPhotosPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), this::handlePickedStarPhotos);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        repository = RepositoryProvider.get(requireContext());

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
        vpHomeStar = view.findViewById(R.id.vp_home_star);
        layoutHomeStarIndicator = view.findViewById(R.id.layout_home_star_indicator);
        btnHomeUploadStarPhotos = view.findViewById(R.id.btn_home_upload_star_photos);

        applyStatusBarInset(view);
        bindHomeSummary();
        setupStarBanner();

        View btnStart = view.findViewById(R.id.btn_start_train);
        View cardRecommend1 = view.findViewById(R.id.card_home_recommend_1);
        View cardRecommend2 = view.findViewById(R.id.card_home_recommend_2);
        View cardRecommend3 = view.findViewById(R.id.card_home_recommend_3);
        View btnCourseTry1 = view.findViewById(R.id.btn_home_course_try_1);
        View btnCourseTry2 = view.findViewById(R.id.btn_home_course_try_2);
        View btnCourseTry3 = view.findViewById(R.id.btn_home_course_try_3);

        applyPressFeedback(btnStart);
        applyPressFeedback(cardRecommend1);
        applyPressFeedback(cardRecommend2);
        applyPressFeedback(cardRecommend3);
        applyPressFeedback(btnHomeUploadStarPhotos);
        applyPressFeedback(btnCourseTry1);
        applyPressFeedback(btnCourseTry2);
        applyPressFeedback(btnCourseTry3);

        btnStart.setOnClickListener(v -> navigateToTrain(null));
        cardRecommend1.setOnClickListener(v -> navigateToTrain(TrainFragment.MODE_KEY_SHOOT));
        cardRecommend2.setOnClickListener(v -> navigateToTrain(TrainFragment.MODE_KEY_DRIBBLE));
        cardRecommend3.setOnClickListener(v -> navigateToTrain(TrainFragment.MODE_KEY_PASS));
        btnHomeUploadStarPhotos.setOnClickListener(v -> starPhotosPickerLauncher.launch("image/*"));
        btnCourseTry1.setOnClickListener(v -> openExternalCourse(getString(R.string.home_course_url_1)));
        btnCourseTry2.setOnClickListener(v -> openExternalCourse(getString(R.string.home_course_url_2)));
        btnCourseTry3.setOnClickListener(v -> openExternalCourse(getString(R.string.home_course_url_3)));

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
    public void onStart() {
        super.onStart();
        scheduleStarBannerAutoScroll();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopStarBannerAutoScroll();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindHomeSummary();
        setupStarBanner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopStarBannerAutoScroll();
        if (vpHomeStar != null) {
            vpHomeStar.unregisterOnPageChangeCallback(starPageChangeCallback);
            vpHomeStar.setAdapter(null);
        }
        homeStarBannerAdapter = null;
        vpHomeStar = null;
        layoutHomeStarIndicator = null;
        btnHomeUploadStarPhotos = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeTrainingRefresh();
    }

    private void bindHomeSummary() {
        if (!isAdded()) {
            return;
        }
        String account = repository.getCurrentAccount();
        String name = TextUtils.isEmpty(account) || "default".equals(account) ? getString(R.string.home_player_default) : account;
        tvHomeGreeting.setText(getString(R.string.home_greeting_format, name));

        MilestoneDbHelper.TrainingSummary summary = repository.getTrainingSummary(account);
        float realAvgScore = repository.getAvgScore(account);
        summary.avgScore = Math.round(realAvgScore);

        tvHomeTotalCount.setText(getString(R.string.home_total_count_format, summary.totalCount));
        tvHomeModeBreakdown.setText(getString(
                R.string.home_mode_breakdown_format,
                summary.shootCount,
                summary.dribbleCount,
                summary.passCount));
        tvHomeSuccessRate.setText(getString(
                R.string.home_success_rate_format,
                realAvgScore,
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

    private void navigateToTrain(String presetMode) {
        if (!isAdded()) {
            return;
        }
        ((MainActivity) requireActivity()).openTrainTab(presetMode);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyPressFeedback(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                v.performClick();
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }

    private void setupStarBanner() {
        if (vpHomeStar == null || !isAdded()) {
            return;
        }
        List<HomeStarBannerAdapter.StarBannerItem> items = buildStarBannerItems();

        homeStarBannerAdapter = new HomeStarBannerAdapter(items);
        vpHomeStar.setAdapter(homeStarBannerAdapter);
        vpHomeStar.unregisterOnPageChangeCallback(starPageChangeCallback);
        vpHomeStar.registerOnPageChangeCallback(starPageChangeCallback);
        vpHomeStar.setOffscreenPageLimit(1);
        updateStarIndicators(0);
        scheduleStarBannerAutoScroll();
    }

    private List<HomeStarBannerAdapter.StarBannerItem> buildStarBannerItems() {
        List<File> localFiles = loadLocalStarPhotos();
        if (!localFiles.isEmpty()) {
            List<HomeStarBannerAdapter.StarBannerItem> items = new ArrayList<>();
            for (File file : localFiles) {
                items.add(HomeStarBannerAdapter.StarBannerItem.fromFilePath(
                        file.getAbsolutePath(),
                        file.getName(),
                        getString(R.string.home_star_local_subtitle)
                ));
            }
            return items;
        }

        List<HomeStarBannerAdapter.StarBannerItem> assetItems = loadAssetStarPhotos();
        if (!assetItems.isEmpty()) {
            return assetItems;
        }

        return Arrays.asList(
                HomeStarBannerAdapter.StarBannerItem.fromDrawable(
                        R.drawable.bg_home_star_card_blue,
                        getString(R.string.home_star_banner_title_1),
                        getString(R.string.home_star_banner_subtitle_1)),
                HomeStarBannerAdapter.StarBannerItem.fromDrawable(
                        R.drawable.bg_home_star_card_green,
                        getString(R.string.home_star_banner_title_2),
                        getString(R.string.home_star_banner_subtitle_2)),
                HomeStarBannerAdapter.StarBannerItem.fromDrawable(
                        R.drawable.bg_home_star_card_gold,
                        getString(R.string.home_star_banner_title_3),
                        getString(R.string.home_star_banner_subtitle_3))
        );
    }

    private List<HomeStarBannerAdapter.StarBannerItem> loadAssetStarPhotos() {
        List<HomeStarBannerAdapter.StarBannerItem> items = new ArrayList<>();
        try {
            String[] names = requireContext().getAssets().list(STAR_PHOTO_ASSET_DIR);
            if (names == null || names.length == 0) {
                return items;
            }
            Arrays.sort(names);
            for (String name : names) {
                String lower = name.toLowerCase(Locale.US);
                if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")
                        && !lower.endsWith(".png") && !lower.endsWith(".webp")) {
                    continue;
                }
                String assetPath = STAR_PHOTO_ASSET_DIR + "/" + name;
                items.add(HomeStarBannerAdapter.StarBannerItem.fromAssetPath(assetPath, "", ""));
            }
        } catch (IOException ignored) {
            // Fallback when asset folder is missing.
        }
        return items;
    }

    private List<File> loadLocalStarPhotos() {
        List<String> paths = repository.getLocalStarPhotoPaths();
        if (paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }

        List<File> files = new ArrayList<>();
        for (String path : paths) {
            if (TextUtils.isEmpty(path)) {
                continue;
            }
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                continue;
            }
            String name = file.getName().toLowerCase(Locale.US);
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png") || name.endsWith(".webp")) {
                files.add(file);
            }
        }
        files.sort((left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return files;
    }

    private void handlePickedStarPhotos(List<Uri> uris) {
        if (!isAdded()) {
            return;
        }
        if (uris == null || uris.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.home_star_upload_none), Toast.LENGTH_SHORT).show();
            return;
        }
        int copied = repository.importStarPhotos(uris);
        if (copied > 0) {
            Toast.makeText(requireContext(), getString(R.string.home_star_upload_success_format, copied), Toast.LENGTH_SHORT).show();
            setupStarBanner();
        } else {
            Toast.makeText(requireContext(), getString(R.string.home_star_upload_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStarIndicators(int position) {
        if (layoutHomeStarIndicator == null || homeStarBannerAdapter == null) {
            return;
        }
        int count = homeStarBannerAdapter.getItemCount();
        if (layoutHomeStarIndicator.getChildCount() != count) {
            layoutHomeStarIndicator.removeAllViews();
            for (int i = 0; i < count; i++) {
                View dot = new View(requireContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
                if (i > 0) {
                    params.setMarginStart(dpToPx(6));
                }
                dot.setLayoutParams(params);
                layoutHomeStarIndicator.addView(dot);
            }
        }
        for (int i = 0; i < layoutHomeStarIndicator.getChildCount(); i++) {
            layoutHomeStarIndicator.getChildAt(i).setBackgroundResource(
                    i == position ? R.drawable.bg_home_dot_active : R.drawable.bg_home_dot_inactive);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void scheduleStarBannerAutoScroll() {
        stopStarBannerAutoScroll();
        if (vpHomeStar == null || homeStarBannerAdapter == null || homeStarBannerAdapter.getItemCount() <= 1) {
            return;
        }
        autoScrollHandler.postDelayed(autoScrollRunnable, STAR_BANNER_AUTO_SCROLL_DELAY_MS);
    }

    private void stopStarBannerAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    private void openExternalCourse(String url) {
        if (!isAdded() || TextUtils.isEmpty(url)) {
            return;
        }
        Uri uri = Uri.parse(url.trim());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), getString(R.string.home_course_open_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void observeTrainingRefresh() {
        TrainingRefreshNotifier.events().observe(getViewLifecycleOwner(), event -> {
            if (event == null || repository == null) {
                return;
            }
            if (!event.matchesAccount(repository.getCurrentAccount())) {
                return;
            }
            if (event.affectsOverview()) {
                bindHomeSummary();
            }
            if (event.affectsMedia()) {
                setupStarBanner();
            }
        });
    }
}
