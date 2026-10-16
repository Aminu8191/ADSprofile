package com.adsprofile.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

class Ui {
    static final int GOLD = 0xFFF2B705;
    static final int MINT = 0xFFB7DCC4;
    static final int DARK = 0xFF04361B;

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

    // Shiny gold fill for the main buttons.
    static GradientDrawable gold(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFD45C, 0xFFF2B705, 0xFFD99A00});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    // Adds a soft touch ripple that stays inside the rounded corners.
    static Drawable ripple(Context c, Drawable content, float radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, shape(c, 0xFFFFFFFF, 0, radiusDp));
    }

    static TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    // One half of the Personal / Work row.
    static Button half(Context c, String text, boolean filled, boolean left) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
        b.setPadding(0, 0, 0, 0);
        if (filled) {
            b.setBackground(ripple(c, gold(c, 24), 24));
            b.setTextColor(DARK);
        } else {
            b.setBackground(ripple(c, shape(c, 0x1AFFFFFF, GOLD, 24), 24));
            b.setTextColor(GOLD);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 64), 1f);
        lp.setMargins(left ? 0 : dp(c, 6), dp(c, 8), left ? dp(c, 6) : 0, dp(c, 8));
        b.setLayoutParams(lp);
        return b;
    }
}
