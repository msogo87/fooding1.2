package com.robotemplates.cityguide.database.query;

import com.robotemplates.cityguide.database.dao.CategoryDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.model.CategoryModel;

import java.sql.SQLException;


public class CategoryCreateQuery extends Query
{
	private CategoryModel mCategory;


	public CategoryCreateQuery(CategoryModel category)
	{
		mCategory = category;
	}
	
	
	@Override
	public Data<Integer> processData() throws SQLException
	{
		Data<Integer> data = new Data<>();
		data.setDataObject(CategoryDAO.create(mCategory));
		return data;
	}
}
