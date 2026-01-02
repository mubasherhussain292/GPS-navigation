package com.example.myapplication.gpsappworktest.models.CurrentWeatherResponseModel


data class Weather(
    val description: String,
    val icon: String,
    val id: Int,
    val main: String
)