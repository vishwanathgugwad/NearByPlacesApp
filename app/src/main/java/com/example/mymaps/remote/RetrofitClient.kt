package com.example.mymaps.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



private const val BASE_URL = "https://maps.googleapis.com/"

object RetrofitClient {

    private var retrofit : Retrofit? = null

    fun getClient() : IGoogleApiService? {

        if (retrofit == null){
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit?.create(IGoogleApiService::class.java)


    }
}