package com.robotemplates.cityguide.communication;

/**
 * Created by msogolovsky on 07/02/2016.
 * DataImporter is comunicating with a remote server and fetching data.
 * It is an Async task that sends a request to a remote server,
 * gets a response of json string with the requested data,
 * parses it using gson-2.5.jar
 */

import android.location.Location;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
*/
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.os.AsyncTask;
import android.util.Log;

import com.robotemplates.cityguide.common.QueryTypeEnum;
import com.robotemplates.cityguide.fragment.PoiListFragment;

public class DataImporter extends AsyncTask<Object, Integer, Integer>
{
    private static final String TAG = "DataImporter"; // Tag for Log prints

    private Location                            mLocation;         // input type for doInBackground
    private List<MainDbObjectData>              mPoiList;          // output data list that goes to call back method    // IMPORTATNT!! : This was public for some reason
    private WeakReference<DataImporterListener> mListener;
    private MainDbObjectData                    mPoi = null;
    private QueryTypeEnum                       mQueryType;
    private String                              mUserId;

    private static final String SERVER_URL_4_LIST_QUERY      = "http://10.0.3.2/fooding/queryPreviewDbByLocation.php";  // Works with Genymotion
    private static final String SERVER_URL_4_MAP_QUERY       = "http://10.0.3.2/fooding/queryPreviewDbByLocation.php";  // Works with Genymotion
    private static final String SERVER_URL_4_POI_QUERY       = "http://10.0.3.2/fooding/queryPreviewDbByLocation.php";  // Works with Genymotion
    private static final String SERVER_URL_4_FAVORITES_QUERY = "http://10.0.3.2/fooding/update_favorites.php";
    private static final String SERVER_URL_4_QUERY_USER_ID   = "http://10.0.3.2/fooding/getUserId.php";
//    public static final String SERVER_URL = "http://localhost/fooding/queryPreviewDbByLocation.php";
//    public static final String SERVER_URL = "http://192.168.1.6:8080/fooding/queryPreviewDbByLocation.php";
//    public static final String SERVER_URL = "http://kylewbanks.com/rest/posts";                                // random server for checking connection
//    public static final String SERVER_URL = "http://192.168.56.1:8080/fooding/queryPreviewDbByLocation.php";

    //Empty Builder
    public DataImporter(DataImporterListener dataImporterListener)
    {
        mPoiList = new ArrayList<MainDbObjectData>();
        //this.mfatherObject = fatherObject;
        mListener = new WeakReference<>(dataImporterListener);
    }

    @Override
    protected Integer doInBackground(Object...params)
    {
        Log.e(TAG, "DataImporeter Execcute start");
        final int expNParamsListQuery = 1;
        final int expNParamsMapQuery  = 2;
        float distance = 0;
        int poiId = 0;
        int userId = 0;
        boolean isFavorite = false;
        mQueryType = (QueryTypeEnum)params[0];
        String serverUrl = null;
        String inputString;
        String latitudeString;
        String longitudeString;
        String distanceString;
        String poiIdString;
        String isFavoriteString;
        String userIdString;
        String queryTypeString = "" + getQueryType(mQueryType);
        String tokenString = "";

        Log.d(TAG, "Connecting to a remote server");

        switch (mQueryType)
        {
            case QUERY_LIST:
                serverUrl = SERVER_URL_4_LIST_QUERY;
                mLocation = (Location)params[1];
                break;

            case QUERY_MAP:
                serverUrl = SERVER_URL_4_MAP_QUERY;
                mLocation = (Location)params[1];
                distance  = (float)params[2];
                break;

            case QUERY_POI:
                serverUrl = SERVER_URL_4_POI_QUERY;
                poiId    = (int)params[1];
                break;

            case QUERY_FAVORITE:
                serverUrl   = SERVER_URL_4_FAVORITES_QUERY;
                poiId       = (int)params[1];
                isFavorite  = (boolean)params[2];
                userId      = (int)params[3];
                tokenString = (String)params[4];
                break;

            case QUERY_USER_ID:
                serverUrl = SERVER_URL_4_QUERY_USER_ID;
                tokenString = (String)params[1];

                break;

            default:
                Log.e(TAG, "doInBackground num of params is: " + params.length + ", expected: " + expNParamsListQuery + " or " + expNParamsMapQuery );
                return null;
        }

        //TODO: add checker for null location ( GPS is off )

        if ( mQueryType == QueryTypeEnum.QUERY_LIST || mQueryType == QueryTypeEnum.QUERY_MAP ) {
            if (0 == mLocation.getLatitude() || 0 == mLocation.getLongitude()) {
                Log.e(TAG, "doInBackground: Error in lng-lat values: Latitude: " + mLocation.getLatitude() +
                        " Longitude: " + mLocation.getLongitude());
                return null;
            }
        }

        try
        {
            assert(serverUrl != null);
            URL url = new URL(serverUrl);

            switch (mQueryType) {
                case QUERY_LIST:
                    latitudeString  = "" + mLocation.getLatitude();
                    longitudeString = "" + mLocation.getLongitude();

                    // inputString - data to be sent to server (in this case -> latitude and longitude)
                    inputString = URLEncoder.encode("queryType", "UTF-8") + "=" + URLEncoder.encode(queryTypeString, "UTF-8") + "&" +
                                  URLEncoder.encode("latitude",  "UTF-8") + "=" + URLEncoder.encode(latitudeString,  "UTF-8") + "&" +
                                  URLEncoder.encode("longitude", "UTF-8") + "=" + URLEncoder.encode(longitudeString, "UTF-8");
                    break;

                case QUERY_MAP:
                    latitudeString  = "" + mLocation.getLatitude();
                    longitudeString = "" + mLocation.getLongitude();
                    distanceString  = "" + distance;

                    inputString = URLEncoder.encode("queryType",  "UTF-8") + "=" + URLEncoder.encode(queryTypeString, "UTF-8") + "&" +
                                  URLEncoder.encode("latitude",   "UTF-8") + "=" + URLEncoder.encode(latitudeString,  "UTF-8") + "&" +
                                  URLEncoder.encode("longitude",  "UTF-8") + "=" + URLEncoder.encode(longitudeString, "UTF-8") + "&" +
                                  URLEncoder.encode("distance",   "UTF-8") + "=" + URLEncoder.encode(distanceString,  "UTF-8");

                    break;

                case QUERY_POI:
                    poiIdString = "" + poiId;
                    inputString = URLEncoder.encode("queryType", "UTF-8") + "=" + URLEncoder.encode(queryTypeString,  "UTF-8") + "&" +
                                  URLEncoder.encode("poiId",     "UTF-8") + "=" + URLEncoder.encode(poiIdString,      "UTF-8");
                    break;



                case QUERY_FAVORITE:
                    poiIdString      = "" + poiId;
                    isFavoriteString = "" + isFavorite;
                    userIdString     = "" + userId;
                    inputString = URLEncoder.encode("userId",     "UTF-8") + "=" + URLEncoder.encode(userIdString,    "UTF-8") + "&" +
                                  URLEncoder.encode("poiId",      "UTF-8") + "=" + URLEncoder.encode(poiIdString,     "UTF-8") + "&" +
                                  URLEncoder.encode("token",      "UTF-8") + "=" + URLEncoder.encode(tokenString,     "UTF-8") + "&" +
                                  URLEncoder.encode("isFavorite", "UTF-8") + "=" + URLEncoder.encode(isFavoriteString,"UTF-8");
                    break;

                case QUERY_USER_ID:
                    inputString =  URLEncoder.encode("token","UTF-8") + "=" + URLEncoder.encode(tokenString, "UTF-8");

                    break;

                default:
                    inputString = "";
                    Log.e(TAG, "ERROR!! Invalid queryType: " + mQueryType);

            }

            Log.i(TAG, "DataImporter::doInBackground : Server input string: " + inputString);

            // open a connection and an output stream to the server.
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setDoOutput(true);
            urlCon.setRequestMethod("POST");
            urlCon.setFixedLengthStreamingMode(inputString.getBytes().length);
            OutputStreamWriter o_stream = new OutputStreamWriter(urlCon.getOutputStream());
            o_stream.write(inputString);
            o_stream.flush();

            if (mQueryType != QueryTypeEnum.QUERY_FAVORITE)
            {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));

                    StringBuilder responseString = new StringBuilder();
                    String line = null;


                    while ((line = reader.readLine()) != null)
                    {
                        if (mQueryType == mQueryType.QUERY_USER_ID)
                        {
                            responseString.append(line);
                        }
                        else
                        {
                            if (line.startsWith("["))
                            {
                                responseString.append(line);
                            }
                        }
                    }
                    reader.close();

                    if (mQueryType == mQueryType.QUERY_USER_ID)
                    {
                        // parse user ID from response string
                        mUserId = responseString.toString();
                    }
                    else
                    {
                        //configure gson (json parser)
                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gsonBuilder.setDateFormat("M/d/yy hh:mm a");
                        Gson gson = gsonBuilder.create();

                        //Read the server response and attempt to parse it as JSON
                        mPoiList = Arrays.asList(gson.fromJson(responseString.toString(), MainDbObjectData[].class));
                        Log.e(TAG, "Got POI list from remote server. list size is: " + mPoiList.size());
                        Log.e(TAG, "DataImporeter Execcute end");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to parse JSON due to: " + ex);
                    //failedLoadingPosts();
                    return 0;
                }
            }
        }
        catch(Exception ex)
        {
            Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
            //failedLoadingPosts();
            return 0;
        }
        return 1;
    }

    // onPostExecute is getting a value result that the method doInBackground returned so that it would be called
    protected void onPostExecute(Integer result)
    {
        Log.e(TAG, "DataImporeter post Execute start...");
        //call the callback function
//        ((PoiListFragment)mfatherObject).onDataImporterTaskCompleted(mPoiList);


        DataImporterListener listener = mListener.get();
        if (listener != null) {
            switch (mQueryType) {
                case QUERY_FAVORITE:
                    Log.e(TAG, "QUERY_FAVORITE finised, nothing to return");
                    break;

                case QUERY_LIST:
                case QUERY_MAP:
                case QUERY_POI:
                    if (mPoiList != null) {
                        Log.e(TAG, "calling onDataImporterTaskCompleted with mPoiList.size() = " + mPoiList.size());
                        listener.onDataImporterTaskCompleted((Object) mPoiList);
                    } else {
                        Log.e(TAG, "ERROR!! query falied! calling onDataImporterTaskFailed... ");
                        listener.onDataImporterTaskFailed(0);
                    }

                    break;

                case QUERY_USER_ID:
                    Log.e(TAG, "QUERY_USER_ID, returning with user ID: " + mUserId);
                    listener.onDataImporterTaskCompleted((Object) mUserId);

                    break;
            }
            Log.e(TAG, "DataImporeter post Execcute end");
        }
    }
    public int getQueryType(QueryTypeEnum queryType) {
        QueryTypeEnum tmp = queryType; // Or whatever
        return tmp.getValue();
    }
}


