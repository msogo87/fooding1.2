package com.robotemplates.cityguide.database.query;

import com.robotemplates.cityguide.database.dao.CategoryDAO;
import com.robotemplates.cityguide.database.data.Data;
import com.robotemplates.cityguide.database.model.CategoryModel;

import java.sql.SQLException;


public class CategoryUpdateQuery extends Query
{
	private CategoryModel mCategory;


	public CategoryUpdateQuery(CategoryModel category)
	{
		mCategory = category;
	}


	@Override
	public Data<Integer> processData() throws SQLException
	{
		Data<Integer> data = new Data<>();
		data.setDataObject(CategoryDAO.update(mCategory));
		return data;
	}
}
