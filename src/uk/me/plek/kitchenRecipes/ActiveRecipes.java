package uk.me.plek.kitchenRecipes;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.acra.ErrorReporter;
import uk.me.plek.kitchenRecipes.DatabaseHelper.DatabaseEventListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

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
public class ActiveRecipes extends Activity implements DatabaseEventListener, OnItemClickListener {
	public static String ACTION_STATUS_CALLBACK = "me.uk.plek.kitchenRecipes.STATUSBAR_CALLBACK";
	private ActiveRecipeAdapter activeRecipes;
	private SeparatedListAdapter compositeAdapter;
	private String authUser;
	private String authPass;
	private ProgressDialog openingDbDialog;
	private DatabaseHelper dbConn;
	private ListView recipesList;
	private String currentRecipeUri; // a string uri describing the current recipe. Used by the display recipe routines.
	private FullRecipe currentRecipe;
	private String currentRecipeHTML;
	private ProgressDialog loadingRecipeDialog;
	private boolean showingRecipe = false;
	private long tempRowId;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.active_recipes);

		recipesList = (ListView)findViewById(R.id.ActiveRecipeList);
		ViewFlipper f = (ViewFlipper)findViewById(R.id.RecipeViewFlipper);
		f.setDisplayedChild(0);

		openingDbDialog = ProgressDialog.show(this, "Please wait...", "Loading data", true);
		dbConn = new DatabaseHelper(this, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		prefs.getString("recipesUrl", "");
		authUser = prefs.getString("authUsername", "");
		authPass = prefs.getString("authPassword", "");

	}


	/* Menu handling methods */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		if (this.showingRecipe) {
			inflater.inflate(R.menu.active_recipe_menu, menu);
		}
		else {
			inflater.inflate(R.menu.active_recipes_list_menu, menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.menuShowActiveRecipes:
			runOnUiThread(doShowActiveRecipes);
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
		recipesList.setOnItemClickListener(this);


		// we've now got an active DB connection, and have set up the listview.
		// we need to see what intent we were called by, and then possibly load and run
		// a recipe.
		Intent ourIntent = this.getIntent();

		if (ourIntent.getAction().equals(Intent.ACTION_VIEW)) {
			// we've been asked to view something.

			// we should now show a progress dialog.
			loadingRecipeDialog = ProgressDialog.show(this, "Please wait", "Loading Recipe...", true);

			// do we already have that recipe as an active recipe?
			boolean recipeOpen = dbConn.isRecipeActive(ourIntent.getDataString());

			if (!recipeOpen) {
				// we need to load the recipe information first...
				this.currentRecipeUri = ourIntent.getDataString();
				Thread t = new Thread(null, doDownloadRecipe, "backgroundRecipeDownload");
				t.start();
			}
			else {
				this.currentRecipeUri = ourIntent.getDataString();
				Thread t = new Thread(null, doGetRecipeFromDBBySourceUri, "backgroundRecipeLoading");
				t.start();
			}
		}
		else if (ourIntent.getAction().equals(ActiveRecipes.ACTION_STATUS_CALLBACK)) {
			// our callback from the status bar. We're already showing the active recipes, so
			// we should be fine.
		}
		else {
			Log.e(Global.TAG, "Got an intent with an unexpected action: " + ourIntent.getAction());
		}
	}

	private String getRecipeHTML(FullRecipe recipe) {
		RecipeTemplater templater = RecipeTemplater.templateBuilder("default"); // TODO: implement other styles
		return templater.generateStyledRecipe(recipe); 
	}

	Runnable doShowRecipe = new Runnable() {
		// intended to be run on the ui thread.

		@Override
		public void run() {
			WebView wv = (WebView)findViewById(R.id.recipeWebView);
			String mimetype = "text/html";
			String encoding = "UTF-8";
			wv.loadData(currentRecipeHTML, mimetype, encoding);

			ViewFlipper vf = (ViewFlipper)findViewById(R.id.RecipeViewFlipper);
			vf.setInAnimation(AnimationHelper.centreSpinViewInAnimation());
			vf.setOutAnimation(AnimationHelper.centreSpinViewOutAnimation());
			vf.setDisplayedChild(1);
			showingRecipe = true;

		}

	};

	Runnable doShowActiveRecipes = new Runnable() {
		// intended to be run on the ui thread.

		@Override
		public void run() {
			ViewFlipper vf = (ViewFlipper)findViewById(R.id.RecipeViewFlipper);
			vf.setInAnimation(AnimationHelper.centreSpinViewInAnimation());
			vf.setOutAnimation(AnimationHelper.centreSpinViewOutAnimation());
			vf.setDisplayedChild(0);
			showingRecipe = false;

		}

	};

	Runnable doGetRecipeFromDBByRowId = new Runnable() {

		@Override
		public void run() {
			currentRecipeUri = dbConn.getSourceUriByRowId(tempRowId);

			doGetRecipeFromDBBySourceUri.run();
		}

	};

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
							FullRecipe recipe = (FullRecipe)foo;

							currentRecipe = recipe;
							currentRecipeHTML = getRecipeHTML(recipe);
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

	Runnable doDownloadRecipe = new Runnable() {

		@Override
		public void run() {
			/* Server request for information */
			String absoluteUrl = currentRecipeUri.replace("recipe://", "http://");


			try {
				Authenticator.setDefault(new BasicAuthenticator(authUser, authPass));

				HttpURLConnection c = (HttpURLConnection) new URL(absoluteUrl).openConnection();
				c.setUseCaches(false);
				c.connect();

				if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
					XMLRecipeDocument doc = new XMLRecipeDocument(c.getInputStream());

					if (doc.isValid) {
						if (doc.getRecipes().size() == 1) {
							// we've found our fullrecipe...
							String xml = doc.getRawXML();
							BasicRecipe foo = doc.getRecipes().get(0);
							if (foo instanceof FullRecipe) {
								FullRecipe recipe = (FullRecipe)foo;

								dbConn.addActiveRecipe(recipe.getId(), 
										recipe.getTitle(), 
										xml, 
										currentRecipeUri, 
										null
								); // TODO: images aren't supported yet.

								currentRecipe = recipe;
								currentRecipeHTML = getRecipeHTML(recipe);
								runOnUiThread(doShowRecipe);
							}
							else {
								runOnUiThread(doShowLoadRecipeErrorToast);
								Exception e = new Exception("Exactly 1 recipe found, but it's not a full recipe: " + absoluteUrl);
								ErrorReporter.getInstance().handleSilentException(e); // DEBUG - for PHP script
							}
						}
						else {
							// it's either no recipes, or more than 1. Either way, we're not happy with it.
							runOnUiThread(doShowLoadRecipeErrorToast);
							Exception e = new Exception("Invalid number of recipes: " + doc.getRecipes().size() + " from " + absoluteUrl);
							ErrorReporter.getInstance().handleSilentException(e); // DEBUG - for PHP script
						}

					}
					else {
						runOnUiThread(doShowLoadRecipeErrorToast);
					}

				}
				else {
					runOnUiThread(doShowLoadRecipeErrorToast);
				}

			} catch (MalformedURLException e) {
				Log.e(Global.TAG, "Malformed URL exception: " + absoluteUrl);
				e.printStackTrace();

			} catch (IOException e) {
				Log.e(Global.TAG, "IOException loading recipes");
				e.printStackTrace();

			} finally {
				runOnUiThread(dismissLoadingDialog);
			}

		}
	};

	Runnable doShowLoadRecipeErrorToast = new Runnable() {

		@Override
		public void run() {
		}
	};

	Runnable dismissLoadingDialog = new Runnable() {

		@Override
		public void run() {
			if (loadingRecipeDialog != null) {
				loadingRecipeDialog.dismiss();
				loadingRecipeDialog = null;
			}
		}
	};

	@Override
	public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
		this.tempRowId = this.compositeAdapter.getItemId(position);
		Thread t = new Thread(null, doGetRecipeFromDBByRowId, "backgroundRecipeLoading");
		t.start();
	}

}
