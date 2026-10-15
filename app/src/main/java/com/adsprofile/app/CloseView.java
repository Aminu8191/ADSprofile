package com.adsprofile.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

// The "X" target at the bottom of the screen. Drop the floating button on it to remove it.
class CloseView extends View {
    boolean hot;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    CloseView(Context c) {
        super(c);
    }

    void setHot(boolean h) {
        if (hot != h) {
            hot = h;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), cx = w / 2f, cy = getHeight() / 2f, r = w / 2f - 2;
        p.setStyle(Paint.Style.FILL);
        p.setColor(hot ? 0xFFE53935 : 0xCC1B1B1B);
        cv.drawCircle(cx, cy, hot ? r : r * 0.88f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.07f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(0xFFFFFFFF);
        float d = w * 0.17f;
        cv.drawLine(cx - d, cy - d, cx + d, cy + d, p);
        cv.drawLine(cx - d, cy + d, cx + d, cy - d, p);
    }
}
