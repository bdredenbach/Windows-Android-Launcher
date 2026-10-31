package com.nthexperience.desktop;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Aero {
    static final int INK = Color.rgb(24, 49, 65);
    static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static GradientDrawable panel(Context c, int top, int bottom, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        shape.setCornerRadius(dp(c, radius));
        if (stroke != 0) shape.setStroke(dp(c, 1), stroke);
        return shape;
    }
    static TextView text(Context c, String value, float size, int color) {
        TextView view = new TextView(c);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    static TextView button(Context c, String label, Runnable click) {
        TextView view = text(c, label, 14, INK);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(c, 48));
        view.setPadding(dp(c, 12), dp(c, 4), dp(c, 12), dp(c, 4));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44549bb9),
            panel(c, 0xfffeffff, 0xffd9e5eb, 5, 0xff92a9b7), null));
        view.setOnClickListener(v -> click.run());
        return view;
    }
    static LinearLayout column(Context c) {
        LinearLayout layout = new LinearLayout(c); layout.setOrientation(LinearLayout.VERTICAL); return layout;
    }
    static void bold(TextView view) { view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); }
    static void pad(View v, int dp) { int p = dp(v.getContext(), dp); v.setPadding(p, p, p, p); }
    private Aero() {}
}
