package com.adsprofile.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

// The floating button: rounded square with a "reset" arrow.
class FabView extends View {
    int fill;
    boolean down;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    FabView(Context c, int fill) {
        super(c);
        this.fill = fill;
    }

    void setDown(boolean d) {
        down = d;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float rad = w * 0.26f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(down ? 0xFFF2B705 : fill);
        cv.drawRoundRect(new RectF(2, 2, w - 2, h - 2), rad, rad, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w * 0.05f);
        p.setColor(0xFFF2B705);
        cv.drawRoundRect(new RectF(2, 2, w - 2, h - 2), rad, rad, p);

        float cx = w / 2f, cy = h / 2f, r = w * 0.22f;
        int col = down ? 0xFF04361B : 0xFFFFFFFF;
        p.setColor(col);
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
    }
}
