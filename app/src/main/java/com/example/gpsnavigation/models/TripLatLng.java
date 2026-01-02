package com.example.gpsnavigation.models;


import com.google.android.gms.maps.model.LatLng;

public class TripLatLng
{
    private LatLng latLng;
    private SpeedState speedState;

    public TripLatLng(LatLng latLng, SpeedState speedState )
    {
        this.latLng = latLng;
        this.speedState = speedState;
    }

    public LatLng getLatLng()
    {
        return latLng;
    }

    public void setLatLng(LatLng latLng)
    {
        this.latLng = latLng;
    }

    public SpeedState getSpeedState()
    {
        return speedState;
    }

    public void setSpeedState(SpeedState speedState)
    {
        this.speedState = speedState;
    }
}

