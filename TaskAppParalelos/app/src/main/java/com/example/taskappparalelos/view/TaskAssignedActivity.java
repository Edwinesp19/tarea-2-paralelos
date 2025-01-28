package com.example.taskappparalelos.view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.taskappparalelos.R;
import com.example.taskappparalelos.model.TaskAssignmentsResponse;
import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.viewmodel.TaskAssignedViewModel;
import java.util.List;


public class TaskAssignedActivity extends AppCompatActivity {
    ImageView btnBack;
    private TaskAssignedViewModel mViewModel;
    private LinearLayout assignmentContainer;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_assigned);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navBack();
            }
        });

        assignmentContainer = findViewById(R.id.assignmentContainer);
        progressBar = findViewById(R.id.progressBar);

        mViewModel = new ViewModelProvider(this).get(TaskAssignedViewModel.class);

        mViewModel.getProgress().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer visibility) {
                progressBar.setVisibility(visibility);
            }
        });

        mViewModel.getAssignments().observe(this, new Observer<List<TaskAssignmentsResponse.Assignment>>() {
            @Override
            public void onChanged(List<TaskAssignmentsResponse.Assignment> assignments) {
                displayAssignments(assignments);
            }
        });

        mViewModel.fetchTaskAssignments();
    }

    private void displayAssignments(List<TaskAssignmentsResponse.Assignment> assignments) {
        assignmentContainer.removeAllViews();

        if (assignments != null && !assignments.isEmpty()) {
            for (TaskAssignmentsResponse.Assignment assignment : assignments) {
                View assignmentView = LayoutInflater.from(this).inflate(R.layout.item_task_assigned, assignmentContainer, false);
                TextView tvNameInitials = assignmentView.findViewById(R.id.tvUserNameInitials);
                TextView tvName = assignmentView.findViewById(R.id.tvUserName);
                TextView tvEmail = assignmentView.findViewById(R.id.tvUserEmail);
                TextView tvTaskCount = assignmentView.findViewById(R.id.tvTaskCount);

                String[] strArray = assignment.getName().split(" ");
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
                tvName.setText(assignment.getName());
                tvEmail.setText(assignment.getEmail());
                tvTaskCount.setText(assignment.getTasks().size() + " Tareas");



                assignmentContainer.addView(assignmentView);
            }
        }
    }

    private void navBack() {
        setResult(RESULT_OK); // Indica que hubo un cambio en los datos
        finish(); // Cierra la actividad actual
    }
}