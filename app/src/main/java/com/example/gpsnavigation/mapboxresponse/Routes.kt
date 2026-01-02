package com.example.gpsnavigation.mapboxresponse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Routes (

    @SerializedName("weight_name" ) var weightName  : String?         = null,
    @SerializedName("weight"      ) var weight      : Double?         = null,
    @SerializedName("duration"    ) var duration    : Double?         = null,
    @SerializedName("distance"    ) var distance    : Double?         = null,
    @SerializedName("legs"        ) var legs        : ArrayList<Legs> = arrayListOf(),
    @SerializedName("geometry"    ) var geometry    : Geometry?       = Geometry(),
    @SerializedName("voiceLocale" ) var voiceLocale : String?         = null

)