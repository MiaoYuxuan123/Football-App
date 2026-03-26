package com.example.football.utils;

import android.media.MediaMetadataRetriever;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoUtil {
    public static class VideoMeta {
        public final int width;
        public final int height;
        public final int rotation; // degrees
        public final long durationMs;

        public VideoMeta(int width, int height, int rotation, long durationMs) {
            this.width = width;
            this.height = height;
            this.rotation = rotation;
            this.durationMs = durationMs;
        }
    }

    @Nullable
    public static VideoMeta extractMeta(@NonNull String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int iw = w != null ? Integer.parseInt(w) : 0;
            int ih = h != null ? Integer.parseInt(h) : 0;
            int rot = rotation != null ? Integer.parseInt(rotation) : 0;
            long dur = duration != null ? Long.parseLong(duration) : 0L;
            return new VideoMeta(iw, ih, rot, dur);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }
}

