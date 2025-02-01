package com.example.taskappparalelos.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.view.View;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.example.taskappparalelos.repository.MainRepository;
import com.example.taskappparalelos.model.LoginBody;
import com.example.taskappparalelos.model.LoginResponse;


public class MainViewModel extends AndroidViewModel {
    private static final String PREFS_NAME = "MyPrefs";
    private static final String PLAYER_ID_KEY = "player_id";
    MutableLiveData<Integer> mProgressMutableData = new MutableLiveData<>();
    MutableLiveData<String> mLoginResultMutableData = new MutableLiveData<>();

    MainRepository mMainRepository;
    private Application application;
    private SharedPreferences sharedPreferences;


    public MainViewModel(Application application) {
        super(application);
        //inicializar el mProgressMutableDatalogin data
        mProgressMutableData.postValue(View.INVISIBLE);
        mLoginResultMutableData.postValue("Not logged in");
        mMainRepository = new MainRepository();

        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
    }

    public void login(String email, String password){
        mProgressMutableData.postValue(View.VISIBLE);
        mLoginResultMutableData.postValue("Checking");

        // Recuperar el playerId desde SharedPreferences
        String playerId = sharedPreferences.getString(PLAYER_ID_KEY, "");
        mMainRepository.loginRemote(new LoginBody(email, password,playerId), new MainRepository.ILoginResponse() {
            @Override
            public void onResponse(LoginResponse loginResponse) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mLoginResultMutableData.postValue("Login Success");
            }

            @Override
            public void onFailure(Throwable t) {
                mProgressMutableData.postValue(View.INVISIBLE);
                mLoginResultMutableData.postValue("Login failure: " + t.getLocalizedMessage());
            }
        });
    }

    public LiveData<String> getLoginResult(){
        return mLoginResultMutableData;
    }

    public LiveData<Integer> getProgress(){
        return mProgressMutableData;
    }


}
