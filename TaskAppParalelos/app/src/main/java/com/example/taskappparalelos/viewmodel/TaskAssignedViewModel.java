package com.example.taskappparalelos.viewmodel;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.taskappparalelos.model.TaskAssignmentsResponse;
import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.repository.TaskRepository;

import java.util.List;
public class TaskAssignedViewModel extends ViewModel {
    private final TaskRepository mTaskRepository;
    private final MutableLiveData<List<TaskAssignmentsResponse.Assignment>> mAssignmentsMutableData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mProgressMutableData = new MutableLiveData<>();

    public TaskAssignedViewModel() {
        mTaskRepository = new TaskRepository();
        mProgressMutableData.setValue(View.INVISIBLE);
    }

    public void fetchTaskAssignments() {
        mProgressMutableData.setValue(View.VISIBLE);

        mTaskRepository.getTaskAssignments(new TaskRepository.ITaskAssignmentsResponse() {
            @Override
            public void onResponse(List<TaskAssignmentsResponse.Assignment> assignments) {
                mProgressMutableData.setValue(View.INVISIBLE);
                mAssignmentsMutableData.setValue(assignments);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.setValue(View.INVISIBLE);
            }
        });
    }

    public LiveData<List<TaskAssignmentsResponse.Assignment>> getAssignments() {
        return mAssignmentsMutableData;
    }

    public LiveData<Integer> getProgress() {
        return mProgressMutableData;
    }
}
