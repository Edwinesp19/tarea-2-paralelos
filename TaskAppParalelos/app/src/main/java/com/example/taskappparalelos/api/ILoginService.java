package com.example.taskappparalelos.api;

import com.example.taskappparalelos.model.LoginBody;
import com.example.taskappparalelos.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ILoginService {

    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginBody loginBody);
}
