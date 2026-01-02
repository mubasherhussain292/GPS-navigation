package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Legs (

    @SerializedName("via_waypoints" ) var viaWaypoints : ArrayList<String> = arrayListOf(),
    @SerializedName("admins"        ) var admins       : ArrayList<Admins> = arrayListOf(),
    @SerializedName("weight"        ) var weight       : Double?           = null,
    @SerializedName("duration"      ) var duration     : Double?           = null,
    @SerializedName("steps"         ) var steps        : ArrayList<Steps>  = arrayListOf(),
    @SerializedName("distance"      ) var distance     : Double?           = null,
    @SerializedName("summary"       ) var summary      : String?           = null

)