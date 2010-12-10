<?php

function outputRecipeJPG($recipeId, $uri, $jpgCache, $maxAge) {

  $createJpg = true; // assume we need to create one
  $jpgFilename = $jpgCache . '/' . $recipeId . '.jpg';

  // see if we have a cached version
  if (file_exists( $jpgFilename ) ) {
    $lastmod = filemtime( $jpgFilename );
    $age = time() - $lastmod;
    if ($age < $maxAge) {
      $createJpg = false;
    }
  }

  if ($createJpg == true) {
    $cmd = 'cutycapt "' . $uri . '" "' . $jpgFilename . '"';
    system($cmd);
  }

  if (file_exists( $jpgFilename) ) {
    header('Content-type: image/jpeg');
    echo file_get_contents($jpgFilename);    
  }
  else {
    header('Content-type: text/plain');
    echo "500\n";
    echo "Unable to service your request. Please try again later.";
  }

}


?>
