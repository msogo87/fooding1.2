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

    @SerializedName("restName")
    public String restName;                  // Restaurant Name

    @SerializedName("restSlogan")
    public String restSlogan;                // Restaurant Type

    @SerializedName("restType")
    public String restType;                  // Restaurant Type

    @SerializedName("discount_type")
    public int    discountType;              // Discount Type

    @SerializedName("discount_details")
    public String discountDetails;           // Discount Details

    @SerializedName("location_lat")
    public double location_lat;              // Location Latitude

    @SerializedName("location_long")
    public double location_lng;              // Location Longtitude

    @SerializedName("start_time")
    public String startTime;                 // deal's start time

    @SerializedName("duration")
    public String duration;                  // deal's duration

    @SerializedName("phone_number")
    public String phoneNum;

    @SerializedName("main_image_url")
    public String picUrl;                    // Link to picture

    @SerializedName("menu_url")
    public String menuUrl;

    @SerializedName("restaurant_description")
    public String poiDescription;


//    @SerializedName("discount_url")
//    public String discountUrl;               // Discount Type



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
