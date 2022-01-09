package com.example.mymaps.remote

import com.example.mymaps.models.Places
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface IGoogleApiService {



    @GET
     fun getNearByPlaces(@Url str : String) : Call<Places>

}