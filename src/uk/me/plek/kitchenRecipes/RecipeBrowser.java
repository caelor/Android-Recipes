package uk.me.plek.kitchenRecipes;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class RecipeBrowser extends Activity implements OnItemClickListener, /*OnItemLongClickListener,*/ OnClickListener, FilterChooserCallback {
	private ArrayList<BasicRecipe> recipes = new ArrayList<BasicRecipe>();
	private ArrayList<ActiveFilter> filters = new ArrayList<ActiveFilter>();
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

	private Runnable doShowErrorToast = new Runnable() {
		@Override
		public void run() {
			Toast errorToast = Toast.makeText(RecipeBrowser.this, "Unable to get data from the server. Please try again later.", Toast.LENGTH_LONG);
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

	private ArrayList<AvailableField> availableFields = new ArrayList<AvailableField>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recipes);

		// set up default headings
		recipeAdapterHeadingText = getString(R.string.recipes_heading);
		filterAdapterHeadingText = getString(R.string.active_filters_heading);

		// add the RecipeAdapter to the Recipe List
		this.recipeAdapter = new RecipeAdapter(this, R.layout.recipe_row, recipes);
		this.filterAdapter = new FilterAdapter(this, R.layout.active_filter_row, filters);

		this.compositeAdapter = new SeparatedListAdapter(this);
		this.compositeAdapter.addSection(filterAdapterHeadingText, filterAdapter);
		this.compositeAdapter.addSection(recipeAdapterHeadingText, recipeAdapter);

		ListView recipesList = (ListView)findViewById(R.id.RecipeList);
		recipesList.setAdapter(this.compositeAdapter);

		recipesList.setOnItemClickListener(this);
		/*recipesList.setOnItemLongClickListener(this); -- enable for long click listening */
	}

	@Override
	public void onResume() {
		super.onResume();

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

			if (dialog != null) { Log.e(Global.TAG, "Attempting to create a new dialog when one is already active."); }
			dialog = ProgressDialog.show(RecipeBrowser.this, "Please wait...", "Retrieving data...", true);

			Thread thread = new Thread(null, doRequest, "BackgroundRecipeFetch");
			thread.start();
		}
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


	/* Server request for information */
	private boolean makeServerRequest() {
		boolean retVal = true;

		String absoluteUrl = this.baseUrl + this.relativeUri;

		try {

			Authenticator.setDefault(new BasicAuthenticator(this.authUser, this.authPass));

			HttpURLConnection c = (HttpURLConnection) new URL(absoluteUrl).openConnection();
			c.setUseCaches(false);
			c.connect();

			if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
				parseResponse(c.getInputStream());
			}
			else {
				new AlertDialog.Builder(this)
					.setMessage("Unable to contact the server. Please try again later.")
					.setPositiveButton("OK", new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							RecipeBrowser.this.finish();
						}
					})
					.show();
			}

		} catch (MalformedURLException e) {
			retVal = false;
			Log.e(Global.TAG, "Malformed URL exception: " + absoluteUrl);
			e.printStackTrace();

		} catch (IOException e) {
			retVal = false;
			Log.e(Global.TAG, "IOException loading recipes");
			e.printStackTrace();

		} catch (ParserConfigurationException e) {
			retVal = false;
			e.printStackTrace();

		} catch (SAXException e) {
			retVal = false;
			e.printStackTrace();
		} finally {
			runOnUiThread(dismissDialog);
		}

		return retVal;
	}

	/* Response parsing */
	private void parseResponse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = null;

		try {
			doc = db.parse(is);
		} catch (Exception e1) {
			Log.e(Global.TAG, "Error parsing response from server. Request URL was " + this.relativeUri);
		}

		if (doc != null) {
			Element root = doc.getDocumentElement();

			/* the documents _always_ have the following features (even if they're empty):
			 *   - /recipeDB/filter          (unimplemented here)
			 *   - /recipeDB/availableFields (unimplemented here)
			 *   - /recipeDB/recipes         (unimplemented here)
			 */

			NodeList topNodes = root.getChildNodes();
			for (int foo = 0; foo < topNodes.getLength(); foo++) {
				Node n = topNodes.item(foo);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element)n;
					String tagName = e.getTagName();

					if (tagName.equals("filter")) {
						// handle existing filter conditions
						this.filters.clear();
						this.filters.add(new ActiveFilter()); // always have "add new filter" as the first item.

						// now populate the active filters...
						NodeList filterNodes = e.getChildNodes();
						for (int fid = 0; fid < filterNodes.getLength(); fid++) {
							Node filterNode = filterNodes.item(fid);
							if (filterNode.getNodeType() != Node.ELEMENT_NODE) {
								Log.w(Global.TAG, "Non-element node found within filter element: " + filterNode.toString());
							}
							else {
								Element filterElement = (Element)filterNode;
								if (filterElement.getTagName().equals("filterItem")) {
									String field = filterElement.getAttribute("field");
									String value = filterElement.getAttribute("value");
									String deletedUri = filterElement.getAttribute("deletedRelativeUri");
									ActiveFilter f = new ActiveFilter(field, value, deletedUri);
									this.filters.add(f);
								}
								else {
									Log.w(Global.TAG, "Non-filterItem element found within filter definition: " + filterElement.getTagName());
								}
							}
						}
						
						String items = Integer.toString(this.filters.size() - 1);
						this.filterAdapterHeadingText = getString(R.string.active_filters_heading) + " (" + items + ")";
					}
					else if (tagName.equals("availableFields")) {
						// handle available fields
						this.availableFields.clear();

						NodeList fieldNodes = e.getChildNodes();

						for (int fid = 0; fid < fieldNodes.getLength(); fid++) {
							Node fieldNode = fieldNodes.item(fid);
							if (fieldNode.getNodeType() != Node.ELEMENT_NODE) {
								Log.w(Global.TAG, "Non-element node found within AvailableFields element: " + fieldNode.toString());
							}
							else {
								Element fieldElement = (Element)fieldNode;
								if (fieldElement.getTagName().equals("field")) {
									// we have a field tag.
									String name = fieldElement.getAttribute("name");
									String uri = fieldElement.getAttribute("relativeuri");
									String type = fieldElement.getAttribute("type");
									AvailableField af = new AvailableField(name, uri, type);
									availableFields.add(af);
								}
								else {
									Log.w(Global.TAG, "Non-field element found within AvailableFields element: " + fieldElement.getTagName());
								}
							}
						}
					}
					else if (tagName.equals("recipes")) {
						// handle returned recipes
						String matches = e.getAttribute("matched");
						this.recipeAdapterHeadingText = getString(R.string.recipes_heading) + " (" + matches + ")";
						// the only allowed subtags of this are "recipe" tags...
						NodeList recipeNodes = e.getChildNodes();
						this.recipes.clear();
						for (int rid = 0; rid < recipeNodes.getLength(); rid++) {
							Node recipeNode = recipeNodes.item(rid);
							if (recipeNode.getNodeType() != Node.ELEMENT_NODE) {
								Log.w(Global.TAG, "Non-element node found within Recipes element: " + recipeNode.toString());
							}
							else {
								Element recipeElement = (Element)recipeNode;
								if (recipeElement.getTagName().equals("recipe")) {
									this.recipes.add(createRecipeFromElement(recipeElement));
								}
								else {
									Log.w(Global.TAG, "Non-recipe element found within Recipes element: " + recipeElement.getTagName());
								}
							}
						}
					}
					else {
						Log.w(Global.TAG, "Unexpected tag found in XML, Level 1: " + tagName);
					}
				}
				else {
					Log.w(Global.TAG, "Unexpected Node found in XML Level 1: " + n.toString());
				}
			}

		}

		// make sure UI updates get run on the UI thread.
		runOnUiThread(syncUI);
	}

	private BasicRecipe createRecipeFromElement(Element recipeElement) {
		// a recipe element always has a "type" attribute...
		String type = recipeElement.getAttribute("type");
		if (type.equals("full")) {
			return new FullRecipe(recipeElement);
		}
		else {
			return new BasicRecipe(recipeElement);
		}
	}

	private Runnable syncUI = new Runnable() {
		@Override
		public void run() {
			recipeAdapter.notifyDataSetChanged();
			filterAdapter.notifyDataSetChanged();

			// create new composite adapter - it seems to be the only way we can update the headings
			compositeAdapter = new SeparatedListAdapter(RecipeBrowser.this);
			compositeAdapter.addSection(filterAdapterHeadingText, filterAdapter);
			compositeAdapter.addSection(recipeAdapterHeadingText, recipeAdapter);

			ListView recipesList = (ListView)findViewById(R.id.RecipeList);
			recipesList.setAdapter(compositeAdapter);

		}

	};

	private Runnable dismissDialog = new Runnable() {
		@Override
		public void run() {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
		}

	};

	private CharSequence[] getAvailableFieldsForDialog() {
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
			BasicRecipe recipe = (BasicRecipe)o;
			Log.i(Global.TAG, "Basic Recipe clicked.");
			// this basically means we should fire off an intent to show that particular recipe.
			// broadcast an intent with a scheme of "recipe://"...
			Intent i = new Intent(Intent.ACTION_VIEW);
			String url = this.baseUrl + this.relativeUri + "recipe/" + recipe.identifier;
			url = url.replace("http://", "recipe://");
			i.setData(Uri.parse(url));
			startActivity(i);
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

	/*@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
			long id) {

		boolean consumed = false;

		Object o = compositeAdapter.getItem(position);
		if (o instanceof BasicRecipe) {
			BasicRecipe recipe = (BasicRecipe)o;
			Log.i(Global.TAG, "Basic Recipe long clicked.");
		}
		else if (o instanceof ActiveFilter) {
			ActiveFilter filter = (ActiveFilter)o;
			Log.i(Global.TAG, "Active Filter long clicked.");
		}
		return consumed;
	}*/

	/* Dialog onClick implementation */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (dialog == this.fieldSelectorDialog) {
			// which will contain the position of the item that was clicked...
			// luckily this is also the index in availableFields...
			fieldSelectorDialog.dismiss();
			String url = availableFields.get(which).getUri();

			new FilterOptionsChooser(this, url, availableFields.get(which).getName(), this);
		}
		else if (dialog == this.deleteFilterDialog) {
			deleteFilterDialog.dismiss();
			if (which == AlertDialog.BUTTON_POSITIVE) {
				// we should delete it.
				this.relativeUri = filterDeletionCandidate.getDeletedUri();
				


				if (this.dialog != null) { Log.e(Global.TAG, "Attempting to create a new dialog when one is already active."); }
				this.dialog = ProgressDialog.show(RecipeBrowser.this, "Please wait...", "Retrieving data...", true);

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

		if (dialog != null) { Log.e(Global.TAG, "Attempting to create a new dialog when one is already active."); }
		dialog = ProgressDialog.show(RecipeBrowser.this, "Please wait...", "Retrieving data...", true);

		Thread thread = new Thread(null, doRequest, "BackgroundRecipeFetch");
		thread.start();

	}
}