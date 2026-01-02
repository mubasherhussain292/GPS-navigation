package com.example.myapplication.gpsappworktest.models.nearbyplacesresponse;

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val routable_points: List<RoutablePoint>
)