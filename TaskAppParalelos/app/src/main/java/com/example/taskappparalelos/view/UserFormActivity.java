package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taskappparalelos.R;
import com.example.taskappparalelos.model.Task;
import com.example.taskappparalelos.model.User;
import com.example.taskappparalelos.model.UserBody;
import com.example.taskappparalelos.model.UserResponse;
import com.example.taskappparalelos.viewmodel.UserFormViewModel;

public class UserFormActivity extends AppCompatActivity {

    EditText etUserName, etUserEmail, etUserPassword;
    ProgressBar progressBar;
    Button btnSave;
    TextView tvUserFormTitle;
    ImageView btnBack;

    UserFormViewModel mViewModel;

    int userIdToUpdate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_form);

        User user = getIntent().getParcelableExtra("user");

        tvUserFormTitle = findViewById(R.id.tvUserFormTitle);
        etUserName = findViewById(R.id.etUserName);
        etUserEmail = findViewById(R.id.etUserEmail);
        etUserPassword = findViewById(R.id.etUserPassword);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);


        mViewModel = new ViewModelProvider(this).get(UserFormViewModel.class);

        mViewModel.getProgress().observe(this, visibility -> progressBar.setVisibility(visibility));
        mViewModel.getUserResult().observe(this, message -> {
            showMessage(message);
            if (message.contains("correctamente")) {
                navBack();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navBack();
            }
        });

        btnSave.setOnClickListener(v -> saveUser());

        if (user != null) {
            userIdToUpdate = user.getId();
            etUserName.setText(user.getName());
            etUserEmail.setText(user.getEmail());
            tvUserFormTitle.setText("Usuario #"+user.getId());
        }else{
            tvUserFormTitle.setText("Nuevo usuario");
        }
    }

    private void saveUser() {
        String name = etUserName.getText().toString();
        String email = etUserEmail.getText().toString();
        String password = etUserPassword.getText().toString();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        UserBody userBody = new UserBody(name, email, password);

        if (userIdToUpdate == 0) {
            mViewModel.registerUser(userBody);
        } else {
            mViewModel.updateUser(userIdToUpdate, userBody);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navBack() {
        setResult(RESULT_OK);
        finish();
    }
}
