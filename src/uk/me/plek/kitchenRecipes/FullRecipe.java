package uk.me.plek.kitchenRecipes;

import java.util.HashSet;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class FullRecipe extends BasicRecipe {
	
	/* Full Recipe Fields */
	protected String cuisine;
	protected String source;
	protected String instructions;
	protected String modifications;
	protected String description;
	protected String link;
	protected HashSet<String> categories = new HashSet<String>();
	protected Vector<IngredientGroup> ingredientGroups = new Vector<IngredientGroup>();

	public FullRecipe(Element recipeElement) {
		super(recipeElement);
		
		cuisine = recipeElement.getAttribute("cuisine");
		source = recipeElement.getAttribute("source");
		link = recipeElement.getAttribute("link");
		
		NodeList childNodes = recipeElement.getChildNodes();
		for (int foo = 0; foo < childNodes.getLength(); foo++) {
			Node n = childNodes.item(foo);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element)n;
				String tagName = e.getTagName();
				
				if (tagName.equals("instructions")) {
					NodeList bar = e.getChildNodes();
					instructions = "";
					for (int subitem = 0; subitem < bar.getLength(); subitem++) {
						Node item = bar.item(subitem);
						if (item.getNodeType() == Node.TEXT_NODE) {
							instructions = instructions + bar.item(subitem).getNodeValue();
						}
						else if (item.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
							// html character codes...
							instructions = instructions + "&" + item.getNodeName() + ";";
						}
					}
				}

				else if (tagName.equals("modifications")) {
					NodeList bar = e.getChildNodes();
					if (bar.getLength() > 0) {
						modifications = bar.item(0).getNodeValue();
					}
				}

				else if (tagName.equals("description")) {
					NodeList bar = e.getChildNodes();
					if (bar.getLength() > 0) {
						modifications = bar.item(0).getNodeValue();
					}
				}

				else if (tagName.equals("categories")) {
					NodeList bar = e.getChildNodes();
					for (int bar2 = 0; bar2 < bar.getLength(); bar2++) {
						Node bar3 = bar.item(bar2);
						if (bar3.getNodeType() == Node.ELEMENT_NODE) {
							Element categoryElement = (Element)bar3;
							String category = categoryElement.getChildNodes().item(0).getNodeValue();
							categories.add(category);
						}
						else {
							Log.w(Global.TAG, "Non-category node found in categories.");
						}
					}
				}

				else if (tagName.equals("ingredients")) {
					NodeList bar = e.getChildNodes();
					ingredientGroups.clear();
					for (int bar2 = 0; bar2 < bar.getLength(); bar2++) {
						Node ingGroupNode = bar.item(bar2);
						if (ingGroupNode.getNodeType() == Node.ELEMENT_NODE) {
							Element ingGroupElement = (Element)ingGroupNode;
							String groupName = ingGroupElement.getAttribute("name");
							NodeList ingredients = ingGroupElement.getChildNodes();
							IngredientGroup ingGroup = new IngredientGroup(groupName);
							ingredientGroups.add(ingGroup);
							
							for (int bar3 = 0; bar3 < ingredients.getLength(); bar3++) {
								Node ingNode = ingredients.item(bar3);
								if (ingNode.getNodeType() == Node.ELEMENT_NODE) {
									Element ingElement = (Element)ingNode;
								
									String ingredient = ingElement.getChildNodes().item(0).getNodeValue();
									String reference = ingElement.getAttribute("referencedRecipeAbsoluteUri");
									// getAttribute always returns a string, even if it's empty
									if (reference.length() > 0) { 
										ingGroup.addIngredient(new Ingredient(ingredient, reference));
									}
									else {
										ingGroup.addIngredient(new Ingredient(ingredient));
									}
								}
								else {
									Log.w(Global.TAG, "Unexpected non-element node in ingredient group.");
								}
							}
						}
						else {
							Log.w(Global.TAG, "Non-category node found in categories.");
						}
					}
				}
				else {
					Log.w(Global.TAG, "Unknown element type in Recipe: " + tagName);
				}
			}
			else {
				Log.w(Global.TAG, "Unexpected non-element node in Recipe: " + n.toString());
			}
		}
	}


	public String getCuisine() {
		return cuisine;
	}
	
	public String getSource() {
		return source;
	}
	
	public String getLink() {
		return link;
	}
	
	public String getInstructions() {
		return instructions;
	}
	
	public String getModifications() {
		return modifications;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String[] getCategories() {
		return (String[]) categories.toArray();
	}
	
	public IngredientGroup[] getIngredients() {
		return (IngredientGroup[]) ingredientGroups.toArray();
	}
}
