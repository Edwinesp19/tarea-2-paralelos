package com.example.taskappparalelos.viewmodel;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.taskappparalelos.model.UserBody;
import com.example.taskappparalelos.repository.UserRepository;

public class UserFormViewModel extends ViewModel {

    MutableLiveData<Integer> mProgressMutableData = new MutableLiveData<>();
    MutableLiveData<String> mUserResultMutableData = new MutableLiveData<>();

    UserRepository mUserRepository;

    public UserFormViewModel() {
        mProgressMutableData.postValue(View.INVISIBLE);
        mUserRepository = new UserRepository();
    }

    public void registerUser(UserBody userBody) {
        mProgressMutableData.postValue(View.VISIBLE);
        mUserRepository.registerUser(userBody, new UserRepository.IUserFormResponse() {
            @Override
            public void onSuccess(String message) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mUserResultMutableData.postValue(message);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mUserResultMutableData.postValue("Error al registrar usuario: " + t.getLocalizedMessage());
            }
        });
    }

    public void updateUser(int userId, UserBody userBody) {
        mProgressMutableData.postValue(View.VISIBLE);
        mUserRepository.updateUser(userId, userBody, new UserRepository.IUserFormResponse() {
            @Override
            public void onSuccess(String message) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mUserResultMutableData.postValue(message);
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mUserResultMutableData.postValue("Error al actualizar usuario: " + t.getLocalizedMessage());
            }
        });
    }

    public LiveData<Integer> getProgress() {
        return mProgressMutableData;
    }

    public LiveData<String> getUserResult() {
        return mUserResultMutableData;
    }
}
