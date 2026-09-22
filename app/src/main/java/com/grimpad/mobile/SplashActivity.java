package com.grimpad.mobile;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        SplashView splash = new SplashView(this);
        setContentView(splash);

        // After animation → go to main
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 4500);
    }

    static class SplashView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress = 0f;
        private final Handler h = new Handler(Looper.getMainLooper());
        private long startTime = -1;
        private final int DURATION = 4000;

        // Phases:
        // 0-800ms   → black screen
        // 800-1600ms → "A GRIM ENTERTAINMENT" fade in (like "A ROCKSTAR GAMES PRODUCTION")
        // 1600-2200ms → hold
        // 2200-3000ms → fade out, GRIMPAD fades in big
        // 3000-4000ms → GRIMPAD holds, tagline appears
        // 4000-4500ms → fade to black

        SplashView(android.content.Context ctx) {
            super(ctx);
            setBackgroundColor(Color.BLACK);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startTime = System.currentTimeMillis();
            tick();
        }

        private void tick() {
            invalidate();
            h.postDelayed(this::tick, 16);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (startTime < 0) return;

            long elapsed = System.currentTimeMillis() - startTime;
            float W = getWidth(), H = getHeight();

            canvas.drawColor(Color.BLACK);

            // ── Phase 1: "A GRIM ENTERTAINMENT PRODUCTION" ──────────────
            if (elapsed >= 600 && elapsed <= 2400) {
                float alpha;
                if (elapsed < 1000) {
                    alpha = (elapsed - 600) / 400f; // fade in
                } else if (elapsed < 1800) {
                    alpha = 1f; // hold
                } else {
                    alpha = 1f - (elapsed - 1800) / 600f; // fade out
                }
                alpha = Math.max(0f, Math.min(1f, alpha));

                // Top line — small
                paint.setColor(Color.argb((int)(alpha * 180), 200, 200, 200));
                paint.setTextSize(H * 0.022f);
                paint.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setLetterSpacing(0.25f);
                canvas.drawText("A  G R I M  E N T E R T A I N M E N T", W / 2f, H * 0.46f, paint);

                // Main line
                paint.setColor(Color.argb((int)(alpha * 255), 255, 255, 255));
                paint.setTextSize(H * 0.038f);
                paint.setTypeface(Typeface.create("serif", Typeface.BOLD));
                paint.setLetterSpacing(0.15f);
                canvas.drawText("P R O D U C T I O N", W / 2f, H * 0.54f, paint);

                // Thin line above and below (Rockstar style separator)
                paint.setColor(Color.argb((int)(alpha * 120), 255, 255, 255));
                paint.setStrokeWidth(0.8f);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(W * 0.28f, H * 0.42f, W * 0.72f, H * 0.42f, paint);
                canvas.drawLine(W * 0.28f, H * 0.58f, W * 0.72f, H * 0.58f, paint);
                paint.setStyle(Paint.Style.FILL);
            }

            // ── Phase 2: GRIMPAD logo ─────────────────────────────────────
            if (elapsed >= 2200) {
                float alpha;
                if (elapsed < 2700) {
                    alpha = (elapsed - 2200) / 500f; // fade in
                } else if (elapsed < 3800) {
                    alpha = 1f; // hold
                } else {
                    alpha = 1f - (elapsed - 3800) / 500f; // fade out
                }
                alpha = Math.max(0f, Math.min(1f, alpha));

                // Draw GrimPad shield/logo shape
                drawGrimPadLogo(canvas, W / 2f, H * 0.38f, H * 0.18f, alpha);

                // GRIMPAD text — bold, wide spaced
                paint.setColor(Color.argb((int)(alpha * 255), 255, 255, 255));
                paint.setTextSize(H * 0.09f);
                paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                paint.setLetterSpacing(0.18f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("GRIMPAD", W / 2f, H * 0.62f, paint);

                // Tagline
                if (elapsed > 2800) {
                    float tAlpha = Math.min(1f, (elapsed - 2800) / 400f) * alpha;
                    paint.setColor(Color.argb((int)(tAlpha * 180), 16, 185, 129));
                    paint.setTextSize(H * 0.022f);
                    paint.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
                    paint.setLetterSpacing(0.20f);
                    canvas.drawText("V I R T U A L  X B O X  C O N T R O L L E R", W / 2f, H * 0.72f, paint);
                }

                // Version
                if (elapsed > 3000) {
                    float vAlpha = Math.min(1f, (elapsed - 3000) / 400f) * alpha;
                    paint.setColor(Color.argb((int)(vAlpha * 100), 255, 255, 255));
                    paint.setTextSize(H * 0.014f);
                    paint.setLetterSpacing(0.1f);
                    canvas.drawText("v 1.0", W / 2f, H * 0.78f, paint);
                }
            }

            // ── Scanline overlay (cinematic feel) ─────────────────────────
            paint.setColor(Color.argb(12, 0, 0, 0));
            paint.setStyle(Paint.Style.FILL);
            for (float y = 0; y < H; y += 4) {
                canvas.drawRect(0, y, W, y + 2, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawGrimPadLogo(Canvas canvas, float cx, float cy, float size, float alpha) {
            // Controller silhouette — clean minimal lines
            paint.setColor(Color.argb((int)(alpha * 255), 16, 185, 129));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.04f);

            // Controller body outline
            float bw = size * 1.6f, bh = size * 0.9f;
            float left = cx - bw / 2f, top = cy - bh / 2f;

            // Main body rounded rect
            android.graphics.RectF body = new android.graphics.RectF(left, top, left + bw, top + bh);
            paint.setColor(Color.argb((int)(alpha * 255), 16, 185, 129));
            canvas.drawRoundRect(body, size * 0.2f, size * 0.2f, paint);

            // Left grip
            android.graphics.RectF lGrip = new android.graphics.RectF(left + size * 0.1f, top + bh * 0.6f, left + size * 0.6f, top + bh * 1.4f);
            canvas.drawRoundRect(lGrip, size * 0.15f, size * 0.15f, paint);

            // Right grip
            android.graphics.RectF rGrip = new android.graphics.RectF(left + bw - size * 0.6f, top + bh * 0.6f, left + bw - size * 0.1f, top + bh * 1.4f);
            canvas.drawRoundRect(rGrip, size * 0.15f, size * 0.15f, paint);

            // Left stick circle
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.03f);
            canvas.drawCircle(cx - size * 0.45f, cy + size * 0.05f, size * 0.18f, paint);

            // Right stick circle
            canvas.drawCircle(cx + size * 0.15f, cy + size * 0.2f, size * 0.18f, paint);

            // ABXY dots
            paint.setStyle(Paint.Style.FILL);
            float bx = cx + size * 0.55f, by = cy - size * 0.1f, br = size * 0.065f;
            paint.setColor(Color.argb((int)(alpha * 220), 16, 185, 129));
            canvas.drawCircle(bx, by - br * 2.2f, br, paint); // Y
            canvas.drawCircle(bx + br * 2.2f, by, br, paint); // B
            canvas.drawCircle(bx - br * 2.2f, by, br, paint); // X
            canvas.drawCircle(bx, by + br * 2.2f, br, paint); // A

            // D-pad cross
            paint.setColor(Color.argb((int)(alpha * 180), 16, 185, 129));
            float dx = cx - size * 0.55f, dy = cy - size * 0.15f, dw = size * 0.1f, dl = size * 0.3f;
            canvas.drawRect(dx - dl, dy - dw, dx + dl, dy + dw, paint); // horizontal
            canvas.drawRect(dx - dw, dy - dl, dx + dw, dy + dl, paint); // vertical

            paint.setStyle(Paint.Style.FILL);
        }
    }
}
