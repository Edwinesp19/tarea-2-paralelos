package com.example.taskappparalelos.repository;

import com.example.taskappparalelos.api.ITaskService;
import com.example.taskappparalelos.api.ITaskStatusService;
import com.example.taskappparalelos.model.TaskAssignmentsResponse;
import com.example.taskappparalelos.model.TaskBody;
import com.example.taskappparalelos.model.TaskResponse;
import com.example.taskappparalelos.model.TaskStatusResponse;
import com.example.taskappparalelos.network.RetrofitClientInstance;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskRepository {



    public void getTasks(ITaskResponse taskResponse) {
        ITaskService taskService = RetrofitClientInstance.getInstance().create(ITaskService.class);
        Call<TaskResponse> call = taskService.getTasks();

        call.enqueue(new Callback<TaskResponse>() {
            @Override
            public void onResponse(Call<TaskResponse> call, Response<TaskResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getTasks() != null) {
                    taskResponse.onResponse(response.body().getTasks());
                } else {
                    taskResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<TaskResponse> call, Throwable t) {
                taskResponse.onFailure(t);
            }
        });
    }

    public void getTaskAssignments(ITaskAssignmentsResponse taskAssignmentsResponse) {
        ITaskService taskService = RetrofitClientInstance.getInstance().create(ITaskService.class);
        Call<TaskAssignmentsResponse> call = taskService.getTaskAssignments();

        call.enqueue(new Callback<TaskAssignmentsResponse>() {
            @Override
            public void onResponse(Call<TaskAssignmentsResponse> call, Response<TaskAssignmentsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getAssignments() != null) {
                    taskAssignmentsResponse.onResponse(response.body().getAssignments());
                } else {
                    taskAssignmentsResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<TaskAssignmentsResponse> call, Throwable t) {
                taskAssignmentsResponse.onFailure(t);
            }
        });
    }




    public void saveTask(TaskBody taskBody, ITaskFormResponse taskResponse) {
        ITaskService taskService = RetrofitClientInstance.getInstance().create(ITaskService.class);
        Call<Void> saveTaskCall = taskService.saveTask(taskBody);

        saveTaskCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    taskResponse.onSuccess("Tarea creada correctamente");
                } else {
                    taskResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                taskResponse.onFailure(t);
            }
        });
    }

    public void updateTask(int taskId, TaskBody taskBody, ITaskFormResponse taskResponse) {
        ITaskService taskService = RetrofitClientInstance.getInstance().create(ITaskService.class);
        Call<Void> updateTaskCall = taskService.updateTask(taskId, taskBody);

        updateTaskCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    taskResponse.onSuccess("Tarea actualizada correctamente");
                } else {
                    taskResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                taskResponse.onFailure(t);
            }
        });
    }

    public void getTaskStatuses(ITaskStatusesResponse statusesResponse) {
        ITaskStatusService taskService = RetrofitClientInstance.getInstance().create(ITaskStatusService.class);
        Call<TaskStatusResponse> call = taskService.getTaskStatus();

        call.enqueue(new Callback<TaskStatusResponse>() {
            @Override
            public void onResponse(Call<TaskStatusResponse> call, Response<TaskStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatuses() != null) {
                    statusesResponse.onResponse(response.body().getStatuses());
                } else {
                    statusesResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<TaskStatusResponse> call, Throwable t) {
                statusesResponse.onFailure(t);
            }
        });
    }

    public interface ITaskStatusesResponse {
        void onResponse(List<TaskStatusResponse.TaskStatus> statuses);
        void onFailure(Throwable t);
    }

    public interface ITaskResponse {
        void onResponse(List<TaskResponse.Task> tasks);
        void onFailure(Throwable t);
    }

    public interface ITaskAssignmentsResponse {
        void onResponse(List<TaskAssignmentsResponse.Assignment> assignments);
        void onFailure(Throwable t);
    }


    public interface ITaskFormResponse {
        void onSuccess(String message);
        void onFailure(Throwable t);
    }
}
