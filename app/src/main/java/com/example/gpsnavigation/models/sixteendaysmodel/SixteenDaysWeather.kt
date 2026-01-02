package com.example.myapplication.gpsappworktest.models.sixteendaysmodel

import com.google.gson.annotations.SerializedName



data class SixteenDaysWeather (

  @SerializedName("city"    ) var city    : City?           = City(),
  @SerializedName("cod"     ) var cod     : String?         = null,
  @SerializedName("message" ) var message : Double?         = null,
  @SerializedName("cnt"     ) var cnt     : Int?            = null,
  @SerializedName("list"    ) var list    : ArrayList<List> = arrayListOf()

)