package com.example.taskappparalelos.viewmodel;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.taskappparalelos.model.UserResponse;
import com.example.taskappparalelos.repository.UserRepository;

import java.util.List;

public class UserViewModel extends ViewModel {
    private final UserRepository mUserRepository;
    private final MutableLiveData<List<UserResponse.User>> mUsersMutableData = new MutableLiveData<>();
    private final MutableLiveData<Integer> mProgressMutableData = new MutableLiveData<>();

    public UserViewModel() {
        mUserRepository = new UserRepository();
        mProgressMutableData.setValue(View.INVISIBLE);
    }

    public void fetchUsers() {
        mProgressMutableData.setValue(View.VISIBLE);

        mUserRepository.getUsers(new UserRepository.IUserResponse() {
            @Override
            public void onResponse(List<UserResponse.User> users) {
                mProgressMutableData.setValue(View.INVISIBLE);
                mUsersMutableData.setValue(users);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.setValue(View.INVISIBLE);
            }
        });
    }

    public LiveData<List<UserResponse.User>> getUsers() {
        return mUsersMutableData;
    }

    public LiveData<Integer> getProgress() {
        return mProgressMutableData;
    }
}
