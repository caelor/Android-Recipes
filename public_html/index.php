<?php

function maxRecipesToReturnFully() { 
  return 1;
}

function getDefaultOrderBy() {
  return " ORDER BY recipe.rating DESC, recipe.title ";
}

$recipeDb = "/var/tmp/recipes.db";
$jpgCache = getcwd() . '/cache';
$jpgCacheAge = 60 * 60; // 1 hour

if (file_exists("config.inc.php")) {
  include "config.inc.php";
}

// **************************************************************

require "util.inc.php";
require "simplexmlplus.inc.php";
require "friendlyrecipe.inc.php";
require "cutycapt.inc.php";

$orderBy = $defaultOrderBy;
$opts = $_SERVER['QUERY_STRING'];

if ($opts == "stylesheet.xsl") {
  returnStylesheet();
}
elseif ($opts == "friendly.css") {
  returnCSSStylesheet();
}
elseif (substr($opts,0,4) == "img/") {
  $imgId = substr($opts, 4);
  returnImage($recipeDb, $imgId);
}
elseif (substr($opts,0,6) == "thumb/") {
  $imgId = substr($opts,6);
  returnThumbnail($recipeDb, $imgId);
}
elseif (substr($opts,-4) == ".ttf") {
  $file = 'ttf/' . $opts;
  if (file_exists($file)) {
    header('Content-type: font/ttf');
    echo file_get_contents($file);
  }
  else {
    header('Content-type: text/plain');
    echo "500\n";
    echo "Unable to service your request.";
  }
}
elseif (substr($opts,0,5) == "view/") {
  # friendly view a recipe
  $recipeId = substr($opts,5);
  $dbHandle = null;
  if ($dbHandle = new SQLite3($recipeDb, SQLITE3_OPEN_READONLY)) {
    friendlyRecipe($dbHandle, $recipeId);
  }
}
elseif (substr($opts,0,4) == "jpg/") {
  $recipeId = substr($opts,4);
  $uri = 'http'. ($_SERVER['HTTPS'] ? 's' : null) .'://'. $_SERVER['HTTP_HOST'] . $_SERVER['REQUEST_URI'];
  $uri = str_replace('/jpg/','/view/', $uri);
  outputRecipeJPG($recipeId, $uri, $jpgCache, $jpgCacheAge);
}
else {
  parseOpts($opts, $recipeDb);
}

function tableJoins() {
  // set up the joins.
  $join = "FROM recipe ";
  $join .= "LEFT OUTER JOIN categories ON recipe.id=categories.recipe_id ";
  $join .= "LEFT OUTER JOIN ingredients ON recipe.id=ingredients.recipe_id ";

  return $join;
}

function encodeParam($p) {
  $result = str_replace("/", "~", $p); // otherwise the %2F will get decoded as / and break our parsing.
  return urlencode($result); 
}
function decodeParam($p) {
  $result = urldecode($p);
  return str_replace("~", "/", $result);
}
function friendlyTime($foo) {
  $friendly = "";
  $hours = (int)($foo / 3600);
  $foo = $foo % 3600;
  if ($hours > 0) { $friendly .= "$hours hrs "; }

  $minutes = (int)($foo / 60);
  if ($minutes > 0) { $friendly .= "$minutes mins "; }

  $seconds = $foo % 60;  
  if ($seconds > 0) { $friendly .= "$seconds secs "; }

  return substr($friendly, 0, strlen($friendly)-1);
}

function addAvailableField($fieldsElement, $name, $rel, $abs, $type, $isRanged = 0) {
  $foo = $fieldsElement->addChild('field');
  $foo->addAttribute('name', $name);
  $foo->addAttribute('relativeuri', $rel); 
  $foo->addAttribute('absoluteuri', $abs);
  $foo->addAttribute('type', $type);
  
  if ($isRanged > 0) {
    $foo->addAttribute('isRanged', $isRanged);
  }
}

function createFilterElement($opts, $rootElement) {
  $params = explode("/",$opts);
  
  $allOk = true;
  $filterElement = $rootElement->addChild('filter');
  
  while (sizeof($params) > 0) {
    $field = array_shift($params);
  
    if (sizeof($params) > 0) {
      $urlencoded = array_shift($params); //print "e$urlencoded<br/>";
      $urldecoded = decodeParam($urlencoded); //print "d$urldecoded<br/>";
      $value = SQLite3::escapeString($urldecoded); //print "v$value<br/>";
  
      $filterItemElement = $filterElement->addChild('filterItem');
      $filterItemElement->addAttribute('field', $field);
      $filterItemElement->addAttribute('value', $value);
      $foo = $field . "/" . $urlencoded;
      $filterItemElement->addAttribute('deletedRelativeUri', str_replace("//", "/", str_replace($foo, "", $opts)));
      $filterItemElement->addAttribute('deletedAbsoluteUri', str_replace(":/", "://", str_replace("//", "/", str_replace($foo, "", getBaseUri() . $opts))));
  
      if ($urldecoded != $value) { $allOk = false; }
    }  
  }
  
  if ($allOk) {
    return $filterElement;
  }
  else {
    return NULL;
  }
}

function addFieldOptions($rootElement) {
  $fieldsElement = $rootElement->addChild('availableFields'); 
 
  addAvailableField($fieldsElement, "Sort Order", $relativeBase . "sort", $absoluteBase . "sort", "sort");
  addAvailableField($fieldsElement, "Category", $relativeBase . "category", $absoluteBase . "category", "filter");
  addAvailableField($fieldsElement, "Cuisine", $relativeBase . "cuisine", $absoluteBase . "cuisine", "filter");
  addAvailableField($fieldsElement, "Ingredient", $relativeBase . "ingredient", $absoluteBase . "ingredient", "filter");
  addAvailableField($fieldsElement, "Minimum Servings", $relativeBase . "servingsmin", $absoluteBase . "servingsmin", "filter", 1);
  addAvailableField($fieldsElement, "Maximum Servings", $relativeBase . "servingsmax", $absoluteBase . "servingsmax", "filter", 1);
  addAvailableField($fieldsElement, "Exact Servings", $relativeBase . "servings", $absoluteBase . "servings", "filter");
  addAvailableField($fieldsElement, "Serving Unit", $relativeBase . "servingunit", $absoluteBase . "servingunit", "filter");
  addAvailableField($fieldsElement, "Minimum Rating", $relativeBase . "ratingmin", $absoluteBase . "ratingmin", "filter", 1);
  addAvailableField($fieldsElement, "Maximum Rating", $relativeBase . "ratingmax", $absoluteBase . "ratingmax", "filter", 1);
  addAvailableField($fieldsElement, "Exact Rating", $relativeBase . "rating", $absoluteBase . "rating", "filter");
  addAvailableField($fieldsElement, "Exact Prep Time", $relativeBase . "preptime", $absoluteBase . "preptime", "filter");
  addAvailableField($fieldsElement, "Maximum Prep Time", $relativeBase . "preptimemax", $absoluteBase . "preptimemax", "filter", 1);
  addAvailableField($fieldsElement, "Exact Cook Time", $relativeBase . "cooktime", $absoluteBase . "cooktime", "filter");
  addAvailableField($fieldsElement, "Maximum Cook Time", $relativeBase . "cooktimemax", $absoluteBase . "cooktimemax", "filter", 1);
  addAvailableField($fieldsElement, "Source", $relativeBase . "source", $absoluteBase . "source", "filter");
}

function addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase) {
  //$sql = "SELECT DISTINCT categories.category " . $join . $filter . " ORDER BY categories.category ";
  //$rootElement->addAttribute('sql', $sql);
  $baseElement = $rootElement->addChild('options');
  $baseElement->addAttribute('type', 'list');

  $result = $dbHandle->query($sql);
  while ($output = $result->fetchArray()) {
    $foo = $output[0];
    if ($foo === "") {
      // ignore it
    }
    else {
      $thisEl = $baseElement->addChild('listitem');
      $thisEl->addAttribute('value', $foo);
      $encoded = encodeParam($foo);
      $thisEl->addAttribute('relativeuri', $relativeBase . $encoded);
      $thisEl->addAttribute('absoluteuri', $absoluteBase . $encoded);
    }
  }
}

function determineFilter($opts) {
  $params = explode("/",$opts);
  
  $allOk = true;
  $filter = ''; // a generic clause so we don't end up with an empty where.
  
  while (sizeof($params) > 0) {
    $field = array_shift($params);
  
    if (sizeof($params) > 0) {
      $urlencoded = array_shift($params); //print "e$urlencoded<br/>";
      $urldecoded = decodeParam($urlencoded); //print "d$urldecoded<br/>";
      $value = SQLite3::escapeString($urldecoded); //print "v$value<br/>";
    
      if ($urldecoded != $value) { $allOk = false; }
 
      if ($field != 'sort') { 
        if ($filter != '') {
          $filter .= " AND ";
        }
        else {
          $filter = " WHERE ";
        }
      }
      // implement a filter...   
      if ($field == 'recipe') {
        $filter .= "recipe.id=" . $value;
      }
      elseif ($field == 'category') {
        $filter .= "categories.category='" . $value . "'";
      }
      elseif ($field == 'cuisine') {
        $filter .= "recipe.cuisine='" . $value . "'";
      }
      elseif ($field == 'ingredient') {
        $filter .= "ingredients.ingkey='" . $value . "'";
      }
      elseif ($field == 'servingsmin') {
        $filter .= "recipe.yields>=" . $value;
      }
      elseif ($field == 'servingsmax') {
        $filter .= "recipe.yields<=" . $value;
      }
      elseif ($field == 'servings') {
        $filter .= "recipe.yields=" . $value;
      }
      elseif ($field == 'servingunit') {
        $filter .= "recipe.yield_unit='" . $value . "'";
      }
      elseif ($field == 'ratingmin') {
        $filter .= "recipe.rating>=" . $value;
      }
      elseif ($field == 'ratingmax') {
        $filter .= "recipe.rating<=" . $value;
      }
      elseif ($field == 'rating') {
        $filter .= "recipe.rating=" . $value;
      }
      elseif ($field == 'preptime') {
        $filter .= "recipe.preptime=" . $value;
      }
      elseif ($field == 'preptimemax') {
        $filter .= "recipe.preptime<=" . $value;
      }
      elseif ($field == 'cooktime') {
        $filter .= "recipe.cooktime=" . $value;
      }
      elseif ($field == 'cooktimemax') {
        $filter .= "recipe.cooktime<=" . $value;
      }
      elseif ($field == 'source') {
        $filter .= "recipe.source='" . $value . "'";
      }

      elseif ($field == 'sort') {
      }
  
      else {
        $allOk = false; // bad request
      }
    }  
  }
  
  if ($allOk) {
    return $filter;
  }
  else {
    return NULL;
  }
}

function determineOrderBy($opts) {
  $params = explode("/",$opts);
  
  $allOk = true;
  $orderBy = getDefaultOrderBy(); // a generic clause so we don't end up with an empty where.
  
  while (sizeof($params) > 0) {
    $field = array_shift($params);
  
    if (sizeof($params) > 0) {
      $urlencoded = array_shift($params); //print "e$urlencoded<br/>";
      $urldecoded = decodeParam($urlencoded); //print "d$urldecoded<br/>";
      $value = SQLite3::escapeString($urldecoded); //print "v$value<br/>";
    
      if ($urldecoded != $value) { $allOk = false; }
 
      if ($field == 'sort') {
        // allowed sort fields:
        //   title
        //   modtime
        if ($value == 'title') {
          $orderBy = " ORDER BY recipe.title ";
        }
        elseif ($value == 'modtime') {
          $orderBy = " ORDER BY recipe.last_modified ";
        }
        elseif ($value == 'rating') {
          $orderBy = $defaultOrderBy;
        }
        elseif ($value == 'modtimedesc') {
          $orderBy = " ORDER BY recipe.last_modified DESC ";
        }
        else {
          $allOk = false; // bad request
        }
      }
    }  
  }
  
  if ($allOk) {
    return $orderBy;
  }
  else {
    return NULL;
  }
}

function determineQueryType($opts) {
  $params = explode("/",$opts);
  $query = 'fields'; // default to querying fields
  while (sizeof($params) > 0) {
    $field = array_shift($params);
  
    if (sizeof($params) == 0) {
      $query = $field;
    }
  }
  
  return $query;
}

function handleQuery($dbHandle, $rootElement, $absoluteBase, $relativeBase, $query, $filter) {
    if ($query == "fields") {
      addFieldOptions($rootElement);
    } // end of fields 
    elseif ($query == "category") {
      // return a list of matching categories
      $sql = "SELECT DISTINCT categories.category " . tableJoins() . $filter . " ORDER BY categories.category ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif ($query == "cuisine") {
      // return a list of matching cuisines
      $sql = "SELECT DISTINCT recipe.cuisine " . tableJoins() . $filter . " ORDER BY recipe.cuisine ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif ($query == "ingredient") {
      // return a list of matching ingredients
      $sql = "SELECT DISTINCT LOWER(ingredients.ingkey) " . tableJoins() . $filter . " ORDER BY ingredients.ingkey ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
      //$rootElement->addAttribute('sql', $sql);
      /*$baseElement = $rootElement->addChild('options');
      $baseElement->addAttribute('type', 'list');
  
      $result = $dbHandle->query($sql);
      while ($output = $result->fetchArray()) {
        $foo = $output['ingkey'];
        if ($foo === "") {
          // ignore it
        }
        else {
          $thisEl = $baseElement->addChild('listitem');
          $thisEl->addAttribute('value', $foo);
          $encoded = encodeParam($foo);
          $thisEl->addAttribute('relativeuri', $relativeBase . $encoded);
          $thisEl->addAttribute('absoluteuri', $absoluteBase . $encoded);
        }
      }*/
    }
    elseif (($query == "servingsmin") or ($query == "servingsmax")) {
      // return an indication that this a numeric value, with suitable ranges
      $sql1 = "SELECT MIN(recipe.yields) " . tableJoins() . $filter;
      $sql2 = "SELECT MAX(recipe.yields) " . tableJoins() . $filter;
      $minVal = $dbHandle->querySingle($sql1);
      $maxVal = $dbHandle->querySingle($sql2);
  
      //$rootElement->addAttribute('sql', $sql1);
      //$rootElement->addAttribute('sql2', $sql2);
      $baseElement = $rootElement->addChild('options');
      $baseElement->addAttribute('type', 'numeric');
      $baseElement->addAttribute('minValue', $minVal);
      $baseElement->addAttribute('maxValue', $maxVal);
    }
    elseif ($query == "servings") {
      // return a list of servings values
      $sql = "SELECT DISTINCT recipe.yields " . tableJoins() . $filter . " ORDER BY recipe.yields ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif ($query == "servingunit") {
      // return a list of matching serving units.
      $sql = "SELECT DISTINCT recipe.yield_unit " . tableJoins() . $filter . " ORDER BY recipe.yield_unit";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif (($query == "ratingmin") or ($query == "ratingmax")) {
      // return an indication that this is a numeric value, with suitable ranges
      $sql1 = "SELECT MIN(recipe.rating) " . tableJoins() . $filter;
      $sql2 = "SELECT MAX(recipe.rating) " . tableJoins() . $filter;
      $minVal = $dbHandle->querySingle($sql1);
      $maxVal = $dbHandle->querySingle($sql2);
  
      //$rootElement->addAttribute('sql', $sql1);
      //$rootElement->addAttribute('sql2', $sql2);
      $baseElement = $rootElement->addChild('options');
      $baseElement->addAttribute('type', 'numeric');
      $baseElement->addAttribute('minValue', $minVal);
      $baseElement->addAttribute('maxValue', $maxVal);
    }
    elseif ($query == "rating") {
      // return a list of rating values
      $sql = "SELECT DISTINCT recipe.rating " . tableJoins() . $filter . " ORDER BY recipe.rating ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif ($query == "preptime") {
      // return a list of preptimes
      $sql = "SELECT DISTINCT recipe.preptime " . tableJoins() . $filter . " ORDER BY recipe.preptime ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif ($query == "preptimemax") {
      // return an indication that this is a numeric value, with suitable ranges
      $sql1 = "SELECT MIN(recipe.preptime) " . tableJoins() . $filter;
      $sql2 = "SELECT MAX(recipe.preptime) " . tableJoins() . $filter;
      $minVal = $dbHandle->querySingle($sql1);
      $maxVal = $dbHandle->querySingle($sql2);
  
      //$rootElement->addAttribute('sql', $sql1);
      //$rootElement->addAttribute('sql2', $sql2);
      $baseElement = $rootElement->addChild('options');
      $baseElement->addAttribute('type', 'numeric');
      $baseElement->addAttribute('minValue', $minVal);
      $baseElement->addAttribute('maxValue', $maxVal);
    }
    elseif ($query == "cooktime") {
      // return a list of cooktimes
      $sql = "SELECT DISTINCT recipe.cooktime " . tableJoins() . $filter . " ORDER BY recipe.cooktime ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }
    elseif ($query == "cooktimemax") {
      // return an indication that this is a numeric value, with suitable ranges
      $sql1 = "SELECT MIN(recipe.cooktime) " . tableJoins() . $filter;
      $sql2 = "SELECT MAX(recipe.cooktime) " . tableJoins() . $filter;
      $minVal = $dbHandle->querySingle($sql1);
      $maxVal = $dbHandle->querySingle($sql2);
  
      //$rootElement->addAttribute('sql', $sql1);
      //$rootElement->addAttribute('sql2', $sql2);
      $baseElement = $rootElement->addChild('options');
      $baseElement->addAttribute('type', 'numeric');
      $baseElement->addAttribute('minValue', $minVal);
      $baseElement->addAttribute('maxValue', $maxVal);
    }
    elseif ($query == "source") {
      // return a list of sources
      $sql = "SELECT DISTINCT recipe.source " . tableJoins() . $filter . " ORDER BY recipe.source ";
      addListOptions($dbHandle, $rootElement, $sql, $relativeBase, $absoluteBase);
    }


    elseif ($query == "sort") {
      // return a list of allowed sort orders
      $baseElement = $rootElement->addChild('options');
      $baseElement->addAttribute('type', 'list');

      // check if there is an existing sort order...
      // if there is, take it out of the relative and absolute bases...
      if ($orderBy != $defaultOrderBY) {
        $regex = "/sort\/[a-z]+\//";
        $relativeBase = preg_replace($regex, "", $relativeBase);
        $absoluteBase = preg_replace($regex, "", $absoluteBase);
      }

      // order by rating
      $thisEl = $baseElement->addChild('listitem');
      $thisEl->addAttribute('value', 'Rating');
      $thisEl->addAttribute('relativeuri', $relativeBase . 'rating'); // it's the default
      $thisEl->addAttribute('absoluteuri', $absoluteBase . 'rating');
      $thisEl->addAttribute('friendlyValue', 'Order by Rating (default)');
  
      // order by title
      $thisEl = $baseElement->addChild('listitem');
      $thisEl->addAttribute('value', 'Title');
      $thisEl->addAttribute('relativeuri', $relativeBase . 'title'); // it's the default
      $thisEl->addAttribute('absoluteuri', $absoluteBase . 'title');
      $thisEl->addAttribute('friendlyValue', 'Order by Recipe Title (A-Z)');
  
      // order by modification date
      $thisEl = $baseElement->addChild('listitem');
      $thisEl->addAttribute('value', 'ModTime');
      $thisEl->addAttribute('relativeuri', $relativeBase . 'modtime'); // it's the default
      $thisEl->addAttribute('absoluteuri', $absoluteBase . 'modtime');
      $thisEl->addAttribute('friendlyValue', 'Order with oldest first');
  
      // order by modification date (desc)
      $thisEl = $baseElement->addChild('listitem');
      $thisEl->addAttribute('value', 'ModTimeDesc');
      $thisEl->addAttribute('relativeuri', $relativeBase . 'modtimedesc'); // it's the default
      $thisEl->addAttribute('absoluteuri', $absoluteBase . 'modtimedesc');
      $thisEl->addAttribute('friendlyValue', 'Order with most recent first');
  
    }
}


function addRecipeSummaries($dbHandle, $recipesElement, $filter, $orderBy) {
      $sql = "SELECT DISTINCT recipe.id, recipe.title, recipe.rating, recipe.preptime, recipe.cooktime, recipe.yields, recipe.yield_unit, recipe.image ";
      $sql .= tableJoins() . $filter . $orderBy;

      $result = $dbHandle->query($sql);
      while ($output = $result->fetchArray()) {
        $thisEl = $recipesElement->addChild('recipe');
        $thisEl->addAttribute('type', 'summary');
        $thisEl->addAttribute('id', $output['id']);
        $thisEl->addAttribute('title', $output['title']);
        $thisEl->addAttribute('rating', $output['rating']);
        $thisEl->addAttribute('preptime', $output['preptime']);
        $thisEl->addAttribute('preptime_friendly', friendlyTime($output['preptime']));
        $thisEl->addAttribute('cooktime', $output['cooktime']);
        $thisEl->addAttribute('cooktime_friendly', friendlyTime($output['cooktime']));
        $thisEl->addAttribute('yields', $output['yields']);
        $thisEl->addAttribute('yield_unit', $output['yield_unit']);
        $thisEl->addAttribute('yield_friendly', $output['yields'] . ' ' . $output['yield_unit']);
        $thisEl->addAttribute('recipecard_url', getBaseUri() . '/jpg/' . $output['id']);
        $thisEl->addAttribute('abs_url', getBaseUri() . '/recipe/' . $output['id']);

        if ($output['image'] === null) {
          $thisEl->addAttribute('hasImage', '0');
        }
        else {
          $thisEl->addAttribute('hasImage', '1');
          $thisEl->addAttribute('imageUrl', getBaseUri() . '/img/' . $output['id']);
          $thisEl->addAttribute('thumbUrl', getBaseUri() . '/thumb/' . $output['id']);
        }
      }
}


function parseOpts($opts, $recipeDb) {
  $rootElement = new SimpleXMLElement_Plus("<recipeDB></recipeDB>");
  $rootElement->addAttribute('baseUri', getBaseUri());
  
  // split out the opts and parse them...
  $params = explode("/",$opts);
  
  // recognised fields/queries:
  //   fields (returns this list, not valid as a filter)
  //   recipe
  //   category
  //   cuisine
  //   ingredient
  //   servingsmin
  //   servingsmax
  //   servings   (exact match on a serving)
  //   ratingmin
  //   ratingmax
  //   rating     (exact match on a rating)
  //   preptime
  //   preptimemax
  //   cooktime
  //   cooktimemax
  //   source

  //   sort
  
  $resultCode = 200;
  
  $query = determineQueryType($opts);
  $filter = determineFilter($opts);
  $filterElement = createFilterElement($opts, $rootElement);
  $orderBy = determineOrderBy($opts);
  
  if ($filter === NULL) { $resultCode = 400; }
  if ($filterElement === NULL) { $resultCode = 400; }
  
  // connect to the database, ready...
  $dbHandle = null;
  if (!$dbHandle = new SQLite3($recipeDb, SQLITE3_OPEN_READONLY)) {
    $resultCode = 500; // couldn't open the DB.
  }
  
  if ($query == "") { $query = 'fields'; }
  
  // debugging
  //$rootElement->addAttribute('filter', $filter);
  $rootElement->addAttribute('query', $query);
  
  // append the order by to the filter.
  if ($filter == " WHERE ") {
    $filter = "";
  }
  //$filter .= $orderBy;
  
  // only process if we've been successful up to this point.
  if ($resultCode == 200) {
  
    // useful base uris for below.
    $absoluteBase = "http://" . $_SERVER['SERVER_NAME'] . $_SERVER['REQUEST_URI'];
    $relativeBase = $opts;
    if (substr($_SERVER['REQUEST_URI'],-1,1) != "/") {
      $absoluteBase .= "/";
      $relativeBase .= "/";
    }
 
    handleQuery($dbHandle, $rootElement, $absoluteBase, $relativeBase, $query, $filter);
  
    //elseif ($query == "recipe") {
    // ** always output any matching recipes
      // return a list of matching recipes
    $sql = "SELECT COUNT(DISTINCT recipe.id) " . tableJoins() . $filter; // it's a count - the order doesn't matter
    $recipesCount = $dbHandle->querySingle($sql);
  
    $recipesElement = $rootElement->addChild('recipes');
    $recipesElement->addAttribute('matched', $recipesCount);
 
    if ($recipesCount > maxRecipesToReturnFully()) { 
      addRecipeSummaries($dbHandle, $recipesElement, $filter, $orderBy);
    }
    else {
      addCompleteRecipes($dbHandle, $recipesElement, $filter, $orderBy);
    }
  }
  
  if ($resultCode == 200) {
    header('Content-type: text/xml;charset=utf-8');
    echo $rootElement->asXML();
  }
  elseif ($resultCode == 400) {
    header("HTTP/1.1 400 Bad Request");
    echo "<html><head><title>Bad Request</title></head><body><h1>Bad Request</h1><p>Your request was invalid.</p></body></html>";
  }
  else {
    header("HTTP/1.1 500 Internal Server Error");
    echo "<html><head><title>Internal Error</title></head><body><h1>Internal Error</h1><p>Unable to handle your request due to an internal error.</p></body></html>";
  }

}

function addCompleteRecipes($dbHandle, $recipesElement, $filter, $orderBy) {
      $sql = "SELECT DISTINCT recipe.id, recipe.title, recipe.instructions, recipe.modifications, recipe.cuisine, recipe.rating, ";
      $sql .= "recipe.description, recipe.source, recipe.preptime, recipe.cooktime, recipe.link, recipe.yields, recipe.yield_unit, recipe.image ";
      $sql .= tableJoins() . $filter . $orderBy . " LIMIT " . maxRecipesToReturnFully();
      //$rootElement->addAttribute('recipesql', $sql); // debug
      $result = $dbHandle->query($sql);
      while($output = $result->fetchArray()) {
        $thisEl = $recipesElement->addChild('recipe');
        $thisEl->addAttribute('type', 'full');
        $thisEl->addAttribute('id', $output['id']);
        $thisEl->addAttribute('title', $output['title']);
        $thisEl->addChild('instructions', $output['instructions']);
        $thisEl->addChild('modifications', $output['modifications']);
        $thisEl->addAttribute('cuisine', $output['cuisine']);
        $thisEl->addAttribute('rating', $output['rating']);
        $thisEl->addChild('description', $output['description']);
        $thisEl->addAttribute('source', $output['source']);
        $thisEl->addAttribute('preptime', $output['preptime']);
        $thisEl->addAttribute('preptime_friendly', friendlyTime($output['preptime']));
        $thisEl->addAttribute('cooktime', $output['cooktime']);
        $thisEl->addAttribute('cooktime_friendly', friendlyTime($output['cooktime']));
        $thisEl->addAttribute('link', $output['link']);
        $thisEl->addAttribute('yields', $output['yields']);
        $thisEl->addAttribute('yield_unit', $output['yield_unit']);
        $thisEl->addAttribute('yield_friendly', $output['yields'] . ' ' . $output['yield_unit']);
        $thisEl->addAttribute('recipecard_url', getBaseUri() . '/jpg/' . $output['id']);
        $thisEl->addAttribute('abs_url', getBaseUri() . '/recipe/' . $output['id']);


        if ($output['image'] === null) {
          $thisEl->addAttribute('hasImage', '0');
        }
        else {
          $thisEl->addAttribute('hasImage', '1');
          $thisEl->addAttribute('imageUrl', getBaseUri() . '/img/' . $output['id']);
          $thisEl->addAttribute('thumbUrl', getBaseUri() . '/thumb/' . $output['id']);
        }
    
        // categories.
        $categories = $thisEl->addChild('categories');
        $sql = "SELECT category FROM categories WHERE recipe_id=" . $output['id'];
        $result2 = $dbHandle->query($sql);
        while ($output2 = $result2->fetchArray()) {
          $categories->addChild('category', $output2['category']);
        }
    
        // ingredients
        $ingredientsEl = $thisEl->addChild('ingredients');
        $sql = "SELECT DISTINCT inggroup FROM ingredients WHERE recipe_id=" . $output['id'];
        $result2 = $dbHandle->query($sql);
        while ($output2 = $result2->fetchArray()) {
          $groupEl = $ingredientsEl->addChild('group');
          $groupEl->addAttribute('name', $output2['inggroup']);
    
          $sql = "SELECT refid, unit, amount, rangeamount, item, optional, position FROM ingredients WHERE recipe_id=";
          if ($output2['inggroup'] === null) {
            $sql .= $output['id'] . " AND inggroup IS NULL ORDER BY position";
          }
          else {
            $sql .= $output['id'] . " AND inggroup='" . $output2['inggroup'] . "' ORDER BY position";
          }
          $result3 = $dbHandle->query($sql);
          while ($output3 = $result3->fetchArray()) {
            $friendly = $output3['amount'];
            if ($output3['rangeamount']) { $friendly .= '-' . $output3['rangeamount']; }
            $friendlyAmount = $friendly . " " . $output3['unit'];
    
            $friendly = $friendlyAmount . ' ' . $output3['item'];
    
            if ($output3['optional']) { $friendly .= ' (Optional)'; }
    
            $friendly = str_replace("&", "&amp;", $friendly);
    
            $ingEl = $groupEl->addChild('ingredient', $friendly);
    
            if ($output3['refid'] != null) {
              $ingEl->addAttribute('referencedRecipeID', $output3['refid']);
              $uri = '/recipe/' . $output3['refid'];
              $ingEl->addAttribute('referencedRecipeRelativeUri', $uri);
              $ingEl->addAttribute('referencedRecipeAbsoluteUri', getBaseUri() . $uri);
            }
          }  
        }
      }
}

function returnStylesheet() {
  // stylesheet was requested...
  $user_agent = $_SERVER['USER_AGENT'];

  $xslt = "";

  if ($user_agent == "BrokenMSBrowser") {
    $xslt = file_get_contents("msbrowser.xsl");
  }
  else {
    $xslt = file_get_contents("general.xsl");
  }

  header('Content-type: text/xsl');
  echo $xslt;
}

function returnCSSStylesheet() {
  // stylesheet was requested...
  $user_agent = $_SERVER['USER_AGENT'];

  $xslt = "";

  if ($user_agent == "BrokenMSBrowser") {
    $xslt = file_get_contents("msbrowser.css");
  }
  else {
    $xslt = file_get_contents("friendly.css");
  }

  header('Content-type: text/css');
  echo $xslt;
}

function returnImage($recipeDb, $imgId) {
  // an image is being requested.
  $escaped = SQLite3::escapeString($imgId);

  //$img = "";

  if (($imgId != $escaped) or ($imgId == "")) {
    //$img = file_get_contents("img_invalid_req.jpg");
    header('Content-type: image/png');
    readfile("img_invalid_req.png");
  }
  else {
    $dbHandle = null;
    if (!$dbHandle = new SQLite3($recipeDb, SQLITE3_OPEN_READONLY)) {
      //$img = file_get_contents("img_db_error.jpg");
      header('Content-type: image/png');
      readfile("img_db_error.png");
    }
    else {
      $sql = 'SELECT image FROM recipe WHERE id=' . $imgId;
      $query = $dbHandle->querySingle($sql);
      header('Content-type: image/jpeg');
      echo $query;
    }
  }
}

function returnThumbnail($recipeDb, $imgId) {
  // an image is being requested.
  $escaped = SQLite3::escapeString($imgId);

  //$img = "";

  if ($imgId != $escaped) {
    //$img = file_get_contents("img_invalid_req.jpg");
    header('Content-type: image/jpeg');
    readfile("img_invalid_req.jpg");
  }
  else {
    $dbHandle = null;
    if (!$dbHandle = new SQLite3($recipeDb, SQLITE3_OPEN_READONLY)) {
      //$img = file_get_contents("img_db_error.jpg");
      readfile("img_db_error.jpg");
    }
    else {
      $img = $dbHandle->querySingle(
        'SELECT image FROM recipe WHERE id=' . $imgId
      );

      $im = new Imagick();
      $im->readimageblob($img);
      $im->thumbnailImage(64,64,true);
      $output = $im->getimageblob();
      $ot = $im->getFormat();
      header("Content-type: $ot");
      echo $output;
    }
  }
}


?>
