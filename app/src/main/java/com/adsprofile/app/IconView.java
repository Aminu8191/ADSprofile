package com.adsprofile.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

// Small icons drawn in code (no image files needed).
class IconView extends View {
    static final int GEAR = 0, BACK = 1, MAIL = 2, TELEGRAM = 3, RESET = 4;
    final int type;
    int color;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h);
        cv.save();
        cv.translate((w - s) / 2f, (h - s) / 2f);
        p.setColor(color);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        switch (type) {
            case GEAR: {
                Path g = new Path();
                g.setFillType(Path.FillType.EVEN_ODD);
                float c = s / 2f, r1 = s * 0.30f, r2 = s * 0.45f;
                double[] da = {-14, -9, 9, 14};
                float[] rr = {r1, r2, r2, r1};
                for (int i = 0; i < 8; i++) {
                    for (int k = 0; k < 4; k++) {
                        double t = Math.toRadians(i * 45 + da[k]);
                        float x = c + (float) (rr[k] * Math.cos(t));
                        float y = c + (float) (rr[k] * Math.sin(t));
                        if (i == 0 && k == 0) g.moveTo(x, y);
                        else g.lineTo(x, y);
                    }
                }
                g.close();
                g.addCircle(c, c, s * 0.15f, Path.Direction.CW);
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(g, p);
                break;
            }
            case BACK: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.11f);
                Path a = new Path();
                a.moveTo(s * 0.62f, s * 0.20f);
                a.lineTo(s * 0.32f, s * 0.50f);
                a.lineTo(s * 0.62f, s * 0.80f);
                cv.drawPath(a, p);
                break;
            }
            case MAIL: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.08f);
                cv.drawRoundRect(new RectF(s * 0.12f, s * 0.24f, s * 0.88f, s * 0.76f), s * 0.09f, s * 0.09f, p);
                Path f = new Path();
                f.moveTo(s * 0.16f, s * 0.31f);
                f.lineTo(s * 0.50f, s * 0.57f);
                f.lineTo(s * 0.84f, s * 0.31f);
                cv.drawPath(f, p);
                break;
            }
            case TELEGRAM: {
                p.setStyle(Paint.Style.FILL);
                Path t = new Path();
                t.moveTo(s * 0.90f, s * 0.16f);
                t.lineTo(s * 0.12f, s * 0.47f);
                t.lineTo(s * 0.35f, s * 0.58f);
                t.lineTo(s * 0.43f, s * 0.84f);
                t.lineTo(s * 0.56f, s * 0.68f);
                t.lineTo(s * 0.73f, s * 0.82f);
                t.close();
                cv.drawPath(t, p);
                break;
            }
            case RESET: {
                float c = s / 2f, r = s * 0.30f;
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.11f);
                cv.drawArc(new RectF(c - r, c - r, c + r, c + r), -50, 290, false, p);
                double th = Math.toRadians(240);
                float px = c + (float) (r * Math.cos(th)), py = c + (float) (r * Math.sin(th));
                float tx = (float) -Math.sin(th), ty = (float) Math.cos(th);
                float nx = -ty, ny = tx;
                float hl = s * 0.20f, hw = s * 0.15f;
                Path q = new Path();
                q.moveTo(px + tx * hl, py + ty * hl);
                q.lineTo(px + nx * hw, py + ny * hw);
                q.lineTo(px - nx * hw, py - ny * hw);
                q.close();
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(q, p);
                break;
            }
            default:
                break;
        }
        cv.restore();
    }
}
