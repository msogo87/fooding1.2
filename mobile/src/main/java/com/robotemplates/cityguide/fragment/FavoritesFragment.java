package com.robotemplates.cityguide.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.melnykov.fab.FloatingActionButton;
import com.robotemplates.cityguide.R;
import com.robotemplates.cityguide.activity.PoiDetailActivity;
import com.robotemplates.cityguide.adapter.FavoritesListAdapter;
import com.robotemplates.cityguide.database.data.FavoritesDbRow;
import com.robotemplates.cityguide.utility.Logcat;
import com.robotemplates.cityguide.utility.PermissionUtility;
import com.robotemplates.cityguide.view.GridSpacingItemDecoration;
import com.robotemplates.cityguide.view.StatefulLayout;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by msogolovsky on 23/04/2016.
 */
public class FavoritesFragment extends Fragment implements FavoritesListAdapter.FavViewHolder.OnItemClickListener,  SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = "PoiFavoritesFragment";

    private View                       mRootView;
    private StatefulLayout             mStatefulLayout;
    private FavoritesListAdapter       mAdapter;
    private Map<Long, FavoritesDbRow>  mFavoritesPoiMap      = new HashMap<>();
    private boolean                    isAfterPause          = true;
    private boolean                    mIsFirstTimeViwed     = true;
    ArrayList<FavoritesDbRow>          mFavoritePoiList;


    // empty constructor
    public FavoritesFragment() {


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

///////////////////////////////////////////////
//        // moved to setUserVisibleHint     //
//        setHasOptionsMenu(true);           //
//        setRetainInstance(true);           //
///////////////////////////////////////////////


//        // handle fragment arguments
//        Bundle arguments = getArguments();
//        if(arguments != null)
//        {
//            handleArguments(arguments);
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

/////////////////////////////////////////////
        // moved to setUserVisibleHint     //
        mRootView = inflater.inflate(R.layout.fragment_favorites, container, false);
        setupRecyclerView();
        return mRootView;
/////////////////////////////////////////////

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // setup stateful layout
        setupStatefulLayout(savedInstanceState);

    }

    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser)
        {
            if (mIsFirstTimeViwed) {
                mIsFirstTimeViwed = false;

                setHasOptionsMenu(true);
                setRetainInstance(true);

            }
        }
        else
        {

        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        GetFavoritePoiList();
        // setup stateful layout
        mStatefulLayout.showContent();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);

        SaveFavoriteList();
        isAfterPause = true;
    }

    @Override
    public void onItemClick(View view, int position, long id, int viewType)
    {
        // position
        int poiPosition = mAdapter.getPoiPosition(position);

        // start activity
        FavoritesDbRow poi = mFavoritesPoiMap.get(poiPosition);
        startPoiDetailActivity(view, poi.getId());
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
                Log.e(TAG, "setupStatefulLayout: state is " + state);
                // bind data
                if(state == StatefulLayout.State.CONTENT)
                {
                    // create a sorted arraylist of POIs from mFavoritesPoiMap
                    mFavoritePoiList = new ArrayList<FavoritesDbRow>(mFavoritesPoiMap.values());

                    Log.e(TAG, "state");
                    if( mAdapter != null) //mLazyLoading &&
                    {
                        Log.e(TAG, "Bind Data: refill adapter");
                        mAdapter.refill(mFavoritePoiList);
                        Log.e(TAG, "notifyDataSetChanged - modify the data sent to the adapterS");
                        mAdapter.notifyDataSetChanged();
                    }
                    else
                    {
                        Log.e(TAG, "bind data for the first time");
//                        GetFavoritePoiList();
                        if(mFavoritesPoiMap!=null && !mFavoritesPoiMap.isEmpty()) bindData();
                    }
                }
//
//                // floating action button
//                showFloatingActionButton(state == StatefulLayout.State.CONTENT);
            }
        });

        // restore state
        mStatefulLayout.restoreInstanceState(savedInstanceState);
    }

    private void bindData()
    {
        // reference
        final RecyclerView recyclerView = getRecyclerView();

        // content
        if (recyclerView.getAdapter() == null) {
            Log.e(TAG, "Bind Data: create adapter");
            mAdapter = new FavoritesListAdapter(mFavoritePoiList, this );
        } else {
            Log.e(TAG, "Bind Data: refill adapter");
            mAdapter.refill(mFavoritePoiList, this);
        }

        // set fixed size
        recyclerView.setHasFixedSize(false);

        // add decoration
        RecyclerView.ItemDecoration itemDecoration = new GridSpacingItemDecoration(getResources().getDimensionPixelSize(R.dimen.fragment_poi_list_recycler_item_padding));
        recyclerView.addItemDecoration(itemDecoration);

        // set animator
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // set adapter
        recyclerView.setAdapter(mAdapter);

        // lazy loading
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            private static final int THRESHOLD = 100;

            private int mCounter = 0;
            private Toolbar mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);


            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState)
            {
                super.onScrollStateChanged(recyclerView, newState);

                // reset counter
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    mCounter = 0;
                }

                // disable item animation in adapter
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    mAdapter.setAnimationEnabled(false);
                }
            }


            // when the user is scrolling the list of POIs
            // lazy-load the next items on the list when they become visible

//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
//            {
//                super.onScrolled(recyclerView, dx, dy);
//
//                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
//                int firstVisibleItem              = layoutManager.findFirstVisibleItemPosition();
//                int visibleItemCount              = layoutManager.getChildCount();
//                int totalItemCount                = layoutManager.getItemCount();
//                int lastVisibleItem               = firstVisibleItem + visibleItemCount;
//
////                // lazy-loading POI data if necessary
////                // if POI list is not empty and the last visible item number is less the the threshold LAZY_LOADING_OFFSET,
////                // load the next LAZY_LOADING_TAKE items on the list
////                if ((totalItemCount - lastVisibleItem <= LAZY_LOADING_OFFSET) &&
////                        (mPoiList.size() % LAZY_LOADING_TAKE == 0) &&
////                        (!mPoiList.isEmpty())) {
////                    Log.i(TAG, "onScrolled: LazyLoad - totalItemCount: " + totalItemCount + ", lastVisibleItem: " + ", mPoiList.size: " + mPoiList.size());
////                    if (!mLazyLoading) lazyLoadData();
////                }
            });
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if ( key.compareTo("FAV") == 0 )
        {
            GetFavoritePoiList();
            mStatefulLayout.showContent();
        }
    }

    private void setupRecyclerView()
    {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL , false);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView recyclerView = getRecyclerView();
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
    }

    private RecyclerView getRecyclerView()
    {
        return mRootView!=null ? (RecyclerView) mRootView.findViewById(R.id.fragment_favorites_list_recycler) : null;
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_favorites, container, false);
//    }


    private void startPoiDetailActivity(View view, long poiId)
    {
        Intent intent = PoiDetailActivity.newIntent(getActivity(), poiId);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            ActivityOptions options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight());
            getActivity().startActivity(intent, options.toBundle());
        }
        else
        {
            startActivity(intent);
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
    }

    private void SaveFavoriteList()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(mFavoritesPoiMap);

        editor.putString("FAV", json);
        editor.apply();
    }

    private void UpdateFavoritesList(long id, boolean isShowNotification, String poiName, String picUrl)
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
        mFavoritesPoiMap.put(id, new FavoritesDbRow (id, isShowNotification, poiName, picUrl) );

        // save favorite POIs list
        SaveFavoriteList();
    }

}
