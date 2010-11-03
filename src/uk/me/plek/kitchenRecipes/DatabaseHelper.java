package uk.me.plek.kitchenRecipes;

import java.sql.Blob;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper {
	private static final String DB_NAME = "recipes.db";
	private static final int DB_VERSION = 1;
	
	// field names
	public static final String RECIPE_TITLE = "Title";
	public static final String RECIPE_STARTTIME = "StartTime";
	public static final String RECIPE_IMAGE = "Image";
	public static final String RECIPE_URI = "SourceURI";

	private Activity context;
	private SQLiteDatabase db = null;
	private DatabaseEventListener callback;

	public interface DatabaseEventListener {
		public void databaseOpenedCallback();
	}
	
	/**
	 * Helper class to support creating and upgrading the database
	 * 
	 * @author Andy Boff
	 *
	 */
	private static class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String sql;
			
			sql = "CREATE TABLE activerecipes (";
			sql = sql +  android.provider.BaseColumns._ID + " INTEGER PRIMARY KEY, ";
			sql = sql + " uniqueId TEXT,";
			sql = sql + " Title TEXT,";
			sql = sql + " StartTime TIMESTAMP, ";
			sql = sql + " RecipeXML TEXT, ";
			sql = sql + " SourceURI TEXT, ";
			sql = sql + " Image BLOB";
			sql = sql + ")";
			db.execSQL(sql);
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			int currVersion = oldVersion;
			//String sql;
			
			if ((currVersion < 1) && (newVersion >= 1)) {
				// steps to upgrade to schema 1.
				onCreate(db);
				
				currVersion = 1;
			}
			
			if ((currVersion < 2) && (newVersion >= 2)) {
				// steps to upgrade to schema 2.
				db.beginTransaction();
				
				db.endTransaction();
				
				currVersion = 2;
			}
		}
	}
	
	private Runnable doDBOpenWritable = new Runnable() {

		@Override
		public void run() {
			OpenHelper openHelper = new OpenHelper(context);
			db = openHelper.getWritableDatabase();
			context.runOnUiThread(doDBOpenCallback);
		}
		
	};
	
	private Runnable doDBOpenCallback = new Runnable() {

		@Override
		public void run() {
			callback.databaseOpenedCallback();
		}
		
	};

	public DatabaseHelper(Activity context, DatabaseEventListener callback) {
		this.context = context;
		this.callback = callback;
		Thread openDBThread = new Thread(null, doDBOpenWritable, "OpenPersistentStorage");
		openDBThread.start();
	}
	
	public void close() {
		if (db != null) {
			if (db.isOpen()) {
				db.close();
			}
		}
	}
	
	public boolean isReady() {
		return (db != null);
	}
	
	public Cursor getActiveRecipesCursor() {
		Cursor retval = this.db.query("activerecipes", new String[] { 
				android.provider.BaseColumns._ID, DatabaseHelper.RECIPE_TITLE, 
				DatabaseHelper.RECIPE_STARTTIME, DatabaseHelper.RECIPE_URI
		}, null, null, null, null, "StartTime");
		return retval;
	}
	
	public boolean isRecipeActive(String recipeDataString) {
		Cursor curs = this.db.query("activerecipes", new String[] { "Title", "SourceURI" }, "SourceURI = ?", new String[] { recipeDataString }, null, null, null);
		if (curs.getCount() > 0) {
			curs.close();
			return true;
		}
		else {
			curs.close();
			return false;
		}
	}
	
	public void addActiveRecipe(String uid, String title, String xml, String sourceUri, Blob image) {
		ContentValues cv = new ContentValues();
		cv.put("uniqueId", uid);
		cv.put("Title", title);
		cv.put("RecipeXML", xml);
		cv.put("SourceURI", sourceUri);
		
		Date now = new Date();
		cv.put("StartTime", now.getTime());
		
		/*long rowId = */db.insert("activerecipes", "Title", cv);
	}
	
	public String getXMLForRecipeBySourceUri(String sourceUri) {
		Cursor curs = this.db.query("activerecipes", new String[] { "RecipeXML" }, "SourceURI=?", new String[] { sourceUri }, null, null, null);
		
		String retval = null;
		
		if (curs.getCount() > 0) {
			curs.moveToFirst();
			retval = curs.getString(0);
		}
		
		return retval;
	}
	
	public String getSourceUriByRowId(long rowId) {
		Cursor curs = this.db.query("activerecipes", new String[] { "SourceURI" }, android.provider.BaseColumns._ID + "=?", new String[] { String.valueOf(rowId) }, null, null, null);
		
		String retval = null;
		
		if (curs.getCount() > 0) {
			curs.moveToFirst();
			retval = curs.getString(0);
		}
		
		return retval;
	}
}
