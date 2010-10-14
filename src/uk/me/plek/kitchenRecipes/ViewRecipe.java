package uk.me.plek.kitchenRecipes;


import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ViewRecipe extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe);
        
        WebView web = (WebView)findViewById(R.id.recipeView);
        
        final String mimetype = "text/html";
        final String encoding = "UTF-8";
        String htmldata = "<html><body>This will eventually be a recipe. !!!RECIPE_URL!!!</body></html>";
        String stylesheet = "";

        // fetch the credits from resource.
        /*{
          String data = ResourceUtils.loadResToString(R.raw.credits, this);
          if (data != null) htmldata = data;
        }
        
        // fetch the credits from resource.
        {
          String data = ResourceUtils.loadResToString(R.raw.credits_stylesheet, this);
          if (data != null) stylesheet = data;
        }*/
        
        
        
        htmldata = htmldata.replaceFirst("!!!STYLESHEET!!!", stylesheet);

        htmldata = htmldata.replaceFirst("!!!RECIPE_URL!!!", this.getIntent().getDataString());

        web.loadData(htmldata,
                     mimetype,
                     encoding);
    }
}
