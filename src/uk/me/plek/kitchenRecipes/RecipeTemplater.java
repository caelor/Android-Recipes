package uk.me.plek.kitchenRecipes;

public abstract class RecipeTemplater {
	public abstract String generateStyledRecipe(FullRecipe recipe, boolean landscape);
	
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
	protected String xhtmlDocument(String head, String body) {
		String retval = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"" +
				"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\">";
		
		retval = retval + "<head>" + head + "</head><body>"+ body + "</body></html>";
		
		return retval;
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
	
	protected String xhtmlHeading(int level, String text) {
		return xhtmlHeading(level,text,null,null);
	}
	
	protected String xhtmlHeading(int level, String text, String id, String cssClass) {
		String retval = "<h" + String.valueOf(level) + " ";
		
		if (id != null) { retval = retval + "id='" + id +"' "; }
		if (cssClass != null) { retval = retval + "class='" + cssClass + "' "; }
		
		retval = retval + ">" + text + "</h" + String.valueOf(level) + ">";
		
		return retval;
	}
	
	protected String xhtmlLink(String dest, String text) {
		return xhtmlLink(dest,text,null,null);
	}
	
	protected String xhtmlLink(String dest, String text, String id, String cssClass) {
		String retval = "<a ";
		
		if (id != null) { retval = retval + "id='" + id +"' "; }
		if (cssClass != null) { retval = retval + "class='" + cssClass + "' "; }
		
		retval = retval + " href='" + dest + "'>" + text + "</a>";
		
		return retval;
	}

	protected String xhtmlSpan(String contents) {
		return xhtmlSpan(contents, null, null);
	}
	
	protected String xhtmlSpan(String contents, String id, String cssClass) {
		String retval = "<span ";
		
		if (id != null) { retval = retval + "id='" + id +"' "; }
		if (cssClass != null) { retval = retval + "class='" + cssClass + "' "; }
		
		retval = retval + ">" + contents + "</span>";
		
		return retval;
	}
	
	protected String xhtmlImage(String imgUrl) {
		return xhtmlImage(imgUrl, null, null);
	}
	
	protected String xhtmlImage(String imgUrl, String id, String cssClass) {
		String retval = "<img ";
		
		if (id != null) { retval = retval + "id='" + id +"' "; }
		if (cssClass != null) { retval = retval + "class='" + cssClass + "' "; }
		
		retval = retval + " src='" + imgUrl + "' />";
		
		return retval;
	}
	
}
