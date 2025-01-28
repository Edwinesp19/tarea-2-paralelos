package com.example.taskappparalelos.repository;

import com.example.taskappparalelos.api.IImageService;
import com.example.taskappparalelos.network.RetrofitClientInstance;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;

public class ImageRepository {

    private static final String UNSPLASH_ACCESS_KEY = "pKAOSFoV47l0pWRbsIml_0MOcCiejBGgoHBipfU2pk4";
    private static final String UNSPLASH_API_URL = "https://api.unsplash.com/search/photos/";

    public interface ImageUploadCallback {
        void onUploadStart(String imagePath);
        void onSuccess(String imagePath);
        void onFailure(String imagePath, Throwable t);
    }

    public interface UnsplashSearchCallback {
        void onSuccess(JsonArray images);
        void onFailure(Throwable t);
    }

    public void searchUnsplashImagesInThread(String query, UnsplashSearchCallback callback) {
        new Thread(() -> {
            try {
                // Instancia de Retrofit para Unsplash
                IImageService service = RetrofitClientInstance
                        .getInstanceWithUrl(UNSPLASH_API_URL)
                        .create(IImageService.class);

                // Llamada sincrónica
                retrofit2.Response<JsonObject> response = service.searchPhotos(query, 1, 50, UNSPLASH_ACCESS_KEY).execute();

                if (response.isSuccessful() && response.body() != null) {
                    JsonArray results = response.body().getAsJsonArray("results");
                    callback.onSuccess(results);
                } else {
                    callback.onFailure(new Throwable("Error: " + response.message()));
                }
            } catch (Exception e) {
                callback.onFailure(e);
            }
        }).start();
    }

    public void uploadImageInThread(String imagePath, ImageUploadCallback callback) {
        new Thread(() -> {
            try {
                // Notifica que la subida ha comenzado
                callback.onUploadStart(imagePath);

                IImageService imageService = RetrofitClientInstance.getInstance().create(IImageService.class);

                // Crea el cuerpo de la imagen
                File imageFile = new File(imagePath);
                RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestBody);

                // Llama al servicio de Retrofit
                Call<ResponseBody> call = imageService.uploadImages(new MultipartBody.Part[]{imagePart});
                Response<ResponseBody> response = call.execute();

                if (response.isSuccessful()) {
                    // Éxito
                    callback.onSuccess(imagePath);
                } else {
                    // Error en el servidor
                    callback.onFailure(imagePath, new Throwable("Error: " + response.message()));
                }
            } catch (Exception e) {
                // Error en la subida
                callback.onFailure(imagePath, e);
            }
        }).start();
    }
}
