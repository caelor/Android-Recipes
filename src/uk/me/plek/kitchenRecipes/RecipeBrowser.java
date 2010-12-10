package uk.me.plek.kitchenRecipes;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import uk.me.plek.kitchenRecipes.DatabaseHelper.DatabaseEventListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class RecipeBrowser extends Activity implements OnItemClickListener, OnItemLongClickListener, OnClickListener, FilterChooserCallback, DatabaseEventListener {
	private RecipeAdapter recipeAdapter;
	private FilterAdapter filterAdapter;
	private String recipeAdapterHeadingText;
	private String filterAdapterHeadingText;
	private SeparatedListAdapter compositeAdapter;
	private String relativeUri = "";
	private String baseUrl = "";
	private String authUser = "";
	private String authPass = "";
	private ProgressDialog dialog = null;
	private String cachedBaseUrl = null;
	private String cachedAuthUser = null;
	private String cachedAuthPass = null;
	private AlertDialog fieldSelectorDialog = null;
	private AlertDialog deleteFilterDialog = null;
	private ActiveFilter filterDeletionCandidate = null;
	private XMLRecipeDocument recipeDocument = new XMLRecipeDocument();
	private DatabaseHelper dbHelper;
	private Thread backgroundFetchThread = null;
	private CharSequence cachedMessage = "";
	
	static final int ContextMenu_ShareLink = 0;
	static final int ContextMenu_ShareCard = 1;
	static final int ContextMenu_OpenRecipe = 2;

	private Runnable doShowErrorToast = new Runnable() {
		@Override
		public void run() {
			Toast errorToast = Toast.makeText(RecipeBrowser.this, "Unable to get data from the server. Check your configuration, or try again later.", Toast.LENGTH_LONG);
			errorToast.show();
		}
	};

	private Runnable doRequest = new Runnable() {
		@Override
		public void run() {
			if (!makeServerRequest()) {
				Log.e(Global.TAG, "Unable to complete server request.");
				runOnUiThread(doShowErrorToast);
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recipes);

		// set up default headings
		recipeAdapterHeadingText = getString(R.string.recipes_heading);
		filterAdapterHeadingText = getString(R.string.active_filters_heading);

		// add the RecipeAdapter to the Recipe List
		this.recipeAdapter = new RecipeAdapter(this, R.layout.recipe_row, recipeDocument.getRecipes());
		this.filterAdapter = new FilterAdapter(this, R.layout.active_filter_row, recipeDocument.getFilters());

		this.compositeAdapter = new SeparatedListAdapter(this);
		this.compositeAdapter.addSection(filterAdapterHeadingText, filterAdapter);
		this.compositeAdapter.addSection(recipeAdapterHeadingText, recipeAdapter);

		ListView recipesList = (ListView)findViewById(R.id.RecipeList);
		recipesList.setAdapter(this.compositeAdapter);

		recipesList.setOnItemClickListener(this);
		/*recipesList.setOnItemLongClickListener(this);*/
		registerForContextMenu(recipesList);
	}

	@Override
	public void onResume() {
		super.onResume();

		dbHelper = new DatabaseHelper(this, this);
	}

	@Override
	public void onPause() {
		super.onPause();

		// be tidy and don't leave dialogs lying around.
		/* If the screen orientation changes, our background thread doing the fetch
		 * can stay running. This isn't a problem - we want the fetch to complete,
		 * but if it tries to dismiss its dialog at the end, then it won't be able
		 * to, because the view isn't attached to the window manager any more.
		 */
		dismissDialog.run();
		dbHelper.close();
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
		case R.id.menuGenericPrefs:
			i = new Intent(getApplicationContext(), RecipePreferences.class);
			startActivity(i);
			return true;
		case R.id.menuGenericCredits:
			i = new Intent(getApplicationContext(), ViewCredits.class);
			startActivity(i);
			return true;
		case R.id.menuBrowseRefresh:
			dbHelper.deletedOldServerResponses(0); // delete all cached responses

			Thread thread = new Thread(null, doRequest, "BackgroundRecipeFetch");
			thread.start();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	/* Server request for information */
	private boolean makeServerRequest() {
		boolean retVal = true;

		String absoluteUrl = this.baseUrl + this.relativeUri;

		dbHelper.deletedOldServerResponses(Global.REQUEST_CACHE_MAX_AGE);
		String cachedValue = dbHelper.getCachedServerResponse(absoluteUrl); 
		if (cachedValue != null) {
			try {
				cachedMessage = dbHelper.getCachedServerResponseTimestamp(this, absoluteUrl);
				parseResponse(cachedValue);
			} finally {
				runOnUiThread(dismissDialog);
			}

		}
		else {
			cachedMessage = "";

			runOnUiThread(showServerRequestDialog);

			try {

				Authenticator.setDefault(new BasicAuthenticator(this.authUser, this.authPass));

				HttpURLConnection c = (HttpURLConnection) new URL(absoluteUrl).openConnection();
				c.setUseCaches(false);
				c.connect();

				if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
					String response = XMLRecipeDocument.inputStreamToString(c.getInputStream());
					dbHelper.addServerResponse(absoluteUrl, response);
					parseResponse(response);
				}
				else {
					runOnUiThread(doShowErrorToast);
				}

			} catch (MalformedURLException e) {
				retVal = false;
				Log.e(Global.TAG, "Malformed URL exception: " + absoluteUrl);
				e.printStackTrace();

			} catch (IOException e) {
				retVal = false;
				Log.e(Global.TAG, "IOException loading recipes");
				e.printStackTrace();
			} finally {
				runOnUiThread(dismissDialog);
			}
		}

		return retVal;
	}

	/* Response parsing */
	private void parseResponse(String s) {
		try {
			recipeDocument.parseNewDocument(s);
		}
		finally {
			// make sure UI updates get run on the UI thread.
			runOnUiThread(syncUI);
		}
	}

	private Runnable syncUI = new Runnable() {
		@Override
		public void run() {
			recipeAdapter.notifyDataSetChanged();
			filterAdapter.notifyDataSetChanged();

			// create new composite adapter - it seems to be the only way we can update the headings
			compositeAdapter = new SeparatedListAdapter(RecipeBrowser.this);
			compositeAdapter.addSection(getString(R.string.active_filters_heading) + recipeDocument.getFilterItems(), filterAdapter);
			compositeAdapter.addSection(getString(R.string.recipes_heading) + recipeDocument.getRecipeItems() + cachedMessage, recipeAdapter);

			ListView recipesList = (ListView)findViewById(R.id.RecipeList);
			recipesList.setAdapter(compositeAdapter);

		}

	};

	private Runnable dismissDialog = new Runnable() {
		@Override
		public void run() {
			if (dialog != null) {
				try {
					dialog.dismiss();
				}
				finally {
					dialog = null;
				}
			}
		}

	};

	private Runnable showServerRequestDialog = new Runnable() {
		@Override
		public void run() {
			if (dialog != null) { Log.e(Global.TAG, "Attempting to create a new dialog when one is already active."); }
			dialog = ProgressDialog.show(RecipeBrowser.this, "Please wait...", "Retrieving data...", true);
		}
	};

	private CharSequence[] getAvailableFieldsForDialog() {
		ArrayList<AvailableField> availableFields = recipeDocument.getAvailableFields();
		CharSequence[] retval = new CharSequence[availableFields.size()];

		for (int foo = 0; foo < availableFields.size(); foo++) {
			retval[foo] = availableFields.get(foo).getName();
		}

		return retval;
	}

	/* List Item Click Listener */

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Object o = compositeAdapter.getItem(position);
		if (o instanceof BasicRecipe) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			if (prefs.getBoolean("longTapToOpen", false) == false) {
				BasicRecipe recipe = (BasicRecipe)o;
				Log.i(Global.TAG, "Basic Recipe clicked.");
				// this basically means we should fire off an intent to show that particular recipe.
				// broadcast an intent with a scheme of "recipe://"...
				Intent i = new Intent(Intent.ACTION_VIEW);
				String url = Global.httpToRecipeConvert(this.baseUrl + "/recipe/" + recipe.identifier);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
		}
		else if (o instanceof ActiveFilter) {
			ActiveFilter filter = (ActiveFilter)o;
			//Log.i(Global.TAG, "Active Filter clicked.");

			if (filter.isAddNewFilter()) {
				// need to add a new filter...
				// display a dialog with the available fields...
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Filter Type");
				builder.setItems(getAvailableFieldsForDialog(), this);
				fieldSelectorDialog = builder.create();
				fieldSelectorDialog.show();
			}
			else {
				// tapping this means the user may want to remove the filter.
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Delete Filter?");
				builder.setMessage("Are you sure you want to delete this filter?");
				builder.setCancelable(true);
				builder.setPositiveButton("Yes", this);
				builder.setNegativeButton("No", this);

				filterDeletionCandidate = filter;

				deleteFilterDialog = builder.create();
				deleteFilterDialog.show();
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
			long id) {

		boolean consumed = false;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		Object o = compositeAdapter.getItem(position);
		if ((o instanceof BasicRecipe) && (prefs.getBoolean("longTapToOpen", false))) {
			BasicRecipe recipe = (BasicRecipe)o;
			Log.i(Global.TAG, "Basic Recipe clicked.");
			// this basically means we should fire off an intent to show that particular recipe.
			// broadcast an intent with a scheme of "recipe://"...
			Intent i = new Intent(Intent.ACTION_VIEW);
			String url = Global.httpToRecipeConvert(this.baseUrl + "/recipe/" + recipe.identifier);
			i.setData(Uri.parse(url));
			startActivity(i);

			consumed = true;
		}
		return consumed;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.RecipeList) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			Object o = compositeAdapter.getItem(info.position);
			
			if (o instanceof BasicRecipe) {
				BasicRecipe recipe = (BasicRecipe)o;

				// populate the 
				menu.setHeaderTitle(recipe.title);
				menu.add(Menu.NONE, ContextMenu_ShareLink, Menu.NONE, "Share Link");
				menu.add(Menu.NONE, ContextMenu_ShareCard, Menu.NONE, "Share Recipe Card");
				
				if (prefs.getBoolean("longTapToOpen", false)) {
					menu.add(Menu.NONE, ContextMenu_OpenRecipe, Menu.NONE, "Open Recipe");
				}
			}			
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Object o = compositeAdapter.getItem(info.position);
		
		if (o instanceof BasicRecipe) {
			BasicRecipe recipe = (BasicRecipe)o;
			Intent i;
			
			switch (item.getItemId()) {
			case ContextMenu_ShareLink:
				Global.shareRecipeLink(recipe, this);
				return true;
			case ContextMenu_ShareCard:
				Global.shareRecipeCard(recipe, this);
				return true;
			case ContextMenu_OpenRecipe:
				i = new Intent(Intent.ACTION_VIEW);
				String url = recipe.requestUrl;
				i.setData(Uri.parse(url));
				startActivity(i);
				return true;
			default:
				return false;
		  }
	  }
	  
	  return false;
	}



	/* Dialog onClick implementation */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (dialog == this.fieldSelectorDialog) {
			// which will contain the position of the item that was clicked...
			// luckily this is also the index in availableFields...
			fieldSelectorDialog.dismiss();
			String url = recipeDocument.getAvailableFields().get(which).getUri();

			new FilterOptionsChooser(this, url, recipeDocument.getAvailableFields().get(which).getName(), this);
		}
		else if (dialog == this.deleteFilterDialog) {
			deleteFilterDialog.dismiss();
			if (which == AlertDialog.BUTTON_POSITIVE) {
				// we should delete it.
				this.relativeUri = filterDeletionCandidate.getDeletedUri();

				Thread thread = new Thread(null, doRequest, "BackgroundRecipeFetch");
				thread.start();

			}
			filterDeletionCandidate = null;
		}
		else {
			Log.w(Global.TAG, "Unhandled Dialog click callback.");
		}
	}

	@Override
	public void filterParameterChosen(FilterOptionsChooser caller,
			String destUrl) {
		// called by filteroptionschooser when the user has chosen a new filter.
		// we need to reload...
		this.relativeUri = destUrl;

		Thread thread = new Thread(null, doRequest, "BackgroundRecipeFetch");
		thread.start();

	}

	@Override
	public void databaseOpenedCallback() {
		// update the status bar.
		dbHelper.updateNotificationMessage(this);

		// the background fetching requires the db for caching.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		baseUrl = prefs.getString("recipesUrl", "");
		authUser = prefs.getString("authUsername", "");
		authPass = prefs.getString("authPassword", "");

		if ((cachedBaseUrl != baseUrl) ||
				(cachedAuthUser != authUser) ||
				(cachedAuthPass != authPass)) {

			cachedBaseUrl = baseUrl;
			cachedAuthUser = authUser;
			cachedAuthPass = authPass;

			backgroundFetchThread = new Thread(null, doRequest, "BackgroundRecipeFetch");
			backgroundFetchThread.start();
		}

	}
	
}