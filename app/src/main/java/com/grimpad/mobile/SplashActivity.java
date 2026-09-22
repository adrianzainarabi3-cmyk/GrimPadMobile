package com.grimpad.mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        SplashView view = new SplashView(this);
        setContentView(view);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 4800);
    }

    static class SplashView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler h = new Handler(Looper.getMainLooper());
        private long t0 = -1;

        SplashView(android.content.Context ctx) {
            super(ctx);
            setBackgroundColor(Color.BLACK);
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            t0 = System.currentTimeMillis();
            h.post(new Runnable() {
                @Override public void run() { invalidate(); h.postDelayed(this, 16); }
            });
        }

        @Override protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            h.removeCallbacksAndMessages(null);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (t0 < 0) return;
            long ms = System.currentTimeMillis() - t0;
            float W = getWidth(), H = getHeight();
            c.drawColor(Color.BLACK);

            // ── PHASE 1: "A GRIM ENTERTAINMENT PRODUCTION" (600–2400ms) ──
            if (ms > 600 && ms < 2600) {
                float a;
                if (ms < 1100)      a = (ms - 600f) / 500f;
                else if (ms < 2000) a = 1f;
                else                a = 1f - (ms - 2000f) / 600f;
                a = Math.max(0f, Math.min(1f, a));

                // Thin horizontal lines (Rockstar style)
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.8f);
                p.setColor(Color.argb((int)(a*90), 255,255,255));
                c.drawLine(W*0.22f, H*0.42f, W*0.78f, H*0.42f, p);
                c.drawLine(W*0.22f, H*0.60f, W*0.78f, H*0.60f, p);
                p.setStyle(Paint.Style.FILL);

                // Top small text
                p.setColor(Color.argb((int)(a*160), 200,200,200));
                p.setTextSize(H * 0.021f);
                p.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                p.setTextAlign(Paint.Align.CENTER);
                p.setLetterSpacing(0.28f);
                c.drawText("A  G R I M  E N T E R T A I N M E N T", W/2f, H*0.47f, p);

                // Main text
                p.setColor(Color.argb((int)(a*255), 255,255,255));
                p.setTextSize(H * 0.036f);
                p.setTypeface(Typeface.create("serif", Typeface.BOLD));
                p.setLetterSpacing(0.18f);
                c.drawText("P R O D U C T I O N", W/2f, H*0.55f, p);
            }

            // ── PHASE 2: GRIMPAD LOGO (2400–4800ms) ──────────────────────
            if (ms > 2200) {
                float a;
                if (ms < 2800)      a = (ms - 2200f) / 600f;
                else if (ms < 4200) a = 1f;
                else                a = 1f - (ms - 4200f) / 600f;
                a = Math.max(0f, Math.min(1f, a));

                // Controller icon
                drawController(c, W/2f, H*0.35f, H*0.20f, a);

                // GRIMPAD
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb((int)(a*255), 255,255,255));
                p.setTextSize(H * 0.092f);
                p.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                p.setLetterSpacing(0.20f);
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText("GRIMPAD", W/2f, H*0.64f, p);

                // Green underline
                p.setColor(Color.argb((int)(a*200), 16,185,129));
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(H*0.003f);
                c.drawLine(W*0.28f, H*0.67f, W*0.72f, H*0.67f, p);
                p.setStyle(Paint.Style.FILL);

                // Tagline
                if (ms > 3000) {
                    float ta = Math.min(1f, (ms-3000f)/500f) * a;
                    p.setColor(Color.argb((int)(ta*200), 16,185,129));
                    p.setTextSize(H * 0.020f);
                    p.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
                    p.setLetterSpacing(0.22f);
                    c.drawText("V I R T U A L   X B O X   C O N T R O L L E R", W/2f, H*0.73f, p);
                }

                // Version
                if (ms > 3400) {
                    float va = Math.min(1f, (ms-3400f)/400f) * a;
                    p.setColor(Color.argb((int)(va*80), 255,255,255));
                    p.setTextSize(H * 0.013f);
                    p.setLetterSpacing(0.1f);
                    c.drawText("v 1.0.0", W/2f, H*0.79f, p);
                }
            }

            // Scanlines
            p.setColor(Color.argb(10, 0,0,0));
            for (float y = 0; y < H; y += 4) c.drawRect(0, y, W, y+2, p);
        }

        private void drawController(Canvas c, float cx, float cy, float size, float alpha) {
            int g = Color.argb((int)(alpha*255), 16,185,129);
            int dk = Color.argb((int)(alpha*255), 8,14,10);
            float bw = size*1.7f, bh = size*0.85f;
            float left = cx-bw/2f, top = cy-bh/2f;
            float r = size*0.18f;
            float sw = size*0.045f;

            // Body fill
            p.setStyle(Paint.Style.FILL);
            p.setColor(dk);
            c.drawRoundRect(new RectF(left,top,left+bw,top+bh), r, r, p);

            // Body stroke
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sw);
            p.setColor(g);
            c.drawRoundRect(new RectF(left,top,left+bw,top+bh), r, r, p);

            // Left grip
            RectF lg = new RectF(left+size*0.08f, top+bh*0.6f, left+size*0.58f, top+bh+size*0.55f);
            p.setStyle(Paint.Style.FILL); p.setColor(dk); c.drawRoundRect(lg, r*0.7f, r*0.7f, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(g); c.drawRoundRect(lg, r*0.7f, r*0.7f, p);

            // Right grip
            RectF rg = new RectF(left+bw-size*0.58f, top+bh*0.6f, left+bw-size*0.08f, top+bh+size*0.55f);
            p.setStyle(Paint.Style.FILL); p.setColor(dk); c.drawRoundRect(rg, r*0.7f, r*0.7f, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(g); c.drawRoundRect(rg, r*0.7f, r*0.7f, p);

            float sr = size*0.13f;
            // Left stick
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb((int)(alpha*60), 16,185,129));
            c.drawCircle(cx-size*0.42f, cy+size*0.05f, sr, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw*0.7f); p.setColor(g);
            c.drawCircle(cx-size*0.42f, cy+size*0.05f, sr, p);

            // Right stick
            p.setStyle(Paint.Style.FILL); p.setColor(Color.argb((int)(alpha*60), 16,185,129));
            c.drawCircle(cx+size*0.12f, cy+size*0.20f, sr, p);
            p.setStyle(Paint.Style.STROKE); p.setColor(g);
            c.drawCircle(cx+size*0.12f, cy+size*0.20f, sr, p);

            // ABXY
            float bx=cx+size*0.52f, by=cy-size*0.05f, br=size*0.07f, gap=br*2.4f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb((int)(alpha*255),16,185,129));  c.drawCircle(bx, by-gap, br, p); // Y
            p.setColor(Color.argb((int)(alpha*255),239,68,68));   c.drawCircle(bx+gap, by, br, p); // B
            p.setColor(Color.argb((int)(alpha*255),59,130,246));  c.drawCircle(bx-gap, by, br, p); // X
            p.setColor(Color.argb((int)(alpha*255),16,185,129));  c.drawCircle(bx, by+gap, br, p); // A

            // Dpad
            float dpx=cx-size*0.52f, dpy=cy-size*0.12f, dw=size*0.07f, dl=size*0.21f;
            p.setColor(Color.argb((int)(alpha*200),16,185,129));
            c.drawRect(dpx-dl, dpy-dw, dpx+dl, dpy+dw, p);
            c.drawRect(dpx-dw, dpy-dl, dpx+dw, dpy+dl, p);
        }
    }
}
