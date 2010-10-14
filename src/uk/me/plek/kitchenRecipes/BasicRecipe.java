package uk.me.plek.kitchenRecipes;

import org.w3c.dom.Element;

public class BasicRecipe {
	/* These fields are provided by the summary */
	protected String friendlyCookTime;
	protected String friendlyPrepTime;
	protected String friendlyYield;
	
	protected int rawCookTime;
	protected int rawPrepTime;
	
	protected String identifier;
	protected String title;
	protected String rating;
	
	protected String imageUrl = null;
	protected String thumbUrl = null;
	
	
	public BasicRecipe(Element recipeElement) {
		
		friendlyCookTime = recipeElement.getAttribute("cooktime_friendly");
		friendlyPrepTime = recipeElement.getAttribute("preptime_friendly");
		friendlyYield = recipeElement.getAttribute("yield_friendly");
		
		try {
			rawCookTime = Integer.parseInt(recipeElement.getAttribute("cooktime"));
		} catch (NumberFormatException e) {
			rawCookTime = 0;
		}
		
		try {
			rawPrepTime = Integer.parseInt(recipeElement.getAttribute("preptime"));
		} catch (NumberFormatException e) {
			rawPrepTime = 0;
		}
		
		identifier = recipeElement.getAttribute("id");
		title = recipeElement.getAttribute("title");
		rating = recipeElement.getAttribute("rating");

		if ("1".equals(recipeElement.getAttribute("hasImage"))) {
			imageUrl = recipeElement.getAttribute("imageUrl");
			thumbUrl = recipeElement.getAttribute("thumbUrl");
		}
	}
	
	
	public String getCookTimeFriendly() {
		return friendlyCookTime;
	}

	public int getCookTimeSeconds() {
		return rawCookTime;
	}

	public String getId() {
		return identifier;
	}

	public String getPrepTimeFriendly() {
		return friendlyPrepTime;
	}

	public int getPrepTimeSeconds() {
		return rawPrepTime;
	}

	public String getRating() {
		return rating;
	}

	public String getTitle() {
		return title;
	}

	public String getYieldFriendly() {
		return friendlyYield;
	}
}
