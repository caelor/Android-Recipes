<?

function getBaseUri() {
  // work out our address
  $baseUri = "http://";
  $opts = $_SERVER['QUERY_STRING'];
  if ($_SERVER['HTTPS']) { $baseUri = "https://"; }
  $baseUri .= $_SERVER['HTTP_HOST'];
  $baseUri .= str_replace($opts, "", $_SERVER['REQUEST_URI']);
  if (substr($baseUri, -1, 1) == "/") { 
    $baseUri = substr($baseUri, 0, strlen($baseUri) - 1 );
  }
  return $baseUri;
}

?>
