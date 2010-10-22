package uk.me.plek.kitchenRecipes;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

import org.acra.ErrorReporter;

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
		public enum QueueStatus { WAITING, SUCCESS, FAILED, CANCELLED }
		protected final View _identifier;
		protected final String _url;
		protected final DownloadListener _callback;
		protected final ImageView _dest;
		protected boolean cancelled = false;
		protected Bitmap downloadedImage = null;
		protected QueueStatus status = QueueStatus.WAITING;

		public QueueEntry(View id, ImageView dest, String url, DownloadListener callback) {
			_identifier = id;
			_url = url;
			_callback = callback;
			_dest = dest;
		}

		public View getIdentifier() { return _identifier; }
		public String getUrl() { return _url; }
		public DownloadListener getCallback() { return _callback; }
		public void setImage(Bitmap newBitmap) { downloadedImage = newBitmap; status = QueueStatus.SUCCESS; }
		public Bitmap getImage() { return downloadedImage; }
		public QueueStatus getStatus() { return status; }
		public void setStatus(QueueStatus newStatus) { status = newStatus; }
		public boolean cancelled() { return cancelled; }
		public void cancel() { cancelled = true; }
	}

	protected LinkedList<QueueEntry> downloadQueue = new LinkedList<QueueEntry>();
	protected LinkedList<QueueEntry> notifyQueue = new LinkedList<QueueEntry>();
	protected HashMap<Object,QueueEntry> identifierLookup = new HashMap<Object,QueueEntry>();
	protected final String authUser;
	protected final String authPass;
	protected final Activity _context;
	protected Thread activityThread;
	private static String threadName = "BackgroundImageLoader";

	// the runnable to service the notify queue (on the UI thread)
	private Runnable serviceNotifyQueue = new Runnable() {

		@Override
		public void run() {
			QueueEntry e = notifyQueue.poll();
			while (e != null) {
				removeItemIfQueued(e);
				DownloadListener callback = e.getCallback();

				switch (e.getStatus()) {
				case CANCELLED:
					callback.downloadCancelled(e);
					break;
				case FAILED:
					callback.downloadFailed(e);
					break;
				case SUCCESS:
					callback.downloadComplete(e, e.getImage());
					break;
				default:
					Log.e(Global.TAG, "Download Manager notify queue called on an entry in an invalid state.");
					Exception ex = new Exception("Invalid state in download manager. State=" + e.getStatus().toString());
					ErrorReporter.getInstance().handleSilentException(ex);
				}

				e = notifyQueue.poll();
			}
		}

	};

	// the runnable to run in the thread...
	private Runnable serviceDownloadQueue = new Runnable() {

		private void addToNotifyQueue(QueueEntry entry, QueueEntry.QueueStatus status) {
			entry.setStatus(status);
			notifyQueue.add(entry);
			_context.runOnUiThread(serviceNotifyQueue);
		}

		@Override
		public void run() {
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = 1;

			QueueEntry e = downloadQueue.poll();
			while (e != null) {

				HttpURLConnection c;
				try {
					c = (HttpURLConnection) new URL(e.getUrl()).openConnection();
					//c.setUseCaches(false); -- we can use caches if we like, it's just an image.
					c.connect();

					if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
						Bitmap bitmap = null;
						InputStream in = c.getInputStream();
						bitmap = BitmapFactory.decodeStream(in, null, bmOptions);

						if (bitmap == null) {
							// there was an error decoding...
							Log.w(Global.TAG, "Error decoding server sent image from URL " + e.getUrl());
							addToNotifyQueue(e, QueueEntry.QueueStatus.FAILED);
						}
						else {
							// if the entry in the identifierLookup still exists, then we weren't cancelled.
							if (e.cancelled()) {
								addToNotifyQueue(e, QueueEntry.QueueStatus.FAILED);
							}
							else {
								e.setImage(bitmap);
								addToNotifyQueue(e, QueueEntry.QueueStatus.SUCCESS);
							}
						}
					}
					else {
						// the request failed.
						Log.w(Global.TAG, "Server responded with HTTP code " + c.getResponseMessage() + " to request for URL " + e.getUrl());
						addToNotifyQueue(e, QueueEntry.QueueStatus.FAILED);

					}
				} catch (MalformedURLException e1) {
					// notify callback 
					Log.w(Global.TAG, "Malformed URL exception with url " + e.getUrl());
					addToNotifyQueue(e, QueueEntry.QueueStatus.FAILED);
				} catch (IOException e1) {
					// notify callback 
					Log.w(Global.TAG, "IOException with url " + e.getUrl());
					addToNotifyQueue(e, QueueEntry.QueueStatus.FAILED);
				}

				e = downloadQueue.poll(); // ask for the next item in the queue
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
			activityThread = new Thread(null, serviceDownloadQueue, ImageDownloadManager.threadName);
			activityThread.start();
		}
	}

	public void queueItem(QueueEntry newItem) {
		downloadQueue.add(newItem);
		identifierLookup.put(newItem.getIdentifier(), newItem);

		startDownloadThreadIfRequired();
	}

	public void removeItemIfQueued(QueueEntry itemToRemove) {
		if (itemToRemove != null) {
			itemToRemove.cancel();
			if (identifierLookup.get(itemToRemove.getIdentifier()) == itemToRemove) {
				identifierLookup.remove(itemToRemove.getIdentifier());
			}
		}
	}

	public void removeItemIfQueued(View identifier) {
		QueueEntry item = identifierLookup.get(identifier);

		removeItemIfQueued(item);
	}

}
