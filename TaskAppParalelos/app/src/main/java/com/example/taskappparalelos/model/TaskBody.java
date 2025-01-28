package com.example.taskappparalelos.model;

import com.google.gson.annotations.SerializedName;

public class TaskBody {
    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("date_from")
    private String dateFrom;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("status_id")
    private int statusId;

    public TaskBody(String title, String description, String dateFrom, String dueDate, int statusId) {
        this.title = title;
        this.description = description;
        this.dateFrom = dateFrom;
        this.dueDate = dueDate;
        this.statusId = statusId;
    }

    // Getters y setters
}
