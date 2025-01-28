package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.widget.LinearLayout;

import com.example.taskappparalelos.R;

public class HomeActivity extends AppCompatActivity {

    LinearLayout cardTasks,cardUsers,cardAssign,cardImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        String username = getIntent().getStringExtra("username");

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