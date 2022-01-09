package com.example.mymaps.models


import com.google.gson.annotations.SerializedName

data class Viewport(
    @SerializedName("northeast")
    var northeast: Northeast ,
    @SerializedName("southwest")
    var southwest: Southwest
)