package uk.me.plek.kitchenRecipes;

import java.util.ArrayList;

import uk.me.plek.kitchenRecipes.ImageDownloadManager.DownloadListener;
import uk.me.plek.kitchenRecipes.ImageDownloadManager.QueueEntry;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class RecipeAdapter extends ArrayAdapter<BasicRecipe> implements DownloadListener {

	protected ArrayList<BasicRecipe> items;
	private final Activity _context;
	protected final ImageDownloadManager downloadManager;
	

	public RecipeAdapter(Activity context, int textViewResourceId, ArrayList<BasicRecipe> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		this._context = context;
		
		this.downloadManager = new ImageDownloadManager(_context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.recipe_row, null);
		}
		else {
			
		}
		
		BasicRecipe r = items.get(position);
		if (r != null) {
			ImageView preview = (ImageView) v.findViewById(R.id.RecipeIcon);
			TextView title = (TextView) v.findViewById(R.id.RecipeTitle);
			TextView prepTime = (TextView) v.findViewById(R.id.RecipePrepTime);
			TextView cookTime = (TextView) v.findViewById(R.id.RecipeCookTime);
			TextView yield = (TextView) v.findViewById(R.id.RecipeYield);
			RatingBar rating = (RatingBar) v.findViewById(R.id.RecipeRating);
			
			if (preview != null) {
				// initially, set a random pepper, while the image gets loaded in the background
				// avoid a bug in android, and use the layout file to set the drawable
				//preview.setImageDrawable(v.getResources().getDrawable(R.drawable.random_pepper));
				preview.setImageLevel((int)(Math.random() * 100) + 1);
				
				if (r.imageUrl != null) {
			    	ImageDownloadManager.QueueEntry qe = new ImageDownloadManager.QueueEntry(v, preview, r.thumbUrl, this);
			    	downloadManager.queueItem(qe);

					//BitmapFactory.Options bmOptions = new BitmapFactory.Options();
					//bmOptions.inSampleSize = 1;
					//Bitmap bm = LoadImage(baseUrl + r.imageUrl, bmOptions);
					//preview.setImageBitmap(bm);
				}
			}
			if (title != null) {
				title.setText(r.getTitle());
			}
			if (prepTime != null) {
				if (r.getPrepTimeFriendly().length() == 0) {
					prepTime.setVisibility(TextView.GONE);
				}
				else {
					prepTime.setVisibility(TextView.VISIBLE);
					prepTime.setText("Prep Time: " + r.getPrepTimeFriendly());
				}
			}
			if (cookTime != null) {
				if (r.getCookTimeFriendly().length() == 0) {
					cookTime.setVisibility(TextView.GONE);
				}
				else {
					cookTime.setVisibility(TextView.VISIBLE);
					cookTime.setText("Cook Time: " + r.getCookTimeFriendly());
				}
			}
			if (yield != null) {
				if (r.getYieldFriendly().length() == 0) {
					yield.setVisibility(TextView.GONE);
				}
				else {
					yield.setVisibility(TextView.VISIBLE);
					yield.setText("Yield: " + r.getYieldFriendly());
				}
			}
			if (rating != null) {
				if (r.getRating().length() == 0) {
					rating.setVisibility(RatingBar.GONE);
				}
				else {
					rating.setVisibility(RatingBar.VISIBLE);
					String ratingString = r.getRating();
					int actualRating = Integer.valueOf(ratingString);
					rating.setMax(10);
					rating.setProgress(actualRating);
				}
			}
		}
		return v;
	}
	
	@Override
	public BasicRecipe getItem(int position) {
		return items.get(position);
	}

	@Override
	public void downloadCancelled(QueueEntry queueEntry) {
		// we do nothing, because we don't care.
		
	}

	@Override
	public void downloadComplete(QueueEntry queueEntry, Bitmap imageBitmap) {
		View v = queueEntry.getIdentifier();
		ImageView preview = (ImageView) v.findViewById(R.id.RecipeIcon);
		
		// try and force some GC.
		System.gc();
		
		preview.setImageBitmap(imageBitmap);
	}

	@Override
	public void downloadFailed(QueueEntry queueEntry) {
		// We do nothing, because we already set a random pepper in place.
	}

}
