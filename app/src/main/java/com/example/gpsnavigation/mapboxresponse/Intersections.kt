package com.example.gpsnavigation.mapboxresponse


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Intersections (

    @SerializedName("bearings"          ) var bearings        : ArrayList<Int>     = arrayListOf(),
    @SerializedName("entry"             ) var entry           : ArrayList<Boolean> = arrayListOf(),
    @SerializedName("mapbox_streets_v8" ) var mapboxStreetsV8 : MapboxStreetsV8?   = MapboxStreetsV8(),
    @SerializedName("is_urban"          ) var isUrban         : Boolean?           = null,
    @SerializedName("admin_index"       ) var adminIndex      : Int?               = null,
    @SerializedName("out"               ) var out             : Int?               = null,
    @SerializedName("geometry_index"    ) var geometryIndex   : Int?               = null,
    @SerializedName("location"          ) var location        : ArrayList<Double>  = arrayListOf()

)