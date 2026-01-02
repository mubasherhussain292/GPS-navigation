package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Maneuver (

    @SerializedName("type"           ) var type          : String?           = null,
    @SerializedName("instruction"    ) var instruction   : String?           = null,
    @SerializedName("bearing_after"  ) var bearingAfter  : Int?              = null,
    @SerializedName("bearing_before" ) var bearingBefore : Int?              = null,
    @SerializedName("location"       ) var location      : ArrayList<Double> = arrayListOf()

)