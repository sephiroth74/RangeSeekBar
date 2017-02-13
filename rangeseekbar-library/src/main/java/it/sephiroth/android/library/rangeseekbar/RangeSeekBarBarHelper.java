package it.sephiroth.android.library.rangeseekbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.DrawableWrapper;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;

import java.util.WeakHashMap;

import static android.support.v7.content.res.AppCompatResources.getColorStateList;

/**
 * Created by crugnola on 2/13/17.
 * RangeSeekBar
 */

public class RangeSeekBarBarHelper {
    private static final int[] TINT_ATTRS = {android.R.attr.progressDrawable};
    private static final String TAG = RangeSeekBarBarHelper.class.getSimpleName();
    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;
    private WeakHashMap<Context, SparseArray<ColorStateList>> mTintLists;

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {

        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, PorterDuff.Mode mode) {
            return get(generateCacheKey(color, mode));
        }

        PorterDuffColorFilter put(int color, PorterDuff.Mode mode, PorterDuffColorFilter filter) {
            return put(generateCacheKey(color, mode), filter);
        }

        private static int generateCacheKey(int color, PorterDuff.Mode mode) {
            int hashCode = 1;
            hashCode = 31 * hashCode + color;
            hashCode = 31 * hashCode + mode.hashCode();
            return hashCode;
        }
    }

    private final RangeSeekBar mView;
    private Bitmap mSampleTile;

    public RangeSeekBarBarHelper(final RangeSeekBar seekBar) {
        mView = seekBar;
    }

    public void loadFromAttributes(final AttributeSet attrs, final int defStyleAttr) {
        TypedArray a = mView.getContext().obtainStyledAttributes(attrs, TINT_ATTRS, defStyleAttr, 0);

        Drawable drawable = getProgressDrawableIfKnown(a, 0);
        if (drawable != null) {
            mView.setProgressDrawable(tileify(drawable, false));
        }
        a.recycle();

        a = mView.getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar, defStyleAttr, 0);
        drawable = getThumbDrawableIfKnown(a, RangeSeekBar.WhichThumb.Start);
        if (null != drawable) {
            mView.setThumb(drawable, RangeSeekBar.WhichThumb.Start);
        }
        drawable = getThumbDrawableIfKnown(a, RangeSeekBar.WhichThumb.End);
        if (null != drawable) {
            mView.setThumb(drawable, RangeSeekBar.WhichThumb.End);
        }
        a.recycle();

    }

    public Drawable getThumbDrawableIfKnown(final TypedArray a, RangeSeekBar.WhichThumb which) {

        int resourceId = 0;
        int index = which == RangeSeekBar.WhichThumb.Start ? R.styleable.RangeSeekBar_sephiroth_rsb_leftThumb
            : R.styleable.RangeSeekBar_sephiroth_rsb_rightThumb;
        if (a.hasValue(index)) {
            resourceId = a.getResourceId(index, 0);
        } else {
            resourceId = a.getResourceId(R.styleable.RangeSeekBar_android_thumb, 0);
        }

        if (resourceId != 0) {
            Drawable drawable = ContextCompat.getDrawable(mView.getContext(), resourceId);
            if (null != drawable) {
                return tintThumbDrawable(mView.getContext(), resourceId, true, drawable);
            }
        }
        return null;
    }

    public Drawable getProgressDrawableIfKnown(final TypedArray a, int index) {
        if (a.hasValue(index)) {
            final int resourceId = a.getResourceId(index, 0);
            if (resourceId != 0) {
                Drawable drawable = ContextCompat.getDrawable(mView.getContext(), resourceId);
                if (drawable != null) {
                    return tintProgressDrawable(mView.getContext(), resourceId, drawable);
                }
            }
        }
        return null;
    }

    private Drawable tintThumbDrawable(final Context context, final int resId, final boolean b, Drawable drawable) {
        final ColorStateList tintList = getTintList(context, resId);
        if (tintList != null) {
            if (DrawableUtils.canSafelyMutateDrawable(drawable)) {
                drawable = drawable.mutate();
            }
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(drawable, tintList);
        }
        return drawable;
    }

    private ColorStateList getTintList(final Context context, final int resId) {
        ColorStateList tint = getTintListFromCache(context, resId);
        if (tint == null) {
            if (resId == android.support.v7.appcompat.R.drawable.abc_seekbar_thumb_material) {
                tint = getColorStateList(context, android.support.v7.appcompat.R.color.abc_tint_seek_thumb);
            }

            if (tint != null) {
                addTintListToCache(context, resId, tint);
            }
        }
        return tint;
    }

    private Drawable tintProgressDrawable(
        final Context context, final int resourceId, final Drawable drawable) {
        if (resourceId == R.drawable.sephiroth_rsb_seekbar_track_material) {
            LayerDrawable ld = (LayerDrawable) drawable;
            setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.background),
                ThemeUtils.getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorControlNormal), DEFAULT_MODE
            );
            setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.progress),
                ThemeUtils.getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorControlActivated), DEFAULT_MODE
            );
        } else if (resourceId == R.drawable.sephiroth_rsb_seekbar_track_material_inverted) {
            LayerDrawable ld = (LayerDrawable) drawable;
            setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.progress),
                ThemeUtils.getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorButtonNormal), DEFAULT_MODE
            );
            setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.background),
                ThemeUtils.getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorControlActivated), DEFAULT_MODE
            );
        }
        return drawable;
    }

    static void setPorterDuffColorFilter(Drawable d, int color, PorterDuff.Mode mode) {
        if (DrawableUtils.canSafelyMutateDrawable(d)) {
            d = d.mutate();
        }
        d.setColorFilter(getPorterDuffColorFilter(color, mode == null ? DEFAULT_MODE : mode));
    }

    public static PorterDuffColorFilter getPorterDuffColorFilter(int color, PorterDuff.Mode mode) {
        PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);

        if (filter == null) {
            filter = new PorterDuffColorFilter(color, mode);
            COLOR_FILTER_CACHE.put(color, mode, filter);
        }

        return filter;
    }

    private Drawable tileify(Drawable drawable, boolean clip) {
        if (drawable instanceof DrawableWrapper) {
            Drawable inner = ((DrawableWrapper) drawable).getWrappedDrawable();
            if (inner != null) {
                inner = tileify(inner, clip);
                ((DrawableWrapper) drawable).setWrappedDrawable(inner);
            }
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable background = (LayerDrawable) drawable;
            final int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                int id = background.getId(i);
                outDrawables[i] = tileify(
                    background.getDrawable(i),
                    (id == android.R.id.progress || id == android.R.id.secondaryProgress)
                );
            }
            LayerDrawable newBg = new LayerDrawable(outDrawables);

            for (int i = 0; i < N; i++) {
                newBg.setId(i, background.getId(i));
            }

            return newBg;

        } else if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            final Bitmap tileBitmap = bitmapDrawable.getBitmap();
            if (mSampleTile == null) {
                mSampleTile = tileBitmap;
            }

            final ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());
            final BitmapShader bitmapShader = new BitmapShader(tileBitmap,
                Shader.TileMode.REPEAT, Shader.TileMode.CLAMP
            );
            shapeDrawable.getPaint().setShader(bitmapShader);
            shapeDrawable.getPaint().setColorFilter(bitmapDrawable.getPaint().getColorFilter());
            return (clip) ? new ClipDrawable(shapeDrawable, Gravity.LEFT,
                ClipDrawable.HORIZONTAL
            ) : shapeDrawable;
        }

        return drawable;
    }

    private Shape getDrawableShape() {
        final float[] roundedCorners = new float[]{5, 5, 5, 5, 5, 5, 5, 5};
        return new RoundRectShape(roundedCorners, null, null);
    }

    private ColorStateList getTintListFromCache(@NonNull Context context, @DrawableRes int resId) {
        if (mTintLists != null) {
            final SparseArray<ColorStateList> tints = mTintLists.get(context);
            return tints != null ? tints.get(resId) : null;
        }
        return null;
    }

    private void addTintListToCache(
        @NonNull Context context, @DrawableRes int resId,
        @NonNull ColorStateList tintList) {
        if (mTintLists == null) {
            mTintLists = new WeakHashMap<>();
        }
        SparseArray<ColorStateList> themeTints = mTintLists.get(context);
        if (themeTints == null) {
            themeTints = new SparseArray<>();
            mTintLists.put(context, themeTints);
        }
        themeTints.append(resId, tintList);
    }

}
