package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.widget.LinearLayout;

import com.example.taskappparalelos.R;

public class HomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String USERNAME_KEY = "username";

    LinearLayout cardTasks,cardUsers,cardAssign,cardImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        String username = getIntent().getStringExtra("username");



        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Intent puede contener el username, si no, lo tomamos de SharedPreferences
        if (username == null || username.isEmpty()) {
            username = sharedPreferences.getString(USERNAME_KEY, "Usuario");
        }

        TextView textView = findViewById(R.id.textViewWelcome);
        textView.setText( username);

        cardTasks = findViewById(R.id.linearLayoutTask);
        cardUsers = findViewById(R.id.linearLayoutUser);
        cardAssign = findViewById(R.id.linearLayoutAssign);
        cardImage = findViewById(R.id.linearLayoutLoadPhotos);

        cardTasks.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, TaskActivity.class);
            startActivity(intent);
        });

        cardUsers.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, UserActivity.class);
            startActivity(intent);
        });

        cardAssign.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, TaskAssignedActivity.class);
            startActivity(intent);
        });
        cardImage.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, ImageActivity.class);
            startActivity(intent);
        });
    }
}