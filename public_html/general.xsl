<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="recipeDB">
    <html><head>
      <title>
        <!-- Create a conditional here - if there is only a single 
             returned recipe, then it becomes
             the title. Otherwise, the title should be something 
             else pretty. -->
        <xsl:choose><xsl:when test="number(recipes/@matches=1)">
          Recipes - <xsl:value-of select="recipes/recipe/@title"/>
        </xsl:when><xsl:otherwise>
          <xsl:choose><xsl:when test="@query='fields'">
            Recipes - Search
          </xsl:when><xsl:otherwise>
            Recipes - Define Search Parameter
          </xsl:otherwise></xsl:choose>
        </xsl:otherwise></xsl:choose>
      </title>

      <!-- insert CSS reference here -->

    </head><body>
      <xsl:choose><xsl:when test="@query='fields'">


        <!-- display the list of existing filters -->
        <xsl:choose><xsl:when test="count(filter/filterItem)>0">
          <h3>Applied Filters</h3>
  
          <table>
            <th><td>Field</td><td>Value</td><td/></th>
            <xsl:apply-templates select="filter/filterItem"/>
          </table>
        </xsl:when></xsl:choose>
  
        <!-- display of recipes -->
        <!-- just display a list if there is more than 1, or a formatted 
             recipe if there is only 1 -->
        <xsl:choose><xsl:when test="number(recipes/@matched=1)">
          <!-- there is no point being able to add more filters 
               if there's only 1 matching recipe -->
  
          <!-- display a formatted recipe -->
          <hr/>
          <div id="recipe">
            <h2><xsl:value-of select="recipes/recipe/@title"/></h2>
          </div>
        </xsl:when><xsl:otherwise>
          <ul>
            <xsl:apply-templates select="availableFields/field"/>
          </ul>
  
          <!-- display a list of recipes -->
          <div id="recipes">
            <table>
              <tr>
                <th>Title</th>
                <th>Cuisine</th>
                <th>Categories</th>
                <th>PrepTime</th>
                <th>CookTime</th>
              </tr>
              <xsl:apply-templates select="recipes/recipe"/>
            </table>
          </div>
  
        </xsl:otherwise></xsl:choose>


      </xsl:when><xsl:otherwise>

        <!-- List the options for this particular field -->
        <xsl:if test="options/@type='list'">
          <ul>
            <xsl:apply-templates select="options/listitem"/>
          </ul>
        </xsl:if>
        <xsl:if test="options/@type='numeric'">
          <input type="range" id="numericInput">
            <xsl:attribute name="min"><xsl:value-of select="options/@minValue"/></xsl:attribute>
            <xsl:attribute name="max"><xsl:value-of select="options/@maxValue"/></xsl:attribute>
            <xsl:attribute name="onchange">
document.getElementById('feedback').innerHTML=this.value;
document.getElementById('link').target="<xsl:value-of select="@query"/>/" + this.value
            </xsl:attribute>
          </input>
          <div id="feedback"/>
          <a id="link">
            <xsl:attribute name="href"><xsl:value-of select="@query"/></xsl:attribute>
            Enter
          </a>
        </xsl:if>

      </xsl:otherwise></xsl:choose>

    </body></html>
  </xsl:template>

  <xsl:template match="filterItem">
    <tr>
      <td><xsl:value-of select="@field"/></td>
      <td><xsl:value-of select="@value"/></td>
      <td>
        <a>
          <xsl:attribute name="href">
            <xsl:value-of select="@deletedAbsoluteUri"/>
          </xsl:attribute>
          Remove
        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="recipes/recipe">
    <tr>
      <td>
        <a>
          <xsl:attribute name="href">recipe/<xsl:value-of select="@id"/></xsl:attribute>
<xsl:value-of select="@title"/>
        </a>
      </td>
      <td><xsl:value-of select="@cuisine"/></td>
      <td>
        <xsl:for-each select="categories/category">
          <xsl:if test="position() > 1">, <br/></xsl:if>
          <xsl:value-of select="."/>
        </xsl:for-each>
      </td>
      <td><xsl:value-of select="@preptime_friendly"/></td>
      <td><xsl:value-of select="@cooktime_friendly"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="availableFields/field">
    <li><a>
      <xsl:attribute name="href"><xsl:value-of select="@absoluteuri"/></xsl:attribute>
      <xsl:value-of select="@name"/>
    </a></li>
  </xsl:template>

  <xsl:template match="options/listitem">
    <li><a>
      <xsl:attribute name="href"><xsl:value-of select="@relativeuri"/></xsl:attribute>
      <xsl:value-of select="@value"/>
    </a></li>
  </xsl:template>
</xsl:stylesheet>
