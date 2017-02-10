package it.sephiroth.android.library.rangeseekbar;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import android.support.annotation.NonNull;

public class DrawableUtils {
    private static final String VECTOR_DRAWABLE_CLAZZ_NAME
        = "android.graphics.drawable.VectorDrawable";

    private DrawableUtils() { }

    public static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return defaultMode;
        }
    }

    static void fixDrawable(@NonNull final Drawable drawable) {
        if (Build.VERSION.SDK_INT == 21
            && VECTOR_DRAWABLE_CLAZZ_NAME.equals(drawable.getClass().getName())) {
            fixVectorDrawableTinting(drawable);
        }
    }

    private static void fixVectorDrawableTinting(final Drawable drawable) {
        final int[] originalState = drawable.getState();
        if (originalState == null || originalState.length == 0) {
            drawable.setState(ThemeUtils.CHECKED_STATE_SET);
        } else {
            drawable.setState(ThemeUtils.EMPTY_STATE_SET);
        }
        drawable.setState(originalState);
    }

    public static boolean canSafelyMutateDrawable(@NonNull Drawable drawable) {
        if (Build.VERSION.SDK_INT < 15 && drawable instanceof InsetDrawable) {
            return false;
        } else if (Build.VERSION.SDK_INT < 15 && drawable instanceof GradientDrawable) {
            // GradientDrawable has a bug pre-ICS which results in mutate() resulting
            // in loss of color
            return false;
        } else if (Build.VERSION.SDK_INT < 17 && drawable instanceof LayerDrawable) {
            return false;
        }

        if (drawable instanceof DrawableContainer) {
            // If we have a DrawableContainer, let's traverse it's child array
            final Drawable.ConstantState state = drawable.getConstantState();
            if (state instanceof DrawableContainer.DrawableContainerState) {
                final DrawableContainer.DrawableContainerState containerState =
                    (DrawableContainer.DrawableContainerState) state;
                for (final Drawable child : containerState.getChildren()) {
                    if (!canSafelyMutateDrawable(child)) {
                        return false;
                    }
                }
            }
        } else if (drawable instanceof android.support.v4.graphics.drawable.DrawableWrapper) {
            return canSafelyMutateDrawable(
                ((android.support.v4.graphics.drawable.DrawableWrapper) drawable).getWrappedDrawable());
        } else if (drawable instanceof android.support.v7.graphics.drawable.DrawableWrapper) {
            return canSafelyMutateDrawable(
                ((android.support.v7.graphics.drawable.DrawableWrapper) drawable).getWrappedDrawable());
        } else if (drawable instanceof ScaleDrawable) {
            return canSafelyMutateDrawable(((ScaleDrawable) drawable).getDrawable());
        }

        return true;
    }
}
