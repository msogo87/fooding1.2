package com.robotemplates.cityguide.database.data;

/**
 * Created by msogolovsky on 22/05/2016.
 */
public class FavoritesDbRow {
    private long    id;                        // The ID number of the POI in server's database
    private boolean isShowNotification = true; // true - show notifications / false - don't show notifications
    private String  poiName;                   // The name of the POI
    private String  picUrl;                    // holds the URL of the image to be displayed


    // Constructor
    public FavoritesDbRow (long newId, boolean NewIsShowNotification, String newPoiName, String newPicUrl)
    {
        id                 = newId;
        isShowNotification = NewIsShowNotification;
        poiName            = newPoiName;
        picUrl             = newPicUrl;
    }

    public String  getName()        { return poiName; }
    public String  getImage()       { return picUrl; }
    public boolean getToggle()      { return isShowNotification; }
    public long    getId()          { return id; }
}