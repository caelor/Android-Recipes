package uk.me.plek.kitchenRecipes;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicRecipeTemplater extends RecipeTemplater {
	static final String CONST_NO_INSTRUCTIONS = "<p>This recipe has no instructions.</p>";
	static final String CONST_NO_INGREDIENTS = "<p>This recipe has no ingredients.</p>";

	@Override
	public String generateStyledRecipe(FullRecipe recipe, boolean landscape) {
		if (landscape) {
			return landscapeLayout(recipe);
		}
		else {
			return portraitLayout(recipe);
		}
	}

	private String landscapeLayout(FullRecipe recipe) {
		// head
		String head = xhtmlTitle("Recipe - " + recipe.title);
		head = head + xhtmlRemoteCSS("landscape.css");

		// body
		String body =
			xhtmlHeading(1, recipe.title);

		// details
		String details = xhtmlHeading(2, "Details");

		if (recipe.imageUrl != null) {
			details = details +
			xhtmlSpan(
					xhtmlImage(recipe.imageUrl, "recipeImage", null),
					"recipeImageWrapper",
					null
			);
		}

		if (recipe.cuisine != null) {
			details = details + 
			xhtmlHeading(3, "cuisine") +
			xhtmlSpan(recipe.cuisine, null, "sub3content");
		}

		String foo = null;
		if (recipe.source != null && recipe.link != null) { 
			foo = recipe.source + "<br/>" + xhtmlLink(recipe.link, getLinkHost(recipe.link)); 
		}
		else if (recipe.source != null) {
			if (recipe.source != null && recipe.link != null) { 
				foo = recipe.source; 
			} 
		}
		else if (recipe.link != null) {
			if (recipe.source != null && recipe.link != null) { 
				foo = xhtmlLink(recipe.link, getLinkHost(recipe.link)); 
			} 
		}
		if (foo != null) {
			details = details + 
			xhtmlHeading(3, "source") +
			xhtmlSpan(
					foo,  
					null, 
			"sub3content");
		}

		if (recipe.rating != null) {
			details = details + 
			xhtmlHeading(3, "rating") +
			xhtmlSpan(recipe.rating, null, "sub3content");
		}

		if (recipe.friendlyPrepTime != null) {
			details = details + 
			xhtmlHeading(3, "preptime") +
			xhtmlSpan(recipe.friendlyPrepTime, null, "sub3content");
		}

		if (recipe.friendlyCookTime != null) {
			details = details + 
			xhtmlHeading(3, "cooktime") +
			xhtmlSpan(recipe.friendlyCookTime, null, "sub3content");
		}

		if (recipe.friendlyYield != null) {
			details = details + 
			xhtmlHeading(3, "yield") +
			xhtmlSpan(recipe.friendlyYield, null, "sub3content");
		}


		if (recipe.description != null) {
			details = details + 
			xhtmlHeading(3, "description") +
			xhtmlSpan(recipe.description, null, "sub3content");
		}

		// ingredients
		String ingredients = xhtmlHeading(2, "Ingredients");
		if (recipe.ingredientGroups != null) {
			Iterator<IngredientGroup> igs = recipe.ingredientGroups.iterator();
			while (igs.hasNext()) {
				IngredientGroup ig = igs.next();
				if (ig.groupName.length() > 0) {
					ingredients = ingredients + xhtmlHeading(3, ig.groupName);
				}

				ingredients = ingredients + "<ul>";

				Iterator<String> is = ig.ingredients.iterator();
				while (is.hasNext()) {
					String i = is.next();

					i = performRegexes(i);

					ingredients = ingredients + "<li>" + i + "</li>";
				}

				ingredients = ingredients + "</ul>";
			}
		}
		else {
			ingredients = ingredients + BasicRecipeTemplater.CONST_NO_INGREDIENTS;
		}

		// instructions
		String instructions = xhtmlHeading(2, "Instructions");
		if (recipe.instructions != null) {
			instructions = instructions + formatInstructions(recipe.instructions);
		}
		else {
			instructions = instructions + BasicRecipeTemplater.CONST_NO_INSTRUCTIONS;
		}


		// put it together
		body = body +
		xhtmlSpan(details, "recipeDetails", "block") +
		xhtmlSpan(ingredients, "recipeIngredients", "block") +
		xhtmlSpan(instructions, "recipeInstructions", "block");

		return xhtmlDocument(head,body);

	}

	private String portraitLayout(FullRecipe recipe) {
		// head
		String head = xhtmlTitle("Recipe - " + recipe.title);
		head = head + xhtmlRemoteCSS("portrait.css");

		// body
		String body =
			xhtmlHeading(1, recipe.title);

		// details
		String details = xhtmlHeading(2, "Details");
		if (recipe.imageUrl != null) {
			details = details + xhtmlSpan(
					xhtmlImage(recipe.imageUrl, "recipeImage", null),
					"recipeImageWrapper",
					null
			);
		}

			if (recipe.cuisine != null) {
				details = details + 
				xhtmlHeading(3, "cuisine") +
				xhtmlSpan(recipe.cuisine, null, "sub3content");
			}

			String foo = null;
			if (recipe.source != null && recipe.link != null) { 
				foo = recipe.source + "<br/>" + xhtmlLink(recipe.link, getLinkHost(recipe.link)); 
			}
			else if (recipe.source != null) {
				if (recipe.source != null && recipe.link != null) { 
					foo = recipe.source; 
				} 
			}
			else if (recipe.link != null) {
				if (recipe.source != null && recipe.link != null) { 
					foo = xhtmlLink(recipe.link, getLinkHost(recipe.link)); 
				} 
			}
			if (foo != null) {
				details = details + 
				xhtmlHeading(3, "source") +
				xhtmlSpan(
						foo,  
						null, 
				"sub3content");
			}

			if (recipe.rating != null) {
				details = details + 
				xhtmlHeading(3, "rating") +
				xhtmlSpan(recipe.rating, null, "sub3content");
			}

			if (recipe.friendlyPrepTime != null) {
				details = details + 
				xhtmlHeading(3, "preptime") +
				xhtmlSpan(recipe.friendlyPrepTime, null, "sub3content");
			}

			if (recipe.friendlyCookTime != null) {
				details = details + 
				xhtmlHeading(3, "cooktime") +
				xhtmlSpan(recipe.friendlyCookTime, null, "sub3content");
			}

			if (recipe.friendlyYield != null) {
				details = details + 
				xhtmlHeading(3, "yield") +
				xhtmlSpan(recipe.friendlyYield, null, "sub3content");
			}


			if (recipe.description != null) {
				details = details + 
				xhtmlHeading(3, "description") +
				xhtmlSpan(recipe.description, null, "sub3content");
			}

			// ingredients
			String ingredients = xhtmlHeading(2, "Ingredients");
			if (recipe.ingredientGroups != null) {
				Iterator<IngredientGroup> igs = recipe.ingredientGroups.iterator();
				while (igs.hasNext()) {
					IngredientGroup ig = igs.next();
					if (ig.groupName.length() > 0) {
						ingredients = ingredients + xhtmlHeading(3, ig.groupName);
					}

					ingredients = ingredients + "<ul>";

					Iterator<String> is = ig.ingredients.iterator();
					while (is.hasNext()) {
						String i = is.next();

						i = performRegexes(i);

						ingredients = ingredients + "<li>" + i + "</li>";
					}

					ingredients = ingredients + "</ul>";
				}
			}
			else {
				ingredients = ingredients + BasicRecipeTemplater.CONST_NO_INGREDIENTS;
			}

			// instructions
			String instructions = xhtmlHeading(2, "Instructions");
			if (recipe.instructions != null) {
				instructions = instructions + formatInstructions(recipe.instructions);
			}
			else {
				instructions = instructions + BasicRecipeTemplater.CONST_NO_INSTRUCTIONS;
			}


			// put it together
			body = body +
			xhtmlSpan(details, "recipeDetails", "block") +
			xhtmlSpan(ingredients, "recipeIngredients", "block") +
			xhtmlSpan(instructions, "recipeInstructions", "block");

			return xhtmlDocument(head,body);
		}


		private String formatInstructions(String instructions) {
			String retval = "<ol>";
			// break into a set of string on newlines...
			String[] steps = instructions.split("\n");

			for (int foo = 0; foo < steps.length; foo++) {
				String step = steps[foo];

				String noWhitespace = step.replaceAll("\\s", "");
				if (noWhitespace.length() > 0) {
					// perform any regexes on the step.

					step = performRegexes(step);

					// add it
					retval = retval + "<li>";
					retval = retval + step;
					retval = retval + "</li>";
				}
			}

			retval = retval + "</ol>";
			return retval;
		}


		/**
		 * Performs regexes and transforms on the string to make it more user friendly.
		 * 
		 * @param base The base string to use.
		 * @return A html coded friendly string.
		 */
		private String performRegexes(String base) {
			String i = base;

			// convert 0.5 to 1/2...
			i = i.replaceAll("0\\.5", "&#189;");
			i = i.replaceAll("1/2", "&#189;");

			// convert 1/4
			i = i.replaceAll("0\\.25", "&#188;");
			i = i.replaceAll("1/4", "&#188;");

			// convert 3/4
			i = i.replaceAll("0\\.75", "&#190;");
			i = i.replaceAll("3/4", "&#190;");


			// convert \d+.5 to \d 1/2...
			String regex = "(\\d+)\\.5";
			Matcher m = Pattern.compile(regex).matcher(i);
			if (m.find()) { i = i.replaceAll(regex, m.group(1) + "&#189;"); }

			// convert \d+.25 to \d 1/4...
			regex = "(\\d+)\\.25";
			m = Pattern.compile(regex).matcher(i);
			if (m.find()) { i = i.replaceAll(regex, m.group(1) + "&#188;"); }

			// convert \d+.75 to \d 3/4...
			regex = "(\\d+)\\.75";
			m = Pattern.compile(regex).matcher(i);
			if (m.find()) { i = i.replaceAll(regex, m.group(1) + "&#190;"); }


			return i;
		}

		private String getLinkHost(String link) {
			Matcher m = Pattern.compile("http://(\\S+?)\\/").matcher(link);
			if (m.find()) {
				return m.group(1);
			}
			else {
				return link;
			}
		}
	}
