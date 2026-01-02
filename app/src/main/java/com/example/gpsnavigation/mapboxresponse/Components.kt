package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Components (

    @SerializedName("type" ) var type : String? = null,
    @SerializedName("text" ) var text : String? = null

)