package com.robotemplates.cityguide.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
//import android.app.Fragment;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.robotemplates.cityguide.CityGuideApplication;
import com.robotemplates.cityguide.R;
import com.robotemplates.cityguide.adapter.DrawerAdapter;
import com.robotemplates.cityguide.common.QueryTypeEnum;
import com.robotemplates.cityguide.communication.DataImporter;
import com.robotemplates.cityguide.database.dao.CategoryDAO;
import com.robotemplates.cityguide.database.model.CategoryModel;
import com.robotemplates.cityguide.fragment.FavoritesFragment;
import com.robotemplates.cityguide.fragment.MapFragment;
import com.robotemplates.cityguide.fragment.PoiListFragment;
import com.robotemplates.cityguide.listener.OnSearchListener;
import com.robotemplates.cityguide.utility.ResourcesUtility;
import com.robotemplates.cityguide.view.DrawerDividerItemDecoration;
import com.robotemplates.cityguide.view.ScrimInsetsFrameLayout;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements DrawerAdapter.CategoryViewHolder.OnItemClickListener, OnSearchListener
{
	private String                 TAG  = "MainActivity";

	private DrawerLayout           mDrawerLayout;
	private ActionBarDrawerToggle  mDrawerToggle;
	private ScrimInsetsFrameLayout mDrawerScrimInsetsFrameLayout;
	private DrawerAdapter          mDrawerAdapter;
	private CharSequence           mTitle;
	private CharSequence           mDrawerTitle;
	private List<CategoryModel>    mCategoryList;
	private TabLayout              mTabLayout;// = (TabLayout) findViewById(R.id.tabs);
	private ViewPager              mViewPager;// = (ViewPager) findViewById(R.id.viewpager);
	private Fragment               mPoiListFragment;


	public static Intent newIntent(Context context)
	{
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.e(TAG, "MainActivity start");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupActionBar();

		// configure side sliding drawer
		setupRecyclerView();
		setupDrawer(savedInstanceState);

		// configure tabs
//		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
//		setupViewPager(viewPager);

//		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//		tabLayout.setupWithViewPager(viewPager);

		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		setupViewPager(mViewPager);

		mTabLayout = (TabLayout) findViewById(R.id.tabs);
		mTabLayout.post(new Runnable() {
			@Override
			public void run() {
				mTabLayout.setupWithViewPager(mViewPager);
			}
		});

		// Init Firebase
		FirebaseMessaging.getInstance().subscribeToTopic("pushNotifications");
		FirebaseInstanceId.getInstance().getToken();

		// init analytics tracker
		((CityGuideApplication) getApplication()).getTracker();
	}

	private void setupViewPager(ViewPager viewPager) {
		ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager()); // getSupportFragmentManager()); // getChildFragmentManager()
		mPoiListFragment = new PoiListFragment();
		adapter.addFragment(mPoiListFragment       , "Deals");
		adapter.addFragment(new MapFragment()      , "Map");
		adapter.addFragment(new FavoritesFragment(), "Favorites");
		viewPager.setAdapter(adapter);
		viewPager.setOffscreenPageLimit(2); // 2 tabs that are in the background will not be destroyed
	}

	class ViewPagerAdapter extends FragmentPagerAdapter {
		private final List<Fragment> mFragmentList      = new ArrayList<>();
		private final List<String>   mFragmentTitleList = new ArrayList<>();

		public ViewPagerAdapter(FragmentManager manager) {
			super(manager);
		}

		@Override
		public Fragment getItem(int position) {
			return mFragmentList.get(position);
		}

		@Override
		public int getCount() {
			return mFragmentList.size();
		}

		public void addFragment(Fragment fragment, String title) {
			mFragmentList.add(fragment);
			mFragmentTitleList.add(title);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mFragmentTitleList.get(position);
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();

		// analytics
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
	}
	
	
	@Override
	public void onResume()
	{
		super.onResume();
	}
	
	
	@Override
	public void onPause()
	{
		super.onPause();
	}
	
	
	@Override
	public void onStop()
	{
		super.onStop();

		// analytics
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
	}
	
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// open or close the drawer if home button is pressed
		if(mDrawerToggle.onOptionsItemSelected(item))
		{
			return true;
		}

		// action bar menu behaviour
		switch(item.getItemId())
		{
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}


	@Override
	public void onConfigurationChanged(Configuration newConfiguration)
	{
		super.onConfigurationChanged(newConfiguration);
		mDrawerToggle.onConfigurationChanged(newConfiguration);
	}


	@Override
	public void onBackPressed()
	{
		if(mDrawerLayout.isDrawerOpen(Gravity.LEFT))
		{
			mDrawerLayout.closeDrawer(Gravity.LEFT);
		}
		else
		{
			super.onBackPressed();
		}
	}


	@Override
	public void setTitle(CharSequence title)
	{
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}


	@Override
	public void onItemClick(View view, int position, long id, int viewType)
	{
		// position
		int categoryPosition = mDrawerAdapter.getCategoryPosition(position);
		selectDrawerItem(categoryPosition);
	}


	@Override
	public void onSearch(String query)
	{
		Fragment fragment = PoiListFragment.newInstance(query);
		FragmentManager fragmentManager = getSupportFragmentManager();
//		fragmentManager.beginTransaction().replace(R.id.container_drawer_content, fragment).commitAllowingStateLoss();

		mDrawerAdapter.setSelected(mDrawerAdapter.getRecyclerPositionByCategory(0));
		setTitle(getString(R.string.title_search) + ": " + query);
	}


	private void setupActionBar()
	{
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayUseLogoEnabled(false);
		bar.setDisplayShowTitleEnabled(true);
		bar.setDisplayShowHomeEnabled(true);
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setHomeButtonEnabled(true);
	}


	private void setupRecyclerView()
	{
		// reference
		RecyclerView recyclerView = getRecyclerView();

		// set layout manager
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
		linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(linearLayoutManager);

		// load categories from database
		loadCategoryList();

		// set adapter
		if(recyclerView.getAdapter()==null)
		{
			// create adapter
			mDrawerAdapter = new DrawerAdapter(mCategoryList, this);
		}
		else
		{
			// refill adapter
			mDrawerAdapter.refill(mCategoryList, this);
		}
		recyclerView.setAdapter(mDrawerAdapter);

		// add decoration
		List<Integer> dividerPositions = new ArrayList<>();
		dividerPositions.add(3);
		RecyclerView.ItemDecoration itemDecoration = new DrawerDividerItemDecoration(
				this,
				null,
				dividerPositions,
				getResources().getDimensionPixelSize(R.dimen.global_spacing_xxs));
		recyclerView.addItemDecoration(itemDecoration);
	}


	private void setupDrawer(Bundle savedInstanceState)
	{
		mTitle = getTitle();
		mDrawerTitle = getTitle();

		// reference
		mDrawerLayout = (DrawerLayout) findViewById(R.id.activity_main_drawer_layout);
		mDrawerScrimInsetsFrameLayout = (ScrimInsetsFrameLayout) findViewById(R.id.activity_main_drawer_scrim_layout);

		// set drawer - the sliding menu
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerLayout.setStatusBarBackgroundColor(ResourcesUtility.getValueOfAttribute(this, R.attr.colorPrimaryDark));
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close)
		{
			@Override
			public void onDrawerClosed(View view)
			{
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView)
			{
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// show initial fragment
		if(savedInstanceState == null)
		{
			//selectDrawerItem(0);
		}
	}


	private void selectDrawerItem(int position)
	{
//		Fragment fragment = PoiListFragment.newInstance(mCategoryList.get(position).getId());
//		FragmentManager fragmentManager = getSupportFragmentManager();
//		fragmentManager.beginTransaction().replace(R.id.viewpager, fragment).commitAllowingStateLoss();

//		Fragment fragment = (mViewPager.getAdapter()).getItem(0); // add view page adapter
//		Fragment fragment = PoiListFragment.newInstance(mCategoryList.get(position).getId());
		mPoiListFragment = PoiListFragment.newInstance(1);

		mDrawerAdapter.setSelected(mDrawerAdapter.getRecyclerPositionByCategory(position));
		//setTitle(mCategoryList.get(position).getName());
		setTitle("EatUp!");
		mDrawerLayout.closeDrawer(mDrawerScrimInsetsFrameLayout);
	}


	private void loadCategoryList()
	{
		try
		{
			mCategoryList = CategoryDAO.readAll(-1L, -1L);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}

		CategoryModel all = new CategoryModel();
		all.setId(PoiListFragment.CATEGORY_ID_ALL);
		all.setName(getResources().getString(R.string.drawer_category_all));
		all.setImage("drawable://" + R.drawable.ic_category_all);

		CategoryModel favorites = new CategoryModel();
		favorites.setId(PoiListFragment.CATEGORY_ID_FAVORITES);
		favorites.setName(getResources().getString(R.string.drawer_category_favorites));
		favorites.setImage("drawable://" + R.drawable.ic_category_favorites);

		mCategoryList.add(0, all);
		mCategoryList.add(1, favorites);
	}


	private RecyclerView getRecyclerView()
	{
		return (RecyclerView) findViewById(R.id.activity_main_drawer_recycler);
	}
}
