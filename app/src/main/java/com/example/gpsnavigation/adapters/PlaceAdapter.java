package com.example.gpsnavigation.adapters;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpsnavigation.R;
import com.example.gpsnavigation.activities.RouteDrawActivity;
import com.example.gpsnavigation.db.Recent;
import com.example.gpsnavigation.models.NearbyPlacesDetails;
import com.example.gpsnavigation.utils.MyConstants;
import com.google.android.gms.maps.model.LatLng;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<NearbyPlacesDetails> nearbyPlacesDetails;

    public PlaceAdapter(Context context, ArrayList<NearbyPlacesDetails> nearbyPlacesDetails) {
        this.context = context;
        this.nearbyPlacesDetails = nearbyPlacesDetails;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        RelativeLayout homeMainLayout;
        TextView address;

        public MyViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.name);
            homeMainLayout = view.findViewById(R.id.relativeLayoutNearbyPlaceItem);
            address = view.findViewById(R.id.address);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.nearby_place_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        String name = "";
        if (nearbyPlacesDetails.get(position).getName() == null) {
            name = "";
        } else {
            name = nearbyPlacesDetails.get(position).getName();
        }

        String address = "";
        if (nearbyPlacesDetails.get(position).getPlaceAddress() == null) {
            address = "";
        } else {
            address = nearbyPlacesDetails.get(position).getPlaceAddress();

        }

        if (nearbyPlacesDetails.get(position).getCity() == null) {
            name = name + "";
        } else {
            name = name + ", " + nearbyPlacesDetails.get(position).getCity();
        }

        if (nearbyPlacesDetails.get(position).getCountry() == null) {
            name = name + "";
        } else {
            name = name + ", " + nearbyPlacesDetails.get(position).getCountry();
        }

        holder.name.setText(name);

        holder.address.setText(address);

        holder.homeMainLayout.setOnClickListener(new View.OnClickListener() {
            private boolean isClicked = false;

            @Override
            public void onClick(View v) {
                if (isClicked) return; // prevent multiple clicks
                isClicked = true;

                // Prevents repeated clicks within 1 second (you can adjust delay)
                v.postDelayed(() -> isClicked = false, 1000);

                MyConstants.nearbyPlacesDetails = nearbyPlacesDetails.get(position);

                if (MyConstants.currentLatLng != null) {
                    String name = MyConstants.nearbyPlacesDetails.getName();
                    MyConstants.destLatLng = new LatLng(MyConstants.nearbyPlacesDetails.getLat(), MyConstants.nearbyPlacesDetails.getLon());
                    MyConstants.destName = name;
                    String formattedDate = DateFormat.getDateTimeInstance().format(new Date());
                    Recent recent = new Recent(
                        MyConstants.destName,
                        MyConstants.destLatLng.latitude,
                        MyConstants.destLatLng.longitude,
                        formattedDate
                    );


                    Intent intent = new Intent(context, RouteDrawActivity.class);
                    intent.putExtra("recent_item", recent);
                    context.startActivity(intent);
                }
            }

        });
    }

    @Override
    public int getItemCount() {
        return nearbyPlacesDetails.size();
    }
}
