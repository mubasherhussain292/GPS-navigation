package com.example.myapplication.gpsappworktest.models.nearbyplacesresponse;

data class DiscoverResponse(
    val attribution: String,
    val features: List<Feature>,
    val type: String
)