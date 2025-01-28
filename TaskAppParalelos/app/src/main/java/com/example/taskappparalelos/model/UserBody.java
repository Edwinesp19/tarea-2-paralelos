package com.example.taskappparalelos.model;

import com.google.gson.annotations.SerializedName;

public class UserBody {
    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public UserBody(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters y setters
}
