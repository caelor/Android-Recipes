package uk.me.plek.kitchenRecipes;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class RecipePreferences extends PreferenceActivity {
	/* Lifecycle Methods */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
	
}
