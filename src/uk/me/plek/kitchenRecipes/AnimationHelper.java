package uk.me.plek.kitchenRecipes;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

public class AnimationHelper {
	private static final long SPIN_HALFPOINT = 500; // the halfpoint of the spin animation in ms

	public static Animation centreSpinViewInAnimation() {
		Animation spin = new ScaleAnimation(
				0,1,
				1,1,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f
				);
		spin.setStartOffset(SPIN_HALFPOINT);
		spin.setDuration(SPIN_HALFPOINT);
		spin.setInterpolator(new AccelerateDecelerateInterpolator());
		return spin;
	}
	
	public static Animation centreSpinViewOutAnimation() {
		Animation spin = new ScaleAnimation(
				1,0,
				1,1,
				Animation.RELATIVE_TO_SELF,0.5f,
				Animation.RELATIVE_TO_SELF,0.5f
				);
		spin.setDuration(SPIN_HALFPOINT);
		spin.setInterpolator(new AccelerateDecelerateInterpolator());
		return spin;
	}
}
