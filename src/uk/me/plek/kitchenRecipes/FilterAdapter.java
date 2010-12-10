package uk.me.plek.kitchenRecipes;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FilterAdapter extends ArrayAdapter<ActiveFilter> {

	protected ArrayList<ActiveFilter> items;
	private final Context _context;

	public FilterAdapter(Context context, int textViewResourceId, ArrayList<ActiveFilter> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		this._context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ActiveFilter f = items.get(position);
		View v = convertView;

		if (f != null) {
			boolean needsInflation = false;
			if (v == null) { 
				needsInflation = true; 
			}
			else {
				if (!v.getTag().equals(f.getClass())) {
					needsInflation = true;
				}
			}

			if (needsInflation) {
				LayoutInflater vi = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				if (f.isAddNewFilter()) {
					v = vi.inflate(R.layout.add_filter_row, null);
				}
				else {
					v = vi.inflate(R.layout.active_filter_row, null);
				}
				v.setTag(f.getClass());
			}

			if (!f.isAddNewFilter()) {
				TextView desc = (TextView) v.findViewById(R.id.ActiveFilterDesc);
				//TextView title = (TextView) v.findViewById(R.id.ActiveFilterTitle);
				TextView title = null;
				//ImageView icon = (ImageView) v.findViewById(R.id.ActiveFilterIcon);

				if (desc != null) {
					String fld = f.getField();
					if (fld.equals("category")) { desc.setText("In category '" + f.getValue() + "'..."); }
					else if (fld.equals("cuisine")) { desc.setText("Cuisine is '" + f.getValue() + "'..."); }
					else if (fld.equals("ingredient")) { desc.setText("Recipe contains '" + f.getValue() + "'..."); }
					else if (fld.equals("servingsmin")) { desc.setText("Serves at least " + f.getValue() + "..."); }
					else if (fld.equals("servingsmax")) { desc.setText("Serves at most " + f.getValue() + "..."); }
					else if (fld.equals("servings")) { desc.setText("Serves exactly " + f.getValue() + "..."); }
					else if (fld.equals("servingunit")) { desc.setText("Type of serving is '" + f.getValue() + "'..."); }
					else if (fld.equals("ratingmin")) { desc.setText("Rating at least " + f.getValue() + "..."); }
					else if (fld.equals("ratingmax")) { desc.setText("Rating at most " + f.getValue() + "..."); }
					else if (fld.equals("rating")) { desc.setText("Rating exactly " + f.getValue() + "..."); }
					else if (fld.equals("preptime")) { desc.setText("Prep Time exactly " + f.getValue() + "s..."); }
					else if (fld.equals("preptimemax")) { desc.setText("Prep Time at most " + f.getValue() + "s..."); }
					else if (fld.equals("cooktime")) { desc.setText("Cook Time exactly " + f.getValue() + "s..."); }
					else if (fld.equals("cooktimemax")) { desc.setText("Cook Time at most " + f.getValue() + "s..."); }
					else if (fld.equals("source")) { desc.setText("Source is '" + f.getValue() + "'"); }
					else if (fld.equals("recipe")) { desc.setText("Recipe identifier is '" + f.getValue() + "'..."); }
					else if (fld.equals("sort")) {
						if (f.getValue().equals("rating")) { desc.setText("Sorted with highest rated first."); }
						else if (f.getValue().equals("title")) { desc.setText("Sorted alphabetically, A-Z."); }
						else if (f.getValue().equals("modtime")) { desc.setText("Sorted by age, oldest first."); }
						else if (f.getValue().equals("modtimedesc")) { desc.setText("Sorted by age, newest first"); }
					}
				}
				if (title != null) {
					String fld = f.getField();
					if (fld.equals("category")) { title.setText("Category"); }
					else if (fld.equals("cuisine")) { title.setText("Cuisine"); }
					else if (fld.equals("ingredient")) { title.setText("Ingredient"); }
					else if (fld.equals("servingsmin")) { title.setText("Servings (min)"); }
					else if (fld.equals("servingsmax")) { title.setText("Servings (max)"); }
					else if (fld.equals("servings")) { title.setText("Servings (exact)"); }
					else if (fld.equals("servingunit")) { title.setText("Serving Type"); }
					else if (fld.equals("ratingmin")) { title.setText("Rating (min)"); }
					else if (fld.equals("ratingmax")) { title.setText("Rating (max)"); }
					else if (fld.equals("rating")) { title.setText("Rating (exact)"); }
					else if (fld.equals("preptime")) { title.setText("Prep Time (exact)"); }
					else if (fld.equals("preptimemax")) { title.setText("Prep Time (max)"); }
					else if (fld.equals("cooktime")) { title.setText("Cook Time (exact)"); }
					else if (fld.equals("cooktimemax")) { title.setText("Cook Time (max)"); }
					else if (fld.equals("source")) { title.setText("Source"); }
					else if (fld.equals("recipe")) { title.setText("Specific Recipe"); }
					else if (fld.equals("recipe")) { title.setText("Sort"); }
				}
				/*if (icon != null) {
					String fld = f.getField();
					if (fld.equals("category")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_category)); }
					else if (fld.equals("cuisine")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_cuisine)); }
					else if (fld.equals("ingredient")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_ingredient)); }
					else if (fld.equals("servingsmin")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_servings)); }
					else if (fld.equals("servingsmax")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_servings)); }
					else if (fld.equals("servings")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_servings)); }
					else if (fld.equals("servingunit")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_servingunit)); }
					else if (fld.equals("ratingmin")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_rating)); }
					else if (fld.equals("ratingmax")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_rating)); }
					else if (fld.equals("rating")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_rating)); }
					else if (fld.equals("preptime")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_preptime)); }
					else if (fld.equals("preptimemax")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_preptime)); }
					else if (fld.equals("cooktime")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_cooktime)); }
					else if (fld.equals("cooktimemax")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_cooktime)); }
					else if (fld.equals("source")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_source)); }
					else if (fld.equals("recipe")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_recipe)); }
					else if (fld.equals("sort")) { icon.setImageDrawable(v.getResources().getDrawable(R.drawable.icon_sort)); }
				}*/
			}
		}
		return v;
	}
	
	@Override
	public ActiveFilter getItem(int position) {
		return items.get(position);
	}
}
