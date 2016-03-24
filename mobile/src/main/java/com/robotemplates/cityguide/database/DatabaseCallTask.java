package com.robotemplates.cityguide.database;

import android.os.AsyncTask;

import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.query.Query;
import com.robotemplates.cityguide.utility.Logcat;

import java.lang.ref.WeakReference;


public class DatabaseCallTask extends AsyncTask<Void, Void, Data<?>>
{
	private Query mQuery;
	private WeakReference<DatabaseCallListener> mListener;
	private Exception mException = null;
	
	
	public DatabaseCallTask(Query query, DatabaseCallListener listener)
	{
		mQuery = query;
		setListener(listener);
	}
	
	
	@Override
	protected Data<?> doInBackground(Void... params)
	{
		try
		{
			Data<?> data = mQuery.processData();
			
			if(isCancelled()) return null;
			else return data;
		}
		catch(Exception e)
		{
			mException = e;
			e.printStackTrace();
			return null;
		}
	}
	
	
	@Override
	protected void onPostExecute(Data<?> data)
	{
		if(isCancelled()) return;
		
		DatabaseCallListener listener = mListener.get();
		if(listener != null)
		{
			if(data != null)
			{
				listener.onDatabaseCallRespond(this, data);
			}
			else
			{
				listener.onDatabaseCallFail(this, mException);
			}
		}
	}
	
	
	@Override
	protected void onCancelled()
	{
		Logcat.d("DatabaseCallTask.onCancelled()");
	}


	public Query getQuery()
	{
		return mQuery;
	}


	public void setListener(DatabaseCallListener listener)
	{
		mListener = new WeakReference<>(listener);
	}
}
