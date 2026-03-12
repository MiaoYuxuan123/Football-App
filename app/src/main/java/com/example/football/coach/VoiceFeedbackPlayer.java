package com.example.football.coach;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;

public class VoiceFeedbackPlayer {

    private final Context appContext;
    private MediaPlayer mediaPlayer;

    public VoiceFeedbackPlayer(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void playPreset(@NonNull String rawName) {
        int resId = appContext.getResources().getIdentifier(rawName, "raw", appContext.getPackageName());
        if (resId == 0) {
            return;
        }

        stopCurrent();
        mediaPlayer = MediaPlayer.create(appContext, resId);
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.reset();
            mp.release();
            if (mp == mediaPlayer) {
                mediaPlayer = null;
            }
        });
        mediaPlayer.start();
    }

    public void release() {
        stopCurrent();
    }

    private void stopCurrent() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
    }
}
