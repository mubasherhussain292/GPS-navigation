package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class MapboxResponse (

    @SerializedName("routes"    ) var routes    : ArrayList<Routes>    = arrayListOf(),
    @SerializedName("waypoints" ) var waypoints : ArrayList<Waypoints> = arrayListOf(),
    @SerializedName("code"      ) var code      : String?              = null,
    @SerializedName("uuid"      ) var uuid      : String?              = null

)
