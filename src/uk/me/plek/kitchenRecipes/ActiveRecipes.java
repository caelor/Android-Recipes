package uk.me.plek.kitchenRecipes;

import uk.me.plek.kitchenRecipes.DatabaseHelper.DatabaseEventListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

/**
 * A view to display the recipes currently active.
 * 
 * Also provides the resolution for the "recipe://" scheme, so that if the same recipe is
 * opened twice, it can give the use the option of opening a new instance, or picking
 * from the existing instance(s) of the recipe
 * 
 * @author Andy Boff
 *
 */
public class ActiveRecipes extends Activity implements DatabaseEventListener {
	private ActiveRecipeAdapter activeRecipes;
	private SeparatedListAdapter compositeAdapter;
	private String baseUrl;
	private String authUser;
	private String authPass;
	private ProgressDialog openingDbDialog;
	private DatabaseHelper dbConn;
	private ListView recipesList;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.active_recipes);
		
		recipesList = (ListView)findViewById(R.id.ActiveRecipeList);

		openingDbDialog = ProgressDialog.show(this, "Please wait...", "Loading data", true);
		dbConn = new DatabaseHelper(this, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		baseUrl = prefs.getString("recipesUrl", "");
		authUser = prefs.getString("authUsername", "");
		authPass = prefs.getString("authPassword", "");

	}

	/* Menu handling methods */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.generic_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.menuPrefs:
			i = new Intent(getApplicationContext(), RecipePreferences.class);
			startActivity(i);
			return true;
		case R.id.menuCredits:
			i = new Intent(getApplicationContext(), ViewCredits.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void databaseOpenedCallback() {
		// called by databasehelper when the database has been opened.
		if (openingDbDialog != null) {
			openingDbDialog.dismiss();
			openingDbDialog = null;
		}
		
		// now get the cursor for the listview
		Cursor c = dbConn.getActiveRecipesCursor();
		String[] from = new String[] { 
				DatabaseHelper.RECIPE_TITLE,
				DatabaseHelper.RECIPE_STARTTIME
		};
		
		int[] to = new int[] { 
				R.id.ActiveRecipeTitle,
				R.id.ActiveRecipeStartTime
		};
		
		// add the RecipeAdapter to the Recipe List
		this.activeRecipes = new ActiveRecipeAdapter(this, R.layout.active_recipe_row, c, from, to);

		// set up the composite list adapter (so we get a section heading)
		this.compositeAdapter = new SeparatedListAdapter(this);
		this.compositeAdapter.addSection("Active Recipes", activeRecipes);

		// and set the adapter accordingly
		this.recipesList.setAdapter(this.compositeAdapter);

		//activeRecipes.setOnItemClickListener(this);
		//activeRecipes.setOnItemLongClickListener(this);
		
		
		// we've now got an active DB connection, and have set up the listview.
		// we need to see what intent we were called by, and then possibly load and run
		// a recipe.
	}
}
