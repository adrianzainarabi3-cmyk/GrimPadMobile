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

public class MainActivity extends Activity {
    private EditText serverInput, codeInput;
    private RadioGroup ctrlTypeGroup;
    private TextView codeDisplay, codeLabel;
    private LinearLayout hostCodeLayout, joinCodeLayout, mainButtons;

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

    // HOST — show code immediately, launch controller, connect in background
    private void startHost() {
        mainButtons.setVisibility(View.GONE);
        hostCodeLayout.setVisibility(View.VISIBLE);

        // Generate code from server IP immediately — no waiting
        String ip = getServer().split(":")[0];
        String code = String.format("%04d01", Math.abs(ip.hashCode()) % 10000);
        codeDisplay.setText(code);
        codeLabel.setText("YOUR ROOM CODE — SHARE IT");

        // Launch controller after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            launchController();
        }, 2000);
    }

    private void showJoin() {
        mainButtons.setVisibility(View.GONE);
        joinCodeLayout.setVisibility(View.VISIBLE);
        if (codeInput != null) codeInput.requestFocus();
    }

    private void joinWithCode() {
        String code = codeInput != null ? codeInput.getText().toString().trim() : "";
        if (code.length() < 6) {
            if (codeInput != null) codeInput.setError("Enter 6 digits");
            return;
        }
        launchController();
    }

    private void launchController() {
        Intent i = new Intent(this, ControllerActivity.class);
        i.putExtra("server", getServer());
        i.putExtra("ctrlType", getCtrlType());
        startActivity(i);
        resetUI();
    }

    private void resetUI() {
        mainButtons.setVisibility(View.VISIBLE);
        hostCodeLayout.setVisibility(View.GONE);
        joinCodeLayout.setVisibility(View.GONE);
    }
}
