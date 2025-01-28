package com.example.taskappparalelos.api;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IImageService {
    @Multipart
    @POST("/api/images")
    Call<ResponseBody> uploadImages(@Part MultipartBody.Part[] images);

    @GET("/search/photos")
    Call<JsonObject> searchPhotos(
            @Query("query") String query,
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("client_id") String clientId
    );
}
