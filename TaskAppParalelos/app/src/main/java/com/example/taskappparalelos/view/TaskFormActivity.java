package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.DatePickerDialog;
import android.widget.Toast;
import java.util.Calendar;
import java.util.List;

import android.widget.Spinner;
import android.widget.ProgressBar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;




import com.example.taskappparalelos.R;
import com.example.taskappparalelos.model.Task;
import com.example.taskappparalelos.model.TaskBody;
import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.model.TaskStatusResponse;
import com.example.taskappparalelos.viewmodel.TaskFormViewModel;

public class TaskFormActivity extends AppCompatActivity {

    EditText etTaskTitle;
    EditText etTaskDescription;
    EditText etTaskDateFrom;
    EditText etTaskDueDate;
    ProgressBar progressBar;

    TextView tvTaskFormTitle;
    Spinner spStatusId;

    TaskFormViewModel mViewModel;

    ImageView btnBack;

    int taskIdToUpdate = 0;
    int statusSelected = 0;


    Button btnSave;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_form);

        Task task = getIntent().getParcelableExtra("task");

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        etTaskDateFrom = findViewById(R.id.etTaskDateFrom);
        etTaskDueDate = findViewById(R.id.etTaskDueDate);
        btnSave = findViewById(R.id.btnSave);
        spStatusId = findViewById(R.id.spStatusId);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);


        tvTaskFormTitle= findViewById(R.id.tvTaskFormTitle);

        mViewModel = new ViewModelProvider(this).get(TaskFormViewModel.class);

        mViewModel.getTaskStatuses().observe(this, statuses -> {
            if (statuses != null) {
                ArrayAdapter<TaskStatusResponse.TaskStatus> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        statuses
                );

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spStatusId.setAdapter(adapter);

                if (task != null) {
                    for (int i = 0; i < statuses.size(); i++) {
                        if (statuses.get(i).getId() == task.getStatusId()) {
                            spStatusId.setSelection(i);
                            break;
                        }
                    }
                }
            }
        });

        if (task != null) {
            taskIdToUpdate = task.getId();

            etTaskTitle.setText(task.getTitle());
            etTaskDescription.setText(task.getDescription());
            etTaskDateFrom.setText(task.getDateFrom());
            etTaskDueDate.setText(task.getDueDate());
            tvTaskFormTitle.setText("Tarea #"+task.getId());
            statusSelected = task.getStatusId();
        }else{
            tvTaskFormTitle.setText("Nueva Tarea");
        }



        spStatusId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TaskStatusResponse.TaskStatus selectedStatus = (TaskStatusResponse.TaskStatus) parent.getItemAtPosition(position);
                int statusId = selectedStatus.getId();
                statusSelected = statusId;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Acción si no se selecciona nada
            }
        });

        mViewModel.fetchStatuses();



        mViewModel.getProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer visibility) {
                progressBar.setVisibility(visibility);
            }
        });
        mViewModel.getTaskResult().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s == "Tarea creada correctamente" || s == "Tarea actualizada correctamente"){
                    navBack();
                }
                showMessage(s);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navBack();
            }
        });

        btnSave.setOnClickListener(v -> saveTask());
        etTaskDateFrom.setOnClickListener(v -> showDatePickerDialog(etTaskDateFrom));
        etTaskDueDate.setOnClickListener(v -> showDatePickerDialog(etTaskDueDate));
    }

    private void showDatePickerDialog(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            String date = year1+"-"+(month1 + 1)+"-"+dayOfMonth ;
            editText.setText(date);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void saveTask() {

        String title = etTaskTitle.getText().toString();
        String description = etTaskDescription.getText().toString();
        String dateFrom = etTaskDateFrom.getText().toString();
        String dueDate = etTaskDueDate.getText().toString();

        if (title.isEmpty() || description.isEmpty() || dateFrom.isEmpty() || dueDate.isEmpty()) {
            showMessage("Llena los campos");
            return;
        }

        TaskBody taskBody = new TaskBody(title, description, dateFrom, dueDate, statusSelected);

            if(taskIdToUpdate == 0){
                mViewModel.saveTask(taskBody);
            }else{
                mViewModel.updateTask(taskIdToUpdate, taskBody);
            }
    }

    private void showMessage(String message) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navBack() {
        setResult(RESULT_OK); // Indica que hubo un cambio en los datos
        finish(); // Cierra la actividad actual
    }

}