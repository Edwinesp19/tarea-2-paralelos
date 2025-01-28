package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.ButtonBarLayout;


import com.example.taskappparalelos.R;
import com.example.taskappparalelos.model.User;
import com.example.taskappparalelos.model.UserResponse;
import com.example.taskappparalelos.viewmodel.UserViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.List;

public class UserActivity extends AppCompatActivity {
    ImageView btnBack;
    private UserViewModel mViewModel;
    private LinearLayout userContainer;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navBack();
            }
        });

        userContainer = findViewById(R.id.userContainer);
        progressBar = findViewById(R.id.progressBar);
        fabAdd = findViewById(R.id.fab_add);

        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(UserActivity.this, UserFormActivity.class);
            //            startActivity(intent);
            startActivityForResult(intent, 1);
        });


        mViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        mViewModel.getProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer visibility) {
                progressBar.setVisibility(visibility);
            }
        });

        mViewModel.getUsers().observe(this, new Observer<List<UserResponse.User>>() {
            @Override
            public void onChanged(List<UserResponse.User> users) {
                displayUsers(users);
            }
        });

        mViewModel.fetchUsers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            mViewModel.fetchUsers();
        }
    }

    private void displayUsers(List<UserResponse.User> users) {
        userContainer.removeAllViews();

        if (users != null && !users.isEmpty()) {
            for (UserResponse.User user : users) {
                View userView = LayoutInflater.from(this).inflate(R.layout.item_user, userContainer, false);

                TextView tvNameInitials = userView.findViewById(R.id.tvUserNameInitials);
                TextView tvName = userView.findViewById(R.id.tvUserName);
                TextView tvEmail = userView.findViewById(R.id.tvUserEmail);

                String[] strArray = user.getName().split(" ");
                StringBuilder builder = new StringBuilder();

//First name
                if (strArray.length > 0){
                    builder.append(strArray[0], 0, 1);
                }
//Middle name
                if (strArray.length > 1){
                    builder.append(strArray[1], 0, 1);
                }
//Surname
                if (strArray.length > 2){
                    builder.append(strArray[2], 0, 1);
                }

                tvNameInitials.setText(builder.toString());

                tvName.setText(user.getName());
                tvEmail.setText(user.getEmail());

                 User userData = new User(
                      user.getId(),
                         user.getName(),
                        user.getEmail());

                userView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(UserActivity.this, UserFormActivity.class);
                        intent.putExtra("user", userData);
                        startActivityForResult(intent, 1); // Código de solicitud 1
                    }
                });

                userContainer.addView(userView);
            }
        } else {
            Toast.makeText(this, "No users available", Toast.LENGTH_SHORT).show();
        }
    }

    private void navBack() {
        setResult(RESULT_OK);
        finish();
    }
}