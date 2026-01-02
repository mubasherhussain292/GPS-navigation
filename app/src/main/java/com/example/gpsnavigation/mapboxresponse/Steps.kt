package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Steps (

    @SerializedName("bannerInstructions" ) var bannerInstructions : ArrayList<BannerInstructions> = arrayListOf(),
    @SerializedName("voiceInstructions"  ) var voiceInstructions  : ArrayList<VoiceInstructions>  = arrayListOf(),
    @SerializedName("intersections"      ) var intersections      : ArrayList<Intersections>      = arrayListOf(),
    @SerializedName("maneuver"           ) var maneuver           : Maneuver?                     = Maneuver(),
    @SerializedName("name"               ) var name               : String?                       = null,
    @SerializedName("duration"           ) var duration           : Double?                       = null,
    @SerializedName("distance"           ) var distance           : Double?                       = null,
    @SerializedName("driving_side"       ) var drivingSide        : String?                       = null,
    @SerializedName("weight"             ) var weight             : Double?                       = null,
    @SerializedName("mode"               ) var mode               : String?                       = null,
    @SerializedName("geometry"           ) var geometry           : Geometry?                     = Geometry()

)