package com.robotemplates.cityguide.database.query;

import com.robotemplates.cityguide.database.dao.PoiDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.model.PoiModel;

import java.sql.SQLException;


public class PoiUpdateQuery extends Query
{
	private PoiModel mPoi;


	public PoiUpdateQuery(PoiModel poi)
	{
		mPoi = poi;
	}


	@Override
	public Data<Integer> processData() throws SQLException
	{
		Data<Integer> data = new Data<>();
		data.setDataObject(PoiDAO.update(mPoi));
		return data;
	}
}
