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

    private Location mLocation;                       // input type for doInBackground
    private List<MainDbObjectData> mPoiList;          // output data list that goes to call back method    // IMPORTATNT!! : This was public for some reason
    private WeakReference<DataImporterListener> mListener;
//    private PoiListFragment mPoiListFragment;         // The father class type. Needed for the call back
//    private Object mfatherObject;
    private int mPoiId = 0;
    private MainDbObjectData mPoi = null;
    private QueryTypeEnum queryType;

    private static final String SERVER_URL_4_LIST_QUERY = "http://10.0.3.2/fooding/queryPreviewDbByLocation.php";       // IMPORTATNT!! : This was public for some reason  // Works with Genymotion
    private static final String SERVER_URL_4_MAP_QUERY  = "http://10.0.3.2/fooding/queryPreviewDbByLocation.php";       // IMPORTATNT!! : This was public for some reason  // Works with Genymotion
    private static final String SERVER_URL_4_POI_QUERY  = "http://10.0.3.2/fooding/queryPreviewDbByLocation.php";       // IMPORTATNT!! : This was public for some reason  // Works with Genymotion
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
        String serverUrl = null;
        queryType = (QueryTypeEnum)params[0];
        String inputString;
        String latitudeString;
        String longitudeString;
        String distanceString;
        String queryTypeString = "" + getQueryType(queryType);

        Log.d(TAG, "Connecting to a remote server");

        switch (queryType)
        {
            case QUERY_LIST:
                serverUrl = SERVER_URL_4_LIST_QUERY;
                mLocation = (Location)params[1];
                break;

            case QUERY_MAP:
                serverUrl = SERVER_URL_4_MAP_QUERY;
                mLocation = (Location)params[1];
                distance = (float)params[2];
                break;

            case QUERY_POI:
                serverUrl = SERVER_URL_4_POI_QUERY;
                mPoiId = (int)params[1];
                break;

            default:
                Log.e(TAG, "doInBackground num of params is: " + params.length + ", expected: " + expNParamsListQuery + " or " + expNParamsMapQuery );
                return null;
        }

        if ( queryType == QueryTypeEnum.QUERY_LIST || queryType == QueryTypeEnum.QUERY_MAP ) {
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

            switch (queryType) {
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

                    inputString = URLEncoder.encode("queryType",   "UTF-8") + "=" + URLEncoder.encode(queryTypeString, "UTF-8") + "&" +
                                  URLEncoder.encode("latitude",    "UTF-8") + "=" + URLEncoder.encode(latitudeString,  "UTF-8") + "&" +
                                  URLEncoder.encode("longitude",   "UTF-8") + "=" + URLEncoder.encode(longitudeString, "UTF-8") + "&" +
                                  URLEncoder.encode("distance",    "UTF-8") + "=" + URLEncoder.encode(distanceString,  "UTF-8");

                    break;

                case QUERY_POI:
                    String poiId = "" + mPoiId;
                    inputString = URLEncoder.encode("queryType", "UTF-8") + "=" + URLEncoder.encode(queryTypeString, "UTF-8") + "&" +
                                  URLEncoder.encode("poiId",     "UTF-8") + "=" + URLEncoder.encode(poiId,           "UTF-8");
                    break;

                default:
                    inputString = "";
                    Log.e(TAG, "ERROR!! Invalid queryType: " + queryType);

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

                //configure gson (json parser)
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setDateFormat("M/d/yy hh:mm a");
                Gson gson = gsonBuilder.create();

                //Read the server response and attempt to parse it as JSON
                mPoiList = Arrays.asList(gson.fromJson(responseString.toString(), MainDbObjectData[].class));
                Log.e(TAG, "Got POI list from remote server");
                Log.e(TAG, "DataImporeter Execcute end");
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Failed to parse JSON due to: " + ex);
                //failedLoadingPosts();
                return 0;
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
        if(listener != null)
        {
            if(mPoiList != null)
            {
                listener.onDataImporterTaskCompleted(mPoiList);
            }
            else
            {
                listener.onDataImporterTaskFailed(0);
            }
        }

        Log.e(TAG, "DataImporeter post Execcute end");
    }

    public int getQueryType(QueryTypeEnum queryType) {
        QueryTypeEnum tmp = queryType; // Or whatever
        return tmp.getValue();
    }
}


