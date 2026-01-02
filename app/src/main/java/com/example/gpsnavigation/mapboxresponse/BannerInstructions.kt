package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class BannerInstructions (

    @SerializedName("sub") var sub                   : Sub?     = Sub(),
    @SerializedName("primary"               ) var primary               : Primary? = Primary(),
    @SerializedName("distanceAlongGeometry" ) var distanceAlongGeometry : Double?  = null

)
