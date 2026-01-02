package com.example.gpsnavigation.mapboxresponse



import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Primary (

    @SerializedName("components" ) var components : ArrayList<Components> = arrayListOf(),
    @SerializedName("type"       ) var type       : String?               = null,
    @SerializedName("modifier"   ) var modifier   : String?               = null,
    @SerializedName("text"       ) var text       : String?               = null

)
