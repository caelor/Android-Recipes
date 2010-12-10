package uk.me.plek.kitchenRecipes;

import android.content.Context;
import android.content.Intent;

public class Global {
	static final String TAG = "RecipeBrowser";
	static final String RELEASE = "0.4";
	static final long REQUEST_CACHE_MAX_AGE = 1800000; // 30 min in ms
	
	static public String httpToRecipeConvert(String url) {
		return url.replace("http://", "recipe://");
	}
	static public String recipeToHttpConvert(String url) {
		return url.replace("recipe://", "http://");
	}
	
	static public void shareRecipeLink(BasicRecipe recipe, Context c) {
		String linkUrl = recipe.requestUrl;
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_SUBJECT, "Sharing Recipe: " + recipe.title);
		i.putExtra(Intent.EXTRA_TEXT, linkUrl);
		i.setType("text/plain");
		c.startActivity(Intent.createChooser(
				i,
				"Share Recipe Link")
				);		
	}
	
	static public void shareRecipeCard(BasicRecipe recipe, Context c) {
		String cardUrl = recipe.recipeCardUrl;
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_SUBJECT, "Sharing Recipe: " + recipe.title);
		i.putExtra(Intent.EXTRA_TEXT, "You'll like this recipe. To view it, please click here: " + cardUrl);
		i.setType("text/plain");
		c.startActivity(Intent.createChooser(
				i,
				"Share Recipe Card")
				);		
	}

}
