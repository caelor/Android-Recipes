package uk.me.plek.kitchenRecipes;


import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ViewCredits extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.credits);
        
        WebView web = (WebView)findViewById(R.id.creditsView);
        
        final String mimetype = "text/html";
        final String encoding = "UTF-8";
        String htmldata = "<html><body>There was an error loading the credits.</body></html>";
        String stylesheet = "";

        // fetch the credits from resource.
        {
          String data = ResourceUtils.loadResToString(R.raw.credits, this);
          if (data != null) htmldata = data;
        }
        
        // fetch the credits from resource.
        {
          String data = ResourceUtils.loadResToString(R.raw.credits_stylesheet, this);
          if (data != null) stylesheet = data;
        }
        
        
        
        htmldata = htmldata.replaceFirst("!!!STYLESHEET!!!", stylesheet);

        web.loadData(htmldata,
                     mimetype,
                     encoding);
    }
}
