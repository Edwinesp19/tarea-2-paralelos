package com.example.taskappparalelos.view;

import android.content.Intent;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ButtonBarLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.taskappparalelos.R;
import com.example.taskappparalelos.model.Task;
import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.viewmodel.TaskViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.List;

public class TaskActivity extends AppCompatActivity {
    private TaskViewModel mViewModel;
    private LinearLayout taskContainer;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;


    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        btnBack = findViewById(R.id.btnBack);

        taskContainer = findViewById(R.id.taskContainer);
        progressBar = findViewById(R.id.progressBar);

        mViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        fabAdd = findViewById(R.id.fab_add);

        // Manejar el clic del botón flotante
        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(TaskActivity.this, TaskFormActivity.class);
//            startActivity(intent);
            startActivityForResult(intent, 1);
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navBack();
            }
        });

        mViewModel.getProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer visibility) {
                progressBar.setVisibility(visibility);
            }
        });

        mViewModel.getTasks().observe(this, new Observer<List<TaskResponse.Task>>() {
            @Override
            public void onChanged(List<TaskResponse.Task> tasks) {
                displayTasks(tasks);
            }
        });



        // Fetch tasks on Activity start
        mViewModel.fetchTasks();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            mViewModel.fetchTasks();
        }
    }



    private void displayTasks(List<TaskResponse.Task> tasks) {
        taskContainer.removeAllViews(); // Limpia las vistas anteriores
        if (tasks != null && !tasks.isEmpty()) {
            // Mostrar tareas

        for (TaskResponse.Task task : tasks) {
            // Infla un layout personalizado para cada tarea
            View taskView = LayoutInflater.from(this).inflate(R.layout.item_task, taskContainer, false);

            // Asigna los datos de la tarea a los elementos de la vista
            TextView tvTitle = taskView.findViewById(R.id.tvTaskTitle);
            TextView tvDescription = taskView.findViewById(R.id.tvTaskDescription);
            TextView tvDueDate = taskView.findViewById(R.id.tvTaskDueDate);
            TextView tvStatus = taskView.findViewById(R.id.tvTaskStatus);
            LinearLayout tvStatusChip = taskView.findViewById(R.id.tvTaskStatusChip);
            LinearLayout tvTaskStatusChipIcon = taskView.findViewById(R.id.tvTaskStatusChipIcon);

            if (task.getStatusId() == 2) {
                // Estado completado
                tvStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFFDE")));
                tvTaskStatusChipIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F9524")));
                tvStatus.setTextColor(Color.parseColor("#1F9524"));
            } else {
                // Otro estado
                tvStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E7")));
                tvTaskStatusChipIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF8E00")));
                tvStatus.setTextColor(Color.parseColor("#FF8E00"));
            }

            tvTitle.setText(task.getTitle());
            tvDescription.setText(task.getDescription());
            tvDueDate.setText("Due: " + task.getDueDate());
            tvStatus.setText(task.getStatus());

            Task taskData = new Task(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getDateFrom(),
                    task.getDueDate(),
                    task.getStatus(),
                    task.getStatusId()
            );

            //navegar a la pantalla de fomulario
            taskView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TaskActivity.this, TaskFormActivity.class);
                    intent.putExtra("task", taskData);
                    startActivityForResult(intent, 1);
                }
            });


            // Añade la vista al contenedor
            taskContainer.addView(taskView);
        }} else {
            Toast.makeText(this, "No tasks available", Toast.LENGTH_SHORT).show();
        }
    }

    private void navBack() {
        setResult(RESULT_OK); // Indica que hubo un cambio en los datos
        finish(); // Cierra la actividad actual
    }
}
