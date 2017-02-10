package it.sephiroth.android.library.rangeseekbar;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.support.v4.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.view.Gravity;

public class AppCompatProgressBarHelper {

    private static final int[] TINT_ATTRS = {
        android.R.attr.progressDrawable
    };

    private final RangeProgressBar mView;

    private Bitmap mSampleTile;

    public AppCompatProgressBarHelper(RangeProgressBar view) {
        mView = view;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs, TINT_ATTRS, defStyleAttr, 0);

        final Drawable drawable = a.getDrawableIfKnown(1);
        if (drawable != null) {
            mView.setProgressDrawable(tileify(drawable, false));
        }

        a.recycle();
    }

    public Drawable tileify(Drawable drawable, boolean clip) {
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
                outDrawables[i] = tileify(background.getDrawable(i), (id == android.R.id.progress));
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

    Bitmap getSampleTime() {
        return mSampleTile;
    }

}
