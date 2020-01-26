package com.example.src.controller;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {
    private static Retrofit retrofit;
    public static String baseURL;

    public static Retrofit getRetrofitClient(String b_url) {
        baseURL = b_url;
        //If condition to ensure we don't create multiple retrofit instances in a single application
        if (retrofit == null) {
            //Defining the Retrofit using Builder
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseURL) //This is the only mandatory call on Builder object.
                    .addConverterFactory(GsonConverterFactory.create()) // Convector library used to convert response into POJO
                    .build();

        }
        return retrofit;
    }
}