package com.robotemplates.cityguide.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.robotemplates.cityguide.CityGuideConfig;
import com.robotemplates.cityguide.R;
import com.robotemplates.cityguide.activity.MapActivity;
import com.robotemplates.cityguide.activity.PoiDetailActivity;
import com.robotemplates.cityguide.common.QueryTypeEnum;
import com.robotemplates.cityguide.communication.DataImporter;
import com.robotemplates.cityguide.communication.DataImporterListener;
import com.robotemplates.cityguide.communication.MainDbObjectData;
import com.robotemplates.cityguide.database.DatabaseCallListener;
import com.robotemplates.cityguide.database.DatabaseCallManager;
import com.robotemplates.cityguide.database.DatabaseCallTask;
import com.robotemplates.cityguide.database.dao.CategoryDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.model.CategoryModel;
import com.robotemplates.cityguide.database.model.PoiModel;
import com.robotemplates.cityguide.database.query.PoiReadAllQuery;
import com.robotemplates.cityguide.database.query.Query;
import com.robotemplates.cityguide.graphics.BitmapScaler;
import com.robotemplates.cityguide.utility.Logcat;
import com.robotemplates.cityguide.utility.NetworkUtility;
import com.robotemplates.cityguide.utility.PermissionUtility;
import com.robotemplates.cityguide.utility.Preferences;
import com.robotemplates.cityguide.utility.Version;
import com.robotemplates.cityguide.view.StatefulLayout;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MapFragment extends TaskFragment implements DatabaseCallListener, DataImporterListener, GoogleMap.OnCameraChangeListener
{
	private static final int MAP_ZOOM = 14;
	private static final String TAG = "MapFragment";
	private static final boolean mDebugMode = true;

	private View mRootView;
	private StatefulLayout mStatefulLayout;
	private MapView mMapView;
	private DatabaseCallManager mDatabaseCallManager = new DatabaseCallManager();
	private List<PoiModel> mPoiList = new ArrayList<>();
	private List<MainDbObjectData> mDIPoiList = new ArrayList<>();
	private List<MainDbObjectData> mPois2ClustserList = new ArrayList<>();
	private ClusterManager<MainDbObjectData> mClusterManager = null;
	private Map<Long, BitmapDescriptor> mBitmapDescriptorMap = new HashMap<>();
	private Map<Integer, BitmapDescriptor> mId2BitmapDescriptorMap = new HashMap<>();
	private long mPoiId = -1L;
	private double mPoiLatitude = 0.0;
	private double mPoiLongitude = 0.0;
	private DataImporterListener   mDataImporterListener = this;;
	private LatLng mPrevCenter = new LatLng (0,0);
	private float mPrevRadius = 0;
	private boolean mIsNewCluster = false;
	private CameraPosition mLastCameraPosition = null;
	private boolean mIsFirstTimeViwed = true;

	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);

		// handle intent extras
		Bundle extras = getActivity().getIntent().getExtras();
		if(extras != null)
		{
			handleExtras(extras);
		}
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.e(TAG, "onCreateView");
		mRootView = inflater.inflate(R.layout.fragment_map, container, false);
		initMap();
		mMapView = (MapView) mRootView.findViewById(R.id.fragment_map_mapview);
		mMapView.onCreate(savedInstanceState);
		return mRootView;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		Log.e(TAG, "Map onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		initId2BitmapDescriptorMap();

//		// setup map
//		setupMap();
////		setupClusterManager();
//
		// setup stateful layout
		setupStatefulLayout(savedInstanceState);
//
//		if (mPoiList == null || mPoiList.isEmpty()) loadData();
//
//		// check permissions
//		PermissionUtility.checkPermissionAccessLocation(this);

	}

	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);

		if (isVisibleToUser)
		{
			if (mIsFirstTimeViwed) {
				mIsFirstTimeViwed = false;
				// setup map
				setupMap();

				if (mPoiList == null || mPoiList.isEmpty()) loadData();

				// check permissions
				PermissionUtility.checkPermissionAccessLocation(this);
			}
		}
		else
		{

		}
	}

	public void initId2BitmapDescriptorMap ()
	{
		Bitmap marker;

		// discount type 1 - map_marker_buy_get
		marker = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_buy_get);
		mId2BitmapDescriptorMap.put(1, BitmapDescriptorFactory.fromBitmap( marker ));

		// discount type 2 - map_marker_free_desert
		marker = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_free_desert);
		mId2BitmapDescriptorMap.put(2, BitmapDescriptorFactory.fromBitmap(marker));

		// discount type 3 - map_marker_free_drinks
		marker = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_free_drinks);
		mId2BitmapDescriptorMap.put(3, BitmapDescriptorFactory.fromBitmap(marker));

		// discount type 4 - map_marker_one_plus_one
		marker = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_one_plus_one);
		mId2BitmapDescriptorMap.put(4, BitmapDescriptorFactory.fromBitmap(marker));

		// discount type 5 - map_marker_percentage
		marker = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_percentage);
		mId2BitmapDescriptorMap.put(5, BitmapDescriptorFactory.fromBitmap( marker ));
	}

	@Override
	public void onDataImporterTaskCompleted(final List<MainDbObjectData> dIpoiList)
	{
		runTaskCallback(new Runnable() {
			public void run() {
				Log.e(TAG, "DataImporeter callback start...");
				Log.d(TAG, "onDataImporterTaskCompleted: poiList length is: " + dIpoiList.size());

				mPois2ClustserList.clear();
				for (MainDbObjectData poi : dIpoiList) {
					if (!mDIPoiList.contains(poi))
//					if ( mDIPoiList.Fin )
					{
						mPois2ClustserList.add(poi);
						mDIPoiList.add(poi);
					}
				}

//				mDIPoiList  = dIpoiList;

				// load data
				if (mPoiList == null || mPoiList.isEmpty()) loadData();

				//initId2BitmapDescriptorMap();

				bindData();
//				// lazy loading progress
//				if(mLazyLoading) showLazyLoadingProgress(true);
//
//				// show toolbar if hidden
//				showToolbar(true);
//
//				// calculate distances and sort
//				calculatePoiDistances();
//				sortPoiByDistance();
//				if(mAdapter!=null && mLocation!=null && mPoiList!=null && !mPoiList.isEmpty()) mAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onDataImporterTaskFailed(int retVal)
	{

	}

	@Override
	public void onStart()
	{
		super.onStart();
	}
	
	
	@Override
	public void onResume()
	{
		super.onResume();

		// map
		if(mMapView!=null) mMapView.onResume();
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();

		// map
		if(mMapView!=null) mMapView.onPause();
	}
	
	
	@Override
	public void onStop()
	{
		super.onStop();
	}
	
	
	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		mRootView = null;
	}
	
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// map
		if(mMapView!=null) mMapView.onDestroy();

		// cancel async tasks
		mDatabaseCallManager.cancelAllTasks();
	}
	
	
	@Override
	public void onDetach()
	{
		super.onDetach();
	}


	@Override
	public void onLowMemory()
	{
		super.onLowMemory();

		// map
		if(mMapView!=null) mMapView.onLowMemory();
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// save current instance state
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);

		// stateful layout state
		if(mStatefulLayout!=null) mStatefulLayout.saveInstanceState(outState);

		// map
		if(mMapView!=null) mMapView.onSaveInstanceState(outState);
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		// action bar menu
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_map, menu);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// action bar menu behavior
		switch(item.getItemId())
		{
			case R.id.menu_fragment_map_layers_normal:
				setMapType(GoogleMap.MAP_TYPE_NORMAL);
				return true;

			case R.id.menu_fragment_map_layers_satellite:
				setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				return true;

			case R.id.menu_fragment_map_layers_hybrid:
				setMapType(GoogleMap.MAP_TYPE_HYBRID);
				return true;

			case R.id.menu_fragment_map_layers_terrain:
				setMapType(GoogleMap.MAP_TYPE_TERRAIN);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		switch(requestCode)
		{
			case PermissionUtility.REQUEST_PERMISSION_ACCESS_LOCATION:
			{
				// if request is cancelled, the result arrays are empty
				if(grantResults.length > 0)
				{
					for(int i=0; i<grantResults.length; i++)
					{
						if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
						{
							// permission granted
							String permission = permissions[i];
							if(permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
									permission.equals(Manifest.permission.ACCESS_FINE_LOCATION))
							{
								// do nothing
							}
						}
						else
						{
							// permission denied
						}
					}
				}
				else
				{
					// all permissions denied
				}
				break;
			}
		}
	}


	@Override
	public void onDatabaseCallRespond(final DatabaseCallTask task, final Data<?> data)
	{
		runTaskCallback(new Runnable() {
			public void run() {
				if (mRootView == null) return; // view was destroyed

				if (task.getQuery().getClass().equals(PoiReadAllQuery.class)) {
					Logcat.d("PoiReadAllQuery");

					// get data
					Data<List<PoiModel>> poiReadAllData = (Data<List<PoiModel>>) data;
					List<PoiModel> poiList = poiReadAllData.getDataObject();
					mPoiList.clear();
					Iterator<PoiModel> iterator = poiList.iterator();
					while (iterator.hasNext()) {
						PoiModel poi = iterator.next();
						mPoiList.add(poi);
					}
				}

				// hide progress and bind data
				if (mPoiList != null && !mPoiList.isEmpty()) mStatefulLayout.showContent();
				else mStatefulLayout.showEmpty();

				// finish query
				mDatabaseCallManager.finishTask(task);
			}
		});
	}


	@Override
	public void onDatabaseCallFail(final DatabaseCallTask task, final Exception exception)
	{
		runTaskCallback(new Runnable() {
			public void run() {
				if (mRootView == null) return; // view was destroyed

				if (task.getQuery().getClass().equals(PoiReadAllQuery.class)) {
					Logcat.d("PoiReadAllQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
				}

				// hide progress
				if (mPoiList != null && !mPoiList.isEmpty()) mStatefulLayout.showContent();
				else mStatefulLayout.showEmpty();

				// handle fail
				handleFail();

				// finish query
				mDatabaseCallManager.finishTask(task);
			}
		});
	}


	private void handleFail()
	{
		Toast.makeText(getActivity(), R.string.global_database_fail_toast, Toast.LENGTH_LONG).show();
	}


	private void handleExtras(Bundle extras)
	{
		if(extras.containsKey(MapActivity.EXTRA_POI_ID))
		{
			mPoiId = extras.getLong(MapActivity.EXTRA_POI_ID);
		}
		if(extras.containsKey(MapActivity.EXTRA_POI_LATITUDE))
		{
			mPoiLatitude = extras.getDouble(MapActivity.EXTRA_POI_LATITUDE);
		}
		if(extras.containsKey(MapActivity.EXTRA_POI_LONGITUDE))
		{
			mPoiLongitude = extras.getDouble(MapActivity.EXTRA_POI_LONGITUDE);
		}
	}
	
	
	private void loadData()
	{
		if(!mDatabaseCallManager.hasRunningTask(PoiReadAllQuery.class))
		{
			// show progress
			mStatefulLayout.showProgress();

			// run async task
			Query query = new PoiReadAllQuery();
			mDatabaseCallManager.executeTask(query, this);
		}
	}


	private void bindData()
	{
		// reference
		final GoogleMap map = ((MapView) mRootView.findViewById(R.id.fragment_map_mapview)).getMap();

		// map
		if(map!=null)
		{
			// add pois
//			map.clear();
//			mClusterManager.clearItems();
//			for(PoiModel poi : mPoiList) mDIPoiList

			for(MainDbObjectData poi : mPois2ClustserList)
//			for(MainDbObjectData poi : mDIPoiList)
			{
				mClusterManager.addItem(poi);
			}
			mClusterManager.cluster();
		}

		// admob
		bindDataBanner();
	}


	private void bindDataBanner()
	{
		if(CityGuideConfig.ADMOB_UNIT_ID_MAP != null && !CityGuideConfig.ADMOB_UNIT_ID_MAP.equals("") && NetworkUtility.isOnline(getActivity()))
		{
			// reference
			ViewGroup contentLayout = (ViewGroup) mRootView.findViewById(R.id.container_content);

			// layout params
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.CENTER;

			// create ad view
			AdView adView = new AdView(getActivity());
			adView.setId(R.id.adview);
			adView.setLayoutParams(params);
			adView.setAdSize(AdSize.SMART_BANNER);
			adView.setAdUnitId(CityGuideConfig.ADMOB_UNIT_ID_MAP);

			// add to layout
			contentLayout.removeView(getActivity().findViewById(R.id.adview));
			contentLayout.addView(adView);

			// call ad request
			AdRequest adRequest = new AdRequest.Builder()
					.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
					.addTestDevice(CityGuideConfig.ADMOB_TEST_DEVICE_ID)
					.build();
			adView.loadAd(adRequest);
		}
	}


	private void initMap()
	{
		if(!Version.isSupportedOpenGlEs2(getActivity()))
		{
			Toast.makeText(getActivity(), R.string.global_map_fail_toast, Toast.LENGTH_LONG).show();
		}

		try
		{
			MapsInitializer.initialize(getActivity());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	private void setupStatefulLayout(Bundle savedInstanceState)
	{
		// reference
		mStatefulLayout = (StatefulLayout) mRootView;

		// state change listener
		mStatefulLayout.setOnStateChangeListener(new StatefulLayout.OnStateChangeListener() {
			@Override
			public void onStateChange(View v, StatefulLayout.State state) {
				Logcat.d("" + (state == null ? "null" : state.toString()));

//				// bind data
//				if (state == StatefulLayout.State.CONTENT) {
//					if (mPoiList != null && !mPoiList.isEmpty()) bindData();
//				}
			}
		});

		// restore state
		mStatefulLayout.restoreInstanceState(savedInstanceState);
	}


	private void setupMap()
	{
		Log.e(TAG, "setupMap started");
		// reference
		GoogleMap map = ((MapView) mRootView.findViewById(R.id.fragment_map_mapview)).getMap();
		map.setOnCameraChangeListener(this);
		Location location4Query = null;

		// settings
		if(map!=null)
		{
			Preferences preferences = new Preferences(getActivity());

			map.setMapType(preferences.getMapType());

			// check access location permission
			if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			{
				map.setMyLocationEnabled(true);
			}

			UiSettings settings = map.getUiSettings();
			settings.setAllGesturesEnabled(true);
			settings.setMyLocationButtonEnabled(true);
			settings.setZoomControlsEnabled(true);

			LatLng latLng = null;
			if(mPoiLatitude==0.0 && mPoiLongitude==0.0 &&
					ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			{
				LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
				location4Query = getLastKnownLocation(locationManager);
				if(location4Query!=null) latLng = new LatLng(location4Query.getLatitude(), location4Query.getLongitude());
			}
			else
			{
				latLng = new LatLng(mPoiLatitude, mPoiLongitude);
				location4Query.setLongitude(mPoiLongitude);
				location4Query.setLatitude(mPoiLatitude);
			}

			if(latLng != null)
			{
				Log.e(TAG, "Map Camera zoom" );
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(latLng)
						.zoom(MAP_ZOOM)
						.bearing(0)
						.tilt(0)
						.build();
				map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}
		}
	}

	@Override
	public void onCameraChange (CameraPosition position)
	{
		GoogleMap map = ((MapView) mRootView.findViewById(R.id.fragment_map_mapview)).getMap();
		mLastCameraPosition = position;
		Location location4Query = null;

		if ( mClusterManager == null ) // fisrt time loaded
		{
			setupClusterManager();
			mIsNewCluster = true;

		}
			LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
			location4Query = getLastKnownLocation(locationManager);
			if (null == location4Query ||
					(0 == location4Query.getLatitude() && 0 == location4Query.getLongitude())) {
				Log.e(TAG, "ERROR!! location4Query = null");
			}
			else {
				updateDataIfNeeded(location4Query, position);
			}
	}

	private void updateDataIfNeeded ( Location location4Query , CameraPosition position )
	{
		GoogleMap map = ((MapView) mRootView.findViewById(R.id.fragment_map_mapview)).getMap();
		mClusterManager.onCameraChange(mLastCameraPosition);

		//calculate radius of visible area
		VisibleRegion visibleRegion = map.getProjection().getVisibleRegion();

		LatLng farRight = visibleRegion.farRight;
		LatLng nearLeft = visibleRegion.nearLeft;

		float[] distanceCalc = new float[2];
		Location.distanceBetween(
				farRight.latitude,
				farRight.longitude,
				nearLeft.latitude,
				nearLeft.longitude,
				distanceCalc);
		float currentRadius = distanceCalc[0]/2000; // dividing by 2 -> diameter to radius , dividing by 1000 -> meter to km


		// get center of visible map area
		LatLng currentCenter = map.getCameraPosition().target;
		if ( null == currentCenter ||
			 (0 == currentCenter.latitude && 0 == currentCenter.longitude ) )
		{
			Log.e(TAG, "ERROR!! Invalid currentCenter Coordinates ");
		}

		// calculate distance between current center and previus center
		float distanceBetweenCenters = 0;
		if( 0 == mPrevCenter.latitude && 0 == mPrevCenter.longitude ) // first time showing an area on the map
		{
			distanceBetweenCenters = mPrevRadius;
		}
		else
		{
			float[] centersDistanceCalc = new float[2];
			Location.distanceBetween(
					mPrevCenter.latitude,
					mPrevCenter.longitude,
					currentCenter.latitude,
					currentCenter.longitude,
					centersDistanceCalc);
			distanceBetweenCenters = centersDistanceCalc[0];
		}

		// if (distance(newCenter,prevCenter) + newRadius > prevRadius) --> new area is not inclusive in old area - load POIs
		if ( distanceBetweenCenters + currentRadius > mPrevRadius )
		{
			assert(location4Query == null);

			// get data from server using user's location and radius of visible area
			DataImporter dataImporter = new DataImporter(mDataImporterListener);
			dataImporter.execute(QueryTypeEnum.QUERY_MAP, (Object)location4Query, (Object)currentRadius);

			// update prevCenter and prevRadius
			mPrevRadius = currentRadius;
			mPrevCenter = currentCenter;
		}
	}

	private void setupClusterManager()
	{
		// reference
		GoogleMap map = ((MapView) mRootView.findViewById(R.id.fragment_map_mapview)).getMap();

		// clustering
		if(map!=null)
		{
			mClusterManager = new ClusterManager<>(getActivity(), map);
			mClusterManager.setRenderer(new DefaultClusterRenderer<MainDbObjectData>(getActivity(), map, mClusterManager) {
				@Override
				protected void onBeforeClusterItemRendered(MainDbObjectData poi, MarkerOptions markerOptions) {
//					CategoryModel category = poi.getCategory();
					Integer tmpId = poi.getDiscountType();
					BitmapDescriptor bitmapDescriptor = mId2BitmapDescriptorMap.get(poi.getDiscountType());   //loadBitmapDescriptor(category);

					markerOptions.title(poi.getName());
					markerOptions.icon(bitmapDescriptor);

					super.onBeforeClusterItemRendered(poi, markerOptions);
				}
			});
			mClusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<MainDbObjectData>() {
				@Override
				public void onClusterItemInfoWindowClick(MainDbObjectData poiModel) {
					startPoiDetailActivity(poiModel.getId());
				}
			});
			//map.setOnCameraChangeListener(mClusterManager);
			map.setOnInfoWindowClickListener(mClusterManager);
		}
	}

	private Location getLastKnownLocation(LocationManager locationManager)
	{
//		if ( !PermissionUtility.checkPermissionAccessLocation(this) ) {
//			Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//			Location locationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//		}

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


	private void setMapType(int type)
	{
		GoogleMap map = ((MapView) mRootView.findViewById(R.id.fragment_map_mapview)).getMap();

		if(map!=null)
		{
			map.setMapType(type);

			Preferences preferences = new Preferences(getActivity());
			preferences.setMapType(type);
		}
	}


	private BitmapDescriptor loadBitmapDescriptor(CategoryModel category)
	{
		BitmapDescriptor bitmapDescriptor = mBitmapDescriptorMap.get(category.getId());
		if(bitmapDescriptor==null)
		{
			try
			{
				CategoryDAO.refresh(category);
				bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(loadMarkerBitmap(category.getMarker()));
			}
			catch(SQLException | IOException | IllegalArgumentException e)
			{
				bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(getColorAccentHue());
			}
			mBitmapDescriptorMap.put(category.getId(), bitmapDescriptor);
		}
		return bitmapDescriptor;
	}


	private Bitmap loadMarkerBitmap(String path) throws IOException, IllegalArgumentException
	{
		int size = getActivity().getResources().getDimensionPixelSize(R.dimen.fragment_map_marker_size);
		InputStream inputStream = getActivity().getAssets().open(path);
		Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
		Bitmap bitmap = BitmapScaler.scaleToFill(originalBitmap, size, size);
		if(originalBitmap!=bitmap) originalBitmap.recycle();
		inputStream.close();
		return bitmap;
	}


	private float getColorAccentHue()
	{
		// get accent color
		TypedValue typedValue = new TypedValue();
		getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
		int markerColor = typedValue.data;

		// get hue
		float[] hsv = new float[3];
		Color.colorToHSV(markerColor, hsv);
		return hsv[0];
	}


	private void startPoiDetailActivity(long poiId)
	{
		Intent intent = PoiDetailActivity.newIntent(getActivity(), poiId);
		startActivity(intent);
	}
}
