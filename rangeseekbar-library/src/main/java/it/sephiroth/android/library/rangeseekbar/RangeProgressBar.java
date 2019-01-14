package it.sephiroth.android.library.rangeseekbar;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;

import androidx.annotation.InterpolatorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.Pools;
import it.sephiroth.android.library.simplelogger.LoggerFactory;
import it.sephiroth.android.library.simplelogger.LoggerFactory.LoggerType;

public class RangeProgressBar extends View {
    protected static LoggerFactory.Logger logger =
        LoggerFactory.getLogger("RangeProgressBar", BuildConfig.DEBUG ? LoggerType.Console : LoggerType.Null);

    private static final int TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200;

    /** Interpolator used for smooth progress animations. */
    private static final DecelerateInterpolator PROGRESS_ANIM_INTERPOLATOR =
        new DecelerateInterpolator();

    /** Duration of smooth progress animations. */
    private static final int PROGRESS_ANIM_DURATION = 80;

    protected int mMinMaxStepSize;
    protected int mProgressStartMaxValue;
    protected int mProgressEndMinValue;

    int mMinWidth;
    int mMaxWidth;
    int mMinHeight;
    int mMaxHeight;

    private int mProgressOffset;
    private int mEndProgress;
    private int mStartProgress;
    private int mMax;

    protected boolean mInitialProgressDone;

    private Drawable mProgressDrawable;
    private Drawable mCurrentDrawable;
    private ProgressTintInfo mProgressTintInfo;

    int mSampleWidth = 0;
    private boolean mNoInvalidate;
    private Interpolator mInterpolator;
    private RefreshProgressRunnable mRefreshProgressRunnable;
    private long mUiThreadId;

    private boolean mInDrawing;
    private boolean mAttached;
    private boolean mRefreshIsPosted;

    /** Value used to track progress animation, in the range [0...1]. */
    private float mVisualStartProgress;
    private float mVisualEndProgress;

    boolean mMirrorForRtl = false;

    private boolean mAggregatedIsVisible;

    private final ArrayList<RefreshData> mRefreshData = new ArrayList<>();

    private AccessibilityEventSender mAccessibilityEventSender;
    private Drawable mProgressDrawableIndicator;
    private Rect mProgressIndicatorBounds;
    private int mComputedWidth;
    protected int mPaddingBottom;
    protected int mPaddingTop;
    protected int mPaddingLeft;
    protected int mPaddingRight;

    public RangeProgressBar(Context context) {
        this(context, null);
    }

    public RangeProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sephiroth_rangeProgressBarStyle);
    }

    public RangeProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mUiThreadId = Thread.currentThread().getId();

        initProgressBar();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeProgressBar, defStyleAttr, 0);

        mNoInvalidate = true;

        final Drawable progressDrawable = a.getDrawable(R.styleable.RangeProgressBar_android_progressDrawable);
        if (progressDrawable != null) {
            if (needsTileify(progressDrawable)) {
                setProgressDrawableTiled(progressDrawable);
            } else {
                setProgressDrawable(progressDrawable);
            }
        }

        mMinWidth = a.getDimensionPixelSize(R.styleable.RangeProgressBar_android_minWidth, mMinWidth);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.RangeProgressBar_android_maxWidth, mMaxWidth);
        mMinHeight = a.getDimensionPixelSize(R.styleable.RangeProgressBar_android_minHeight, mMinHeight);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.RangeProgressBar_android_maxHeight, mMaxHeight);
        mMinMaxStepSize = a.getInteger(R.styleable.RangeProgressBar_range_progress_startEnd_minDiff, 0);
        mProgressOffset = a.getDimensionPixelSize(R.styleable.RangeProgressBar_range_progress_offset, 0);
        mProgressEndMinValue = a.getInteger(R.styleable.RangeProgressBar_range_progress_endMinValue, -1);
        mProgressStartMaxValue = a.getInteger(R.styleable.RangeProgressBar_range_progress_startMaxValue, -1);

        final int resID = a.getResourceId(
            R.styleable.RangeProgressBar_android_interpolator,
            android.R.anim.linear_interpolator
        ); // default to linear interpolator

        if (resID > 0) {
            setInterpolator(context, resID);
        }

        setMax(a.getInteger(R.styleable.RangeProgressBar_android_max, mMax));

        mNoInvalidate = false;

        if (a.hasValue(R.styleable.RangeProgressBar_android_progressTintMode)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressTintMode = DrawableUtils.INSTANCE.parseTintMode(a.getInt(
                R.styleable.RangeProgressBar_android_progressTintMode, -1), null);
            mProgressTintInfo.mHasProgressTintMode = true;
        }

        if (a.hasValue(R.styleable.RangeProgressBar_android_progressTint)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressTintList = a.getColorStateList(
                R.styleable.RangeProgressBar_android_progressTint);
            mProgressTintInfo.mHasProgressTint = true;
        }

        if (a.hasValue(R.styleable.RangeProgressBar_android_progressBackgroundTintMode)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressBackgroundTintMode = DrawableUtils.INSTANCE.parseTintMode(a.getInt(
                R.styleable.RangeProgressBar_android_progressBackgroundTintMode, -1), null);
            mProgressTintInfo.mHasProgressBackgroundTintMode = true;
        }

        if (a.hasValue(R.styleable.RangeProgressBar_android_progressBackgroundTint)) {
            if (mProgressTintInfo == null) {
                mProgressTintInfo = new ProgressTintInfo();
            }
            mProgressTintInfo.mProgressBackgroundTintList = a.getColorStateList(
                R.styleable.RangeProgressBar_android_progressBackgroundTint);
            mProgressTintInfo.mHasProgressBackgroundTint = true;
        }

        final int startProgress = a.getInteger(R.styleable.RangeProgressBar_range_progress_startValue, mStartProgress);
        final int endProgress = a.getInteger(R.styleable.RangeProgressBar_range_progress_endValue, mEndProgress);

        a.recycle();

        applyProgressTints();

        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        setProgressStartEndBoundaries(mProgressStartMaxValue, mProgressEndMinValue);

        setInitialProgress(
            startProgress,
            endProgress
        );

        mInitialProgressDone = true;
    }

    public int getMinMapStepSize() {
        return mMinMaxStepSize;
    }

    protected void setInitialProgress(final int startProgress, final int endProgress) {
        setProgress(startProgress, endProgress);
    }

    private static boolean needsTileify(Drawable dr) {
        if (dr instanceof LayerDrawable) {
            final LayerDrawable orig = (LayerDrawable) dr;
            final int N = orig.getNumberOfLayers();
            for (int i = 0; i < N; i++) {
                if (needsTileify(orig.getDrawable(i))) {
                    logger.debug("needsTileify!");
                    return true;
                }
            }
            return false;
        }

        if (dr instanceof StateListDrawable) {
            //throw new RuntimeException("StateListDrawable not supported");
            return false;
            //            final StateListDrawable in = (StateListDrawable) dr;
            //            final int N = in.getStateCount();
            //            for (int i = 0; i < N; i++) {
            //                if (needsTileify(in.getStateDrawable(i))) {
            //                    return true;
            //                }
            //            }
            //            return false;
        }

        return dr instanceof BitmapDrawable;

    }

    private Drawable tileify(Drawable drawable, boolean clip) {
        logger.debug("tileify: " + drawable + ", clip: " + clip);
        // TODO: This is a terrible idea that potentially destroys any drawable
        // that extends any of these classes. We *really* need to remove this.

        if (drawable instanceof LayerDrawable) {
            final LayerDrawable orig = (LayerDrawable) drawable;
            final int N = orig.getNumberOfLayers();
            final Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                final int id = orig.getId(i);
                outDrawables[i] = tileify(
                    orig.getDrawable(i),
                    (id == android.R.id.progress || id == android.R.id.secondaryProgress)
                );
            }

            final LayerDrawable clone = new LayerDrawable(outDrawables);
            for (int i = 0; i < N; i++) {
                clone.setId(i, orig.getId(i));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    clone.setLayerGravity(i, orig.getLayerGravity(i));
                    clone.setLayerWidth(i, orig.getLayerWidth(i));
                    clone.setLayerHeight(i, orig.getLayerHeight(i));
                    clone.setLayerInsetLeft(i, orig.getLayerInsetLeft(i));
                    clone.setLayerInsetRight(i, orig.getLayerInsetRight(i));
                    clone.setLayerInsetTop(i, orig.getLayerInsetTop(i));
                    clone.setLayerInsetBottom(i, orig.getLayerInsetBottom(i));
                    clone.setLayerInsetStart(i, orig.getLayerInsetStart(i));
                    clone.setLayerInsetEnd(i, orig.getLayerInsetEnd(i));
                }
            }

            return clone;
        }

        if (drawable instanceof StateListDrawable) {
            throw new RuntimeException("StateListDrawable not supported");
            //
            //            final StateListDrawable in = (StateListDrawable) drawable;
            //            final StateListDrawable out = new StateListDrawable();
            //            final int N = in.getStateCount();
            //            for (int i = 0; i < N; i++) {
            //                out.addState(in.getStateSet(i), tileify(in.getStateDrawable(i), clip));
            //            }
            //
            //            return out;
        }

        if (drawable instanceof BitmapDrawable) {
            final Drawable.ConstantState cs = drawable.getConstantState();
            assert cs != null;
            final BitmapDrawable clone = (BitmapDrawable) cs.newDrawable(getResources());
            clone.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);

            if (mSampleWidth <= 0) {
                mSampleWidth = clone.getIntrinsicWidth();
            }

            if (clip) {
                return new ClipDrawable(clone, Gravity.START, ClipDrawable.HORIZONTAL);
            } else {
                return clone;
            }
        }

        return drawable;
    }

    private void initProgressBar() {
        mMax = 100;
        mEndProgress = 100;
        mStartProgress = 0;
        mMinWidth = 24;
        mMaxWidth = 48;
        mMinHeight = 24;
        mMaxHeight = 48;
    }

    private void swapCurrentDrawable(Drawable newDrawable) {
        final Drawable oldDrawable = mCurrentDrawable;
        mCurrentDrawable = newDrawable;

        if (oldDrawable != mCurrentDrawable) {
            if (oldDrawable != null) {
                oldDrawable.setVisible(false, false);
            }
            if (mCurrentDrawable != null) {
                mCurrentDrawable.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
            }
        }
    }

    public Drawable getProgressDrawable() {
        return mProgressDrawable;
    }

    public void setProgressDrawable(Drawable d) {
        if (mProgressDrawable != d) {
            if (mProgressDrawable != null) {
                mProgressDrawable.setCallback(null);
                unscheduleDrawable(mProgressDrawable);
            }

            mProgressDrawable = d;

            if (d != null) {
                d.setCallback(this);
                DrawableCompat.setLayoutDirection(d, getLayoutDirection());
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }

                // Make sure the ProgressBar is always tall enough
                int drawableHeight = d.getMinimumHeight();
                if (mMaxHeight < drawableHeight) {
                    mMaxHeight = drawableHeight;
                    requestLayout();
                }

                applyProgressTints();
            }

            swapCurrentDrawable(d);
            postInvalidate();

            updateDrawableBounds(getWidth(), getHeight());
            updateDrawableState();

            doRefreshProgress(android.R.id.progress, mStartProgress, mEndProgress, false, false, false);
        }
    }

    private void applyProgressTints() {
        if (mProgressDrawable != null && mProgressTintInfo != null) {
            applyPrimaryProgressTint();
            applyProgressBackgroundTint();
        }
    }

    private void applyPrimaryProgressTint() {
        if (mProgressTintInfo.mHasProgressTint
            || mProgressTintInfo.mHasProgressTintMode) {
            final Drawable target = getTintTarget(android.R.id.progress, true);
            if (target != null) {
                if (mProgressTintInfo.mHasProgressTint) {
                    DrawableCompat.setTintList(target, mProgressTintInfo.mProgressTintList);
                }
                if (mProgressTintInfo.mHasProgressTintMode) {
                    DrawableCompat.setTintMode(target, mProgressTintInfo.mProgressTintMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    private void applyProgressBackgroundTint() {
        if (mProgressTintInfo.mHasProgressBackgroundTint
            || mProgressTintInfo.mHasProgressBackgroundTintMode) {
            final Drawable target = getTintTarget(android.R.id.background, false);
            if (target != null) {
                if (mProgressTintInfo.mHasProgressBackgroundTint) {
                    DrawableCompat.setTintList(target, mProgressTintInfo.mProgressBackgroundTintList);
                }
                if (mProgressTintInfo.mHasProgressBackgroundTintMode) {
                    DrawableCompat.setTintMode(target, mProgressTintInfo.mProgressBackgroundTintMode);
                }

                // The drawable (or one of its children) may not have been
                // stateful before applying the tint, so let's try again.
                if (target.isStateful()) {
                    target.setState(getDrawableState());
                }
            }
        }
    }

    @SuppressWarnings ("unused")
    public void setProgressTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressTintList = tint;
        mProgressTintInfo.mHasProgressTint = true;

        if (mProgressDrawable != null) {
            applyPrimaryProgressTint();
        }
    }

    @SuppressWarnings ("unused")
    @Nullable
    public ColorStateList getProgressTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressTintList : null;
    }

    @SuppressWarnings ("unused")
    public void setProgressTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressTintMode = tintMode;
        mProgressTintInfo.mHasProgressTintMode = true;

        if (mProgressDrawable != null) {
            applyPrimaryProgressTint();
        }
    }

    @SuppressWarnings ("unused")
    @Nullable
    public PorterDuff.Mode getProgressTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressTintMode : null;
    }

    @SuppressWarnings ("unused")
    public void setProgressBackgroundTintList(@Nullable ColorStateList tint) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBackgroundTintList = tint;
        mProgressTintInfo.mHasProgressBackgroundTint = true;

        if (mProgressDrawable != null) {
            applyProgressBackgroundTint();
        }
    }

    @SuppressWarnings ("unused")
    @Nullable
    public ColorStateList getProgressBackgroundTintList() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBackgroundTintList : null;
    }

    @SuppressWarnings ("unused")
    public void setProgressBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mProgressTintInfo == null) {
            mProgressTintInfo = new ProgressTintInfo();
        }
        mProgressTintInfo.mProgressBackgroundTintMode = tintMode;
        mProgressTintInfo.mHasProgressBackgroundTintMode = true;

        if (mProgressDrawable != null) {
            applyProgressBackgroundTint();
        }
    }

    @SuppressWarnings ("unused")
    @Nullable
    public PorterDuff.Mode getProgressBackgroundTintMode() {
        return mProgressTintInfo != null ? mProgressTintInfo.mProgressBackgroundTintMode : null;
    }

    @Nullable
    private Drawable getTintTarget(int layerId, boolean shouldFallback) {
        Drawable layer = null;

        final Drawable d = mProgressDrawable;
        if (d != null) {
            mProgressDrawable = d.mutate();

            if (d instanceof LayerDrawable) {
                layer = ((LayerDrawable) d).findDrawableByLayerId(layerId);
            }

            if (shouldFallback && layer == null) {
                layer = d;
            }
        }

        return layer;
    }

    public void setProgressDrawableTiled(Drawable d) {
        if (d != null) {
            d = tileify(d, false);
        }

        setProgressDrawable(d);
    }

    Drawable getCurrentDrawable() {
        return mCurrentDrawable;
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mProgressDrawable || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mProgressDrawable != null) {
            mProgressDrawable.jumpToCurrentState();
        }
    }

    //    WTF: why this method is hidden?
    //    @Override
    //    public void onResolveDrawables(int layoutDirection) {
    //        final Drawable d = mCurrentDrawable;
    //        if (d != null) {
    //            d.setLayoutDirection(layoutDirection);
    //        }
    //        if (mIndeterminateDrawable != null) {
    //            mIndeterminateDrawable.setLayoutDirection(layoutDirection);
    //        }
    //        if (mProgressDrawable != null) {
    //            mProgressDrawable.setLayoutDirection(layoutDirection);
    //        }
    //    }

    @Override
    public void postInvalidate() {
        if (!mNoInvalidate) {
            super.postInvalidate();
        }
    }

    private class RefreshProgressRunnable implements Runnable {
        public void run() {
            synchronized (RangeProgressBar.this) {
                final int count = mRefreshData.size();
                for (int i = 0; i < count; i++) {
                    final RefreshData rd = mRefreshData.get(i);
                    doRefreshProgress(rd.id, rd.startValue, rd.endValue, rd.fromUser, true, rd.animate);
                    rd.recycle();
                }
                mRefreshData.clear();
                mRefreshIsPosted = false;
            }
        }
    }

    @SuppressWarnings ("WeakerAccess")
    private static class RefreshData {
        private static final int POOL_MAX = 24;
        private static final Pools.SynchronizedPool<RefreshData> sPool =
            new Pools.SynchronizedPool<>(POOL_MAX);

        public int id;
        public int startValue;
        public int endValue;
        public boolean fromUser;
        public boolean animate;

        public static RefreshData obtain(int id, int startValue, int endValue, boolean fromUser, boolean animate) {
            RefreshData rd = sPool.acquire();
            if (rd == null) {
                rd = new RefreshData();
            }
            rd.id = id;
            rd.startValue = startValue;
            rd.endValue = endValue;
            rd.fromUser = fromUser;
            rd.animate = animate;
            return rd;
        }

        public void recycle() {
            sPool.release(this);
        }
    }

    private synchronized void doRefreshProgress(
        int id, int startValue, int endValue, boolean fromUser,
        boolean callBackToApp, boolean animate) {

        logger.info("doRefreshProgress(%d, %d, %b, %b)", startValue, endValue, fromUser, animate);

        final float scale1 = mMax > 0 ? (float) startValue / mMax : 0;
        final float scale2 = mMax > 0 ? (float) endValue / mMax : 0;

        if (animate) {
            logger.verbose("start: %g to %g", mVisualStartProgress, scale1);
            logger.verbose("end: %g to %g", mVisualEndProgress, scale2);

            final ValueAnimator a1 = ValueAnimator.ofFloat(mVisualStartProgress, scale1);
            final ValueAnimator a2 = ValueAnimator.ofFloat(mVisualEndProgress, scale2);

            a2.addUpdateListener(
                animation -> setVisualProgress(
                    android.R.id.progress, (float) a1.getAnimatedValue(), (float) a2.getAnimatedValue()));

            AnimatorSet set = new AnimatorSet();
            set.playTogether(a1, a2);
            set.setDuration(PROGRESS_ANIM_DURATION);
            set.setInterpolator(PROGRESS_ANIM_INTERPOLATOR);
            set.start();
        } else {
            setVisualProgress(id, scale1, scale2);
        }

        if (callBackToApp) {
            onProgressRefresh(fromUser, startValue, endValue);
        }
    }

    public void onProgressRefresh(boolean fromUser, int startValue, int endValue) {
        logger.debug("onProgressRefresh(%d, %d)", startValue, endValue);
    }

    private void setVisualProgress(int id, float progress1, float progress2) {
        logger.info("setVisualProgress(%g, %g)", progress1, progress2);
        mVisualStartProgress = progress1;
        mVisualEndProgress = progress2;
        invalidate();
        onVisualProgressChanged(id, progress1, progress2);
    }

    public void onVisualProgressChanged(int id, float scale1, float scale2) {
        logger.debug("onVisualProgressChanged(%g, %g)", scale1, scale2);
    }

    private synchronized void refreshProgress(
        @SuppressWarnings ("SameParameterValue") int id, int startValue, int endValue, boolean fromUser,
        boolean animate) {
        if (mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(id, startValue, endValue, fromUser, true, animate);
        } else {
            if (mRefreshProgressRunnable == null) {
                mRefreshProgressRunnable = new RefreshProgressRunnable();
            }

            final RefreshData rd = RefreshData.obtain(id, startValue, endValue, fromUser, animate);
            mRefreshData.add(rd);
            if (mAttached && !mRefreshIsPosted) {
                removeCallbacks(mRefreshProgressRunnable);
                post(mRefreshProgressRunnable);
                mRefreshIsPosted = true;
            }
        }
    }

    public synchronized void setProgress(int startValue, int endValue) {
        logger.info("setProgress(%d, %d)", startValue, endValue);
        setProgressInternal(startValue, endValue, false, false);
    }

    @SuppressWarnings ("unused")
    public void setProgress(int startValue, int endValue, boolean animate) {
        setProgressInternal(startValue, endValue, false, animate);
    }

    synchronized boolean setProgressInternal(int startValue, int endValue, boolean fromUser, boolean animate) {
        logger.info("setProgressInternal(%d, %d)", startValue, endValue);
        startValue = MathUtils.INSTANCE.constrain(startValue, 0, MathUtils.INSTANCE.constrain(endValue, 0, mMax));
        endValue = MathUtils.INSTANCE.constrain(endValue, startValue, mMax);

        if (startValue == mStartProgress && endValue == mEndProgress) {
            return false;
        }

        mEndProgress = endValue;
        mStartProgress = startValue;

        refreshProgress(android.R.id.progress, mStartProgress, mEndProgress, fromUser, animate);

        return true;
    }

    /**
     * Set the start max value and the end min value.<br />
     * This will override the #setMinMaxStepSize
     */
    public void setProgressStartEndBoundaries(int startMax, int endMin) {
        logger.info("setProgressStartEndBoundaries(%d, %d)", startMax, endMin);

        if (startMax > endMin) {
            throw new IllegalArgumentException("startMax cannot be greater than endMin");
        }

        if (startMax > mMax) {
            throw new IllegalArgumentException("startMax cannot be greater max value");
        }

        if (startMax != -1 || endMin != -1) {
            mMinMaxStepSize = 0;
        }

        mProgressStartMaxValue = startMax;
        mProgressEndMinValue = endMin;
    }

    public void setMinMaxStepSize(int value) {
        logger.info("setMinMaxStepSize(%d)", value);

        if (value > mMax) {
            throw new IllegalArgumentException("value cannot be greater than max value");
        }

        if (value != 0) {
            mProgressEndMinValue = -1;
            mProgressStartMaxValue = -1;
        }

        mMinMaxStepSize = value;
    }

    public int getProgressStartMaxValue() {
        if (mProgressStartMaxValue != -1) {
            return mProgressStartMaxValue;
        }
        return getProgressEnd() - mMinMaxStepSize;
    }

    public int getProgressEndMinValue() {
        if (mProgressEndMinValue != -1) {
            return mProgressEndMinValue;
        }
        return getProgressStart() + mMinMaxStepSize;
    }

    public int getProgressEnd() {
        return mEndProgress;
    }

    public int getProgressStart() {
        return mStartProgress;
    }

    public synchronized int getMax() {
        return mMax;
    }

    public synchronized void setMax(int max) {
        logger.info("setMax(%d)", max);
        if (max < 0) {
            max = 0;
        }
        if (max != mMax) {
            mMax = max;
            postInvalidate();

            if (mEndProgress > max) {
                mEndProgress = max;
            }
            refreshProgress(android.R.id.progress, mStartProgress, mEndProgress, false, false);
        }
    }

    @SuppressWarnings ("unused")
    public synchronized final void incrementEndValueBy(int diff) {
        setProgress(mStartProgress, mEndProgress + diff);
    }

    public void setInterpolator(Context context, @InterpolatorRes int resID) {
        setInterpolator(AnimationUtils.loadInterpolator(context, resID));
    }

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    @SuppressWarnings ("unused")
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        if (isVisible != mAggregatedIsVisible) {
            mAggregatedIsVisible = isVisible;

            if (mCurrentDrawable != null) {
                mCurrentDrawable.setVisible(isVisible, false);
            }
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        if (!mInDrawing) {
            if (verifyDrawable(dr)) {
                final Rect dirty = dr.getBounds();
                final int scrollX = getScrollX() + getPaddingLeft();
                final int scrollY = getScrollY() + getPaddingTop();

                invalidate(dirty.left + scrollX, dirty.top + scrollY,
                    dirty.right + scrollX, dirty.bottom + scrollY
                );
            } else {
                super.invalidateDrawable(dr);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(w, h);
    }

    private void updateDrawableBounds(int w, int h) {
        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingTop = getPaddingTop();
        mPaddingBottom = getPaddingBottom();

        w -= mPaddingRight + mPaddingLeft;
        h -= mPaddingTop + mPaddingBottom;

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;

        mProgressDrawableIndicator = null;
        mProgressIndicatorBounds = null;
        mComputedWidth = w;

        if (mProgressDrawable != null) {
            mProgressDrawable.setBounds(left, top, right, bottom);
            mProgressDrawableIndicator = ((LayerDrawable) mProgressDrawable).findDrawableByLayerId(android.R.id.progress);
            mProgressIndicatorBounds = mProgressDrawableIndicator.getBounds();
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTrack(canvas);
    }

    protected boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    void drawTrack(Canvas canvas) {
        final Drawable d = mProgressDrawable;
        if (d != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            final int saveCount = canvas.save();

            if (isLayoutRtl() && mMirrorForRtl) {
                canvas.translate(getWidth() - mPaddingRight, mPaddingTop);
                canvas.scale(-1.0f, 1.0f);
            } else {
                canvas.translate(mPaddingLeft, mPaddingTop);
            }

            if (null != mProgressIndicatorBounds) {
                final int w = mComputedWidth - mProgressOffset;
                final int start = (int) (mVisualStartProgress * w);
                final int end = (int) (mVisualEndProgress * w);

                mProgressDrawableIndicator
                    .setBounds(
                        start,
                        mProgressIndicatorBounds.top,
                        mProgressOffset + end,
                        mProgressIndicatorBounds.bottom
                    );
            }

            d.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    public int getProgressOffset() {
        return mProgressOffset;
    }

    @SuppressWarnings ("unused")
    public void setProgressOffset(final int value) {
        logger.info("setProgressOffset(%d)", value);
        this.mProgressOffset = value;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = 0;
        int dh = 0;

        final Drawable d = mCurrentDrawable;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
        }

        updateDrawableState();

        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable progressDrawable = mProgressDrawable;
        if (progressDrawable != null && progressDrawable.isStateful()) {
            changed = progressDrawable.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mProgressDrawable != null) {
            logger.verbose("setHotspot(%.2f, %.2f)", x, y);
            DrawableCompat.setHotspot(mProgressDrawable, x, y);
        }
    }

    static class SavedState extends BaseSavedState {
        int startValue;
        int endValue;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            startValue = in.readInt();
            endValue = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(startValue);
            out.writeInt(endValue);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
            = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.startValue = mStartProgress;
        ss.endValue = mEndProgress;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setProgress(ss.startValue, ss.endValue);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        synchronized (this) {
            final int count = mRefreshData.size();
            for (int i = 0; i < count; i++) {
                final RefreshData rd = mRefreshData.get(i);
                doRefreshProgress(rd.id, rd.startValue, rd.endValue, rd.fromUser, true, rd.animate);
                rd.recycle();
            }
            mRefreshData.clear();
        }
        mAttached = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mRefreshProgressRunnable != null) {
            removeCallbacks(mRefreshProgressRunnable);
            mRefreshIsPosted = false;
        }
        if (mAccessibilityEventSender != null) {
            removeCallbacks(mAccessibilityEventSender);
        }
        super.onDetachedFromWindow();
        mAttached = false;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return RangeProgressBar.class.getName();
    }

    private class AccessibilityEventSender implements Runnable {
        public void run() {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

    private static class ProgressTintInfo {
        ColorStateList mProgressTintList;
        PorterDuff.Mode mProgressTintMode;
        boolean mHasProgressTint;
        boolean mHasProgressTintMode;

        ColorStateList mProgressBackgroundTintList;
        PorterDuff.Mode mProgressBackgroundTintMode;
        boolean mHasProgressBackgroundTint;
        boolean mHasProgressBackgroundTintMode;

    }
}
