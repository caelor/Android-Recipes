package uk.me.plek.kitchenRecipes;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class ActiveRecipeAdapter extends SimpleCursorAdapter implements ViewBinder {
	private final Context parent;

	public ActiveRecipeAdapter(Context context, Cursor c) {
		super(
				context, 
				R.layout.active_recipe_row, 
				c, 
				new String[] { 
						DatabaseHelper.RECIPE_TITLE,
						DatabaseHelper.RECIPE_STARTTIME,
						DatabaseHelper.RECIPE_URI
				}, 
				new int[] { 
						R.id.ActiveRecipeTitle,
						R.id.ActiveRecipeStartTime
				});
		
		this.parent = context;
		this.setViewBinder(this);
	}

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		// TODO Auto-generated method stub
		if (DatabaseHelper.RECIPE_STARTTIME.equals(cursor.getColumnName(columnIndex))) {
			if (view instanceof TextView) {
				TextView tv = (TextView)view;
				long millis = cursor.getLong(columnIndex);
				CharSequence startTime = DatabaseHelper.decodeStartDate(this.parent, millis);
				
				tv.setText(startTime);
				return true;
			}
			else {
				Log.e(Global.TAG, "Found a column for Recipe Start time, but it wasn't bound to a TextView");
				return false;
			}
		}
		else {
			return false;
		}
	}
}
