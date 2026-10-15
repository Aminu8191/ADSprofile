package com.adsprofile.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

// Soft golden glow that pulses behind the logo on the splash screen.
class GlowView extends View {
    float a = 0.15f;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    GlowView(Context c) {
        super(c);
    }

    void setA(float v) {
        a = v;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy);
        int alpha = (int) (Math.max(0f, Math.min(1f, a)) * 255);
        p.setShader(new RadialGradient(cx, cy, r, new int[]{(alpha << 24) | 0xF2B705, 0x00F2B705}, null, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, p);
    }
}
