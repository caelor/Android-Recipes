package uk.me.plek.kitchenRecipes;

import java.sql.Blob;
import java.util.Date;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateUtils;

public class DatabaseHelper {
	private static final String DB_NAME = "recipes.db";
	private static final int DB_VERSION = 2;

	// field names
	public static final String RECIPE_TITLE = "Title";
	public static final String RECIPE_STARTTIME = "StartTime";
	public static final String RECIPE_IMAGE = "Image";
	public static final String RECIPE_URI = "SourceURI";

	private Activity context;
	private SQLiteDatabase db = null;
	private DatabaseEventListener callback;
	private Notification statusBarNotification;

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

			sql = "CREATE TABLE requestCache (";
			sql = sql +  android.provider.BaseColumns._ID + " INTEGER PRIMARY KEY, ";
			sql = sql + " requestUri TEXT,";
			sql = sql + " inProgress BOOLEAN,";
			sql = sql + " responseXML TEXT, ";
			sql = sql + " timestamp TIMESTAMP ";
			sql = sql + ")";
			db.execSQL(sql);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			int currVersion = oldVersion;
			String sql;

			if ((currVersion < 1) && (newVersion >= 1)) {
				// steps to upgrade to schema 1.
				onCreate(db);

				currVersion = 1;
			}

			if ((currVersion < 2) && (newVersion >= 2)) {
				// steps to upgrade to schema 2.
				db.beginTransaction();

				sql = "CREATE TABLE requestCache (";
				sql = sql +  android.provider.BaseColumns._ID + " INTEGER PRIMARY KEY, ";
				sql = sql + " requestUri TEXT,";
				sql = sql + " inProgress BOOLEAN,";
				sql = sql + " responseXML TEXT, ";
				sql = sql + " timestamp TIMESTAMP ";
				sql = sql + ")";
				db.execSQL(sql);
				
				db.setTransactionSuccessful();
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
	
	public int getNumberOfActiveRecipes() {
		Cursor c = this.db.query("activerecipes", new String[] { 
				"COUNT(1)"
		}, null, null, null, null, null);
		if (c.moveToFirst()) {
			int retval = c.getInt(0);
			c.close();
			return retval;
		}
		else {
			c.close();
			return -1;
		}
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
		
		curs.close();

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

	public void updateNotificationMessage(Context parent) {
		NotificationManager nm = (NotificationManager)parent.getSystemService(Context.NOTIFICATION_SERVICE);

		// recipe count...
		int recipeCount = 0;
		CharSequence ticker = "There are active recipes";
		boolean shouldRemove = false;

		if (db != null) {
			Cursor c = db.query("activerecipes", new String[] { "COUNT(1)" }, null, null, null, null, null);
			if (c.moveToFirst()) {
				recipeCount = c.getInt(0);

				if (recipeCount == 0) {
					shouldRemove = true;
				}
				else if (recipeCount == 1) {
					ticker = "There is 1 active recipe.";
				}
				else {
					ticker = "There are " + String.valueOf(recipeCount) + " active recipes";
				}
			}
			c.close();
		}


		if (!shouldRemove) {
			if (statusBarNotification == null) {
				statusBarNotification = new Notification(
						R.drawable.seal, 
						ticker, 
						System.currentTimeMillis());
			}

			// set up some intents
			// we use application contexts so it's the same notification no matter which activity we
			// got called from
			Intent notificationIntent = new Intent(parent.getApplicationContext(), ActiveRecipes.class);
			notificationIntent.setAction(ActiveRecipes.ACTION_STATUS_CALLBACK);
			PendingIntent contentIntent = PendingIntent.getActivity(parent.getApplicationContext(), 0, notificationIntent, 0);

			statusBarNotification.flags |= Notification.FLAG_ONGOING_EVENT;
			statusBarNotification.number = recipeCount;
			statusBarNotification.setLatestEventInfo(
					parent, 
					"Recipe Viewer", 
					ticker, 
					contentIntent);
			nm.notify(1, statusBarNotification);
		}
		else {
			nm.cancel(1);
			if (statusBarNotification != null) {
				statusBarNotification = null;
			}
		}
	}
	
	public void deleteRecipeByUri(String uri) {
		if (db != null) {
			db.delete("activerecipes", "SourceURI=?", new String[] { uri });
		}
	}
	
	public static CharSequence decodeStartDate(Context context, long millis) {
		CharSequence retval = "Start Time Unknown";
		
		int flags = 0;
		flags |= DateUtils.FORMAT_SHOW_TIME;
		//flags |= DateUtils.FORMAT_SHOW_WEEKDAY;
		//flags |= DateUtils.FORMAT_SHOW_DATE;
		flags |= DateUtils.FORMAT_ABBREV_RELATIVE;
		
		retval = "Recipe started " + DateUtils.getRelativeDateTimeString(
				context, 
				millis, 
				DateUtils.SECOND_IN_MILLIS, 
				DateUtils.WEEK_IN_MILLIS, 
				flags);
	
		return retval;
	}
	
	public static CharSequence decodeCachedDate(Context context, long millis) {
		CharSequence retval = "Start Time Unknown";
		
		int flags = 0;
		flags |= DateUtils.FORMAT_SHOW_TIME;
		//flags |= DateUtils.FORMAT_SHOW_WEEKDAY;
		//flags |= DateUtils.FORMAT_SHOW_DATE;
		flags |= DateUtils.FORMAT_ABBREV_RELATIVE;
		
		retval = " [cached " + DateUtils.getRelativeDateTimeString(
				context, 
				millis, 
				DateUtils.SECOND_IN_MILLIS, 
				DateUtils.WEEK_IN_MILLIS, 
				flags) + "]";
	
		return retval;
	}
	
	public String getCachedServerResponse(String requestUri) {
		Cursor curs = this.db.query("requestCache", 
				new String[] { "responseXML" }, 
				"RequestUri=?", new String[] { requestUri }, null, null, null, "1");

		String retval = null;

		if (curs.getCount() > 0) {
			curs.moveToFirst();
			retval = curs.getString(0);
		}

		return retval;

	}

	public CharSequence getCachedServerResponseTimestamp(Context context, String requestUri) {
		Cursor curs = this.db.query("requestCache", 
				new String[] { "timestamp" }, 
				"RequestUri=?", new String[] { requestUri }, null, null, null, "1");

		String retval = null;

		if (curs.getCount() > 0) {
			curs.moveToFirst();
			long timestamp = curs.getLong(0);
			return DatabaseHelper.decodeCachedDate(context, timestamp);
		}

		return retval;

	}

	public void deletedOldServerResponses(long maxAge) {
		Date nowDate = new Date();
		long nowLong = nowDate.getTime();
		long limit = nowLong - maxAge;
		
		this.db.delete("requestCache", "timestamp < ?", new String[] { String.valueOf(limit) });
	}
	
	public void addServerResponse(String requestUri, String responseXML) {
		Date nowDate = new Date();
		long nowLong = nowDate.getTime();
		
		ContentValues cv = new ContentValues();
		cv.put("requestUri", requestUri);
		cv.put("responseXML", responseXML);
		cv.put("timestamp", nowLong);
		
		this.db.insert("requestCache", "timestamp", cv);
	}
}
