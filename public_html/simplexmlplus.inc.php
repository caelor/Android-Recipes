<?php

/* From http://php.net/manual/en/class.simplexmlelement.php#95229 */

class SimpleXMLElement_Plus extends SimpleXMLElement { 

    public function addProcessingInstruction( $name, $value ) 
    { 
        // Create a DomElement from this simpleXML object 
        $dom_sxe = dom_import_simplexml($this); 
        
        // Create a handle to the owner doc of this xml 
        $dom_parent = $dom_sxe->ownerDocument; 
        
        // Find the topmost element of the domDocument 
        $xpath = new DOMXPath($dom_parent); 
        $first_element = $xpath->evaluate('/*[1]')->item(0); 
        
        // Add the processing instruction before the topmost element            
        $pi = $dom_parent->createProcessingInstruction($name, $value); 
        $dom_parent->insertBefore($pi, $first_element); 
    } 
} 

?>
