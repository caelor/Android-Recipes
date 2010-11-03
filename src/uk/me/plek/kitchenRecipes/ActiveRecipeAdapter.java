package uk.me.plek.kitchenRecipes;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

public class ActiveRecipeAdapter extends SimpleCursorAdapter {

	public ActiveRecipeAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
	}

}
