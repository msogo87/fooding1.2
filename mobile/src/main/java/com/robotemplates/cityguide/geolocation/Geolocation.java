package com.robotemplates.cityguide.geolocation;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.robotemplates.cityguide.utility.LocationUtility;
import com.robotemplates.cityguide.utility.Logcat;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class Geolocation implements LocationListener
{
	private static final int                     LOCATION_AGE = 60000 * 5; // = 5min   // [milliseconds]
	private static final int                     LOCATION_TIMEOUT = 30000; // = 0.5min // [milliseconds]
    private static final int                     CB_CNT_THRESHOLD = 4; // after CB_CNT_THRESHOLD location callbacks, mMinTime value will be changed.
    private static final String                  TAG = "Geolocation";

	private WeakReference<GeolocationListener>   mListener;
	private LocationManager                      mLocationManager;
	private static Location                      mCurrentLocation = null;
	private static Location                      mPreviuosLocation = null;
	private Timer                                mTimer;
    private float                                mMinDistance = 100;   // [meters]
    private long                                 mMinTime = 10 * 1000; // = 10sec // [milliseconds]
    private static int                           mNumOfLocationCbs = 0;  // counts how many Callbacks had accured, after CB_CNT_THRESHOLD times, mMinTime changes.



    public Geolocation(LocationManager locationManager, GeolocationListener listener)
	{
		mLocationManager = locationManager; // (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE); 
		mListener = new WeakReference<>(listener);
		mTimer = new Timer();
		init();
	}
	
	
	@Override
	public void onLocationChanged(Location location)
	{
        int distanceDelta = 0;
		Logcat.d(location.getProvider() + " / " + location.getLatitude() + " / " + location.getLongitude() + " / " + new Date(location.getTime()).toString());
		
		// check location age
		long timeDelta = System.currentTimeMillis() - location.getTime();
		if(timeDelta > LOCATION_AGE)
		{
			Log.d(TAG,"old location, do nothing ");
			return;
		}
		
		// return location
        if ( mCurrentLocation != null )
        {
            mPreviuosLocation = mCurrentLocation;
        }
		mCurrentLocation = new Location(location);
        // FOR DEBUG
        ///////////////////
        mCurrentLocation.setLatitude(32.14);
        mCurrentLocation.setLongitude(34.8);
        ///////////////////

		stop();
		GeolocationListener listener = mListener.get();

        if ( mPreviuosLocation != null )
        {
            // calc distance delta
            LatLng currLocation = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            LatLng prevLocation = new LatLng(mPreviuosLocation.getLatitude(), mPreviuosLocation.getLongitude());
            distanceDelta = LocationUtility.getDistance(currLocation, prevLocation);
        }
        else
        {
            distanceDelta = ((int)mMinDistance) + 1; // enable callback
        }

		if(listener!=null && location!=null && distanceDelta > mMinDistance)
        {
            mNumOfLocationCbs ++;
            if ( mNumOfLocationCbs > CB_CNT_THRESHOLD )
            {
                mMinTime = 1000 * 60;
            }
            Log.e(TAG, " Latitude: mCurrentLocation.getLatitude() , Longitude: mCurrentLocation.getLongitude() ");
            listener.onGeolocationRespond(Geolocation.this, mCurrentLocation);
        }
	}


	@Override
	public void onProviderDisabled(String provider)
	{
		Logcat.d(provider);
	}


	@Override
	public void onProviderEnabled(String provider) {
        Logcat.d(provider);
	}


	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		Logcat.d(provider);
		switch(status) 
		{
			case LocationProvider.OUT_OF_SERVICE:
				Logcat.d("status OUT_OF_SERVICE");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Logcat.d("status TEMPORARILY_UNAVAILABLE");
				break;
			case LocationProvider.AVAILABLE:
				Logcat.d("status AVAILABLE");
				break;
		}
	}


	public void stop()
	{
		Logcat.d("");
		if(mTimer!=null) mTimer.cancel();
		if(mLocationManager!=null) 
		{
			mLocationManager.removeUpdates(this);
			mLocationManager = null;
		}
	}


	private void init()
	{
		// get last known location
		Location lastKnownLocation = getLastKnownLocation(mLocationManager);
		
		// try to listen last known location
		if(lastKnownLocation != null)
		{
			onLocationChanged(lastKnownLocation);
		}
		
		if(mCurrentLocation == null)
		{
			// start timer to check timeout
			TimerTask task = new TimerTask()
			{
				public void run()
				{
					if(mCurrentLocation == null)
					{
						Logcat.d("timeout");
						stop();
						GeolocationListener listener = mListener.get();
						if(listener != null) listener.onGeolocationFail(Geolocation.this);
					}
				}
			};
			mTimer.schedule(task, LOCATION_TIMEOUT);

			// register location updates
			try
			{
				//TODO: consider using fused location provider instead
                         //                                   provider,     minTime , minDistance , listener
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mMinTime, mMinDistance, this);
			}
			catch(IllegalArgumentException e)
			{
                e.printStackTrace();
			}
			try
			{
				//TODO: consider using fused location provider instead
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	// returns last known freshest location from network or GPS
	private Location getLastKnownLocation(LocationManager locationManager)
	{
		Logcat.d("");

		Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		long timeNet = 0L;
		long timeGps = 0L;
		
		if(locationNet!=null)
		{
			timeNet = locationNet.getTime();
		}
		
		if(locationGps!=null)
		{
			timeGps = locationGps.getTime();
		}
		
		if(timeNet>timeGps) return locationNet;
		else return locationGps;
	}
}
