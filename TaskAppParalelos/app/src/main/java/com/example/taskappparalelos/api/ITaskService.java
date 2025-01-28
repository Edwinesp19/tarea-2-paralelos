package com.example.taskappparalelos.api;

import com.example.taskappparalelos.model.LoginBody;
import com.example.taskappparalelos.model.LoginResponse;
import com.example.taskappparalelos.model.TaskAssignmentsResponse;
import com.example.taskappparalelos.model.TaskBody;
import com.example.taskappparalelos.model.TaskResponse;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ITaskService {
    @GET("/api/tasks")
    Call<TaskResponse> getTasks();

    @POST("/api/tasks")
    Call<Void> saveTask(@Body TaskBody taskBody);

    @PUT("/api/tasks/{id}")
    Call<Void> updateTask(@Path("id") int taskId, @Body TaskBody taskBody);

    @GET("/api/user/taskAssignments")
    Call<TaskAssignmentsResponse> getTaskAssignments();

}