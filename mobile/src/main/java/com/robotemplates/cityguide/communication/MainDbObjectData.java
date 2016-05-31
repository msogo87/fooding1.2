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

    @SerializedName("phone_number")
    public String phoneNum;

    @SerializedName("menu_url")
    public String menuUrl;

    @SerializedName("logo_url")
    public String picUrl;                    // Link to picture

    @SerializedName("discount_type")
    public int    discountType;              // Discount Type

    @SerializedName("discount_url")
    public String discountUrl;            // Discount Type

    @SerializedName("discount_text")
    public String discountDetails;           // Discount Details

    public double location_lat;              // Location Latitude
    public double location_lng;              // Location Longtitude
    // TODO [MSOGO]: add Experation Time     // Time of discount experation


    private double distance;                 // distance from user's location

    private boolean isFavorite = false;

    // Empty Constructor
    public MainDbObjectData()
    {

    }


    //  SETTERS
    //~~~~~~~~~~~
    public void setDistance         ( int distance)
    {
        this.distance = distance;
    }

    public void setFavorite         ( boolean isFavorite )
    {
        this.isFavorite = isFavorite;
    }

    //  GETTERS
    //~~~~~~~~~~~
    public LatLng getPosition()     { return ( new LatLng(this.location_lat, this.location_lng) ); }

    public double getLatitude()     { return (this.location_lat); }

    public double getLongitude()    { return (this.location_lng); }

    public String getName()         { return restName; }

    public double getDistance()     { return distance; }

    public String getImage()        { return picUrl; }

    public long getId()             { return Long.parseLong(id); }

    public int getDiscountType()    { return discountType; }

    public boolean equals(Object o)
    {
        return (restName.equals( ((MainDbObjectData)o).restName ));
    }

    public boolean isFavorite()     { return isFavorite; }

    public String getIntro()        { return restType; }

    public String getAddress()      { return "This is an Address"; }

    public String getLink()         { return menuUrl; }

    public String getPhone()        { return phoneNum;}

    public String getEmail()        { return "This is E-mail";}

    public String getDescription() { return discountDetails;}
}
