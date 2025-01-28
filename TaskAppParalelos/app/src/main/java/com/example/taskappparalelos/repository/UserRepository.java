package com.example.taskappparalelos.repository;

import com.example.taskappparalelos.api.IUserService;
import com.example.taskappparalelos.model.UserBody;
import com.example.taskappparalelos.model.UserResponse;
import com.example.taskappparalelos.network.RetrofitClientInstance;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    public void getUsers(IUserResponse userResponse) {
        IUserService userService = RetrofitClientInstance.getInstance().create(IUserService.class);
        Call<UserResponse> call = userService.getUsers();

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUsers() != null) {
                    userResponse.onResponse(response.body().getUsers());
                } else {
                    userResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                userResponse.onFailure(t);
            }
        });
    }

    public void registerUser(UserBody userBody, IUserFormResponse userResponse) {
        IUserService userService = RetrofitClientInstance.getInstance().create(IUserService.class);
        Call<Void> call = userService.registerUser(userBody);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    userResponse.onSuccess("Usuario registrado correctamente");
                } else {
                    userResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                userResponse.onFailure(t);
            }
        });
    }

    public void updateUser(int userId, UserBody userBody, IUserFormResponse userResponse) {
        IUserService userService = RetrofitClientInstance.getInstance().create(IUserService.class);
        Call<Void> call = userService.updateUser(userId, userBody);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    userResponse.onSuccess("Usuario actualizado correctamente");
                } else {
                    userResponse.onFailure(new Throwable(response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                userResponse.onFailure(t);
            }
        });
    }

    public interface IUserResponse {
        void onResponse(List<UserResponse.User> users);
        void onFailure(Throwable t);
    }
    public interface IUserFormResponse {
        void onSuccess(String message);

        void onFailure(Throwable t);
    }
}
