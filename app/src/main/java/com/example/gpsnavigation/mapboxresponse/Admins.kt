package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Admins (

    @SerializedName("iso_3166_1_alpha3" ) var iso31661Alpha3 : String? = null,
    @SerializedName("iso_3166_1"        ) var iso31661       : String? = null

)
