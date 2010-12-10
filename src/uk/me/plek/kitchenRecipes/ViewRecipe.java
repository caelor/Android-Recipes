package uk.me.plek.kitchenRecipes;


import org.acra.ErrorReporter;

import uk.me.plek.kitchenRecipes.DatabaseHelper.DatabaseEventListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class ViewRecipe extends Activity implements DatabaseEventListener {
	private ProgressDialog openingDbDialog;
	private DatabaseHelper dbConn;
	private String currentRecipeUri; // a string uri describing the current recipe. Used by the display recipe routines.
	private String currentRecipeHTML;
	private long fetchRowId;
	private String recipeTemplate;
	private boolean isLandscape;
	private LoadRecipeWebViewClient webViewClient;
	private FullRecipe recipe;

	private class LoadRecipeWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// fire off the intent...
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
			
			return true;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_recipe);

		webViewClient = new LoadRecipeWebViewClient();

		openingDbDialog = ProgressDialog.show(this, "Please wait...", "Loading data", true);
		dbConn = new DatabaseHelper(this, this);

		// find out if we're portrait or landscape
		isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
	}

	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		prefs.getString("recipesUrl", "");
		recipeTemplate = prefs.getString("recipeTemplate", "default");

	}

	/* Menu handling methods */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.active_recipe_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuShowActiveRecipes:
			//i = new Intent(getApplicationContext(), ActiveRecipes.class);
			//startActivity(i);
			return true;
		/*case R.id.menuActiveRecipeCredits:
			i = new Intent(getApplicationContext(), ViewCredits.class);
			startActivity(i);
			return true;*/
		case R.id.menuEndRecipe:
			Thread t = new Thread(null, doCancelCurrentRecipe, "backgroundRecipeDelete");
			t.start();
			return true;
		case R.id.menuShareLink:
			Global.shareRecipeLink(recipe, this);
			return true;
		case R.id.menuShareCard:
			Global.shareRecipeCard(recipe, this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void databaseOpenedCallback() {
		// called by databasehelper when the database has been opened.

		dbConn.updateNotificationMessage(this);

		// we've now got an active DB connection, and have set up the listview.
		// we need to see what intent we were called by, and then possibly load and run
		// a recipe.
		Intent ourIntent = this.getIntent();

		// we should now show a progress dialog.
		//loadingRecipeDialog = ProgressDialog.show(this, "Please wait", "Loading Recipe...", true);

		// do we already have that recipe as an active recipe?
		boolean recipeOpen = dbConn.isRecipeActive(ourIntent.getDataString());

		if (!recipeOpen) {
			this.finish(); // we should bounce straight back. We can't display a non-open recipe.
		}
		else {
			this.currentRecipeUri = ourIntent.getDataString();
			Thread t = new Thread(null, doGetRecipeFromDBBySourceUri, "backgroundRecipeLoading");
			t.start();
		}
	}

	Runnable doGetRecipeFromDBBySourceUri = new Runnable() {

		@Override
		public void run() {
			try {
				String xml = dbConn.getXMLForRecipeBySourceUri(currentRecipeUri);
				XMLRecipeDocument doc = new XMLRecipeDocument(xml);

				if (doc.isValid) {
					if (doc.getRecipes().size() == 1) {
						// we've found our fullrecipe...
						BasicRecipe foo = doc.getRecipes().get(0);
						if (foo instanceof FullRecipe) {
							recipe = (FullRecipe)foo;

							RecipeTemplater templater = RecipeTemplater.templateBuilder(recipeTemplate);
							currentRecipeHTML = templater.generateStyledRecipe(recipe, isLandscape);
							runOnUiThread(doShowRecipe);
						}
						else {
							runOnUiThread(doShowLoadRecipeErrorToast);
							Exception e = new Exception("Exactly 1 recipe found, but it's not a full recipe. Recipe is in DB.");
							ErrorReporter.getInstance().handleSilentException(e); // DEBUG - for PHP script
						}
					}
					else {
						// it's either no recipes, or more than 1. Either way, we're not happy with it.
						runOnUiThread(doShowLoadRecipeErrorToast);
						Exception e = new Exception("Invalid number of recipes: " + doc.getRecipes().size() + " in cached DB copy. ");
						ErrorReporter.getInstance().handleSilentException(e); // DEBUG - for PHP script
					}

				}
				else {
					runOnUiThread(doShowLoadRecipeErrorToast);
				}
			} finally {
				runOnUiThread(dismissLoadingDialog);
			}


		}

	};

	Runnable doGetRecipeFromDBByRowId = new Runnable() {

		@Override
		public void run() {
			currentRecipeUri = dbConn.getSourceUriByRowId(fetchRowId);

			doGetRecipeFromDBBySourceUri.run();
		}

	};

	Runnable doShowLoadRecipeErrorToast = new Runnable() {

		@Override
		public void run() {
			Toast errorToast = Toast.makeText(ViewRecipe.this, "There was an error loading the recipe. Check your configuration, or try again later.", Toast.LENGTH_LONG);
			errorToast.show();
		}
	};

	Runnable dismissLoadingDialog = new Runnable() {

		@Override
		public void run() {
			//if (loadingRecipeDialog != null) {
			//	loadingRecipeDialog.dismiss();
			//	loadingRecipeDialog = null;
			//}
			if (openingDbDialog != null) {
				try {
					openingDbDialog.dismiss();
				}
				finally {
					openingDbDialog = null;
				}
			}
		}
	};

	Runnable doShowRecipe = new Runnable() {

		@Override
		public void run() {
			WebView webview = (WebView)ViewRecipe.this.findViewById(R.id.recipeWebView);
			webview.setWebViewClient(webViewClient);

			String mimetype = "text/html";
			String encoding = "UTF-8";

			webview.loadDataWithBaseURL(
					"file:///android_asset/templates/" + recipeTemplate + "/",
					currentRecipeHTML, 
					mimetype, 
					encoding,
					null);
		}
	};

	Runnable doCancelCurrentRecipe = new Runnable() {

		@Override
		public void run() {
			dbConn.deleteRecipeByUri(currentRecipeUri);
			ViewRecipe.this.finish();
		}
	};

}
