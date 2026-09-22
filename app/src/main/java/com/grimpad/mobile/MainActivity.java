package com.grimpad.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private EditText serverInput, codeInput;
    private RadioGroup ctrlTypeGroup;
    private TextView codeDisplay, codeLabel;
    private LinearLayout hostCodeLayout, joinCodeLayout, mainButtons;
    private OkHttpClient codeClient;
    private WebSocket codeWS;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);

        serverInput   = findViewById(R.id.serverInput);
        ctrlTypeGroup = findViewById(R.id.ctrlTypeGroup);
        codeDisplay   = findViewById(R.id.codeDisplay);
        codeLabel     = findViewById(R.id.codeLabel);
        codeInput     = findViewById(R.id.codeInput);
        hostCodeLayout= findViewById(R.id.hostCodeLayout);
        joinCodeLayout= findViewById(R.id.joinCodeLayout);
        mainButtons   = findViewById(R.id.mainButtons);

        ((Button)findViewById(R.id.btnHost)).setOnClickListener(v -> startHost());
        ((Button)findViewById(R.id.btnJoin)).setOnClickListener(v -> showJoin());
        ((Button)findViewById(R.id.btnJoinConfirm)).setOnClickListener(v -> joinWithCode());
        ((Button)findViewById(R.id.btnHostBack)).setOnClickListener(v -> resetUI());
        ((Button)findViewById(R.id.btnJoinBack)).setOnClickListener(v -> resetUI());
    }

    private String getServer() {
        String s = serverInput.getText().toString().trim();
        return s.isEmpty() ? "localhost:8765" : s;
    }

    private String getCtrlType() {
        RadioButton sel = findViewById(ctrlTypeGroup.getCheckedRadioButtonId());
        return (sel != null && sel.getId() == R.id.radio360) ? "360" : "series";
    }

    private void startHost() {
        mainButtons.setVisibility(View.GONE);
        hostCodeLayout.setVisibility(View.VISIBLE);
        codeDisplay.setText("......");
        codeLabel.setText("CONNECTING...");

        codeClient = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS).build();

        codeWS = codeClient.newWebSocket(
            new Request.Builder().url("ws://"+getServer()).build(),
            new WebSocketListener() {
                @Override public void onOpen(@NonNull WebSocket ws, @NonNull Response r) {
                    try {
                        JSONObject reg = new JSONObject();
                        reg.put("type","register");
                        reg.put("controllerType", getCtrlType());
                        ws.send(reg.toString());
                    } catch(Exception ignored){}
                }
                @Override public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                    try {
                        JSONObject m = new JSONObject(text);
                        if("registered".equals(m.getString("type"))) {
                            int slot = m.getInt("slotIndex");
                            String ip = getServer().split(":")[0];
                            String code = String.format("%04d%02d", Math.abs(ip.hashCode())%10000, slot+1);
                            h.post(() -> {
                                codeDisplay.setText(code);
                                codeLabel.setText("SHARE THIS CODE");
                                h.postDelayed(() -> launchController(), 2000);
                            });
                        }
                    } catch(Exception ignored){}
                }
                @Override public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response r) {
                    h.post(() -> { codeDisplay.setText("ERROR"); codeLabel.setText("SERVER NOT FOUND"); });
                }
            });
    }

    private void showJoin() {
        mainButtons.setVisibility(View.GONE);
        joinCodeLayout.setVisibility(View.VISIBLE);
    }

    private void joinWithCode() {
        String code = codeInput.getText().toString().trim();
        if(code.length() < 6) { codeInput.setError("6 digits needed"); return; }
        launchController();
    }

    private void launchController() {
        if(codeWS != null) { codeWS.cancel(); codeWS = null; }
        Intent i = new Intent(this, ControllerActivity.class);
        i.putExtra("server", getServer());
        i.putExtra("ctrlType", getCtrlType());
        startActivity(i);
        resetUI();
    }

    private void resetUI() {
        if(codeWS != null) { codeWS.cancel(); codeWS = null; }
        mainButtons.setVisibility(View.VISIBLE);
        hostCodeLayout.setVisibility(View.GONE);
        joinCodeLayout.setVisibility(View.GONE);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if(codeWS != null) codeWS.cancel();
        if(codeClient != null) codeClient.dispatcher().executorService().shutdown();
    }
}
