package com.example.src.controller;

import com.example.src.model.ObstaclePojo;
import com.example.src.model.SpeedPojo;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EvilCarAPI {
    @GET("/speed")
    Call<SpeedPojo> getSpeed(@Query("long")double longitude,@Query("lat")double latitude);

    @POST("/report")
    Call<ResponseBody> sendViolation(@Body RequestBody params);

    @GET("/obstacles")
    Call<ObstaclePojo> sendObstacle(@Query("long") double longitude, @Query("lat")double latitude);
}
