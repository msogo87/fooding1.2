package com.robotemplates.cityguide;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.robotemplates.cityguide.utility.ImageLoaderUtility;


public class CityGuideApplication extends Application
{
	private static CityGuideApplication sInstance;

	private Tracker mTracker;


	public static Context getContext()
	{
		return sInstance;
	}


	public CityGuideApplication()
	{
		sInstance = this;
	}


	@Override
	public void onCreate()
	{
		super.onCreate();
		
		// force AsyncTask to be initialized in the main thread due to the bug:
		// http://stackoverflow.com/questions/4280330/onpostexecute-not-being-called-in-asynctask-handler-runtime-exception
		try
		{
			Class.forName("android.os.AsyncTask");
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		// init image caching
		ImageLoaderUtility.init(getApplicationContext());
	}


	public synchronized Tracker getTracker()
	{
		if(mTracker==null)
		{
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			analytics.setDryRun(CityGuideConfig.ANALYTICS_TRACKING_ID == null || CityGuideConfig.ANALYTICS_TRACKING_ID.equals(""));
			mTracker = analytics.newTracker(R.xml.analytics_app_tracker);
			mTracker.set("&tid", CityGuideConfig.ANALYTICS_TRACKING_ID);
		}
		return mTracker;
	}
}
