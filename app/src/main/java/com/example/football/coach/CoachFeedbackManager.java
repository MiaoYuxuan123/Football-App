package com.example.football.coach;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.football.R;

import java.util.HashMap;
import java.util.Map;

public class CoachFeedbackManager {

    public interface FeedbackCallback {
        void onFeedback(@NonNull String text);
    }

    private static final long GLOBAL_COOLDOWN_MS = 2600L;
    private static final long SUCCESS_COOLDOWN_MS = 3200L;
    private static final long CORRECTION_COOLDOWN_MS = 5600L;
    private static final long DUPLICATE_WINDOW_MS = 10_000L;
    private static final int BAD_FRAME_STREAK_THRESHOLD = 18;

    // 建议在 app/src/main/res/raw 下放入同名语音文件（mp3/wav/ogg）。
    private static final String VOICE_GOOD_SHOOT = "coach_good_shoot";
    private static final String VOICE_GOOD_DRIBBLE = "coach_good_dribble";
    private static final String VOICE_GOOD_PASS = "coach_good_pass";
    private static final String VOICE_FIX_SHOOT = "coach_fix_shoot";
    private static final String VOICE_FIX_DRIBBLE = "coach_fix_dribble";
    private static final String VOICE_FIX_PASS = "coach_fix_pass";
    private static final String VOICE_CHALLENGE_DONE = "coach_challenge_done";
    private static final String VOICE_START_INSTRUCTION = "start_instruction";

    private final Context context;
    private final FeedbackCallback callback;
    private final VoiceFeedbackPlayer voicePlayer;
    private final Map<String, Long> messageHistory = new HashMap<>();

    private long lastGlobalSpeakMs = 0L;
    private long lastSuccessSpeakMs = 0L;
    private long lastCorrectionSpeakMs = 0L;
    private int badFrameStreak = 0;
    private String currentMode = "";

    public CoachFeedbackManager(@NonNull Context context, @NonNull FeedbackCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.voicePlayer = new VoiceFeedbackPlayer(context);
    }

    public void startSession(@NonNull String mode) {
        currentMode = mode;
        badFrameStreak = 0;
        lastGlobalSpeakMs = 0L;
        lastSuccessSpeakMs = 0L;
        lastCorrectionSpeakMs = 0L;
        messageHistory.clear();
        emit(context.getString(R.string.train_ai_coach_session_start), VOICE_START_INSTRUCTION, true, false);
    }

    public void updateMode(@NonNull String mode) {
        currentMode = mode;
    }

    public void onFrameAnalyzed(float confidence, int frameScore) {
        if (confidence >= 0.5f && frameScore >= 60) {
            badFrameStreak = 0;
            return;
        }
        badFrameStreak++;
        if (badFrameStreak >= BAD_FRAME_STREAK_THRESHOLD) {
            badFrameStreak = 0;
            if (allowCorrectionSpeak()) {
                emit(getCorrectionTextByMode(), getCorrectionVoiceByMode(), false, false);
            }
        }
    }

    public void onRepCounted(int actionCount) {
        if (actionCount <= 0 || actionCount % 2 != 0) {
            return;
        }
        if (allowSuccessSpeak()) {
            emit(getSuccessTextByMode(), getSuccessVoiceByMode(), false, false);
        }
    }

    public void onChallengeCompleted(int actionCount, int avgScore) {
        String title = resolveChallengeTitle(actionCount, avgScore);
        String message = context.getString(R.string.train_ai_challenge_done_format, title);
        emit(message, VOICE_CHALLENGE_DONE, true, false);
    }

    public void showIdleHint() {
        callback.onFeedback(context.getString(R.string.train_ai_coach_ready));
    }

    public void release() {
        voicePlayer.release();
    }

    private boolean allowSuccessSpeak() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastSuccessSpeakMs < SUCCESS_COOLDOWN_MS) {
            return false;
        }
        lastSuccessSpeakMs = now;
        return true;
    }

    private boolean allowCorrectionSpeak() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastCorrectionSpeakMs < CORRECTION_COOLDOWN_MS) {
            return false;
        }
        lastCorrectionSpeakMs = now;
        return true;
    }

    private void emit(@NonNull String text, @Nullable String voiceRawName, boolean forceSpeak, boolean textOnly) {
        callback.onFeedback(text);
        if (textOnly || voiceRawName == null || voiceRawName.trim().isEmpty()) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        Long lastSameMessage = messageHistory.get(text);
        if (!forceSpeak) {
            if (now - lastGlobalSpeakMs < GLOBAL_COOLDOWN_MS) {
                return;
            }
            if (lastSameMessage != null && now - lastSameMessage < DUPLICATE_WINDOW_MS) {
                return;
            }
        }

        messageHistory.put(text, now);
        lastGlobalSpeakMs = now;
        voicePlayer.playPreset(voiceRawName);
    }

    private String getSuccessTextByMode() {
        if (currentMode.equals(context.getString(R.string.train_mode_dribble))) {
            return context.getString(R.string.train_ai_feedback_good_dribble);
        }
        if (currentMode.equals(context.getString(R.string.train_mode_pass))) {
            return context.getString(R.string.train_ai_feedback_good_pass);
        }
        return context.getString(R.string.train_ai_feedback_good_shoot);
    }

    private String getSuccessVoiceByMode() {
        if (currentMode.equals(context.getString(R.string.train_mode_dribble))) {
            return VOICE_GOOD_DRIBBLE;
        }
        if (currentMode.equals(context.getString(R.string.train_mode_pass))) {
            return VOICE_GOOD_PASS;
        }
        return VOICE_GOOD_SHOOT;
    }

    private String getCorrectionTextByMode() {
        if (currentMode.equals(context.getString(R.string.train_mode_dribble))) {
            return context.getString(R.string.train_ai_feedback_fix_dribble);
        }
        if (currentMode.equals(context.getString(R.string.train_mode_pass))) {
            return context.getString(R.string.train_ai_feedback_fix_pass);
        }
        return context.getString(R.string.train_ai_feedback_fix_shoot);
    }

    private String getCorrectionVoiceByMode() {
        if (currentMode.equals(context.getString(R.string.train_mode_dribble))) {
            return VOICE_FIX_DRIBBLE;
        }
        if (currentMode.equals(context.getString(R.string.train_mode_pass))) {
            return VOICE_FIX_PASS;
        }
        return VOICE_FIX_SHOOT;
    }

    private String resolveChallengeTitle(int actionCount, int avgScore) {
        boolean advanced = actionCount >= 20 && avgScore >= 80;
        if (currentMode.equals(context.getString(R.string.train_mode_dribble))) {
            return advanced
                    ? context.getString(R.string.train_ai_challenge_title_dribble)
                    : context.getString(R.string.train_ai_challenge_title_dribble_starter);
        }
        if (currentMode.equals(context.getString(R.string.train_mode_pass))) {
            return advanced
                    ? context.getString(R.string.train_ai_challenge_title_pass)
                    : context.getString(R.string.train_ai_challenge_title_pass_starter);
        }
        return advanced
                ? context.getString(R.string.train_ai_challenge_title_shoot)
                : context.getString(R.string.train_ai_challenge_title_shoot_starter);
    }
}
