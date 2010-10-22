package uk.me.plek.kitchenRecipes;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.acra.ErrorReporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;


public class XMLRecipeDocument {
	
	protected boolean isValid = false;
	private ArrayList<BasicRecipe> recipes = new ArrayList<BasicRecipe>();
	private ArrayList<ActiveFilter> filters = new ArrayList<ActiveFilter>();
	private ArrayList<AvailableField> availableFields = new ArrayList<AvailableField>();

	private String filterItems = "";
	private String recipeItems = "";
	
	private String xmlSource = null;

	public XMLRecipeDocument() {
		// empty.
	}
	
	public XMLRecipeDocument(InputStream is) {
		parseNewDocument(is);
	}
	
	public XMLRecipeDocument(String src) {
		parseNewDocument(src);
	}

	public void parseNewDocument(InputStream is) {
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = r.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
		} catch (IOException e) {
			// ignore it - the document will be marked invalid.
		}
		
		parseNewDocument(sb.toString());

	}
	
	public void parseNewDocument(String src) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc = null;
		
		xmlSource = src;
		
		try {
			db = dbf.newDocumentBuilder();

			doc = db.parse(new ByteArrayInputStream(src.getBytes("UTF-8")));
		} catch (Exception e1) {
			Log.e(Global.TAG, "Error parsing response from server.");
			ErrorReporter.getInstance().handleSilentException(e1); // log a silent exception to ACRA - at least until we work out what might make this fail.
			isValid = false;
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
						this.filterItems = " (" + items + ")";
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
						this.recipeItems = " (" + matches + ")";
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

		isValid = true;
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
	
	public String getFilterItems() { return this.filterItems; }
	public String getRecipeItems() { return this.recipeItems; }
	public ArrayList<BasicRecipe> getRecipes() { return this.recipes; }
	public ArrayList<ActiveFilter> getFilters() { return this.filters; }
	public ArrayList<AvailableField> getAvailableFields() { return this.availableFields; }

	public String getRawXML() { return this.xmlSource; }
}
