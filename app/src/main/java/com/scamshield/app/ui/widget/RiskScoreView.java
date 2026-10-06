package com.scamshield.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.scamshield.app.R;
import com.scamshield.app.data.model.RiskLevel;

public class RiskScoreView extends View {

    private Paint backgroundArcPaint;
    private Paint progressArcPaint;
    private Paint scoreTextPaint;
    private Paint labelTextPaint;
    private RectF arcBounds;

    private int score = 0;
    private float animatedScore = 0f;
    private RiskLevel riskLevel = RiskLevel.LOW_RISK;

    public RiskScoreView(Context context) {
        super(context);
        init();
    }

    public RiskScoreView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RiskScoreView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeWidth(12 * density);
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundArcPaint.setColor(ContextCompat.getColor(getContext(), R.color.divider_light));

        progressArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressArcPaint.setStyle(Paint.Style.STROKE);
        progressArcPaint.setStrokeWidth(12 * density);
        progressArcPaint.setStrokeCap(Paint.Cap.ROUND);
        progressArcPaint.setColor(ContextCompat.getColor(getContext(), R.color.risk_low));

        scoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scoreTextPaint.setTextAlign(Paint.Align.CENTER);
        scoreTextPaint.setTextSize(36 * density);
        scoreTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary_light));
        scoreTextPaint.setFakeBoldText(true);

        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTextSize(13 * density);
        labelTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary_light));

        arcBounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        float padding = 16 * density;
        float size = Math.min(w, h);
        float left = (w - size) / 2f + padding;
        float top = (h - size) / 2f + padding;
        float right = left + size - (2 * padding);
        float bottom = top + size - (2 * padding);
        arcBounds.set(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background track (240 degree gauge from 150 deg to 390 deg)
        canvas.drawArc(arcBounds, 150, 240, false, backgroundArcPaint);

        // Draw progress arc
        float sweepAngle = (animatedScore / 100f) * 240f;
        canvas.drawArc(arcBounds, 150, sweepAngle, false, progressArcPaint);

        // Draw Score Number
        float centerX = arcBounds.centerX();
        float centerY = arcBounds.centerY();

        String scoreStr = String.valueOf((int) animatedScore);
        canvas.drawText(scoreStr, centerX, centerY + 8, scoreTextPaint);

        // Draw Subtitle "/ 100"
        canvas.drawText("/ 100", centerX, centerY + 36, labelTextPaint);
    }

    public void setRiskData(int newScore, RiskLevel level) {
        this.score = Math.max(0, Math.min(100, newScore));
        this.riskLevel = level != null ? level : RiskLevel.fromScore(this.score);

        int color = ContextCompat.getColor(getContext(), this.riskLevel.getColorResId());
        progressArcPaint.setColor(color);

        ValueAnimator animator = ValueAnimator.ofFloat(0, this.score);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedScore = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }
}
