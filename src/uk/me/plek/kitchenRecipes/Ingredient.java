package uk.me.plek.kitchenRecipes;

public class Ingredient {
	protected final String _ingredient;
	protected final String _referencedRecipeUri;
	
	public Ingredient(String ingredient) {
		_ingredient = ingredient;
		_referencedRecipeUri = null;
	}
	
	public Ingredient(String ingredient, String uri) {
		_ingredient = ingredient;
		_referencedRecipeUri = uri;
	}
	
	public boolean hasReference() {
		return (_referencedRecipeUri != null);
	}
	
	public String getIngredient() {
		return _ingredient;
	}
	
	public String getReferenceUri() {
		return _referencedRecipeUri;
	}
}
