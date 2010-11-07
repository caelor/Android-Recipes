package uk.me.plek.kitchenRecipes;

import org.acra.CrashReportingApplication;
import org.acra.ErrorReporter;

public class RecipeBrowserApplication extends CrashReportingApplication {

	public RecipeBrowserApplication() {
		super();
		
		ErrorReporter.getInstance().addCustomData("Release", Global.RELEASE);
	}
	
	@Override
	public String getFormId() {
		return "dE1LRHgxNktCaTg4ekpBUU5LQ2x1VEE6MQ";
	}

}
