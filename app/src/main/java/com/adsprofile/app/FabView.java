package com.adsprofile.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;

// The floating button: rounded square with a "reset" arrow.
class FabView extends View {
    int fill;
    boolean down;
    float spinAngle;
    ValueAnimator spin;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    FabView(Context c, int fill) {
        super(c);
        this.fill = fill;
    }

    void setDown(boolean d) {
        down = d;
        invalidate();
    }

    // spins the arrow while a reset is running
    void setSpin(boolean on) {
        if (on) {
            if (spin != null) return;
            spin = ValueAnimator.ofFloat(0f, 360f);
            spin.setDuration(800);
            spin.setRepeatCount(ValueAnimator.INFINITE);
            spin.setInterpolator(new LinearInterpolator());
            spin.addUpdateListener(a -> {
                spinAngle = (Float) a.getAnimatedValue();
                invalidate();
            });
            spin.start();
        } else {
            if (spin != null) {
                spin.cancel();
                spin = null;
            }
            spinAngle = 0f;
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (spin != null) {
            spin.cancel();
            spin = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas cv) {
        boolean hot = down || spin != null;
        float w = getWidth(), h = getHeight();
        float rad = w * 0.26f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(hot ? 0xFFF2B705 : fill);
        cv.drawRoundRect(new RectF(2, 2, w - 2, h - 2), rad, rad, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.05f);
        p.setColor(0xFFF2B705);
        cv.drawRoundRect(new RectF(2, 2, w - 2, h - 2), rad, rad, p);

        float cx = w / 2f, cy = h / 2f, r = w * 0.22f;
        int col = hot ? 0xFF04361B : 0xFFFFFFFF;
        p.setColor(col);
        cv.save();
        cv.rotate(spinAngle, cx, cy);
        p.setStrokeWidth(w * 0.075f);
        p.setStrokeCap(Paint.Cap.ROUND);
        cv.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -50, 290, false, p);
        // arrow head at the end of the arc
        double th = Math.toRadians(240);
        float px = cx + (float) (r * Math.cos(th)), py = cy + (float) (r * Math.sin(th));
        float tx = (float) -Math.sin(th), ty = (float) Math.cos(th);   // direction of travel
        float nx = -ty, ny = tx;                                        // perpendicular
        float hl = w * 0.13f, hw = w * 0.10f;
        Path q = new Path();
        q.moveTo(px + tx * hl, py + ty * hl);
        q.lineTo(px + nx * hw, py + ny * hw);
        q.lineTo(px - nx * hw, py - ny * hw);
        q.close();
        p.setStyle(Paint.Style.FILL);
        cv.drawPath(q, p);
        cv.restore();
    }
}
