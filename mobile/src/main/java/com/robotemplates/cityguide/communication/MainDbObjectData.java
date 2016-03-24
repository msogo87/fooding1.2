package com.robotemplates.cityguide.communication;

import java.util.Date;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Created by msogolovsky on 07/02/2016.
 * MainDbObjectData is a class that holds all the data of an object (DB row)
 * from the main database - list of POIs
 */
public class MainDbObjectData {

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

    // Empty Constructor
    public MainDbObjectData()
    {

    }
}
