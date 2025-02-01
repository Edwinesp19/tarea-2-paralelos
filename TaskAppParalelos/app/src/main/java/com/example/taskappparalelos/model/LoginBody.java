package com.example.taskappparalelos.model;

import com.google.gson.annotations.SerializedName;

public class LoginBody {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("player_id")
    private String playerId;

    public LoginBody(String email, String password, String playerId) {
        this.email = email;
        this.password = password;
        this.playerId = playerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
