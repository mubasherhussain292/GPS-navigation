package com.example.gpsnavigation.models;


public class NearByModel {
    String name;
    int icon;
    String catId;

    int selectedIcon;

    public NearByModel(String name, int icon, String catId, int selectedIcon) {
        this.name = name;
        this.icon = icon;
        this.catId = catId;
        this.selectedIcon = selectedIcon;
    }


    public String getCatId() {
        return catId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getselectedIcon() {
        return selectedIcon;
    }

    public void setselectedIcon(int selectedIcon) {
        this.selectedIcon = selectedIcon;
    }
}

