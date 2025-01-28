package com.example.taskappparalelos.api;

import com.example.taskappparalelos.model.UserBody;
import com.example.taskappparalelos.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface IUserService {
    @GET("/api/users")
    Call<UserResponse> getUsers();

    @POST("/api/register")
    Call<Void> registerUser(@Body UserBody userBody);

    @PUT("/api/user/{id}")
    Call<Void> updateUser(@Path("id") int userId, @Body UserBody userBody);
}
