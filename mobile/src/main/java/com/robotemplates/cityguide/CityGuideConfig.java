package com.robotemplates.cityguide;


public class CityGuideConfig
{
	// tracking id for Google Analytics,
	// leave this constant empty if you do not want to use Google Analytics
	public static final String ANALYTICS_TRACKING_ID = "UA-XXXXXXXX-X";

	// unit ids for AdMob,
	// leave these constants empty if you do not want to use AdMob
	public static final String ADMOB_UNIT_ID_POI_LIST = "ca-app-pub-XXXXXXXXXXXXXXXXXXXXXXXXXXX";
	public static final String ADMOB_UNIT_ID_POI_DETAIL = "ca-app-pub-XXXXXXXXXXXXXXXXXXXXXXXXXXX";
	public static final String ADMOB_UNIT_ID_MAP = "ca-app-pub-XXXXXXXXXXXXXXXXXXXXXXXXXXX";

	// test device id for AdMob,
	// setup this constant if you want to avoid invalid impressions,
	// you can find your hashed device id in the logcat output by requesting an ad when debugging on your device
	public static final String ADMOB_TEST_DEVICE_ID = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

	// file name of the SQLite database, this file should be placed in assets folder
	public static final String DATABASE_NAME = "cityguide.db";

	// database version, should be incremented if database has been changed
	public static final int DATABASE_VERSION = 2;

	// debug logs, value is set via build config in build.gradle
	public static final boolean LOGS = BuildConfig.LOGS;
}
