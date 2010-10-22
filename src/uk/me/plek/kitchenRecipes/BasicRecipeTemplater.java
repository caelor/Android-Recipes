package uk.me.plek.kitchenRecipes;

public class BasicRecipeTemplater extends RecipeTemplater {

	@Override
	public String generateStyledRecipe(FullRecipe recipe) {
		String html = xhtmlStart();
		
		// head
		html = html + xhtmlHeadStart();
		html = html + xhtmlTitle("Recipe");
		html = html + xhtmlHeadEnd();
		
		// body
		html = html + xhtmlBodyStart();
		html = html + "<h1>" + recipe.title + "</h1>";
		html = html + "Hello World";
		html = html + xhtmlBodyEnd();
		
		html = html + xhtmlEnd();
		
		return html;
	}

}
