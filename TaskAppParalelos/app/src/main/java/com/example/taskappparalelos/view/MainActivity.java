package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;
import com.example.taskappparalelos.viewmodel.MainViewModel;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import com.example.taskappparalelos.R;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    TextView tvLoginResult;
    EditText etEmail, etPassword;
    Button bLogin;
    ProgressBar progressBar;

    MainViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvLoginResult = findViewById(R.id.tvLoginResult);
        bLogin = findViewById(R.id.bLogin);


        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);


        mViewModel.getProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer visibility) {
                progressBar.setVisibility(visibility);
            }
        });


        mViewModel.getLoginResult().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvLoginResult.setText(s);

                if(s == "Login Success"){
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);

                    // Agregar datos al intent (opcional)
                    intent.putExtra("username", "Edwin Espinal");

                    // Iniciar TaskActivity
                    startActivity(intent);
                }
            }
        });


        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewModel.login(etEmail.getText().toString(), etPassword.getText().toString());
            }
        });

    }

}