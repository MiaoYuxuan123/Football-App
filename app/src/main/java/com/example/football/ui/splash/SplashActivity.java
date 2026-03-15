package com.example.football.ui.splash;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.football.R;
import com.example.football.data.AppRepository;
import com.example.football.data.RepositoryProvider;
import com.example.football.ui.login.LoginActivity;
import com.example.football.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2000;
    private static final int PROGRESS_INTERVAL_MS = 40;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProgressBar pbSplashProgress;
    private TextView tvSplashProgressPercent;
    private View vSplashProgressComet;
    private TextView tvSplashBrand;
    private TextView tvSplashTitle;
    private ValueAnimator titleGlowAnimator;
    private ValueAnimator cometPulseAnimator;

    private AppRepository repository;

    private final Runnable progressRunnable = new Runnable() {
        private int elapsedMs = 0;

        @Override
        public void run() {
            elapsedMs = Math.min(SPLASH_DURATION_MS, elapsedMs + PROGRESS_INTERVAL_MS);
            int percent = Math.min(100, Math.round(elapsedMs * 100f / SPLASH_DURATION_MS));
            if (pbSplashProgress != null) {
                pbSplashProgress.setProgress(percent);
            }
            updateCometPosition(percent);
            if (tvSplashProgressPercent != null) {
                tvSplashProgressPercent.setText(getString(R.string.splash_percent_format, percent));
            }
            if (elapsedMs < SPLASH_DURATION_MS) {
                handler.postDelayed(this, PROGRESS_INTERVAL_MS);
            }
        }
    };

    private final Runnable navigateRunnable = () -> {
        boolean isLogin = repository.isLoggedIn();
        Intent intent;
        if (isLogin) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        repository = RepositoryProvider.get(this);

        pbSplashProgress = findViewById(R.id.pb_splash_progress);
        tvSplashProgressPercent = findViewById(R.id.tv_splash_progress_percent);
        vSplashProgressComet = findViewById(R.id.v_splash_progress_comet);

        tvSplashBrand = findViewById(R.id.tv_splash_brand);
        tvSplashTitle = findViewById(R.id.tv_splash_title);
        applyDualToneText(tvSplashBrand);
        applyDualToneText(tvSplashTitle);
        startTitleGlowAnimation();
        startCometPulseAnimation();

        if (pbSplashProgress != null) {
            pbSplashProgress.post(() -> updateCometPosition(0));
        }

        handler.post(progressRunnable);
        handler.postDelayed(navigateRunnable, SPLASH_DURATION_MS);
    }

    private void applyDualToneText(TextView textView) {
        if (textView == null) {
            return;
        }
        CharSequence rawText = textView.getText();
        if (rawText == null || rawText.length() == 0) {
            return;
        }
        int split = Math.max(1, rawText.length() / 2);
        SpannableString styled = new SpannableString(rawText);
        int white = ContextCompat.getColor(this, R.color.white);
        int orange = 0xFFFF6A00;
        styled.setSpan(new ForegroundColorSpan(white), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(orange), split, rawText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(styled);
    }

    private void startTitleGlowAnimation() {
        if (tvSplashBrand == null || tvSplashTitle == null) {
            return;
        }
        titleGlowAnimator = ValueAnimator.ofFloat(0f, 1f);
        titleGlowAnimator.setDuration(1200L);
        titleGlowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        titleGlowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        titleGlowAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        titleGlowAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            float brandRadius = 5f + (2.5f * t);
            float titleRadius = 7f + (3f * t);
            int glowColor = blendColor(0x33FF6A00, 0x99FF6A00, t);
            tvSplashBrand.setShadowLayer(brandRadius, 0f, 0f, glowColor);
            tvSplashTitle.setShadowLayer(titleRadius, 0f, 0f, glowColor);
        });
        titleGlowAnimator.start();
    }

    private void startCometPulseAnimation() {
        if (vSplashProgressComet == null) {
            return;
        }
        cometPulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        cometPulseAnimator.setDuration(680L);
        cometPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        cometPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        cometPulseAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            float scale = 0.88f + (0.24f * t);
            float alpha = 0.72f + (0.28f * t);
            vSplashProgressComet.setScaleX(scale);
            vSplashProgressComet.setScaleY(scale);
            vSplashProgressComet.setAlpha(alpha);
        });
        cometPulseAnimator.start();
    }

    private void updateCometPosition(int percent) {
        if (pbSplashProgress == null || vSplashProgressComet == null || pbSplashProgress.getWidth() <= 0) {
            return;
        }
        int left = pbSplashProgress.getLeft() + pbSplashProgress.getPaddingLeft();
        int trackWidth = pbSplashProgress.getWidth() - pbSplashProgress.getPaddingLeft() - pbSplashProgress.getPaddingRight();
        if (trackWidth <= 0) {
            return;
        }
        float ratio = Math.max(0f, Math.min(1f, percent / 100f));
        float centerX = left + (trackWidth * ratio);
        float cometHalf = vSplashProgressComet.getWidth() / 2f;
        float minX = left - cometHalf;
        float maxX = left + trackWidth - cometHalf;
        float targetX = Math.max(minX, Math.min(maxX, centerX - cometHalf));
        vSplashProgressComet.setTranslationX(targetX);
    }

    private int blendColor(int from, int to, float fraction) {
        int a = (int) (((from >> 24) & 0xFF) + ((((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * fraction));
        int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * fraction));
        int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * fraction));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * fraction));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
        handler.removeCallbacks(navigateRunnable);
        if (titleGlowAnimator != null) {
            titleGlowAnimator.cancel();
            titleGlowAnimator = null;
        }
        if (cometPulseAnimator != null) {
            cometPulseAnimator.cancel();
            cometPulseAnimator = null;
        }
    }
}