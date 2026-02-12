package com.example.project1_atripat7;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class TitleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_title);

        Button btnFullGame = findViewById(R.id.btnFullGame);
        Button btnMiniGame = findViewById(R.id.btnMiniGame);
        btnMiniGame.setOnClickListener(v -> {
            Intent intent = new Intent(TitleActivity.this, MiniGameActivity.class);
            startActivity(intent);
        });


        // Go to full Yahtzee game
        btnFullGame.setOnClickListener(v -> {
            Intent intent = new Intent(TitleActivity.this, MainActivity.class);
            startActivity(intent);
        });

        Intent intent = new Intent(TitleActivity.this, MiniGameActivity.class);
        startActivity(intent);

    }
}
