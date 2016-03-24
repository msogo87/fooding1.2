package com.robotemplates.cityguide.database.query;

import com.robotemplates.cityguide.database.dao.PoiDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.model.PoiModel;

import java.sql.SQLException;


public class PoiReadQuery extends Query
{
	private long mId;


	public PoiReadQuery(long id)
	{
		mId = id;
	}


	@Override
	public Data<PoiModel> processData() throws SQLException
	{
		Data<PoiModel> data = new Data<>();
		data.setDataObject(PoiDAO.read(mId));
		return data;
	}
}
