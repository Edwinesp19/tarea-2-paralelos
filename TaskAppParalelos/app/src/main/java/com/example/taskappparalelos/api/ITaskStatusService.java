package com.example.taskappparalelos.api;

import com.example.taskappparalelos.model.TaskBody;
import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.model.TaskStatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ITaskStatusService {
    @GET("/api/taskStatuses")
    Call<TaskStatusResponse> getTaskStatus();

}