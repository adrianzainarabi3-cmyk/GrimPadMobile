package com.grimpad.mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        SplashView view = new SplashView(this);
        setContentView(view);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 4500);
    }

    static class SplashView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler h = new Handler(Looper.getMainLooper());
        private long t0 = -1;
        private Bitmap iconBitmap;

        SplashView(android.content.Context ctx) {
            super(ctx);
            setBackgroundColor(Color.BLACK);
            // Load app icon as bitmap for splash
            try {
                iconBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ic_launcher);
            } catch (Exception ignored) {}
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

            // Phase 1: "A GRIM ENTERTAINMENT PRODUCTION" (500-2200ms)
            if (ms > 500 && ms < 2400) {
                float a;
                if (ms < 1000)      a = (ms - 500f) / 500f;
                else if (ms < 1800) a = 1f;
                else                a = 1f - (ms - 1800f) / 600f;
                a = Math.max(0f, Math.min(1f, a));

                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.8f);
                p.setColor(Color.argb((int)(a*80), 255,255,255));
                c.drawLine(W*0.20f, H*0.43f, W*0.80f, H*0.43f, p);
                c.drawLine(W*0.20f, H*0.60f, W*0.80f, H*0.60f, p);
                p.setStyle(Paint.Style.FILL);

                p.setColor(Color.argb((int)(a*150), 180,180,180));
                p.setTextSize(H * 0.020f);
                p.setTypeface(Typeface.create("serif", Typeface.NORMAL));
                p.setTextAlign(Paint.Align.CENTER);
                p.setLetterSpacing(0.28f);
                c.drawText("A  G R I M  E N T E R T A I N M E N T", W/2f, H*0.48f, p);

                p.setColor(Color.argb((int)(a*255), 255,255,255));
                p.setTextSize(H * 0.034f);
                p.setTypeface(Typeface.create("serif", Typeface.BOLD));
                p.setLetterSpacing(0.20f);
                c.drawText("P R O D U C T I O N", W/2f, H*0.56f, p);
            }

            // Phase 2: Logo + GRIMPAD (2000-4500ms)
            if (ms > 2000) {
                float a;
                if (ms < 2600)      a = (ms - 2000f) / 600f;
                else if (ms < 3900) a = 1f;
                else                a = 1f - (ms - 3900f) / 600f;
                a = Math.max(0f, Math.min(1f, a));

                // Draw actual app icon/logo
                if (iconBitmap != null) {
                    float iconSize = H * 0.28f;
                    float iconLeft = W/2f - iconSize/2f;
                    float iconTop  = H * 0.20f;
                    p.setAlpha((int)(a * 255));
                    c.drawBitmap(iconBitmap,
                        new android.graphics.Rect(0,0,iconBitmap.getWidth(),iconBitmap.getHeight()),
                        new RectF(iconLeft, iconTop, iconLeft+iconSize, iconTop+iconSize), p);
                    p.setAlpha(255);
                }

                // GRIMPAD text
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb((int)(a*255), 255,255,255));
                p.setTextSize(H * 0.085f);
                p.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                p.setLetterSpacing(0.18f);
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText("GRIMPAD", W/2f, H*0.62f, p);

                // Green underline
                p.setColor(Color.argb((int)(a*180), 16,185,129));
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(H*0.003f);
                c.drawLine(W*0.30f, H*0.655f, W*0.70f, H*0.655f, p);
                p.setStyle(Paint.Style.FILL);

                if (ms > 2800) {
                    float ta = Math.min(1f, (ms-2800f)/500f) * a;
                    p.setColor(Color.argb((int)(ta*180), 16,185,129));
                    p.setTextSize(H * 0.018f);
                    p.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
                    p.setLetterSpacing(0.22f);
                    c.drawText("V I R T U A L   X B O X   C O N T R O L L E R", W/2f, H*0.72f, p);
                }

                if (ms > 3200) {
                    float va = Math.min(1f, (ms-3200f)/400f) * a;
                    p.setColor(Color.argb((int)(va*70), 255,255,255));
                    p.setTextSize(H * 0.012f);
                    p.setLetterSpacing(0.1f);
                    c.drawText("v 1.0.0", W/2f, H*0.78f, p);
                }
            }

            // Scanlines
            p.setColor(Color.argb(8, 0,0,0));
            for (float y = 0; y < H; y += 4) c.drawRect(0, y, W, y+2, p);
        }
    }
}
