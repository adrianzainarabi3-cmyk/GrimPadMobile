package com.grimpad.mobile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ControllerActivity extends Activity {
    private final float[] axes = {0f,0f,0f,0f};
    private final boolean[] buttons = new boolean[17];
    private final float[] btnVal = new float[17];
    private String serverAddr, ctrlType;
    private OkHttpClient httpClient;
    private WebSocket ws;
    private boolean wsOpen = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable reconnectTask;
    private String wsUrl;
    private TextView statusText;
    private TextView slotText;
    private Vibrator vibrator;
    private ControllerView controllerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        serverAddr = getIntent().getStringExtra("server");
        ctrlType = getIntent().getStringExtra("ctrlType");
        if (ctrlType == null) ctrlType = "series";
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF080814);
        controllerView = new ControllerView(this);
        root.addView(controllerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        statusText = new TextView(this);
        statusText.setTextColor(0xFF555555);
        statusText.setTextSize(10f);
        statusText.setTypeface(android.graphics.Typeface.MONOSPACE);
        statusText.setPadding(16,8,0,0);
        root.addView(statusText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        slotText = new TextView(this);
        slotText.setTextColor(0xFF000000);
        slotText.setTextSize(12f);
        slotText.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        slotText.setBackgroundColor(0xFF10B981);
        slotText.setPadding(20,6,20,6);
        slotText.setVisibility(View.GONE);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        sp.setMargins(0,8,16,0);
        root.addView(slotText, sp);
        setContentView(root);
        httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).pingInterval(15, TimeUnit.SECONDS).build();
        wsUrl = "ws://" + serverAddr;
        wsConnect();
        mainHandler.post(inputLoop);
    }

    private final Runnable inputLoop = new Runnable() {
        @Override public void run() { sendInput(); mainHandler.postDelayed(this, 16); }
    };

    private void sendInput() {
        if (!wsOpen || ws == null) return;
        try {
            JSONArray axArr = new JSONArray();
            for (float a : axes) axArr.put(a);
            JSONArray btnArr = new JSONArray();
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i] || btnVal[i] > 0) {
                    JSONObject b = new JSONObject();
                    b.put("index", i); b.put("pressed", buttons[i]); b.put("value", btnVal[i]);
                    btnArr.put(b);
                }
            }
            JSONObject msg = new JSONObject();
            msg.put("type","input"); msg.put("axes",axArr); msg.put("buttons",btnArr);
            ws.send(msg.toString());
        } catch (Exception ignored) {}
    }

    private void wsConnect() {
        statusText.setText("Connecting...");
        ws = httpClient.newWebSocket(new Request.Builder().url(wsUrl).build(), new WebSocketListener() {
            @Override public void onOpen(@NonNull WebSocket w, @NonNull Response r) {
                wsOpen = true;
                try { JSONObject reg = new JSONObject(); reg.put("type","register"); reg.put("controllerType",ctrlType); w.send(reg.toString()); } catch (Exception ignored) {}
                mainHandler.post(() -> statusText.setText("Registering..."));
            }
            @Override public void onMessage(@NonNull WebSocket w, @NonNull String text) {
                try {
                    JSONObject m = new JSONObject(text);
                    String type = m.getString("type");
                    if ("registered".equals(type)) {
                        int slot = m.getInt("slotIndex");
                        mainHandler.post(() -> { statusText.setText("Connected — Controller "+(slot+1)); slotText.setText("P"+(slot+1)); slotText.setVisibility(View.VISIBLE); });
                    } else if ("haptic".equals(type)) {
                        int dur = m.optInt("duration",80);
                        mainHandler.post(() -> vibe(dur));
                    }
                } catch (Exception ignored) {}
            }
            @Override public void onClosed(@NonNull WebSocket w, int c, @NonNull String r) { mainHandler.post(ControllerActivity.this::onDisc); }
            @Override public void onFailure(@NonNull WebSocket w, @NonNull Throwable t, Response r) { mainHandler.post(() -> { statusText.setText("Failed: "+t.getMessage()); onDisc(); }); }
        });
    }

    private void onDisc() {
        wsOpen = false; statusText.setText("Disconnected — retrying..."); slotText.setVisibility(View.GONE);
        if (reconnectTask != null) mainHandler.removeCallbacks(reconnectTask);
        reconnectTask = this::wsConnect;
        mainHandler.postDelayed(reconnectTask, 3000);
    }

    private void vibe(int ms) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(ms);
    }

    void setAxis(int i, float v) { if(i>=0&&i<4) axes[i]=v; }
    void setButton(int i, boolean p, float v) { if(i>=0&&i<buttons.length){buttons[i]=p;btnVal[i]=v;} }
    void onButtonDown() { vibe(25); }

    class ControllerView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int COL_BG=0xFF080814,COL_CARD=0xFF1A1A2E,COL_BORDER=0xFF2A2A3E;
        private final int COL_GREEN=0xFF10B981,COL_CYAN=0xFF00C8FF,COL_RED=0xFFEF4444;
        private final int COL_BLUE=0xFF3B82F6,COL_YELLOW=0xFFF5C518,COL_TEXT=0xFF888888;
        private float W,H;
        private float lsBaseX,lsBaseY,lsR,lsThumbX,lsThumbY;
        private float rsBaseX,rsBaseY,rsR,rsThumbX,rsThumbY;
        private int lsPointer=-1,rsPointer=-1,ltPointer=-1,rtPointer=-1,dpadPointer=-1;
        private float ltStartY=-1,rtStartY=-1,ltVal=0f,rtVal=0f;
        private float ltTop,rtTop,trigW,trigH;
        private float dpadCx,dpadCy,dpadArmW,dpadArmH;
        private float[][] circleButtons;
        private float[][] rectButtons;
        private final Map<Integer,Boolean> pressed = new HashMap<>();

        ControllerView(Context ctx) { super(ctx); setBackgroundColor(COL_BG); }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) { W=w; H=h; layout(); }

        private void layout() {
            trigW=W*0.18f; trigH=H*0.20f; ltTop=H*0.02f; rtTop=ltTop;
            lsR=Math.min(W,H)*0.13f; lsBaseX=W*0.14f; lsBaseY=H*0.62f; lsThumbX=lsBaseX; lsThumbY=lsBaseY;
            rsR=lsR; rsBaseX=W*0.72f; rsBaseY=H*0.72f; rsThumbX=rsBaseX; rsThumbY=rsBaseY;
            float abxyCx=W*0.86f,abxyCy=H*0.45f,abxyR=Math.min(W,H)*0.055f,abxyGap=abxyR*2.4f;
            circleButtons=new float[][]{{abxyCx,abxyCy+abxyGap,abxyR,0},{abxyCx+abxyGap,abxyCy,abxyR,1},{abxyCx-abxyGap,abxyCy,abxyR,2},{abxyCx,abxyCy-abxyGap,abxyR,3},{W*0.5f,H*0.38f,abxyR*0.85f,16}};
            dpadCx=W*0.28f; dpadCy=H*0.70f; dpadArmW=H*0.10f; dpadArmH=H*0.10f;
            float bumpY=ltTop+trigH+4,bumpH=H*0.09f,bumpW=W*0.16f,lbX=W*0.08f,rbX=W-lbX-bumpW;
            float selX=W*0.39f,selY=H*0.24f,selW=W*0.07f,selH=H*0.07f,stX=W*0.53f;
            float lsX=lsBaseX-lsR*0.6f,lsY2=lsBaseY+lsR+8,rsX2=rsBaseX-lsR*0.6f,rsY2=rsBaseY+rsR+8,bsBW=lsR*1.2f,bsBH=H*0.07f;
            rectButtons=new float[][]{{lbX,bumpY,lbX+bumpW,bumpY+bumpH,4},{rbX,bumpY,rbX+bumpW,bumpY+bumpH,5},{selX,selY,selX+selW,selY+selH,8},{stX,selY,stX+selW,selY+selH,9},{lsX,lsY2,lsX+bsBW,lsY2+bsBH,10},{rsX2,rsY2,rsX2+bsBW,rsY2+bsBH,11}};
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked(),pIdx=e.getActionIndex(),pId=e.getPointerId(pIdx);
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) handleDown(pId,e.getX(pIdx),e.getY(pIdx));
            else if(action==MotionEvent.ACTION_MOVE) for(int i=0;i<e.getPointerCount();i++) handleMove(e.getPointerId(i),e.getX(i),e.getY(i));
            else handleUp(pId,e.getX(pIdx),e.getY(pIdx));
            invalidate(); return true;
        }

        private void handleDown(int pid,float x,float y) {
            if(x<W*0.22f&&y<ltTop+trigH+H*0.05f){ltPointer=pid;ltStartY=y;onButtonDown();return;}
            if(x>W*0.78f&&y<rtTop+trigH+H*0.05f){rtPointer=pid;rtStartY=y;onButtonDown();return;}
            float ldx=x-lsBaseX,ldy=y-lsBaseY;
            if((float)Math.sqrt(ldx*ldx+ldy*ldy)<lsR*1.3f){lsPointer=pid;onButtonDown();return;}
            float rdx=x-rsBaseX,rdy=y-rsBaseY;
            if((float)Math.sqrt(rdx*rdx+rdy*rdy)<rsR*1.3f){rsPointer=pid;onButtonDown();return;}
            if(hitDpad(x,y)){dpadPointer=pid;updateDpad(x,y);onButtonDown();return;}
            for(float[] cb:circleButtons){float dx=x-cb[0],dy=y-cb[1];if((float)Math.sqrt(dx*dx+dy*dy)<cb[2]*1.2f){int bi=(int)cb[3];pressed.put(bi,true);setButton(bi,true,1f);onButtonDown();return;}}
            for(float[] rb:rectButtons){if(x>=rb[0]&&x<=rb[2]&&y>=rb[1]&&y<=rb[3]){int bi=(int)rb[4];pressed.put(bi,true);setButton(bi,true,1f);onButtonDown();return;}}
        }

        private void handleMove(int pid,float x,float y) {
            if(pid==ltPointer){float v=Math.max(0f,Math.min(1f,(y-ltStartY)/(trigH*0.8f)));ltVal=v;setButton(6,v>0.05f,v);}
            else if(pid==rtPointer){float v=Math.max(0f,Math.min(1f,(y-rtStartY)/(trigH*0.8f)));rtVal=v;setButton(7,v>0.05f,v);}
            else if(pid==lsPointer){float dx=x-lsBaseX,dy=y-lsBaseY,dist=(float)Math.sqrt(dx*dx+dy*dy),maxD=lsR-lsR*0.25f;if(dist>maxD){float a=(float)Math.atan2(dy,dx);dx=(float)Math.cos(a)*maxD;dy=(float)Math.sin(a)*maxD;}lsThumbX=lsBaseX+dx;lsThumbY=lsBaseY+dy;float nx=Math.abs(dx/maxD)<0.08f?0f:dx/maxD,ny=Math.abs(dy/maxD)<0.08f?0f:dy/maxD;setAxis(0,clamp(nx));setAxis(1,clamp(ny));}
            else if(pid==rsPointer){float dx=x-rsBaseX,dy=y-rsBaseY,dist=(float)Math.sqrt(dx*dx+dy*dy),maxD=rsR-rsR*0.25f;if(dist>maxD){float a=(float)Math.atan2(dy,dx);dx=(float)Math.cos(a)*maxD;dy=(float)Math.sin(a)*maxD;}rsThumbX=rsBaseX+dx;rsThumbY=rsBaseY+dy;float nx=Math.abs(dx/maxD)<0.08f?0f:dx/maxD,ny=Math.abs(dy/maxD)<0.08f?0f:dy/maxD;setAxis(2,clamp(nx));setAxis(3,clamp(ny));}
            else if(pid==dpadPointer) updateDpad(x,y);
        }

        private void handleUp(int pid,float x,float y) {
            if(pid==ltPointer){ltPointer=-1;ltVal=0f;ltStartY=-1;setButton(6,false,0f);}
            else if(pid==rtPointer){rtPointer=-1;rtVal=0f;rtStartY=-1;setButton(7,false,0f);}
            else if(pid==lsPointer){lsPointer=-1;lsThumbX=lsBaseX;lsThumbY=lsBaseY;setAxis(0,0f);setAxis(1,0f);}
            else if(pid==rsPointer){rsPointer=-1;rsThumbX=rsBaseX;rsThumbY=rsBaseY;setAxis(2,0f);setAxis(3,0f);}
            else if(pid==dpadPointer){dpadPointer=-1;clearDpad();}
            else {
                for(float[] cb:circleButtons){float dx=x-cb[0],dy=y-cb[1];if((float)Math.sqrt(dx*dx+dy*dy)<cb[2]*1.5f){int bi=(int)cb[3];pressed.remove(bi);setButton(bi,false,0f);}}
                for(float[] rb:rectButtons){if(x>=rb[0]-20&&x<=rb[2]+20&&y>=rb[1]-20&&y<=rb[3]+20){int bi=(int)rb[4];pressed.remove(bi);setButton(bi,false,0f);}}
            }
        }

        private boolean hitDpad(float x,float y){float hw=dpadArmW/2f,hh=dpadArmH/2f;return(x>=dpadCx-dpadArmW&&x<=dpadCx+dpadArmW&&y>=dpadCy-hh&&y<=dpadCy+hh)||(y>=dpadCy-dpadArmH&&y<=dpadCy+dpadArmH&&x>=dpadCx-hw&&x<=dpadCx+hw);}
        private void updateDpad(float x,float y){clearDpad();float dx=x-dpadCx,dy=y-dpadCy;if(Math.abs(dx)>Math.abs(dy)){int b=dx>0?15:14;pressed.put(b,true);setButton(b,true,1f);}else{int b=dy>0?13:12;pressed.put(b,true);setButton(b,true,1f);}}
        private void clearDpad(){for(int b:new int[]{12,13,14,15}){setButton(b,false,0f);pressed.remove(b);}}
        private float clamp(float v){return Math.max(-1f,Math.min(1f,v));}

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if(W==0) return;
            int accent="360".equals(ctrlType)?COL_GREEN:COL_CYAN;
            drawTrigger(c,W*0.04f,ltTop,trigW,trigH,ltVal,"LT",accent);
            drawTrigger(c,W-W*0.04f-trigW,rtTop,trigW,trigH,rtVal,"RT",accent);
            if(rectButtons!=null&&rectButtons.length>=2){drawRectBtn(c,rectButtons[0],"LB",pressed.containsKey(4),accent);drawRectBtn(c,rectButtons[1],"RB",pressed.containsKey(5),accent);}
            drawStick(c,lsBaseX,lsBaseY,lsR,lsThumbX,lsThumbY,"L",accent);
            drawStick(c,rsBaseX,rsBaseY,rsR,rsThumbX,rsThumbY,"R",accent);
            drawDpad(c);
            if(circleButtons!=null){int[] cols={COL_GREEN,COL_RED,COL_BLUE,COL_YELLOW,COL_GREEN};String[] lbls={"A","B","X","Y","⊙"};for(int i=0;i<circleButtons.length;i++){float[] cb=circleButtons[i];drawCircleBtn(c,cb[0],cb[1],cb[2],lbls[i],cols[i],pressed.containsKey((int)cb[3]));}}
            if(rectButtons!=null&&rectButtons.length>=6){String sl="360".equals(ctrlType)?"◀":"⊞",st="360".equals(ctrlType)?"▶":"☰";drawRectBtn(c,rectButtons[2],sl,pressed.containsKey(8),COL_TEXT);drawRectBtn(c,rectButtons[3],st,pressed.containsKey(9),COL_TEXT);drawRectBtn(c,rectButtons[4],"LS↓",pressed.containsKey(10),COL_TEXT);drawRectBtn(c,rectButtons[5],"RS↓",pressed.containsKey(11),COL_TEXT);}
            paint.setColor(COL_TEXT);paint.setTextSize(H*0.038f);paint.setTypeface(android.graphics.Typeface.MONOSPACE);paint.setTextAlign(Paint.Align.CENTER);
            c.drawText("360".equals(ctrlType)?"XBOX 360":"XBOX SERIES",W*0.5f,H*0.16f,paint);
        }

        private void drawTrigger(Canvas c,float x,float y,float w,float h,float val,String lbl,int accent){paint.setColor(COL_CARD);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+w,y+h,16,16,paint);if(val>0){paint.setColor(accent&0x55FFFFFF|0x33000000);c.drawRoundRect(x,y+h*(1-val),x+w,y+h,16,16,paint);}paint.setColor(val>0.05f?accent:COL_BORDER);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);c.drawRoundRect(x,y,x+w,y+h,16,16,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(val>0.05f?accent:COL_TEXT);paint.setTextSize(h*0.30f);paint.setTextAlign(Paint.Align.CENTER);c.drawText(lbl,x+w/2f,y+h*0.45f,paint);paint.setColor(0xFF444444);paint.setTextSize(h*0.18f);c.drawText(Math.round(val*100)+"%",x+w/2f,y+h*0.72f,paint);}
        private void drawStick(Canvas c,float bx,float by,float r,float tx,float ty,String lbl,int accent){paint.setColor(0xFF0D0D1A);paint.setStyle(Paint.Style.FILL);c.drawCircle(bx,by,r,paint);paint.setColor(accent);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f);c.drawCircle(bx,by,r,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(accent);c.drawCircle(tx,ty,r*0.38f,paint);paint.setColor(0xFF000000);paint.setTextSize(r*0.38f);paint.setTextAlign(Paint.Align.CENTER);c.drawText(lbl,tx,ty+r*0.13f,paint);}
        private void drawDpad(Canvas c){float hw=dpadArmW/2f,hh=dpadArmH/2f;paint.setColor(COL_CARD);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(dpadCx-dpadArmW,dpadCy-hh,dpadCx+dpadArmW,dpadCy+hh,8,8,paint);c.drawRoundRect(dpadCx-hw,dpadCy-dpadArmH,dpadCx+hw,dpadCy+dpadArmH,8,8,paint);paint.setColor(COL_GREEN);if(pressed.containsKey(12))c.drawRoundRect(dpadCx-hw,dpadCy-dpadArmH,dpadCx+hw,dpadCy,8,8,paint);if(pressed.containsKey(13))c.drawRoundRect(dpadCx-hw,dpadCy,dpadCx+hw,dpadCy+dpadArmH,8,8,paint);if(pressed.containsKey(14))c.drawRoundRect(dpadCx-dpadArmW,dpadCy-hh,dpadCx,dpadCy+hh,8,8,paint);if(pressed.containsKey(15))c.drawRoundRect(dpadCx,dpadCy-hh,dpadCx+dpadArmW,dpadCy+hh,8,8,paint);paint.setColor(COL_TEXT);paint.setTextSize(hh*1.1f);paint.setTextAlign(Paint.Align.CENTER);c.drawText("▲",dpadCx,dpadCy-dpadArmH*0.35f,paint);c.drawText("▼",dpadCx,dpadCy+dpadArmH*0.75f,paint);c.drawText("◀",dpadCx-dpadArmW*0.55f,dpadCy+hh*0.35f,paint);c.drawText("▶",dpadCx+dpadArmW*0.55f,dpadCy+hh*0.35f,paint);}
        private void drawCircleBtn(Canvas c,float cx,float cy,float r,String lbl,int color,boolean p){paint.setColor(p?color:(color&0x22FFFFFF|0x11000000));paint.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,r,paint);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(p?3f:2f);c.drawCircle(cx,cy,r,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(p?Color.BLACK:color);paint.setTextSize(r*0.75f);paint.setTextAlign(Paint.Align.CENTER);c.drawText(lbl,cx,cy+r*0.28f,paint);}
        private void drawRectBtn(Canvas c,float[] rb,String lbl,boolean p,int color){paint.setColor(p?color:COL_CARD);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(rb[0],rb[1],rb[2],rb[3],10,10,paint);paint.setColor(p?color:COL_BORDER);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);c.drawRoundRect(rb[0],rb[1],rb[2],rb[3],10,10,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(p?Color.BLACK:COL_TEXT);paint.setTextSize((rb[3]-rb[1])*0.40f);paint.setTextAlign(Paint.Align.CENTER);c.drawText(lbl,(rb[0]+rb[2])/2f,(rb[1]+rb[3])/2f+(rb[3]-rb[1])*0.14f,paint);}
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(inputLoop);
        if(reconnectTask!=null) mainHandler.removeCallbacks(reconnectTask);
        if(ws!=null) ws.cancel();
        httpClient.dispatcher().executorService().shutdown();
    }

    @Override public void onWindowFocusChanged(boolean h) {
        super.onWindowFocusChanged(h);
        if(h) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
