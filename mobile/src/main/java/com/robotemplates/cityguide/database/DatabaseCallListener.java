package com.robotemplates.cityguide.database;

import com.robotemplates.cityguide.database.data.Data;


public interface DatabaseCallListener
{
	void onDatabaseCallRespond(DatabaseCallTask task, Data<?> data);
	void onDatabaseCallFail(DatabaseCallTask task, Exception exception);
}
