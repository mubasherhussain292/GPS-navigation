package com.example.gpsnavigation.models;


public class SixteenDaysRecyclerViewModel
{
    Double minTemp;

    Double maxTemp;

    String icon;

    String dtTxt;


    String weatherType;

    public SixteenDaysRecyclerViewModel(Double minTemp,Double maxTemp, String icon, String dtTxt,String weatherType) {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.icon = icon;
        this.dtTxt = dtTxt;
        this.weatherType=weatherType;
    }

    public void setWeatherType(String weather) {
        this.weatherType = weather;
    }


    public String getWeatherType() {
        return weatherType;
    }


    public String getTime() {
        return dtTxt;
    }

    public Double getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(Double temp) {
        this.minTemp = temp;
    }

    public Double getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(Double temp) {
        this.maxTemp = temp;
    }



    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

