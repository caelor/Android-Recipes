package uk.me.plek.kitchenRecipes;

import java.util.HashSet;

public class IngredientGroup {
	protected final String groupName;
	protected HashSet<String> ingredients;

	public IngredientGroup(String name) {
		groupName = name;
		ingredients = new HashSet<String>();
	}
	
	public void addIngredient(String ingredient) {
		ingredients.add(ingredient);
	}
	
	public boolean containsIngredient(String ingredient) {
		return ingredients.contains(ingredient);
	}
	
	public String[] getIngredients() {
		return (String[]) ingredients.toArray();
	}
	
	public String getGroupName() {
		return groupName;
	}
}
