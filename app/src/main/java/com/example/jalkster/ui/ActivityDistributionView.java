package com.example.jalkster.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.jalkster.R;

public class ActivityDistributionView extends View {

    private static final float STROKE_WIDTH = 40f;
    private static final float RADIUS_OFFSET = 20f;
    private static final float INNER_RADIUS_RATIO = 0.6f;

    private float jogTime = 0f;
    private float walkTime = 0f;
    private float restTime = 0f;
    private float xcSkiTime = 0f;
    private float animationProgress = 0f;

    private final Paint jogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint walkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint restPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint xcSkiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();
    private ValueAnimator currentAnimator;

    public ActivityDistributionView(Context context) {
        super(context);
        init(context);
    }

    public ActivityDistributionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ActivityDistributionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        jogPaint.setStyle(Paint.Style.STROKE);
        walkPaint.setStyle(Paint.Style.STROKE);
        restPaint.setStyle(Paint.Style.STROKE);
        xcSkiPaint.setStyle(Paint.Style.STROKE);

        jogPaint.setStrokeWidth(STROKE_WIDTH);
        walkPaint.setStrokeWidth(STROKE_WIDTH);
        restPaint.setStrokeWidth(STROKE_WIDTH);
        xcSkiPaint.setStrokeWidth(STROKE_WIDTH);

        jogPaint.setColor(ContextCompat.getColor(context, R.color.jog_accent));
        walkPaint.setColor(ContextCompat.getColor(context, R.color.walk_accent));
        restPaint.setColor(ContextCompat.getColor(context, R.color.rest_accent));
        xcSkiPaint.setColor(ContextCompat.getColor(context, R.color.xc_ski_accent));

        jogPaint.setStrokeCap(Paint.Cap.BUTT);
        walkPaint.setStrokeCap(Paint.Cap.BUTT);
        restPaint.setStrokeCap(Paint.Cap.BUTT);
        xcSkiPaint.setStrokeCap(Paint.Cap.BUTT);

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.black));

        centerPaint.setStyle(Paint.Style.FILL);
    }

    public void setTimes(long jogMillis, long walkMillis, long restMillis) {
        setTimes(jogMillis, walkMillis, restMillis, 0L);
    }

    public void setTimes(long jogMillis, long walkMillis, long restMillis, long xcSkiMillis) {
        this.jogTime = Math.max(0, jogMillis);
        this.walkTime = Math.max(0, walkMillis);
        this.restTime = Math.max(0, restMillis);
        this.xcSkiTime = Math.max(0, xcSkiMillis);
        startAnimation();
    }

    private void startAnimation() {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
        animationProgress = 0f;
        currentAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(1200);
        currentAnimator.setInterpolator(new DecelerateInterpolator());
        currentAnimator.addUpdateListener(a -> {
            animationProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        currentAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (currentAnimator != null) {
            currentAnimator.cancel();
            currentAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) / 2f - RADIUS_OFFSET;

        ovalRect.set(
                (width / 2f) - radius,
                (height / 2f) - radius,
                (width / 2f) + radius,
                (height / 2f) + radius
        );

        canvas.drawCircle(width / 2f, height / 2f, radius, bgPaint);

        float total = jogTime + walkTime + restTime + xcSkiTime;
        if (total <= 0f) {
            drawCenterCircle(canvas, width, height, radius, ContextCompat.getColor(getContext(), R.color.rest_accent));
            return;
        }

        float jogAngle = (jogTime / total) * 360f;
        float walkAngle = (walkTime / total) * 360f;
        float restAngle = (restTime / total) * 360f;
        float xcSkiAngle = (xcSkiTime / total) * 360f;

        float jogProgress = Math.min(1f, animationProgress);
        float walkProgress = Math.min(1f, Math.max(0f, animationProgress - 0.25f) * 1.333f);
        float restProgress = Math.min(1f, Math.max(0f, animationProgress - 0.5f) * 2f);
        float xcSkiProgress = Math.min(1f, Math.max(0f, animationProgress - 0.75f) * 4f);

        float animatedJogAngle = jogAngle * jogProgress;
        float animatedWalkAngle = walkAngle * walkProgress;
        float animatedRestAngle = restAngle * restProgress;
        float animatedXcSkiAngle = xcSkiAngle * xcSkiProgress;

        float startAngle = -90f;

        if (jogAngle > 0f) {
            canvas.drawArc(ovalRect, startAngle, animatedJogAngle, false, jogPaint);
            startAngle += jogAngle;
        }

        if (walkAngle > 0f) {
            canvas.drawArc(ovalRect, startAngle, animatedWalkAngle, false, walkPaint);
            startAngle += walkAngle;
        }

        if (restAngle > 0f) {
            canvas.drawArc(ovalRect, startAngle, animatedRestAngle, false, restPaint);
            startAngle += restAngle;
        }

        if (xcSkiAngle > 0f) {
            canvas.drawArc(ovalRect, startAngle, animatedXcSkiAngle, false, xcSkiPaint);
        }

        int jogColor = ContextCompat.getColor(getContext(), R.color.jog_accent);
        int walkColor = ContextCompat.getColor(getContext(), R.color.walk_accent);
        int restColor = ContextCompat.getColor(getContext(), R.color.rest_accent);
        int xcSkiColor = ContextCompat.getColor(getContext(), R.color.xc_ski_accent);

        float wJog = jogTime / total;
        float wWalk = walkTime / total;
        float wRest = restTime / total;
        float wXcSki = xcSkiTime / total;

        int r = (int) (Color.red(jogColor) * wJog
                + Color.red(walkColor) * wWalk
                + Color.red(restColor) * wRest
                + Color.red(xcSkiColor) * wXcSki);
        int g = (int) (Color.green(jogColor) * wJog
                + Color.green(walkColor) * wWalk
                + Color.green(restColor) * wRest
                + Color.green(xcSkiColor) * wXcSki);
        int b = (int) (Color.blue(jogColor) * wJog
                + Color.blue(walkColor) * wWalk
                + Color.blue(restColor) * wRest
                + Color.blue(xcSkiColor) * wXcSki);

        int blended = Color.rgb(r, g, b);

        drawCenterCircle(canvas, width, height, radius, blended);
    }

    private void drawCenterCircle(Canvas canvas, float width, float height, float outerRadius, int color) {
        float innerRadius = outerRadius * INNER_RADIUS_RATIO;
        centerPaint.setColor(color);
        centerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width / 2f, height / 2f, innerRadius, centerPaint);
    }
}
