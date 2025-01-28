package com.example.taskappparalelos.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Task implements Parcelable {
    private int id;
    private String title;
    private String description;
    private String dateFrom;
    private String dueDate;
    private String status;
    private int statusId;

    // Constructor
    public Task(int id, String title, String description, String dateFrom, String dueDate, String status, int statusId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateFrom = dateFrom;
        this.dueDate = dueDate;
        this.status = status;
        this.statusId = statusId;
    }

    protected Task(Parcel in) {
        id = in.readInt();
        title = in.readString();
        description = in.readString();
        dateFrom = in.readString();
        dueDate = in.readString();
        status = in.readString();
        statusId = in.readInt();
    }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) {
            return new Task(in);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(dateFrom);
        dest.writeString(dueDate);
        dest.writeString(status);
        dest.writeInt(statusId);
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public int getStatusId() {
        return statusId;
    }
}
