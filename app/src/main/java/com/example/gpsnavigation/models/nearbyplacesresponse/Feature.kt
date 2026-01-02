package com.example.myapplication.gpsappworktest.models.nearbyplacesresponse;

data class Feature(
    val geometry: Geometry,
    val properties: Properties,
    val type: String
)