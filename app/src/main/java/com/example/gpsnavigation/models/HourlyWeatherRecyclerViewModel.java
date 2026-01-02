package com.example.gpsnavigation.models;


public class HourlyWeatherRecyclerViewModel
{
//    String name;

    Double temp;

    String icon;

    String dtTxt;


    public HourlyWeatherRecyclerViewModel(Double temp, String icon, String dtTxt) {
        this.temp = temp;
        this.icon = icon;
        this.dtTxt = dtTxt;
    }

    public String getTime() {
        return dtTxt;
    }

    public Double getTemp() {
        return temp;
    }

    public void setTemp(Double temp) {
        this.temp = temp;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

