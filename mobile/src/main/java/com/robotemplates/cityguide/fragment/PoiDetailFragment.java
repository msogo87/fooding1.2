package com.robotemplates.cityguide.fragment;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.melnykov.fab.FloatingActionButton;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.robotemplates.cityguide.CityGuideApplication;
import com.robotemplates.cityguide.CityGuideConfig;
import com.robotemplates.cityguide.R;
import com.robotemplates.cityguide.activity.MapActivity;
import com.robotemplates.cityguide.activity.PoiDetailActivity;
import com.robotemplates.cityguide.communication.DataImporter;
import com.robotemplates.cityguide.communication.DataImporterListener;
import com.robotemplates.cityguide.communication.MainDbObjectData;
import com.robotemplates.cityguide.database.DatabaseCallListener;
import com.robotemplates.cityguide.database.DatabaseCallManager;
import com.robotemplates.cityguide.database.DatabaseCallTask;
import com.robotemplates.cityguide.database.dao.PoiDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.data.FavoritesDbRow;
import com.robotemplates.cityguide.database.model.PoiModel;
import com.robotemplates.cityguide.database.query.PoiReadQuery;
import com.robotemplates.cityguide.database.query.Query;
import com.robotemplates.cityguide.dialog.AboutDialogFragment;
import com.robotemplates.cityguide.geolocation.Geolocation;
import com.robotemplates.cityguide.geolocation.GeolocationListener;
import com.robotemplates.cityguide.listener.AnimateImageLoadingListener;
import com.robotemplates.cityguide.utility.ImageLoaderUtility;
import com.robotemplates.cityguide.utility.LocationUtility;
import com.robotemplates.cityguide.utility.Logcat;
import com.robotemplates.cityguide.utility.NetworkUtility;
import com.robotemplates.cityguide.utility.PermissionUtility;
import com.robotemplates.cityguide.utility.ResourcesUtility;
import com.robotemplates.cityguide.view.ObservableStickyScrollView;
import com.robotemplates.cityguide.view.StatefulLayout;
import com.robotemplates.cityguide.common.QueryTypeEnum;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class PoiDetailFragment extends TaskFragment implements DataImporterListener, DatabaseCallListener, GeolocationListener
{
	private static final String    DIALOG_ABOUT               = "about";
	private static final String    TAG                        = "PoiDetailFragment";
	private static final long      TIMER_DELAY                = 60000L; // in milliseconds
	private static final int       MAP_ZOOM                   = 14;
	private static final String    SERVER_URL_4_QUERY_USER_ID = "http://10.0.3.2/fooding/getUserId.php";

	private View                       mRootView;
	private StatefulLayout             mStatefulLayout;
	private DatabaseCallManager        mDatabaseCallManager  = new DatabaseCallManager();
	private ImageLoader                mImageLoader          = ImageLoader.getInstance();
	private DisplayImageOptions        mDisplayImageOptions;
	private ImageLoadingListener       mImageLoadingListener;
	private Geolocation                mGeolocation          = null;
	private Location                   mLocation             = null;
	private Handler                    mTimerHandler;
	private Runnable                   mTimerRunnable;
	private Integer                    mPoiId;
	private MainDbObjectData           mPoi;
	private DataImporterListener       mDataImporterListener = this;
	private TaskFragment               TaskFragmentListener  = this;
	private Location                   mUserLocation         = null;
	private Map<Long, FavoritesDbRow>  mFavoritesPoiMap      = new HashMap<>();
	private boolean                    isAfterPause          = true;
	private boolean                    mIsFavFirstVal        = false;
	
	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
	}

//	public enum Query {
//		QUERY_LIST,
//		QUERY_MAP,
//		QUERY_POI
//	}
	
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

		// image caching options
		setupImageLoader();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mRootView = inflater.inflate(R.layout.fragment_poi_detail, container, false);
		return mRootView;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		// setup stateful layout
		setupStatefulLayout(savedInstanceState);

		// load data
		if(mPoi==null) loadData();

		// check permissions
		PermissionUtility.checkPermissionAll(this);

		// init timer task
//		setupTimer();                         // IMPORTANT!! FOR SOME REASON SETUPTIMER IS REQUIRED!!!

		LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
		mUserLocation = getLastKnownLocation(locationManager);

//		GetFavoritePoiList();


		DataImporter dataImporter = new DataImporter(mDataImporterListener);
		dataImporter.execute(QueryTypeEnum.QUERY_POI, mPoiId);

	}

	private Location getLastKnownLocation(LocationManager locationManager)
	{
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


	@Override
	public void onDataImporterTaskCompleted(final Object object)
	{
		final List<MainDbObjectData> dIpoiList = (List<MainDbObjectData>)object;
		runTaskCallback(new Runnable()
		{
			public void run()
			{
				Log.e(TAG, "DataImporeter callback start...");
				if ( dIpoiList.size() != 1)
				{
					Log.e(TAG, "ERROR!!  DataImporeter callback returned with " + dIpoiList.size() + " POIs instead of 1" );
				}
				mPoi = dIpoiList.get(0);

				// hide progress and bind data
				if(mPoi!=null) mStatefulLayout.showContent();
				else mStatefulLayout.showEmpty();

				// check permissions
				PermissionUtility.checkPermissionAll(TaskFragmentListener);
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

		GetFavoritePoiList();
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();

		isAfterPause = true;

		// stop geolocation
		if(mGeolocation!=null) mGeolocation.stop();
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
		if ( mIsFavFirstVal != mPoi.getFavorite())
		{
			int userId = GetUserId();
			DataImporter dataImporter = new DataImporter(mDataImporterListener);
			dataImporter.execute(QueryTypeEnum.QUERY_FAVORITE, (int)mPoi.getId(), mPoi.getFavorite(), userId, FirebaseInstanceId.getInstance().getToken());

		}

		// cancel async tasks
		mDatabaseCallManager.cancelAllTasks();
	}


	private int GetUserId()
	{
		int userId = 0;
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		String userIdString = sharedPrefs.getString("USER_ID", null);
		if ( userIdString != null)
		{
			return (Integer.parseInt(userIdString));
		}
		else
		{
			Log.e(TAG, "User Id was not found, performing query from server");
			String tokenString = FirebaseInstanceId.getInstance().getToken();
			try {
				String inputString = URLEncoder.encode("token", "UTF-8") + "=" + URLEncoder.encode(tokenString, "UTF-8");
				URL url = new URL(SERVER_URL_4_QUERY_USER_ID);
				HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
				urlCon.setDoOutput(true);
				urlCon.setRequestMethod("POST");
				urlCon.setFixedLengthStreamingMode(inputString.getBytes().length);
				OutputStreamWriter o_stream = new OutputStreamWriter(urlCon.getOutputStream());
				o_stream.write(inputString);
				o_stream.flush();

				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));

					StringBuilder responseString = new StringBuilder();
					String line = null;


					while ((line = reader.readLine()) != null) {
						if (line.startsWith("[")) {
							responseString.append(line);
						}
					}
					reader.close();

				} catch (Exception ex) {
					Log.e(TAG, "EXCEPTION!! failed reading user ID from server");
				}
			} catch (Exception ex) {
				Log.e(TAG, "EXCEPTION!! token to string failed!!");
			}

			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putString("USER_ID", userIdString);
			editor.apply();

			return (userId);
		}

	}
	
	@Override
	public void onDetach()
	{
		super.onDetach();
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// save current instance state
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);

		// stateful layout state
		if(mStatefulLayout!=null) mStatefulLayout.saveInstanceState(outState);
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		// action bar menu
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_poi_detail, menu);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// action bar menu behavior
		switch(item.getItemId())
		{
			case R.id.menu_fragment_poi_detail_share:
				if(mPoi!=null)
				{
					startShareActivity(getString(R.string.fragment_poi_detail_share_subject), getPoiText());
				}
				return true;

			case R.id.menu_rate:
				startWebActivity(getString(R.string.app_store_uri, CityGuideApplication.getContext().getPackageName()));
				return true;

			case R.id.menu_about:
				showAboutDialog();
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
			case PermissionUtility.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
			case PermissionUtility.REQUEST_PERMISSION_ACCESS_LOCATION:
			case PermissionUtility.REQUEST_PERMISSION_ALL:
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
							if(permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE))
							{
								synchronized(this)
								{
									ImageLoader.getInstance().destroy();
									ImageLoaderUtility.init(getActivity().getApplicationContext());
								}
							}
							else if(permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
									permission.equals(Manifest.permission.ACCESS_FINE_LOCATION))
							{
								mGeolocation = null;
								mGeolocation = new Geolocation((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE), this);
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
		runTaskCallback(new Runnable()
		{
			public void run()
			{
//				if(mRootView == null) return; // view was destroyed
//
//				if(task.getQuery().getClass().equals(PoiReadQuery.class))
//				{
//					Logcat.d("PoiReadQuery");
//
//					// get data
//					Data<PoiModel> poiReadData = (Data<PoiModel>) data;
//					mPoi = poiReadData.getDataObject();
//				}
//
//				// hide progress and bind data
//				if(mPoi!=null) mStatefulLayout.showContent();
//				else mStatefulLayout.showEmpty();
//
//				// finish query
//				mDatabaseCallManager.finishTask(task);
			}
		});
	}


	@Override
	public void onDatabaseCallFail(final DatabaseCallTask task, final Exception exception)
	{
		runTaskCallback(new Runnable()
		{
			public void run()
			{
				if(mRootView == null) return; // view was destroyed

				if(task.getQuery().getClass().equals(PoiReadQuery.class))
				{
					Logcat.d("PoiReadQuery / exception " + exception.getClass().getSimpleName() + " / " + exception.getMessage());
				}

				// hide progress
				if(mPoi!=null) mStatefulLayout.showContent();
				else mStatefulLayout.showEmpty();

				// handle fail
				handleFail();

				// finish query
				mDatabaseCallManager.finishTask(task);
			}
		});
	}


	@Override
	public void onGeolocationRespond(Geolocation geolocation, final Location location)
	{
		runTaskCallback(new Runnable()
		{
			public void run()
			{
				if(mRootView == null) return; // view was destroyed

				Logcat.d("onGeolocationRespond() = " + location.getProvider() + " / " + location.getLatitude() + " / " + location.getLongitude() + " / " + new Date(location.getTime()).toString());
				mLocation = location;
				if(mPoi!=null) bindDataInfo();
			}
		});
	}


	@Override
	public void onGeolocationFail(Geolocation geolocation)
	{
		runTaskCallback(new Runnable()
		{
			public void run()
			{
				if(mRootView == null) return; // view was destroyed

				Logcat.d("onGeolocationFail()");
			}
		});
	}


	private void handleFail()
	{
		Toast.makeText(getActivity(), R.string.global_database_fail_toast, Toast.LENGTH_LONG).show();
	}


	private void handleExtras(Bundle extras)
	{
		Long temp;
		mPoiId = (int) extras.getLong(PoiDetailActivity.EXTRA_POI_ID);
//		mPoiId = (int)(long)temp;
	}

	
	private void loadData()
	{
		// load poi
		if(!mDatabaseCallManager.hasRunningTask(PoiReadQuery.class))
		{
			// show progress
			mStatefulLayout.showProgress();

			// run async task
			Query query = new PoiReadQuery(mPoiId);
			mDatabaseCallManager.executeTask(query, this);
		}
	}


	private void showFloatingActionButton(boolean visible)
	{
		final FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
		if(visible)
		{
			fab.animate()
					.scaleX(1)
					.scaleY(1)
					.setDuration(300)
					.setInterpolator(new AccelerateDecelerateInterpolator())
					.setListener(new Animator.AnimatorListener()
					{
						@Override
						public void onAnimationStart(Animator animator)
						{
							fab.show(false);
							fab.setVisibility(View.VISIBLE);
							fab.setEnabled(false);
						}

						@Override
						public void onAnimationEnd(Animator animator)
						{
							fab.setEnabled(true);
						}

						@Override
						public void onAnimationCancel(Animator animator) {}

						@Override
						public void onAnimationRepeat(Animator animator) {}
					});
		}
		else
		{
			fab.animate()
					.alpha(0F)
					.setDuration(50)
					.setInterpolator(new AccelerateDecelerateInterpolator())
					.setListener(new Animator.AnimatorListener()
					{
						@Override
						public void onAnimationStart(Animator animator)
						{
							fab.setEnabled(false);
						}


						@Override
						public void onAnimationEnd(Animator animator)
						{
							fab.setScaleX(0);
							fab.setScaleY(0);
							fab.setAlpha(1F);
							fab.hide(false);
							fab.setVisibility(View.GONE);
							fab.setEnabled(true);
						}


						@Override
						public void onAnimationCancel(Animator animator)
						{
						}


						@Override
						public void onAnimationRepeat(Animator animator)
						{
						}
					});
		}
	}


	private void bindData()
	{
		bindDataToolbar();
		bindDataInfo();
		bindDataBanner();
		bindDataMap();
		bindDataDescription();
		bindDataGap();
	}

	
	private void bindDataToolbar()
	{
		// reference
		final ObservableStickyScrollView observableStickyScrollView = (ObservableStickyScrollView) mRootView.findViewById(R.id.container_content);
		final FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.fab);
		final View panelTopView = mRootView.findViewById(R.id.toolbar_image_panel_top);
		final View panelBottomView = mRootView.findViewById(R.id.toolbar_image_panel_bottom);
		final ImageView imageView = (ImageView) mRootView.findViewById(R.id.toolbar_image_imageview);
		final TextView titleTextView = (TextView) mRootView.findViewById(R.id.toolbar_image_title);

		// title
		titleTextView.setText(mPoi.getName());

		// image
		mImageLoader.displayImage(mPoi.getImage(), imageView, mDisplayImageOptions, mImageLoadingListener);

		// scroll view
		observableStickyScrollView.setOnScrollViewListener(new ObservableStickyScrollView.ScrollViewListener()
		{
			private final int THRESHOLD = PoiDetailFragment.this.getResources().getDimensionPixelSize(R.dimen.toolbar_image_gap_height);
			private final int PADDING_LEFT = PoiDetailFragment.this.getResources().getDimensionPixelSize(R.dimen.toolbar_image_title_padding_right);
			private final int PADDING_BOTTOM = PoiDetailFragment.this.getResources().getDimensionPixelSize(R.dimen.global_spacing_xs);
			private final float SHADOW_RADIUS = 16;

			private int mPreviousY = 0;
			private ColorDrawable mTopColorDrawable = new ColorDrawable();
			private ColorDrawable mBottomColorDrawable = new ColorDrawable();


			@SuppressWarnings("deprecation")
			@Override
			public void onScrollChanged(ObservableStickyScrollView scrollView, int x, int y, int oldx, int oldy)
			{
				// floating action button
				if(y > THRESHOLD)
				{
					if(floatingActionButton.getVisibility() == View.GONE && floatingActionButton.isEnabled())
					{
						showFloatingActionButton(true);
					}
				}
				else
				{
					if(floatingActionButton.getVisibility() == View.VISIBLE && floatingActionButton.isEnabled())
					{
						showFloatingActionButton(false);
					}
				}

				// do not calculate if header is hidden
				if(y > THRESHOLD && mPreviousY > THRESHOLD) return;

				// calculate panel alpha
				int alpha = (int) (y * (255F / (float) THRESHOLD));
				if(alpha > 255) alpha = 255;

				// set color drawables
				mTopColorDrawable.setColor(ResourcesUtility.getValueOfAttribute(getActivity(), R.attr.colorPrimary));
				mTopColorDrawable.setAlpha(alpha);
				mBottomColorDrawable.setColor(ResourcesUtility.getValueOfAttribute(getActivity(), R.attr.colorPrimary));
				mBottomColorDrawable.setAlpha(alpha);

				// set panel background
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				{
					panelTopView.setBackground(mTopColorDrawable);
					panelBottomView.setBackground(mBottomColorDrawable);
				}
				else
				{
					panelTopView.setBackgroundDrawable(mTopColorDrawable);
					panelBottomView.setBackgroundDrawable(mBottomColorDrawable);
				}

				// calculate image translation
				float translation = y / 2;

				// set image translation
				imageView.setTranslationY(translation);

				// calculate title padding
				int paddingLeft = (int) (y * (float) PADDING_LEFT / (float) THRESHOLD);
				if(paddingLeft > PADDING_LEFT) paddingLeft = PADDING_LEFT;

				int paddingRight = PADDING_LEFT - paddingLeft;

				int paddingBottom = (int) ((THRESHOLD - y) * (float) PADDING_BOTTOM / (float) THRESHOLD);
				if(paddingBottom < 0) paddingBottom = 0;

				// set title padding
				titleTextView.setPadding(paddingLeft, 0, paddingRight, paddingBottom);

				// calculate title shadow
				float radius = ((THRESHOLD - y) * SHADOW_RADIUS / (float) THRESHOLD);

				// set title shadow
				titleTextView.setShadowLayer(radius, 0F, 0F, ContextCompat.getColor(getActivity(), android.R.color.black));

				// previous y
				mPreviousY = y;
			}
		});


		// invoke scroll event because of orientation change toolbar refresh
		observableStickyScrollView.post(new Runnable()
		{
			@Override
			public void run()
			{
				observableStickyScrollView.scrollTo(0, observableStickyScrollView.getScrollY() - 1);
			}
		});

        FavoritesDbRow favRow = mFavoritesPoiMap.get(mPoi.getId());
        if ( favRow != null )
        {
            mPoi.setFavorite(favRow.getToggle());
			mIsFavFirstVal = mPoi.getFavorite();
        }
        else
        {
            mPoi.setFavorite(false);
        }

		// floating action button
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) floatingActionButton.getLayoutParams();
		params.topMargin = getResources().getDimensionPixelSize(R.dimen.toolbar_image_collapsed_height) - getResources().getDimensionPixelSize(R.dimen.fab_mini_size) / 2;
		floatingActionButton.setLayoutParams(params);
		floatingActionButton.setImageDrawable(mPoi.getFavorite() ? ContextCompat.getDrawable(getActivity(), R.drawable.ic_menu_favorite_checked) : ContextCompat.getDrawable(getActivity(), R.drawable.ic_menu_favorite_unchecked));
		floatingActionButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
					mPoi.setFavorite(!mPoi.getFavorite());
					floatingActionButton.setImageDrawable(mPoi.getFavorite() ? ContextCompat.getDrawable(getActivity(), R.drawable.ic_menu_favorite_checked) : ContextCompat.getDrawable(getActivity(), R.drawable.ic_menu_favorite_unchecked));
					UpdateFavoritesList(mPoi.getId(),true, mPoi.getName(), mPoi.getImage(), mPoi.getFavorite());
			}
		});
	}


	private void bindDataInfo()
	{
		// reference
		TextView introTextView    = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_info_intro);
		TextView addressTextView  = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_info_address);
		TextView distanceTextView = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_info_distance);
		TextView linkTextView     = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_info_link);
		TextView phoneTextView    = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_info_phone);
		TextView emailTextView    = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_info_email);

		// intro
		if(mPoi.getIntro()!=null && !mPoi.getIntro().trim().equals(""))
		{
			introTextView.setText(mPoi.getIntro());
			introTextView.setVisibility(View.VISIBLE);
		}
		else
		{
			introTextView.setVisibility(View.GONE);
		}

		// address
		if(mPoi.getAddress()!=null && !mPoi.getAddress().trim().equals(""))
		{
			addressTextView.setText(mPoi.getAddress());
			addressTextView.setVisibility(View.VISIBLE);
			addressTextView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startMapActivity(mPoi);
				}
			});
		}
		else
		{
			addressTextView.setVisibility(View.GONE);
		}

		// distance
		if(mLocation!=null)
		{
			LatLng myLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
			LatLng poiLocation = new LatLng(mPoi.getLatitude(), mPoi.getLongitude());
			String distance = LocationUtility.getDistanceString(LocationUtility.getDistance(myLocation, poiLocation), LocationUtility.isMetricSystem());
			distanceTextView.setText(distance);
			distanceTextView.setVisibility(View.VISIBLE);
			distanceTextView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startNavigateActivity(mPoi.getLatitude(), mPoi.getLongitude());
				}
			});
		}
		else
		{
			distanceTextView.setVisibility(View.GONE);
		}

		// link
		if(mPoi.getLink()!=null && !mPoi.getLink().trim().equals(""))
		{
			linkTextView.setText(mPoi.getLink());
			linkTextView.setVisibility(View.VISIBLE);
			linkTextView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startWebActivity(mPoi.getLink());
				}
			});
		}
		else
		{
			linkTextView.setVisibility(View.GONE);
		}

		// phone
		if(mPoi.getPhone()!=null && !mPoi.getPhone().trim().equals(""))
		{
			phoneTextView.setText(mPoi.getPhone());
			phoneTextView.setVisibility(View.VISIBLE);
			phoneTextView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startCallActivity(mPoi.getPhone());
				}
			});
		}
		else
		{
			phoneTextView.setVisibility(View.GONE);
		}

		// email
		if(mPoi.getEmail()!=null && !mPoi.getEmail().trim().equals(""))
		{
			emailTextView.setText(mPoi.getEmail());
			emailTextView.setVisibility(View.VISIBLE);
			emailTextView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startEmailActivity(mPoi.getEmail());
				}
			});
		}
		else
		{
			emailTextView.setVisibility(View.GONE);
		}
	}


	private void bindDataBanner()
	{
		// reference
		final ViewGroup bannerViewGroup = (ViewGroup) mRootView.findViewById(R.id.fragment_poi_detail_banner);

		if(CityGuideConfig.ADMOB_UNIT_ID_POI_DETAIL != null && !CityGuideConfig.ADMOB_UNIT_ID_POI_DETAIL.equals("") && NetworkUtility.isOnline(getActivity()))
		{
			// layout params
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.CENTER;

			// create ad view
			AdView adView = new AdView(getActivity());
			adView.setId(R.id.adview);
			adView.setLayoutParams(params);
			adView.setAdSize(AdSize.LARGE_BANNER);
			adView.setAdUnitId(CityGuideConfig.ADMOB_UNIT_ID_POI_DETAIL);

			// add to layout
			bannerViewGroup.removeView(getActivity().findViewById(R.id.adview));
			bannerViewGroup.addView(adView);
			bannerViewGroup.setVisibility(View.VISIBLE);

			// call ad request
			AdRequest adRequest = new AdRequest.Builder()
					.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
					.addTestDevice(CityGuideConfig.ADMOB_TEST_DEVICE_ID)
					.build();
			adView.loadAd(adRequest);
		}
		else
		{
			bannerViewGroup.setVisibility(View.GONE);
		}
	}


	private void bindDataMap()
	{
		// reference
		final ImageView imageView = (ImageView) mRootView.findViewById(R.id.fragment_poi_detail_map_image);
		final ViewGroup wrapViewGroup = (ViewGroup) mRootView.findViewById(R.id.fragment_poi_detail_map_image_wrap);
		final Button exploreButton = (Button) mRootView.findViewById(R.id.fragment_poi_detail_map_explore);
		final Button navigateButton = (Button) mRootView.findViewById(R.id.fragment_poi_detail_map_navigate);

		// image
		String key = getString(R.string.google_maps_api_key);
		String url = getStaticMapUrl(key, mPoi.getLatitude(), mPoi.getLongitude(), MAP_ZOOM);
		mImageLoader.displayImage(url, imageView, mDisplayImageOptions, mImageLoadingListener);

		// wrap
		wrapViewGroup.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startMapActivity(mPoi);
			}
		});

		// explore
		exploreButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startMapActivity(mPoi);
			}
		});

		// navigate
		navigateButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startNavigateActivity(mPoi.getLatitude(), mPoi.getLongitude());
			}
		});
	}


	private void bindDataDescription()
	{
		// reference
		TextView descriptionTextView = (TextView) mRootView.findViewById(R.id.fragment_poi_detail_description_text);

		// content
		if(mPoi.getDescription()!=null && !mPoi.getDescription().trim().equals(""))
		{
			descriptionTextView.setText(mPoi.getDescription());
		}
	}


	private void bindDataGap()
	{
		// reference
		final View gapView = mRootView.findViewById(R.id.fragment_poi_detail_gap);
		final CardView mapCardView = (CardView) mRootView.findViewById(R.id.fragment_poi_detail_map);

		// add gap in scroll view so favorite floating action button can be shown on tablet
		if(gapView!=null)
		{
			mapCardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
			{
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout()
				{
					// cardview height
					int cardHeight = mapCardView.getHeight();

					// toolbar height
					int toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_image_collapsed_height);

					// screen height
					Display display = getActivity().getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					int screenHeight = size.y;

					// calculate gap height
					int gapHeight = screenHeight - cardHeight - toolbarHeight;
					if(gapHeight > 0)
					{
						ViewGroup.LayoutParams params = gapView.getLayoutParams();
						params.height = gapHeight;
						gapView.setLayoutParams(params);
					}

					// remove layout listener
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
					{
						mapCardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
					else
					{
						mapCardView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}
				}
			});
		}
	}


	private void setupStatefulLayout(Bundle savedInstanceState)
	{
		// reference
		mStatefulLayout = (StatefulLayout) mRootView;

		// state change listener
		mStatefulLayout.setOnStateChangeListener(new StatefulLayout.OnStateChangeListener()
		{
			@Override
			public void onStateChange(View v, StatefulLayout.State state)
			{
				Logcat.d("" + (state==null ? "null" : state.toString()));

				// bind data
				if(state==StatefulLayout.State.CONTENT)
				{
					if(mPoi!=null) bindData();
				}

				// floating action button
				if(state!=StatefulLayout.State.CONTENT)
				{
					showFloatingActionButton(false);
				}

				// toolbar background and visibility
				Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

				if(state==StatefulLayout.State.CONTENT)
				{
					toolbar.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.transparent));
				}
				else
				{
					toolbar.setBackgroundColor(ResourcesUtility.getValueOfAttribute(getActivity(), R.attr.colorPrimary));
				}

				if(state==StatefulLayout.State.PROGRESS)
				{
					toolbar.setVisibility(View.GONE);
				}
				else
				{
					toolbar.setVisibility(View.VISIBLE);
				}
			}
		});

		// restore state
		mStatefulLayout.restoreInstanceState(savedInstanceState);
	}


	private void setupImageLoader()
	{
		mDisplayImageOptions = new DisplayImageOptions.Builder()
				.showImageOnLoading(android.R.color.transparent)
				.showImageForEmptyUri(R.drawable.placeholder_photo)
				.showImageOnFail(R.drawable.placeholder_photo)
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.displayer(new SimpleBitmapDisplayer())
				.build();
		mImageLoadingListener = new AnimateImageLoadingListener();
	}


	private void setupTimer()
	{
		mTimerHandler  = new Handler();
		mTimerRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				Logcat.d("timer");

				// check access location permission
				if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
				{
					// start geolocation
					mGeolocation = null;
					mGeolocation = new Geolocation((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE), PoiDetailFragment.this);
				}

				mTimerHandler.postDelayed(this, TIMER_DELAY);
			}
		};
	}


	private void startTimer()
	{
		mTimerHandler.postDelayed(mTimerRunnable, 0);
	}


	private void stopTimer()
	{
		mTimerHandler.removeCallbacks(mTimerRunnable);
	}


	private String getPoiText()
	{
		StringBuilder builder = new StringBuilder();
		builder.append(mPoi.getName());
		builder.append("\n\n");
		if(mPoi.getAddress()!=null && !mPoi.getAddress().trim().equals(""))
		{
			builder.append(mPoi.getAddress());
			builder.append("\n\n");
		}
		if(mPoi.getIntro()!=null && !mPoi.getIntro().trim().equals(""))
		{
			builder.append(mPoi.getIntro());
			builder.append("\n\n");
		}
		if(mPoi.getDescription()!=null && !mPoi.getDescription().trim().equals(""))
		{
			builder.append(mPoi.getDescription());
			builder.append("\n\n");
		}
		if(mPoi.getLink()!=null && !mPoi.getLink().trim().equals(""))
		{
			builder.append(mPoi.getLink());
		}
		return builder.toString();
	}


	private String getStaticMapUrl(String key, double lat, double lon, int zoom)
	{
		TypedValue typedValue = new TypedValue();
		getActivity().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
		int markerColor = typedValue.data;
		String markerColorHex = String.format("0x%06x", (0xffffff & markerColor));

		String builder = new String();
		builder = "https://maps.googleapis.com/maps/api/staticmap" + "?key=" + key + "&size=320x320" + "&scale=2" + "&maptype=roadmap" + "&zoom=" +
		zoom + "&center=" + lat + "," + lon + "&markers=color:" + markerColorHex + "%7C" + lat + "," + lon ;
		return builder.toString();
	}


	private void showAboutDialog()
	{
		// create and show the dialog
		DialogFragment newFragment = AboutDialogFragment.newInstance();
		newFragment.setTargetFragment(this, 0);
		newFragment.show(getFragmentManager(), DIALOG_ABOUT);
	}


	private void startMapActivity(MainDbObjectData poi)
	{
		Intent intent = MapActivity.newIntent(getActivity(), poi.getId(), poi.getLatitude(), poi.getLongitude());
		startActivity(intent);
	}


	private void startShareActivity(String subject, String text)
	{
		try
		{
			Intent intent = new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
			intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
			startActivity(intent);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			// can't start activity
		}
	}


	private void startWebActivity(String url)
	{
		try
		{
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(intent);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			// can't start activity
		}
	}


	private void startCallActivity(String phoneNumber)
	{
		try
		{
			String builder = new String();
			builder = "tel:" + phoneNumber;

			Intent intent = new Intent(android.content.Intent.ACTION_DIAL, Uri.parse(builder.toString()));
			startActivity(intent);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			// can't start activity
		}
	}


	private void startEmailActivity(String email)
	{
		try
		{
			String builder = new String();
			builder = "mailto:" + email;

			Intent intent = new Intent(android.content.Intent.ACTION_SENDTO, Uri.parse(builder.toString()));
			startActivity(intent);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			// can't start activity
		}
	}


	private void startNavigateActivity(double lat, double lon)
	{
		try
		{
			String uri = String.format("http://maps.google.com/maps?daddr=%s,%s", Double.toString(lat), Double.toString(lon));
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
			startActivity(intent);
		}
		catch(android.content.ActivityNotFoundException e)
		{
			// can't start activity
		}
	}

	private void GetFavoritePoiList()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		Gson gson = new Gson();
		String json = sharedPrefs.getString("FAV", null);
        if ( json == null)
        {
            Log.e(TAG, "Error in SaveFavoriteList");
            return;
        }
		Type type = new TypeToken<Map<Long, FavoritesDbRow>>() {}.getType();
        mFavoritesPoiMap = gson.fromJson(json, type);

//        Map<Long, FavoritesDbRow> favoritesMap = null;
//        try {
//            // create an ObjectInputStream for the file we created before
//            ObjectInputStream ois =
//                    new ObjectInputStream(new FileInputStream("test.txt"));
//
//            // read and print an object and cast it as string
//            System.out.println("" + (Map<Long, FavoritesDbRow>) ois.readObject());
//
//            // read and print an object and cast it as string
//            favoritesMap = (Map<Long, FavoritesDbRow>) ois.readObject();
//
//        } catch (Exception ex){
//            Log.e(TAG, "Error in SaveFavoriteList");
//        }

	}

	private void SaveFavoriteList()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		SharedPreferences.Editor editor = sharedPrefs.edit();
		Gson gson = new Gson();

		String json = gson.toJson(mFavoritesPoiMap);

		editor.putString("FAV", json);
//        editor.remove("FAV");
		editor.apply();


//        try {
//            // create a new file with an ObjectOutputStream
//            FileOutputStream out = new FileOutputStream("test.txt");
//            ObjectOutputStream oout = new ObjectOutputStream(out);
//
//            // write something in the file
//            oout.writeObject(mFavoritesPoiMap);
//            oout.flush();
//        } catch (Exception ex){
//                Log.e(TAG, "Error in SaveFavoriteList");
//            }

	}

	private void UpdateFavoritesList(long id, boolean isShowNotification, String poiName, String picUrl, boolean isAddFavPoi)
	{
		if ( isAfterPause ) {
			// get list of favorite POIs list
			GetFavoritePoiList();
			isAfterPause = false;
		}

		// mFavoritesPoiList doesn't exist, create it
		if ( mFavoritesPoiMap == null )
		{
            mFavoritesPoiMap = new HashMap<>();
		}

		// update favorite POIs list with new entry
        if ( isAddFavPoi )
        {
            mFavoritesPoiMap.put(id, new FavoritesDbRow(id, isShowNotification, poiName, picUrl));
        }
        else {
            mFavoritesPoiMap.remove(id);
        }

		// save favorite POIs list
		SaveFavoriteList();
	}
}

