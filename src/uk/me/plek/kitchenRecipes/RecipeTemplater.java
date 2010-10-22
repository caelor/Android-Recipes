package uk.me.plek.kitchenRecipes;

public abstract class RecipeTemplater {
	public abstract String generateStyledRecipe(FullRecipe recipe);
	
	public static String[] availableTemplates = new String[] {
		"basic"
	};
	
	public static RecipeTemplater templateBuilder(String identifier) {
		if (identifier.equals("basic")) {
			return new BasicRecipeTemplater();
		}
		else {
			// specify a default
			return new BasicRecipeTemplater(); 
		}
	}
	
	
	/* HTML Helper Methods */
	protected String xhtmlStart() {
		return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"" +
				"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\">";
	}
	
	protected String xhtmlEnd() {
		return "</html>";
	}

	protected String xhtmlHeadStart() {
		return "<head>";
	}
	protected String xhtmlHeadEnd() {
		return "</head>";
	}

	protected String xhtmlBodyStart() {
		return "<body>";
	}
	protected String xhtmlBodyEnd() {
		return "</body>";
	}

	protected String xhtmlRemoteCSS(String remote) {
		return "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + remote + "\" />";
	}
	
	protected String xhtmlInlineCSS(String style) {
		return "<style>" + style + "</style>";
	}
	
	protected String xhtmlTitle(String title) {
		return "<title>" + title + "</title>";
	}
	
	protected String xhtmlRemoteJS(String remote) {
		return "<script type=\"text/javascript\" src=\"" + remote + "\"/>";
	}
	
	protected String xhtmlInlineJS(String javascript) {
		return "<script type=\"text/javascript\">" + javascript + "</script>";
	}
}
