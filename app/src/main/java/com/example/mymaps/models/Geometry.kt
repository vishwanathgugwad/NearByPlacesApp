package com.example.mymaps.models


import com.google.gson.annotations.SerializedName

data class Geometry(
    @SerializedName("location")
    var location: Location ,
    @SerializedName("viewport")
    var viewport: Viewport
)