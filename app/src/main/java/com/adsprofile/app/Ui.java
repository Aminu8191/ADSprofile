package com.adsprofile.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

class Ui {
    static int dp(Context c, float v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    static GradientDrawable bg() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF02200F, 0xFF0A6B38});
    }

    static GradientDrawable shape(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(dp(c, 2), stroke);
        return g;
    }

    static TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    static Button button(Context c, String text, boolean filled) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(19);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
        if (filled) {
            b.setBackground(shape(c, 0xFFF2B705, 0, 24));
            b.setTextColor(0xFF04361B);
        } else {
            b.setBackground(shape(c, 0x1AFFFFFF, 0xFFF2B705, 24));
            b.setTextColor(0xFFF2B705);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c, 64));
        lp.setMargins(0, dp(c, 8), 0, dp(c, 8));
        b.setLayoutParams(lp);
        return b;
    }

    static TextView pill(Context c, String name, boolean sel) {
        TextView v = tv(c, name, 14, sel ? 0xFF04361B : 0xFFF2B705, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        v.setBackground(shape(c, sel ? 0xFFF2B705 : 0x1AFFFFFF, 0xFFF2B705, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(c, 6), 0, 0, 0);
        v.setLayoutParams(lp);
        return v;
    }
}
