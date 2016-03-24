package com.robotemplates.cityguide.database.query;

import com.robotemplates.cityguide.database.dao.PoiDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.model.PoiModel;

import java.sql.SQLException;
import java.util.List;


public class PoiSearchQuery extends Query
{
	private String mQuery;
	private long mSkip = -1L;
	private long mTake = -1L;


	public PoiSearchQuery(String query)
	{
		mQuery = query;
	}


	public PoiSearchQuery(String query, long skip, long take)
	{
		mQuery = query;
		mSkip = skip;
		mTake = take;
	}


	@Override
	public Data<List<PoiModel>> processData() throws SQLException
	{
		Data<List<PoiModel>> data = new Data<>();
		data.setDataObject(PoiDAO.search(mQuery, mSkip, mTake));
		return data;
	}
}
