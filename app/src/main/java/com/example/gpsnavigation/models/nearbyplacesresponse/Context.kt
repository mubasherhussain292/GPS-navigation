package com.example.myapplication.gpsappworktest.models.nearbyplacesresponse;

data class Context(
    val address: Address,
    val country: Country,
    val locality: Locality,
    val place: Place,
    val postcode: Postcode,
    val street: Street
)