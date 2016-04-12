package com.robotemplates.cityguide.communication;

import java.util.Date;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by msogolovsky on 07/02/2016.
 * MainDbObjectData is a class that holds all the data of an object (DB row)
 * from the main database - list of POIs
 */
public class MainDbObjectData implements ClusterItem {

    public String id;                        // ID - database uniqe number

    @SerializedName("name")
    public String restName;                  // Restaurant Name

    @SerializedName("slogan")
    public String restType;                  // Restaurant Type

    @SerializedName("discount_type")
    public int    discountType;              // Discount Type

    @SerializedName("discount_text")
    public String discountDetails;           // Discount Details

    public double location_lat;              // Location Latitude
    public double location_lng;              // Location Longtitude
    // TODO [MSOGO]: add Experation Time     // Time of discount experation

    @SerializedName("logo_url")
    public String picUrl;                    // Link to picture

    private double distance;                    // distance from user's location

    // Empty Constructor
    public MainDbObjectData()
    {

    }



    //  SETTERS
    //~~~~~~~~~~~
    public void setDistance( int distance)
    {
        this.distance = distance;
    }

    //  GETTERS
    //~~~~~~~~~~~
    public LatLng getPosition()
    {
        return ( new LatLng(this.location_lat, this.location_lng) );
    }

    public String getName()
    {
        return restName;
    }

    public double getDistance()
    {
        return distance;
    }

    public String getImage()
    {
        return picUrl;
    }

    public long getId() { return Long.parseLong(id); }

    public int getDiscountType() { return discountType; }

    public boolean equals(Object o)
    {
        return (restName.equals( ((MainDbObjectData)o).restName ));
    }

}
