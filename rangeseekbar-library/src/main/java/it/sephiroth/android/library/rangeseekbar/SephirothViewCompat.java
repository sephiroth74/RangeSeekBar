package it.sephiroth.android.library.rangeseekbar;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * Created by sephiroth on 2/9/17.
 */

public class SephirothViewCompat {
    public static boolean isInScrollingContainer(final View view) {
        ViewParent p = view.getParent();
        while (p != null && p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }
}
