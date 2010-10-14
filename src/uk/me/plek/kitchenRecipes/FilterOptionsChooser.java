package uk.me.plek.kitchenRecipes;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FilterOptionsChooser implements OnClickListener, OnSeekBarChangeListener, android.view.View.OnClickListener {
	
	private class ListItem {
		private final String _value;
		private final String _url;
		private final String _friendly;
		public ListItem(String value, String url, String friendly) { _value = value; _url = url; _friendly = friendly; }  
		public String getValue() { return _value; }
		public String getUrl() { return _url; }
		public String getFriendly() { return _friendly; }
	}

	protected final Activity _context;
	private final String _requestUrl;
	private final String baseUrl;
	private final String authUser;
	private final String authPass;
	private Runnable doRequest;
	private ProgressDialog progressDialog;
	private ArrayList<ListItem> listOptions = null;
	private final String dialogTitle;
	private AlertDialog listOptionsDialog = null;
	private Dialog numericDialog = null;
	private final FilterChooserCallback _callback;
	private int numericMin = 0;
	private int numericMax = 0;
	private String parameterType;
	private int seekBarValue = 0;
	
	private Button dialogOkButton = null;
	private TextView dialogValueLabel = null;
	
	
	public FilterOptionsChooser(Activity parent, String requestUrl, String title, FilterChooserCallback callback) {
		super();
		
		_context = parent;
		dialogTitle = title;
		_callback = callback;
		_requestUrl = requestUrl;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context.getBaseContext());
		baseUrl = prefs.getString("recipesUrl", "") + requestUrl;
		authUser = prefs.getString("authUsername", "");
		authPass = prefs.getString("authPassword", "");

		doRequest = new Runnable() {
			@Override
			public void run() {
				if (!makeServerRequest()) {
					Log.e(Global.TAG, "Unable to complete server request.");
				}
			}
		};

		progressDialog = ProgressDialog.show(_context, "", "Please wait ...", true);

		Thread thread = new Thread(null, doRequest, "BackgroundRecipeFetch");
		thread.start();
	}
	
	private boolean makeServerRequest() {
		boolean retVal = true;

		try {

			Authenticator.setDefault(new BasicAuthenticator(this.authUser, this.authPass));

			HttpURLConnection c = (HttpURLConnection) new URL(this.baseUrl).openConnection();
			c.setUseCaches(false);
			c.connect();

			parseResponse(c.getInputStream());

		} catch (MalformedURLException e) {
			retVal = false;
			Log.e(Global.TAG, "Malformed URL exception: " + this.baseUrl);
			e.printStackTrace();

		} catch (IOException e) {
			retVal = false;
			Log.e(Global.TAG, "IOException loading recipes");
			e.printStackTrace();

		} catch (ParserConfigurationException e) {
			retVal = false;
			e.printStackTrace();

		} catch (SAXException e) {
			retVal = false;
			e.printStackTrace();
		} finally {
			_context.runOnUiThread(dismissDialog);
		}

		return retVal;
	}
	
	/* Response parsing */
	private void parseResponse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(is);
		Element root = doc.getDocumentElement();

		NodeList topNodes = root.getChildNodes();
		for (int foo = 0; foo < topNodes.getLength(); foo++) {
			Node n = topNodes.item(foo);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element)n;
				String tagName = e.getTagName();

				if (tagName.equals("options")) {
					// handle the options
					parameterType = e.getAttribute("type");
					
					if (parameterType.equals("list")) {
						// it's a list of items
						
						listOptions = new ArrayList<ListItem>();
						
						NodeList nl = e.getChildNodes();
						for (int foo2 = 0; foo2 < nl.getLength(); foo2++) {
							Node n2 = nl.item(foo2);
							if (n2.getNodeType() != Node.ELEMENT_NODE) {
								Log.w(Global.TAG, "Unexpected non-element found in options: " + n2.toString());
							}
							else {
								Element listitem = (Element)n2;
								if (listitem.getTagName().equals("listitem")) {
									listOptions.add(new ListItem(
											listitem.getAttribute("value"),
											listitem.getAttribute("relativeuri"),
											listitem.getAttribute("friendlyValue")
											));
								}
								else {
									Log.w(Global.TAG, "Unexpected non-listitem found in list options: " + listitem.getTagName());
								}
							}
						}
					}
					else if (parameterType.equals("numeric")) {
						// it's a numeric entry
						try {
							this.numericMin = Integer.valueOf(e.getAttribute("minValue"));
							this.numericMax = Integer.valueOf(e.getAttribute("maxValue"));
						} catch (NumberFormatException e1) {
							Log.e(Global.TAG, "Invalid number format exception while converting min/max values for parameter.");
						}
					}
					else {
						Log.w(Global.TAG, "Unknown option type: " + parameterType);
					}

				}
			}
			else {
				Log.w(Global.TAG, "Unexpected Node found in XML Level 1: " + n.toString());
			}
		}



		// make sure UI updates get run on the UI thread.
		_context.runOnUiThread(syncUI);
	}
	
	private Runnable syncUI = new Runnable() {
		@Override
		public void run() {
			progressDialog.dismiss(); // dismiss the please wait dialog...
			
			if (listOptions != null) {
				// display a list of the options...
				AlertDialog.Builder builder = new AlertDialog.Builder(_context);
				builder.setTitle(dialogTitle);
				builder.setItems(getAvailableFieldsForDialog(), FilterOptionsChooser.this);
				listOptionsDialog = builder.create();
				listOptionsDialog.show();
			}
			else if (parameterType.equals("numeric")) {
				numericDialog = new Dialog(_context);
				numericDialog.setContentView(R.layout.slider_dialog);
				numericDialog.setTitle(dialogTitle);
				numericDialog.setCancelable(false);
				
				SeekBar s = (SeekBar)numericDialog.findViewById(R.id.DialogSeekBar);
				s.setOnSeekBarChangeListener(FilterOptionsChooser.this);
				s.setMax(numericMax - numericMin);
				s.setProgress((int)((numericMax - numericMin) / 2));
				
				TextView minLabel = (TextView)numericDialog.findViewById(R.id.DialogSeekMinimum);
				TextView maxLabel = (TextView)numericDialog.findViewById(R.id.DialogSeekMaximum);
				dialogValueLabel = (TextView)numericDialog.findViewById(R.id.DialogSeekCurrent);
				dialogOkButton = (Button)numericDialog.findViewById(R.id.DialogOkButton);
				
				dialogValueLabel.setText(Integer.toString(s.getProgress() + numericMin));
				minLabel.setText(Integer.toString(numericMin));
				maxLabel.setText(Integer.toString(numericMax));
				
				dialogOkButton.setOnClickListener(FilterOptionsChooser.this);
				numericDialog.show();
			}
			else {
				Log.w(Global.TAG, "Unknown paremeter type: " + parameterType);
			}
		}

		private CharSequence[] getAvailableFieldsForDialog() {
			CharSequence[] retval = new CharSequence[listOptions.size()];
			
			for (int foo = 0; foo < listOptions.size(); foo++) {
				String bar = listOptions.get(foo).getFriendly();
				if ("".equals(bar) || (bar == null)) {
					bar = listOptions.get(foo).getValue();
				}
				retval[foo] = bar;
			}
			
			return retval;
		}

	};
	
	private Runnable dismissDialog = new Runnable() {
		@Override
		public void run() {
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}

	};

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (dialog == this.listOptionsDialog) {
			// the which is the option number...
			dialog.dismiss();
			String url = this.listOptions.get(which).getUrl();
			_callback.filterParameterChosen(this, url);
		}
		else if (dialog == this.numericDialog) {
			dialog.dismiss();
			String url = this._requestUrl + "/" + Integer.toString(which);
			_callback.filterParameterChosen(this, url);
		}
		else {
			Log.w(Global.TAG, "Dialog click event received with unknown dialog.");
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		this.seekBarValue = seekBar.getProgress();
		if (this.dialogValueLabel != null) {
			this.dialogValueLabel.setText(Integer.toString(numericMin + this.seekBarValue));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// ignore
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// ignore
	}

	@Override
	public void onClick(View arg0) {
		// called by button widget.
		
		if (arg0 == this.dialogOkButton) {
			this.onClick(this.numericDialog, this.seekBarValue + this.numericMin);
		}
		else {
			Log.w(Global.TAG, "Unknown button click event received.");
		}
		
	}
}

