package com.example.gpsnavigation.utils;

import com.example.gpsnavigation.mapboxresponse.MapboxResponse;
import com.example.gpsnavigation.models.NearbyPlacesDetails;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;


public class MyConstants {

    public static LatLng destLatLng;
    public static String destName;
    public static MapboxResponse mapboxResponse;
    public static LatLng currentLatLng;
    public static String sourceName = "Your Location";
    public static ArrayList<NearbyPlacesDetails> nearbyPlaceDetails;
    public static NearbyPlacesDetails nearbyPlacesDetails = null;
    public static String SPEED_FORMAT_FOR_SPEAK = "#0";

    public static Double weatherLatitude;
    public static Double weatherLongitude;


}
