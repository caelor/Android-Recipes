<?php

function performRegexes($base) {
  // convert 0.33 to 1/3
  $base = preg_replace("/0\.33(3*)/", "&#x2153;", $base);
  $base = preg_replace("/1\/3/", "&#x2153;", $base);

  // convert 0.66 to 2/3
  $base = preg_replace("/0\.66(6*)/", "&#x2154;", $base);
  $base = preg_replace("/2\/3/", "&#x2154;", $base);

  // convert 0.5 to 1/2...
  $base = preg_replace("/0\.5/", "&#189;", $base);
  $base = preg_replace("/1\/2/", "&#189;", $base);
  
  // convert 1/4..
  $base = preg_replace("/0\.25/", "&#188;", $base);
  $base = preg_replace("/1\/4/", "&#188;", $base);

  // convert 0.2 -> 1/5
  $base = preg_replace("/0\.2/", "&#x2155;", $base);
  $base = preg_replace("/1\/5/", "&#x2155;", $base);

  // convert 0.4 -> 2/5
  $base = preg_replace("/0\.4/", "&#x2156;", $base);
  $base = preg_replace("/2\/5/", "&#x2156;", $base);

  // convert 0.6 -> 3/5
  $base = preg_replace("/0\.6/", "&#x2157;", $base);
  $base = preg_replace("/3\/5/", "&#x2157;", $base);

  // convert 0.8 -> 4/5
  $base = preg_replace("/0\.8/", "&#x2158;", $base);
  $base = preg_replace("/4\/5/", "&#x2158;", $base);

  // convert 3/4..
  $base = preg_replace("/0\.75/", "&#190;", $base);
  $base = preg_replace("/3\/4/", "&#190;", $base);

  // convert \d+.5 to \d 1/2...
  $base = preg_replace("/(\d+)\.5/", "$1&#189;", $base);

  // convert \d+.25 to \d 1/4...
  $base = preg_replace("/(\d+)\.25/", "$1&#188;", $base);

  // convert \d+.75 to \d 3/4...
  $base = preg_replace("/(\d+)\.75/", "$1&#190;", $base);

  // convert \d+.33 to \d 1/3...
  $base = preg_replace("/(\d+)\.33(3*)/", "$1&#x2153;", $base);

  // convert \d+.66 to \d 2/3...
  $base = preg_replace("/(\d+)\.66(6*)/", "$1&#x2154;", $base);

  // convert \d+.2 to \d 1/5...
  $base = preg_replace("/(\d+)\.2/", "$1&#x2155;", $base);

  // convert \d+.4 to \d 2/5...
  $base = preg_replace("/(\d+)\.4/", "$1&#x2156;", $base);

  // convert \d+.6 to \d 3/5...
  $base = preg_replace("/(\d+)\.6/", "$1&#x2157;", $base);

  // convert \d+.8 to \d 4/5...
  $base = preg_replace("/(\d+)\.8/", "$1&#x2158;", $base);


  // if the step starts with 1 or more digits and an optional character, remove.
  $base = preg_replace("/^\d+[\.\):]?/","", $base);
  return $base;
}

function getLinkHost($link) {
  return preg_replace("/http(s?):\/\/(\S+?)\/.*/", "$2", $link);
}	
	
function formatInstructions($instructions) {
  $retval = "<ol>";
  // break into a set of string on newlines...
  $steps = explode("\n",$instructions);

  while (sizeof($steps) > 0) {
    $step = array_shift($steps);
    $noWhitespace = str_replace(" ", "", $step);
    
    if (strlen($noWhitespace) > 0) {
      $step = performRegexes($step);
      $retval = $retval . "<li>" . $step . "</li>";
    }
    
  }
  
  return $retval . "</ol>";
}

function friendlyRecipe($dbHandle, $recipeId) {
  // get the recipe out of the database
  $rootElement = new SimpleXMLElement_Plus("<recipeDB></recipeDB>");
  addCompleteRecipes($dbHandle, $rootElement, "WHERE recipe.id=" . $recipeId . " ", "ORDER BY recipe.id");
  
  $recipe = $rootElement->recipe;
  
  ?><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">
<html xmlns=\"http://www.w3.org/1999/xhtml\">
  <head>
    <title>Recipe - <?php print $recipe->attributes()->title; ?></title>
    <link rel="stylesheet" type="text/css" href="<?php print getBaseUri() . "/friendly.css";?>" />
  </head>
  <body>
    <span class='limitWidth'>
      <h1><?php print $recipe->attributes()->title?></h1>
      
        <!-- Column 1 -->
      <h2 class="colbreak">Details</h2>
      <span class="no-multicolumn">
        <?php if ($recipe->attributes()->hasImage == 1) { ?>
        <span id='recipeImageWrapper'>
          <img src="<?php print $recipe->attributes()->imageUrl; ?>"/>
        </span>
        <?php } ?>

        <h3 class="float">Cuisine</h3>
        <span class="sub3content"><?php print $recipe->attributes()->cuisine;?></span>

        <h3 class="float">Source</h3>
        <span class="sub3content"><?php print $recipe->attributes()->source;?></span>

        <!--<h3>Link</h3>
        <span class="sub3content">
          <a href="<?php print $recipe->attributes()->link;?>"><?php print $recipe->attributes()->link;?></a>
        </span>-->

        <h3 class="float">Rating</h3>
        <span class="sub3content"><?php print $recipe->attributes()->rating;?></span>

        <h3 class="float">Prep Time</h3>
        <span class="sub3content"><?php print $recipe->attributes()->preptime_friendly;?></span>

        <h3 class="float">Cook Time</h3>
        <span class="sub3content"><?php print $recipe->attributes()->cooktime_friendly;?></span>

        <h3 class="float">Yield</h3>
        <span class="sub3content"><?php print $recipe->attributes()->yield_friendly;?></span>

	<span class="block" style="clear: both">&nbsp;</span>
      </span>

      <span class="multicolumn">
        <h2>Ingredients</h2>
        <?php foreach ($recipe->ingredients->group as $group) { ?>
          <?php if (strlen($group->attributes()->name) > 0) { ?>
            <h3><?php print $group->attributes()->name; ?></h3>
          <?php } ?>
          <ul>
          <?php foreach ($group->ingredient as $ingredient) { ?>
            <?php if ($ingredient->hasReference) { ?>
            <?php } else { ?>
            <li><?php print performRegexes(htmlentities($ingredient, ENT_QUOTES, "UTF-8")); ?></li>
            <?php } ?>
          <?php } ?>
          </ul>
		
        <?php } ?>
		
        <!-- Column 2 -->
        <h2>Instructions</h2>
        <?php print formatInstructions(htmlentities($recipe->instructions, ENT_QUOTES, "UTF-8")); ?>

        <?php if (strlen($recipe->modifications) > 0) { ?>
          <h2>Notes</h2>
          <p class="notes"><?php print htmlentities($recipe->modifications, ENT_QUOTES, "UTF-8");?></p>
        <?php } ?>

        <span style="clear:both"/>
      </span>
    </span>
    <span class="footnotes">
      <p>The font used for the headings is <span class='quote'>Writing Stuff</span> by Brittney Murphy. See <a href="http://www.dafont.com/writing-stuff.font">www.dafont.com/writing-stuff.font</a>.
      The font used for the text is <span class='quote'>Timeless</span> by Manfred Klein. See <a href="http://www.dafont.com/timeless.font">www.dafont.com/timeless.font</a></p>
      <p>This recipe, its photograph, and the fonts used in this file may be covered by appropriate copyrights.</p>
    </span>
  </body>
</html><?php
}

?>
