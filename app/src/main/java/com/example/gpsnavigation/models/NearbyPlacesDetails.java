package com.example.gpsnavigation.models;



public class NearbyPlacesDetails {
    String name, city, country;
    double lat, lon;
    double distance;
//    Icon icon;
    String placeID;
    String placeType;
    String placeAddress;

    public NearbyPlacesDetails(String name, String city, String country, double lat, double lon, double distance, String placeID, String placeType, String placeAddress) {
        this.name = name;
        this.city = city;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
        this.distance = distance;
//        this.icon = icon;
        this.placeID = placeID;
        this.placeType = placeType;
        this.placeAddress = placeAddress;
    }

    public String getPlaceType() {
        return placeType;
    }

    public String getPlaceAddress() {
        return placeAddress;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getPlaceID() {
        return placeID;
    }

//    public Icon getIcon() {
//        return icon;
//    }
//
//    public void setIcon(Icon icon) {
//        this.icon = icon;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public double getDistance() {
        return distance;
    }
}

