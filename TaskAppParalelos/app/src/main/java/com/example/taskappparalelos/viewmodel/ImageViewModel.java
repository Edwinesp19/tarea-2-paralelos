package com.example.taskappparalelos.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.taskappparalelos.repository.ImageRepository;
import com.google.gson.JsonArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageViewModel extends ViewModel {
    private final ImageRepository imageRepository;
    private final MutableLiveData<Map<String, String>> uploadStatus = new MutableLiveData<>();
    private final MutableLiveData<JsonArray> unsplashImages = new MutableLiveData<>();


    public ImageViewModel() {
        imageRepository = new ImageRepository();
    }

    public LiveData<Map<String, String>> getUploadStatus() {
        return uploadStatus;
    }

    public LiveData<JsonArray> getUnsplashImages() {
        return unsplashImages;
    }

    public void searchUnsplashImages(String query) {
        imageRepository.searchUnsplashImagesInThread(query, new ImageRepository.UnsplashSearchCallback() {
            @Override
            public void onSuccess(JsonArray images) {
                unsplashImages.postValue(images);
            }

            @Override
            public void onFailure(Throwable t) {
                unsplashImages.postValue(null); // Manejo de errores
            }
        });
    }

    public void uploadImages(List<String> imagePaths) {
        Map<String, String> statusMap = new HashMap<>();

        for (String imagePath : imagePaths) {
            statusMap.put(imagePath, "Uploading...");
            uploadStatus.setValue(statusMap);

            imageRepository.uploadImageInThread(imagePath, new ImageRepository.ImageUploadCallback() {
                @Override
                public void onUploadStart(String imagePath) {
                    statusMap.put(imagePath, "Uploading...");
                    uploadStatus.postValue(statusMap);
                }

                @Override
                public void onSuccess(String imagePath) {
                    statusMap.put(imagePath, "Success");
                    uploadStatus.postValue(statusMap);
                }

                @Override
                public void onFailure(String imagePath, Throwable t) {
                    statusMap.put(imagePath, "Failed: " + t.getMessage());
                    uploadStatus.postValue(statusMap);
                }
            });
        }
    }
}
