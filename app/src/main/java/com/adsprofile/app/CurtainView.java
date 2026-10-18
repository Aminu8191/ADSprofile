package com.adsprofile.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.animation.LinearInterpolator;

// The curtain that hides the Ads page while the reset runs: logo with a spinning gold ring. No text.
class CurtainView extends View {
    final GradientDrawable bg = Ui.bg();
    final Drawable logo;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF oval = new RectF();
    float angle;
    boolean done;
    ValueAnimator va;

    CurtainView(Context c) {
        super(c);
        logo = c.getDrawable(R.drawable.logo);
    }

    void setDone() {
        done = true;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        va = ValueAnimator.ofFloat(0f, 360f);
        va.setDuration(900);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(a -> {
            angle = (Float) a.getAnimatedValue();
            invalidate();
        });
        va.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (va != null) va.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        bg.setBounds(0, 0, (int) w, (int) h);
        bg.draw(cv);
        float cx = w / 2f, cy = h / 2f;
        int d = Ui.dp(getContext(), 84);
        if (logo != null) {
            logo.setBounds((int) (cx - d / 2f), (int) (cy - d / 2f), (int) (cx + d / 2f), (int) (cy + d / 2f));
            logo.draw(cv);
        }
        float rr = Ui.dp(getContext(), 58);
        oval.set(cx - rr, cy - rr, cx + rr, cy + rr);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(getContext(), 3));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0x33F2B705);
        cv.drawCircle(cx, cy, rr, p);
        p.setColor(0xFFF2B705);
        if (done) cv.drawCircle(cx, cy, rr, p);
        else cv.drawArc(oval, angle, 90, false, p);
    }
}
