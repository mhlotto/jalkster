package com.example.jalkster;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class XCSkiSessionConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xc_ski_session_config);

        MaterialButton startButton = findViewById(R.id.button_start_xc_ski);
        MaterialButton backButton = findViewById(R.id.button_back);

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(XCSkiSessionConfigActivity.this,
                    XCSkiLiveSessionActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }
}
