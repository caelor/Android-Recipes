package uk.me.plek.kitchenRecipes;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Class to manage a download queue, primarily for downloading thumbnails for recipes
 * 
 * @author Andy Boff
 * 
 */
public class ImageDownloadManager {
	
	/**
	 * Callback interface for the download manager
	 *  
	 * @author Andy Boff
	 *
	 */
	public interface DownloadListener {
		public void downloadComplete(QueueEntry queueEntry, Bitmap imageBitmap);
		public void downloadCancelled(QueueEntry queueEntry);
		public void downloadFailed(QueueEntry queueEntry);
	}
	
	/**
	 * Definition of an entry in the queue
	 * @author Andy Boff
	 *
	 */
	public static class QueueEntry {
		protected final View _identifier;
		protected final String _url;
		protected final DownloadListener _callback;
		protected final ImageView _dest;
		protected boolean cancelled = false;
		protected Bitmap downloadedImage = null;
		
		public QueueEntry(View id, ImageView dest, String url, DownloadListener callback) {
			_identifier = id;
			_url = url;
			_callback = callback;
			_dest = dest;
		}
		
		public Runnable notifyCallbackOfSuccess = new Runnable() {
			@Override
			public void run() {
				_dest.setImageBitmap(downloadedImage);
				_callback.downloadComplete(QueueEntry.this, downloadedImage);
			}
		};
		public Runnable notifyCallbackOfFailure = new Runnable() {
			@Override
			public void run() {
				_callback.downloadFailed(QueueEntry.this);
			}
		};
		public Runnable notifyCallbackOfCancel = new Runnable() {
			@Override
			public void run() {
				_callback.downloadCancelled(QueueEntry.this);
			}
		};
		
		public View getIdentifier() { return _identifier; }
		public String getUrl() { return _url; }
		public DownloadListener getCallback() { return _callback; }
		public boolean isCancelled() { return cancelled; }
		public void cancel() { cancelled = true; }
		public void setImage(Bitmap newBitmap) { downloadedImage = newBitmap; }
		public Bitmap getImage() { return downloadedImage; }
	}
	
	protected LinkedList<QueueEntry> queue = new LinkedList<QueueEntry>();
	protected HashMap<Object,QueueEntry> identifierLookup = new HashMap<Object,QueueEntry>();
	protected final String authUser;
	protected final String authPass;
	protected final Activity _context;
	protected Thread activityThread;
	private static String threadName = "BackgroundImageLoader";
	
	// the runnable to run in the thread...
	private Runnable doServiceQueue = new Runnable() {

		@Override
		public void run() {
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = 1;

			QueueEntry e = queue.poll();
			while (e != null) {
				
				HttpURLConnection c;
				try {
					c = (HttpURLConnection) new URL(e.getUrl()).openConnection();
					c.setUseCaches(false);
					c.connect();
					
					if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
						Bitmap bitmap = null;
						InputStream in = c.getInputStream();
						bitmap = BitmapFactory.decodeStream(in, null, bmOptions);
						
						if (bitmap == null) {
							// there was an error decoding...
							Log.w(Global.TAG, "Error decoding server sent image from URL " + e.getUrl());
							_context.runOnUiThread(e.notifyCallbackOfFailure);
						}
						else {
							// if the entry in the identifierLookup still exists, then we weren't cancelled.
							if (identifierLookup.containsValue(e)) {
								identifierLookup.remove(e.getIdentifier());
								_context.runOnUiThread(e.notifyCallbackOfSuccess);
							}
							else {
								_context.runOnUiThread(e.notifyCallbackOfCancel);
							}
						}
					}
					else {
						// the request failed.
						Log.w(Global.TAG, "Server responded with HTTP code " + c.getResponseMessage() + " to request for URL " + e.getUrl());
						e.getCallback().downloadFailed(e);
					}
				} catch (MalformedURLException e1) {
					// notify callback 
					Log.w(Global.TAG, "Malformed URL exception with url " + e.getUrl());
					_context.runOnUiThread(e.notifyCallbackOfFailure);
				} catch (IOException e1) {
					// notify callback 
					Log.w(Global.TAG, "IOException with url " + e.getUrl());
					_context.runOnUiThread(e.notifyCallbackOfFailure);
				}

				e = queue.poll(); // ask for the next item in the queue
			}
		}
		
	};
	
	public ImageDownloadManager(Activity context) {
		super();
		
		_context = context;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context.getBaseContext());
		authUser = prefs.getString("authUsername", "");
		authPass = prefs.getString("authPassword", "");

		Authenticator.setDefault(new BasicAuthenticator(this.authUser, this.authPass));
	}
	
	private void startDownloadThreadIfRequired() {
		boolean startThread = false;
		if (activityThread == null) { 
			startThread = true;
		}
		else if (activityThread.isAlive() == false) {
			startThread = true;
		}

		if (startThread) {
			activityThread = new Thread(null, doServiceQueue, ImageDownloadManager.threadName);
			activityThread.start();
		}
	}

	public void queueItem(QueueEntry newItem) {
		queue.add(newItem);
		identifierLookup.put(newItem.getIdentifier(), newItem);
		
		startDownloadThreadIfRequired();
	}

	public boolean removeItemIfQueued(QueueEntry itemToRemove) {
		boolean retval = queue.remove(itemToRemove);
		
		if (retval) {
			identifierLookup.remove(itemToRemove.getIdentifier());
		}
		
		return retval; 
	}
	
	public boolean removeItemByIdentifierIfQueued(Object identifier) {
		QueueEntry item = identifierLookup.get(identifier);
		
		return removeItemIfQueued(item);
	}

}
