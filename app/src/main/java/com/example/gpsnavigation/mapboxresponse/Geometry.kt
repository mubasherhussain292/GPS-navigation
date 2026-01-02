package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Geometry (

    @SerializedName("coordinates" ) var coordinates : ArrayList<ArrayList<Double>> = arrayListOf(),
    @SerializedName("type"        ) var type        : String?                      = null

)