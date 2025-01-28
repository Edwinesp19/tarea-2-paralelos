package com.example.taskappparalelos.viewmodel;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.taskappparalelos.model.TaskBody;
import com.example.taskappparalelos.model.TaskStatusResponse;
import com.example.taskappparalelos.repository.TaskRepository;

import java.util.List;

public class TaskFormViewModel extends ViewModel {

    MutableLiveData<List<TaskStatusResponse.TaskStatus>> mStatusesLiveData = new MutableLiveData<>();

    MutableLiveData<Integer> mProgressMutableData = new MutableLiveData<>();
    MutableLiveData<String> mTaskResultMutableData = new MutableLiveData<>();
     MutableLiveData<String> mErrorLiveData = new MutableLiveData<>();

    TaskRepository mTaskRepository;

    public TaskFormViewModel() {
        mProgressMutableData.postValue(View.INVISIBLE);
        mTaskRepository = new TaskRepository();
    }

    public void saveTask(TaskBody taskBody) {
        mProgressMutableData.postValue(View.VISIBLE);
        mTaskRepository.saveTask(taskBody, new TaskRepository.ITaskFormResponse() {
            @Override
            public void onSuccess(String message) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mTaskResultMutableData.postValue(message);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mTaskResultMutableData.postValue("Task saving failed: " + t.getLocalizedMessage());
            }
        });
    }

    public void updateTask(int taskId, TaskBody taskBody) {
        mProgressMutableData.postValue(View.VISIBLE);
        mTaskRepository.updateTask(taskId, taskBody, new TaskRepository.ITaskFormResponse() {
            @Override
            public void onSuccess(String message) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mTaskResultMutableData.postValue(message);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mTaskResultMutableData.postValue("Task update failed: " + t.getLocalizedMessage());
            }
        });
    }

    public void fetchStatuses() {
        mProgressMutableData.postValue(View.VISIBLE);
        mTaskRepository.getTaskStatuses(new TaskRepository.ITaskStatusesResponse() {
            @Override
            public void onResponse(List<TaskStatusResponse.TaskStatus> statuses) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mStatusesLiveData.postValue(statuses);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.postValue(View.INVISIBLE);
            }
        });
    }


    public LiveData<List<TaskStatusResponse.TaskStatus>> getTaskStatuses() {
        return mStatusesLiveData;
    }
    public LiveData<Integer> getProgress() {
        return mProgressMutableData;
    }

    public LiveData<String> getTaskResult() {
        return mTaskResultMutableData;
    }

    public LiveData<String> getError() {
        return mErrorLiveData;
    }
}
