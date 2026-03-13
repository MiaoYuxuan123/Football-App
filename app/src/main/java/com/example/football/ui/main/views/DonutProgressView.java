package com.example.football.ui.main.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

public class DonutProgressView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private int max = 100;
    private int progress = 0;
    private float strokeWidthPx;

    public DonutProgressView(Context context) {
        super(context);
        init();
    }

    public DonutProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidthPx = dpToPx();

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setColor(0xFF474C56);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setColor(0xFFFF6A13);
    }

    public void setProgress(int value) {
        progress = Math.max(0, Math.min(max, value));
        invalidate();
    }

    public void setMax(int value) {
        max = Math.max(1, value);
        progress = Math.min(progress, max);
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float halfStroke = strokeWidthPx / 2f;
        arcBounds.set(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);

        canvas.drawArc(arcBounds, -90f, 360f, false, trackPaint);

        float sweepAngle = 360f * progress / Math.max(1, max);
        if (sweepAngle > 0f) {
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint);
        }
    }

    private float dpToPx() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f,
                getResources().getDisplayMetrics());
    }
}
