package com.myrecord.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

final class DashboardRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private int percent;
    private int accent = Color.rgb(244, 245, 247);
    private String label = "오늘 체크";

    DashboardRingView(Context context) {
        super(context);
        setContentDescription("오늘 체크 달성률 0%");
    }

    void setData(int value, String categoryLabel, int ringColor) {
        percent = Math.max(0, Math.min(100, value));
        label = categoryLabel;
        accent = ringColor;
        setContentDescription(label + " 달성률 " + percent + "%");
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = Ui.dp(getContext(), 168);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float stroke = Math.max(Ui.dp(getContext(), 15), width * 0.105f);
        float inset = stroke / 2f + Ui.dp(getContext(), 3);
        bounds.set(inset, inset, width - inset, height - inset);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(47, 51, 58));
        canvas.drawArc(bounds, -90f, 360f, false, paint);
        if (percent > 0) {
            paint.setColor(accent);
            canvas.drawArc(bounds, -90f, Math.max(3f, percent * 3.6f), false, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Ui.TEXT);
        paint.setTextSize(width * 0.205f);
        Paint.FontMetrics percentMetrics = paint.getFontMetrics();
        float percentY = height / 2f - (percentMetrics.ascent + percentMetrics.descent) / 2f
                - Ui.dp(getContext(), 7);
        canvas.drawText(percent + "%", width / 2f, percentY, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Ui.MUTED);
        paint.setTextSize(width * 0.075f);
        canvas.drawText(label, width / 2f, height / 2f + Ui.dp(getContext(), 25), paint);
    }
}
