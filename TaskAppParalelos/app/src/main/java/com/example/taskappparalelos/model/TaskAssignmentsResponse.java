package com.example.taskappparalelos.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TaskAssignmentsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("assignments")
    private List<Assignment> assignments;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public static class Assignment {
        @SerializedName("email")
        private String email;

        @SerializedName("name")
        private String name;

        @SerializedName("tasks")
        private List<TaskResponse.Task> tasks;

        @SerializedName("user_id")
        private int userId;

        // Getters y setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<TaskResponse.Task> getTasks() {
            return tasks;
        }

        public void setTasks(List<TaskResponse.Task> tasks) {
            this.tasks = tasks;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }
    }
}
