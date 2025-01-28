package com.example.taskappparalelos.viewmodel;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.repository.TaskRepository;

import java.util.List;

public class TaskViewModel extends ViewModel {
    private final TaskRepository mTaskRepository;
    private final MutableLiveData<List<TaskResponse.Task>> mTasksMutableData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mProgressMutableData = new MutableLiveData<>();

    public TaskViewModel() {
        mTaskRepository = new TaskRepository();
        mProgressMutableData.setValue(View.INVISIBLE);
    }

    public void fetchTasks() {
        mProgressMutableData.setValue(View.VISIBLE);

        mTaskRepository.getTasks(new TaskRepository.ITaskResponse() {
            @Override
            public void onResponse(List<TaskResponse.Task> tasks) {
                mProgressMutableData.setValue(View.INVISIBLE);
                mTasksMutableData.setValue(tasks);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.setValue(View.INVISIBLE);
            }
        });
    }

    public LiveData<List<TaskResponse.Task>> getTasks() {
        return mTasksMutableData;
    }

    public LiveData<Integer> getProgress() {
        return mProgressMutableData;
    }
}
