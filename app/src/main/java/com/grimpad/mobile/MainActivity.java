package com.grimpad.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends Activity {
    private EditText serverInput;
    private RadioGroup ctrlTypeGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);
        serverInput   = findViewById(R.id.serverInput);
        ctrlTypeGroup = findViewById(R.id.ctrlTypeGroup);
        ((Button)findViewById(R.id.btnHost)).setOnClickListener(v -> launch("host"));
        ((Button)findViewById(R.id.btnJoin)).setOnClickListener(v -> launch("join"));
    }

    private void launch(String mode) {
        String addr = serverInput.getText().toString().trim();
        if (addr.isEmpty()) addr = "localhost:8765";
        RadioButton sel = findViewById(ctrlTypeGroup.getCheckedRadioButtonId());
        String type = (sel != null && sel.getId() == R.id.radio360) ? "360" : "series";
        Intent i = new Intent(this, ControllerActivity.class);
        i.putExtra("server", addr);
        i.putExtra("mode", mode);
        i.putExtra("ctrlType", type);
        startActivity(i);
    }
}
