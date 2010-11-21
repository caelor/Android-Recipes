package uk.me.plek.kitchenRecipes;

public class Global {
	static final String TAG = "RecipeBrowser";
	static final String RELEASE = "0.2c_dev";
	static final long REQUEST_CACHE_MAX_AGE = 1800000; // 30 min in ms
	
	static public String httpToRecipeConvert(String url) {
		return url.replace("http://", "recipe://");
	}
	static public String recipeToHttpConvert(String url) {
		return url.replace("recipe://", "http://");
	}

}
