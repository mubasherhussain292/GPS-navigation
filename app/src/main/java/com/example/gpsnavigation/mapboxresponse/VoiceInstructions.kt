package com.example.gpsnavigation.mapboxresponse



import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class VoiceInstructions (

    @SerializedName("ssmlAnnouncement"      ) var ssmlAnnouncement      : String? = null,
    @SerializedName("announcement"          ) var announcement          : String? = null,
    @SerializedName("distanceAlongGeometry" ) var distanceAlongGeometry : Double? = null

)