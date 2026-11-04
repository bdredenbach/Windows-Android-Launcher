package com.nthexperience.desktop;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.util.List;
import java.util.function.Function;

/** A touch-friendly stack of perspective window previews. */
final class FlipStack extends LinearLayout {
    private final List<DesktopWindow> windows;
    private final List<String> apps;
    private final WindowPreviews.Host host;
    private final Function<DesktopWindow, Bitmap> snapshot;
    private final Runnable dismiss;
    private final LruCache<DesktopWindow, Bitmap> images = new LruCache<>(8);
    private final Stage stage;
    private final TextView status;
    private final int count;
    private int selected;

    FlipStack(Activity activity, List<DesktopWindow> windows, List<String> apps,
              WindowPreviews.Host host, Function<DesktopWindow, Bitmap> snapshot, Runnable dismiss) {
        super(activity);
        this.windows = windows;
        this.apps = apps;
        this.host = host;
        this.snapshot = snapshot;
        this.dismiss = dismiss;
        count = windows.size() + apps.size();
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(4), dp(8), dp(8));

        stage = new Stage();
        stage.setBackground(Aero.gloss(activity,
                new int[]{0xff42677c, 0xff234052, 0xff142a3b, 0xff223b4f}, 4, 0xff6a8ba2));
        int height = Math.max(100, Math.min(290, getResources().getConfiguration().screenHeightDp - 180));
        addView(stage, new LayoutParams(-1, dp(height)));
        status = Aero.text(activity, "", 13, Aero.INK);
        status.setGravity(Gravity.CENTER);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        addView(status, new LayoutParams(-1, dp(32)));

        LinearLayout controls = new LinearLayout(activity);
        TextView previous = Aero.button(activity, "‹", () -> step(-1));
        previous.setContentDescription("Previous window");
        previous.setEnabled(count > 1);
        controls.addView(previous, new LayoutParams(dp(48), dp(46)));
        TextView open = Aero.button(activity, "Switch to window", () -> open(selected));
        open.setEnabled(count > 0);
        controls.addView(open, new LayoutParams(0, dp(46), 1));
        TextView next = Aero.button(activity, "›", () -> step(1));
        next.setContentDescription("Next window");
        next.setEnabled(count > 1);
        controls.addView(next, new LayoutParams(dp(48), dp(46)));
        addView(controls);
        stage.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
            if (r-l != or-ol || b-t != ob-ot) render(0);
        });
        updateStatus();
    }

    private int dp(int value) { return Aero.dp(getContext(), value); }
    private String title(int index) {
        return index < windows.size() ? windows.get(index).caption() : host.label(apps.get(index - windows.size()));
    }
    private void updateStatus() {
        status.setContentDescription(count == 0 ? null : "Selected window: " + title(selected));
        status.setText(count == 0 ? "No open windows" : title(selected) + "  •  " + (selected + 1) + " of " + count);
    }
    void step(int direction) {
        if (count < 2) return;
        selected = Math.floorMod(selected + direction, count);
        updateStatus();
        render(direction);
    }
    private void open(int index) {
        if (count == 0) return;
        dismiss.run();
        if (index < windows.size()) {
            DesktopWindow window = windows.get(index);
            if (window.getParent() != null) window.restore();
        } else host.open(apps.get(index - windows.size()));
    }
    private void render(int direction) {
        stage.removeAllViews();
        int width = stage.getWidth(), height = stage.getHeight();
        if (count == 0 || width == 0 || height == 0) return;
        for (int depth = Math.min(3, count - 1); depth >= 0; depth--) {
            int index = (selected + depth) % count;
            LinearLayout card = card(index);
            float scale = 1f - depth * .09f;
            FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(
                    Math.round(width * .70f), Math.round(height * .78f), Gravity.CENTER);
            stage.addView(card, size);
            card.setCameraDistance(dp(1400));
            card.setRotationY(-27f);
            card.setScaleX(scale);
            card.setScaleY(scale);
            float x = width * (-.105f + depth * .087f);
            card.setTranslationX(x);
            card.setTranslationY(height * (.045f - depth * .04f));
            card.setAlpha(1f - depth * .09f);
            if (depth == 0 && direction != 0 && Aero.motion(getContext())) {
                card.setTranslationX(x + direction * width * .13f);
                card.setAlpha(.65f);
                card.animate().translationX(x).alpha(1f).setDuration(180).start();
            }
        }
    }
    private LinearLayout card(int index) {
        LinearLayout card = Aero.column(getContext());
        card.setBackground(Aero.panel(getContext(), 0xffecf7ff, 0xffc7dce8, 4, 0xff77b7d9));
        card.setPadding(dp(3), dp(3), dp(3), dp(3));
        card.setFocusable(true);
        card.setOnClickListener(view -> open(index));
        TextView caption = Aero.text(getContext(), title(index), 12, Aero.INK);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setGravity(Gravity.CENTER_VERTICAL);
        caption.setPadding(dp(5), 0, dp(3), 0);
        caption.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(caption, new LayoutParams(-1, dp(25)));

        DesktopWindow window = index < windows.size() ? windows.get(index) : null;
        boolean privateWindow = window != null && !WindowPreviews.allowed(window);
        card.setContentDescription("Flip to " + title(index)
                + (privateWindow ? ". InPrivate — preview hidden" : ""));
        Bitmap bitmap = null;
        if (window != null && !privateWindow) {
            bitmap = images.get(window);
            if (bitmap == null) {
                bitmap = snapshot.apply(window);
                if (bitmap != null) images.put(window, bitmap);
            }
        }
        if (bitmap != null) {
            ImageView preview = new ImageView(getContext());
            preview.setImageBitmap(bitmap);
            preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.setBackgroundColor(0xff182c3d);
            preview.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.addView(preview, new LayoutParams(-1, 0, 1));
        } else {
            TextView fallback = Aero.text(getContext(), privateWindow ? "InPrivate — preview hidden"
                    : window == null ? "Android app" : "Preview unavailable", 12, Aero.INK);
            fallback.setGravity(Gravity.CENTER);
            card.addView(fallback, new LayoutParams(-1, 0, 1));
        }
        return card;
    }

    private final class Stage extends FrameLayout {
        private final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        private float startX, startY;
        Stage() { super(FlipStack.this.getContext()); }
        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startX = event.getX();
                startY = event.getY();
            }
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE || count < 2) return false;
            float x = Math.abs(event.getX() - startX);
            return x > slop && x > Math.abs(event.getY() - startY);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(event.getX() - startX) > slop)
                step(event.getX() < startX ? 1 : -1);
            return true;
        }
        @Override public boolean onGenericMotionEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_SCROLL && count > 1) {
                float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (scroll == 0) scroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                if (scroll != 0) { step(scroll < 0 ? 1 : -1); return true; }
            }
            return super.onGenericMotionEvent(event);
        }
    }
}
