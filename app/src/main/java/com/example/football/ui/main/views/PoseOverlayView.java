package com.example.football.ui.main.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PoseOverlayView extends View {

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

    private static final float MIN_DRAW_VISIBILITY = 0.5f;
    private static final float MIN_DRAW_PRESENCE = 0.5f;

    private final Paint linePaint = new Paint();
    private final Paint pointPaint = new Paint();

    private final Object lock = new Object();
    private List<NormalizedLandmark> landmarks = new ArrayList<>();
    private boolean mirror;
    private int sourceImageWidth = 1;
    private int sourceImageHeight = 1;

    public PoseOverlayView(Context context) {
        super(context);
        initPaint();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public PoseOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
    }

    private void initPaint() {
        linePaint.setColor(Color.parseColor("#66FFEB3B"));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        pointPaint.setColor(Color.parseColor("#FFFF5722"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);
    }

    public void setImageSourceInfo(int imageWidth, int imageHeight) {
        synchronized (lock) {
            sourceImageWidth = Math.max(1, imageWidth);
            sourceImageHeight = Math.max(1, imageHeight);
        }
    }

    public void setResults(List<NormalizedLandmark> newLandmarks, boolean isMirror) {
        synchronized (lock) {
            landmarks = newLandmarks == null ? new ArrayList<>() : new ArrayList<>(newLandmarks);
            mirror = isMirror;
        }
        postInvalidate();
    }

    public void clear() {
        synchronized (lock) {
            landmarks = new ArrayList<>();
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        final List<NormalizedLandmark> snapshot;
        final boolean mirrorSnapshot;
        final int srcWidth;
        final int srcHeight;
        synchronized (lock) {
            snapshot = landmarks;
            mirrorSnapshot = mirror;
            srcWidth = sourceImageWidth;
            srcHeight = sourceImageHeight;
        }

        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }

        float width = getWidth();
        float height = getHeight();

        for (int[] connection : POSE_CONNECTIONS) {
            if (connection[0] >= snapshot.size() || connection[1] >= snapshot.size()) {
                continue;
            }
            NormalizedLandmark start = snapshot.get(connection[0]);
            NormalizedLandmark end = snapshot.get(connection[1]);
            if (!isReliable(start) || !isReliable(end)) {
                continue;
            }

            float[] startPoint = mapToView(start.x(), start.y(), width, height, srcWidth, srcHeight, mirrorSnapshot);
            float[] endPoint = mapToView(end.x(), end.y(), width, height, srcWidth, srcHeight, mirrorSnapshot);

            canvas.drawLine(startPoint[0], startPoint[1], endPoint[0], endPoint[1], linePaint);
        }

        for (NormalizedLandmark point : snapshot) {
            if (!isReliable(point)) {
                continue;
            }
            float[] mappedPoint = mapToView(point.x(), point.y(), width, height, srcWidth, srcHeight, mirrorSnapshot);
            canvas.drawCircle(mappedPoint[0], mappedPoint[1], 8f, pointPaint);
        }
    }

    private boolean isReliable(NormalizedLandmark landmark) {
        float visibility = landmark.visibility().orElse(1f);
        float presence = landmark.presence().orElse(1f);
        return visibility >= MIN_DRAW_VISIBILITY && presence >= MIN_DRAW_PRESENCE;
    }

    private float[] mapToView(
            float normalizedX,
            float normalizedY,
            float viewWidth,
            float viewHeight,
            int imageWidth,
            int imageHeight,
            boolean isMirror) {
        float clampedX = Math.max(0f, Math.min(1f, normalizedX));
        float clampedY = Math.max(0f, Math.min(1f, normalizedY));

        float srcX = clampedX * imageWidth;
        float srcY = clampedY * imageHeight;

        // Match PreviewView.ScaleType.FILL_CENTER: center-crop by using max scale.
        float scale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight);
        float offsetX = (viewWidth - imageWidth * scale) / 2f;
        float offsetY = (viewHeight - imageHeight * scale) / 2f;

        float viewX = srcX * scale + offsetX;
        float viewY = srcY * scale + offsetY;
        if (isMirror) {
            viewX = viewWidth - viewX;
        }
        return new float[]{viewX, viewY};
    }
}
