package com.example.gpsnavigation.models;


import android.graphics.Color;

public enum SpeedState
{
    OVER_SPEED
            {
                public int getColor()
                {
                    return Color.rgb(218, 35, 25 );//  Color.RED;
                }

            },
    UNDER_SPEED
            {
                public int getColor()
                {
                    //return Color.rgb( 255, 245, 64 );// Color.YELLOW;
                    return Color.rgb( 255, 136, 11 );
                }

            },
    NORMAL
            {
                public int getColor()
                {
                    return Color.rgb( 93, 218, 93 );// Color.Green;
                }
            };

    public abstract int getColor();
}

