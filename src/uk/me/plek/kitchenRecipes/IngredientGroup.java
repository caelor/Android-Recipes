package uk.me.plek.kitchenRecipes;

import java.util.Vector;

public class IngredientGroup {
	protected final String groupName;
	protected Vector<Ingredient> ingredients;

	public IngredientGroup(String name) {
		groupName = name;
		ingredients = new Vector<Ingredient>();
	}
	
	public void addIngredient(Ingredient ingredient) {
		ingredients.add(ingredient);
	}

	public void addReferencedIngredient(String ingredient, String uri) {
		ingredients.add(new Ingredient(ingredient, uri));
	}

	@Deprecated
	public boolean containsIngredient(Ingredient ingredient) {
		return ingredients.contains(ingredient);
	}
	
	public Ingredient[] getIngredients() {
		return (Ingredient[]) ingredients.toArray();
	}
	
	public String getGroupName() {
		return groupName;
	}
}
