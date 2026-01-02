package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Waypoints (

    @SerializedName("distance" ) var distance : Double?           = null,
    @SerializedName("name"     ) var name     : String?           = null,
    @SerializedName("location" ) var location : ArrayList<Double> = arrayListOf()

)