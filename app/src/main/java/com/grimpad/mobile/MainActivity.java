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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;

public class MainActivity extends Activity {

    private EditText serverInput;
    private RadioGroup ctrlTypeGroup;
    private TextView codeDisplay;
    private TextView codeLabel;
    private EditText codeInput;
    private LinearLayout hostCodeLayout;
    private LinearLayout joinCodeLayout;
    private LinearLayout mainButtons;
    private Button btnBack;

    // For code generation
    private OkHttpClient codeClient;
    private WebSocket codeWS;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);

        serverInput    = findViewById(R.id.serverInput);
        ctrlTypeGroup  = findViewById(R.id.ctrlTypeGroup);
        codeDisplay    = findViewById(R.id.codeDisplay);
        codeLabel      = findViewById(R.id.codeLabel);
        codeInput      = findViewById(R.id.codeInput);
        hostCodeLayout = findViewById(R.id.hostCodeLayout);
        joinCodeLayout = findViewById(R.id.joinCodeLayout);
        mainButtons    = findViewById(R.id.mainButtons);
        btnBack        = findViewById(R.id.btnBack);

        ((Button)findViewById(R.id.btnHost)).setOnClickListener(v -> startHost());
        ((Button)findViewById(R.id.btnJoin)).setOnClickListener(v -> showJoinCode());
        ((Button)findViewById(R.id.btnJoinConfirm)).setOnClickListener(v -> joinWithCode());
        btnBack.setOnClickListener(v -> resetUI());
    }

    private String getServer() {
        String s = serverInput.getText().toString().trim();
        return s.isEmpty() ? "localhost:8765" : s;
    }

    private String getCtrlType() {
        RadioButton sel = findViewById(ctrlTypeGroup.getCheckedRadioButtonId());
        return (sel != null && sel.getId() == R.id.radio360) ? "360" : "series";
    }

    // ── HOST: connect directly as P1, get room code ──────────────────────
    private void startHost() {
        // Connect to relay, register, get slot → show code = slot+1 padded
        mainButtons.setVisibility(View.GONE);
        hostCodeLayout.setVisibility(View.VISIBLE);
        codeDisplay.setText("......");
        codeLabel.setText("CONNECTING...");

        codeClient = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build();

        codeWS = codeClient.newWebSocket(
            new Request.Builder().url("ws://" + getServer()).build(),
            new WebSocketListener() {
                @Override public void onOpen(@NonNull WebSocket ws, @NonNull Response r) {
                    try {
                        JSONObject reg = new JSONObject();
                        reg.put("type", "register");
                        reg.put("controllerType", getCtrlType());
                        ws.send(reg.toString());
                    } catch (Exception ignored) {}
                }
                @Override public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                    try {
                        JSONObject m = new JSONObject(text);
                        if ("registered".equals(m.getString("type"))) {
                            int slot = m.getInt("slotIndex");
                            // 6-digit room code: server IP hash + slot
                            String serverIP = getServer().split(":")[0];
                            String code = generateCode(serverIP, slot);
                            h.post(() -> {
                                codeDisplay.setText(code);
                                codeLabel.setText("SHARE THIS CODE WITH PLAYERS");
                                // Launch controller after 2s
                                h.postDelayed(() -> launchController(getServer(), getCtrlType()), 2000);
                            });
                        }
                    } catch (Exception ignored) {}
                }
                @Override public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, Response r) {
                    h.post(() -> { codeDisplay.setText("ERROR"); codeLabel.setText("SERVER NOT FOUND"); });
                }
            }
        );
    }

    // Generate memorable 6-digit code from IP + slot
    private String generateCode(String ip, int slot) {
        int hash = ip.hashCode() & 0xFFFF;
        return String.format("%04d%02d", hash % 10000, slot + 1);
    }

    // Decode server from code (simplified — same network assumed)
    private void showJoinCode() {
        mainButtons.setVisibility(View.GONE);
        joinCodeLayout.setVisibility(View.VISIBLE);
        codeInput.requestFocus();
    }

    private void joinWithCode() {
        String code = codeInput.getText().toString().trim();
        if (code.length() < 6) {
            codeInput.setError("Enter 6-digit code");
            return;
        }
        // For same-network play: use the server IP from input + code slot
        // The slot is last 2 digits of code
        launchController(getServer(), getCtrlType());
    }

    private void launchController(String server, String ctrlType) {
        if (codeWS != null) { codeWS.cancel(); codeWS = null; }
        Intent i = new Intent(this, ControllerActivity.class);
        i.putExtra("server", server);
        i.putExtra("ctrlType", ctrlType);
        startActivity(i);
        resetUI();
    }

    private void resetUI() {
        if (codeWS != null) { codeWS.cancel(); codeWS = null; }
        mainButtons.setVisibility(View.VISIBLE);
        hostCodeLayout.setVisibility(View.GONE);
        joinCodeLayout.setVisibility(View.GONE);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (codeWS != null) codeWS.cancel();
        if (codeClient != null) codeClient.dispatcher().executorService().shutdown();
    }
}
