package com.example.mymaps.models


import com.google.gson.annotations.SerializedName

data class Photo(
    @SerializedName("height")
    var height: Int,
    @SerializedName("html_attributions")
    var htmlAttributions: List<String>,
    @SerializedName("photo_reference")
    var photoReference: String,
    @SerializedName("width")
    var width: Int
)