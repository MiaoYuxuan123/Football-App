package com.example.football.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.Arrays;
import java.util.List;

public class PoseFrameDrawer {

    private static final List<int[]> POSE_CONNECTIONS = Arrays.asList(
            new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 3}, new int[]{3, 7},
            new int[]{0, 4}, new int[]{4, 5}, new int[]{5, 6}, new int[]{6, 8},
            new int[]{9, 10},
            new int[]{11, 12}, new int[]{11, 13}, new int[]{13, 15}, new int[]{15, 17}, new int[]{15, 19}, new int[]{15, 21},
            new int[]{12, 14}, new int[]{14, 16}, new int[]{16, 18}, new int[]{16, 20}, new int[]{16, 22},
            new int[]{11, 23}, new int[]{12, 24}, new int[]{23, 24},
            new int[]{23, 25}, new int[]{25, 27}, new int[]{27, 29}, new int[]{29, 31},
            new int[]{24, 26}, new int[]{26, 28}, new int[]{28, 30}, new int[]{30, 32}
    );

    private static final float MIN_VISIBILITY = 0.5f;
    private static final float MIN_PRESENCE = 0.5f;

    private final Paint linePaint = new Paint();
    private final Paint pointPaint = new Paint();

    public PoseFrameDrawer() {
        linePaint.setColor(Color.parseColor("#66FFEB3B"));
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        pointPaint.setColor(Color.parseColor("#FFFF5722"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);
    }

    public void draw(@NonNull Bitmap targetBitmap, PoseLandmarkerResult result, boolean mirror) {
        if (result == null || result.landmarks().isEmpty()) {
            return;
        }

        List<NormalizedLandmark> points = result.landmarks().get(0);
        if (points == null || points.isEmpty()) {
            return;
        }

        float width = targetBitmap.getWidth();
        float height = targetBitmap.getHeight();
        Canvas canvas = new Canvas(targetBitmap);

        for (int[] edge : POSE_CONNECTIONS) {
            if (edge[0] >= points.size() || edge[1] >= points.size()) {
                continue;
            }

            NormalizedLandmark p1 = points.get(edge[0]);
            NormalizedLandmark p2 = points.get(edge[1]);
            if (!isReliable(p1) || !isReliable(p2)) {
                continue;
            }

            float x1 = mapX(p1.x(), width, mirror);
            float y1 = mapY(p1.y(), height);
            float x2 = mapX(p2.x(), width, mirror);
            float y2 = mapY(p2.y(), height);
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }

        for (NormalizedLandmark point : points) {
            if (!isReliable(point)) {
                continue;
            }
            float x = mapX(point.x(), width, mirror);
            float y = mapY(point.y(), height);
            canvas.drawCircle(x, y, 6f, pointPaint);
        }
    }

    private boolean isReliable(NormalizedLandmark point) {
        float visibility = point.visibility().orElse(1f);
        float presence = point.presence().orElse(1f);
        return visibility >= MIN_VISIBILITY && presence >= MIN_PRESENCE;
    }

    private float mapX(float normalizedX, float width, boolean mirror) {
        float x = Math.max(0f, Math.min(1f, normalizedX)) * width;
        return mirror ? width - x : x;
    }

    private float mapY(float normalizedY, float height) {
        return Math.max(0f, Math.min(1f, normalizedY)) * height;
    }
}

