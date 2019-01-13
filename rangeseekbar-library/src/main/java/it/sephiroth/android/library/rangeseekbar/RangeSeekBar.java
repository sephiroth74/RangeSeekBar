
package it.sephiroth.android.library.rangeseekbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

@SuppressWarnings ("unused")
public class RangeSeekBar extends RangeProgressBar {

    private int mInitialStartValue;
    private int mInitialEndValue;

    public interface OnRangeSeekBarChangeListener {

        void onProgressChanged(RangeSeekBar seekBar, int progressStart, int progressEnd, boolean fromUser);

        void onStartTrackingTouch(RangeSeekBar seekBar);

        void onStopTrackingTouch(RangeSeekBar seekBar);
    }

    private final Rect mTempRect1 = new Rect();
    private final Rect mTempRect2 = new Rect();
    private OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener;

    public enum WhichThumb {
        Start, End, None
    }

    private int mStepSize = 1;
    private int mThumbClipInset = 0;
    private Drawable mThumbStart;
    private Drawable mThumbEnd;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;
    private boolean mHasThumbTint = false;
    private boolean mHasThumbTintMode = false;

    private Drawable mTickMark;
    private ColorStateList mTickMarkTintList = null;
    private PorterDuff.Mode mTickMarkTintMode = null;
    private boolean mHasTickMarkTint = false;
    private boolean mHasTickMarkTintMode = false;

    private int mThumbOffset;
    private boolean mSplitTrack;

    /**
     * On touch, this offset plus the scaled value from the position of the
     * touch will form the progress value. Usually 0.
     */
    float mTouchProgressOffset;

    /**
     * Whether this is user seekable.
     */
    boolean mIsUserSeekable = true;

    private int mKeyProgressIncrement = 1;
    private static final int NO_ALPHA = 0xFF;
    private float mDisabledAlpha;

    private int mScaledTouchSlop;
    private float mTouchDownX;
    private boolean mIsDragging;
    private WhichThumb mWhichThumb = WhichThumb.None;

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sephiroth_rangeSeekBarStyle);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(
            attrs, R.styleable.RangeSeekBar, defStyleAttr, defStyleRes);

        Drawable thumb;
        Drawable thumb2;

        if (a.hasValue(R.styleable.RangeSeekBar_sephiroth_rsb_leftThumb)) {
            thumb = a.getDrawable(R.styleable.RangeSeekBar_sephiroth_rsb_leftThumb);
        } else {
            thumb = a.getDrawable(R.styleable.RangeSeekBar_android_thumb);
        }

        if (a.hasValue(R.styleable.RangeSeekBar_sephiroth_rsb_rightThumb)) {
            thumb2 = a.getDrawable(R.styleable.RangeSeekBar_sephiroth_rsb_rightThumb);
        } else {
            thumb2 = a.getDrawable(R.styleable.RangeSeekBar_android_thumb);
        }

        setThumb(thumb, WhichThumb.Start);
        setThumb(thumb2, WhichThumb.End);

        if (a.hasValue(R.styleable.RangeSeekBar_android_thumbTintMode)) {
            mThumbTintMode = DrawableUtils.parseTintMode(a.getInt(
                R.styleable.RangeSeekBar_android_thumbTintMode, -1), mThumbTintMode);
            mHasThumbTintMode = true;
        }

        if (a.hasValue(R.styleable.RangeSeekBar_android_thumbTint)) {
            mThumbTintList = a.getColorStateList(R.styleable.RangeSeekBar_android_thumbTint);
            mHasThumbTint = true;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            final Drawable tickMark = a.getDrawable(R.styleable.RangeSeekBar_android_tickMark);
            setTickMark(tickMark);

            if (a.hasValue(R.styleable.RangeSeekBar_android_tickMarkTintMode)) {
                mTickMarkTintMode = DrawableUtils.parseTintMode(a.getInt(
                    R.styleable.RangeSeekBar_android_tickMarkTintMode, -1), mTickMarkTintMode);
                mHasTickMarkTintMode = true;
            }

            if (a.hasValue(R.styleable.RangeSeekBar_android_tickMarkTint)) {
                mTickMarkTintList = a.getColorStateList(R.styleable.RangeSeekBar_android_tickMarkTint);
                mHasTickMarkTint = true;
            }
        }

        mSplitTrack = a.getBoolean(R.styleable.RangeSeekBar_android_splitTrack, false);

        if (a.hasValue(R.styleable.RangeSeekBar_sephiroth_rsb_stepSize)) {
            mStepSize = a.getInt(R.styleable.RangeSeekBar_sephiroth_rsb_stepSize, 1);
        }

        setMinMaxStepSize(getMinMapStepSize());

        mThumbClipInset = a.getDimensionPixelSize(R.styleable.RangeSeekBar_sephiroth_rsb_thumbInset, mThumbClipInset);

        final int thumbOffset = a.getDimensionPixelOffset(
            R.styleable.RangeSeekBar_android_thumbOffset, getThumbOffset());
        setThumbOffset(thumbOffset);

        final boolean useDisabledAlpha = a.getBoolean(R.styleable.RangeSeekBar_sephiroth_rsb_useDisabledAlpha, true);
        a.recycle();

        if (useDisabledAlpha) {
            //            final TypedArray ta = context.obtainStyledAttributes(attrs, android.R.styleable.Theme, 0, 0);
            //            mDisabledAlpha = ta.getFloat(R.styleable.Theme_disabledAlpha, 0.5f);
            //            ta.recycle();

            // TODO: find out
            mDisabledAlpha = 0.5f;
        } else {
            mDisabledAlpha = 1.0f;
        }

        applyTickMarkTint();
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setProgress(mInitialStartValue, mInitialEndValue);
    }

    public void setStepSize(final int value) {
        mStepSize = value;
        setMinMaxStepSize(getMinMapStepSize());
        setProgress(getProgressStart(), getProgressEnd());
    }

    @Override
    public synchronized void setMinMaxStepSize(final int value) {
        mMinMapStepSize = value;
        if (mMinMapStepSize != 0) {
            if (mMinMapStepSize % mStepSize != 0) {
                mMinMapStepSize = Math.max(mStepSize, mMinMapStepSize - (mMinMapStepSize % mStepSize));
            }
        }

        logger.info("setMinMaxStepSize(value: %d -- final: %d)", value, mMinMapStepSize);
    }

    @Override
    protected void setInitialProgress(int startProgress, int endProgress) {
        logger.info("setInitialProgress: %d - %d", startProgress, endProgress);

        if (mProgressStartMaxValue != -1 || mProgressEndMinValue != -1) {
            if (mProgressStartMaxValue != -1) {
                startProgress = Math.min(startProgress, mProgressStartMaxValue);
            }

            if (mProgressEndMinValue != -1) {
                endProgress = Math.max(endProgress, mProgressEndMinValue);
            }
        } else if (mMinMapStepSize != 0) {
            // see later
        }

        mInitialStartValue = startProgress;
        mInitialEndValue = endProgress;
    }

    @Override
    public void onProgressRefresh(final boolean fromUser, final int startValue, final int endValue) {
        super.onProgressRefresh(fromUser, startValue, endValue);

        if (mOnRangeSeekBarChangeListener != null) {
            mOnRangeSeekBarChangeListener.onProgressChanged(this, startValue, endValue, fromUser);
        }
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener l) {
        mOnRangeSeekBarChangeListener = l;
    }

    /**
     * Sets the thumb that will be drawn at the end of the progress meter within the SeekBar.
     * <p>
     * If the thumb is a valid drawable (i.e. not null), half its width will be
     * used as the new thumb offset (@see #setThumbOffset(int)).
     *
     * @param thumb Drawable representing the thumb
     * @param which
     */
    public void setThumb(Drawable thumb, WhichThumb which) {
        final boolean needUpdate;

        Drawable whichThumb = which == WhichThumb.Start ? mThumbStart : mThumbEnd;

        if (whichThumb != null && thumb != whichThumb) {
            whichThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (thumb != null) {
            thumb.setCallback(this);
            DrawableCompat.setLayoutDirection(thumb, ViewCompat.getLayoutDirection(this));
            mThumbOffset = thumb.getIntrinsicWidth() / 2;
            logger.info("mThumbOffset: %d", mThumbOffset);

            if (needUpdate &&
                (thumb.getIntrinsicWidth() != whichThumb.getIntrinsicWidth()
                    || thumb.getIntrinsicHeight() != whichThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }

        if (which == WhichThumb.Start) {
            mThumbStart = thumb;
        } else {
            mThumbEnd = thumb;
        }

        applyThumbTintInternal(which);
        invalidate();

        if (needUpdate) {
            updateThumbAndTrackPos(getWidth(), getHeight());
            if (thumb != null && thumb.isStateful()) {
                // Note that if the states are different this won't work.
                // For now, let's consider that an app bug.
                int[] state = getDrawableState();
                thumb.setState(state);
            }
        }
    }

    public Drawable getThumbStart() {
        return mThumbStart;
    }

    public Drawable getThumbEnd() {
        return mThumbEnd;
    }

    public void setThumbTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        mHasThumbTint = true;

        applyThumbTint();
    }

    @Nullable
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    public void setThumbTintMode(@Nullable PorterDuff.Mode tintMode) {
        mThumbTintMode = tintMode;
        mHasThumbTintMode = true;
        applyThumbTint();
    }

    @Nullable
    public PorterDuff.Mode getThumbTintMode() {
        return mThumbTintMode;
    }

    private void applyThumbTint() {
        applyThumbTintInternal(WhichThumb.Start);
        applyThumbTintInternal(WhichThumb.End);
    }

    private void applyThumbTintInternal(final WhichThumb which) {
        Drawable thumb = which == WhichThumb.Start ? mThumbStart : mThumbEnd;

        if (thumb != null && (mHasThumbTint || mHasThumbTintMode)) {

            if (which == WhichThumb.Start) {
                mThumbStart = thumb.mutate();
                thumb = mThumbStart;
            } else {
                mThumbEnd = thumb.mutate();
                thumb = mThumbEnd;
            }

            if (mHasThumbTint) {
                DrawableCompat.setTintList(thumb, mThumbTintList);
            }

            if (mHasThumbTintMode) {
                DrawableCompat.setTintMode(thumb, mThumbTintMode);
            }

            if (thumb.isStateful()) {
                thumb.setState(getDrawableState());
            }
        }
    }

    /**
     * @see #setThumbOffset(int)
     */
    public int getThumbOffset() {
        return mThumbOffset;
    }

    /**
     * Sets the thumb offset that allows the thumb to extend out of the range of
     * the track.
     *
     * @param thumbOffset The offset amount in pixels.
     */
    public void setThumbOffset(int thumbOffset) {
        mThumbOffset = thumbOffset;
        invalidate();
    }

    /**
     * Specifies whether the track should be split by the thumb. When true,
     * the thumb's optical bounds will be clipped out of the track drawable,
     * then the thumb will be drawn into the resulting gap.
     *
     * @param splitTrack Whether the track should be split by the thumb
     */
    public void setSplitTrack(boolean splitTrack) {
        mSplitTrack = splitTrack;
        invalidate();
    }

    /**
     * Returns whether the track should be split by the thumb.
     */
    public boolean getSplitTrack() {
        return mSplitTrack;
    }

    public void setTickMark(Drawable tickMark) {
        if (mTickMark != null) {
            mTickMark.setCallback(null);
        }

        mTickMark = tickMark;

        if (tickMark != null) {
            tickMark.setCallback(this);
            if (tickMark.isStateful()) {
                tickMark.setState(getDrawableState());
            }
            applyTickMarkTint();
        }

        invalidate();
    }

    /**
     * @return the drawable displayed at each progress position
     */
    public Drawable getTickMark() {
        return mTickMark;
    }

    /**
     * Applies a tint to the tick mark drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setTickMark(Drawable)} will automatically
     * mutate the drawable and apply the specified tint and tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @attr ref android.R.styleable#SeekBar_tickMarkTint
     * @see #getTickMarkTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    public void setTickMarkTintList(@Nullable ColorStateList tint) {
        mTickMarkTintList = tint;
        mHasTickMarkTint = true;

        applyTickMarkTint();
    }

    /**
     * Returns the tint applied to the tick mark drawable, if specified.
     *
     * @return the tint applied to the tick mark drawable
     * @attr ref android.R.styleable#SeekBar_tickMarkTint
     * @see #setTickMarkTintList(ColorStateList)
     */
    @Nullable
    public ColorStateList getTickMarkTintList() {
        return mTickMarkTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setTickMarkTintList(ColorStateList)}} to the tick mark drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @attr ref android.R.styleable#SeekBar_tickMarkTintMode
     * @see #getTickMarkTintMode()
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    public void setTickMarkTintMode(@Nullable PorterDuff.Mode tintMode) {
        mTickMarkTintMode = tintMode;
        mHasTickMarkTintMode = true;

        applyTickMarkTint();
    }

    /**
     * Returns the blending mode used to apply the tint to the tick mark drawable,
     * if specified.
     *
     * @return the blending mode used to apply the tint to the tick mark drawable
     * @attr ref android.R.styleable#SeekBar_tickMarkTintMode
     * @see #setTickMarkTintMode(PorterDuff.Mode)
     */
    @Nullable
    public PorterDuff.Mode getTickMarkTintMode() {
        return mTickMarkTintMode;
    }

    protected void applyTickMarkTint() {
        if (mTickMark != null && (mHasTickMarkTint || mHasTickMarkTintMode)) {
            mTickMark = DrawableCompat.wrap(mTickMark.mutate());

            if (mHasTickMarkTint) {
                DrawableCompat.setTintList(mTickMark, mTickMarkTintList);
            }

            if (mHasTickMarkTintMode) {
                DrawableCompat.setTintMode(mTickMark, mTickMarkTintMode);
            }

            // The drawable (or one of its children) may not have been
            // stateful before applying the tint, so let's try again.
            if (mTickMark.isStateful()) {
                mTickMark.setState(getDrawableState());
            }
        }
    }

    public void setKeyProgressIncrement(int increment) {
        mKeyProgressIncrement = increment < 0 ? -increment : increment;
    }

    public float getKeyProgressIncrement() {
        return mKeyProgressIncrement;
    }

    public synchronized void setMax(int max) {
        super.setMax(max);

        if ((mKeyProgressIncrement == 0) || (getMax() / mKeyProgressIncrement > 20)) {
            setKeyProgressIncrement(getMax() / 20);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mThumbStart || who == mThumbEnd || who == mTickMark || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mThumbStart != null) {
            mThumbStart.jumpToCurrentState();
        }

        if (mThumbEnd != null) {
            mThumbEnd.jumpToCurrentState();
        }

        if (mTickMark != null) {
            mTickMark.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        logger.info("drawableStateChanged(%s)", mWhichThumb);

        final Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null && mDisabledAlpha < 1.0f) {
            progressDrawable.setAlpha(isEnabled() ? NO_ALPHA : (int) (NO_ALPHA * mDisabledAlpha));
        }

        if (mWhichThumb != WhichThumb.None) {
            Drawable thumb = mWhichThumb == WhichThumb.Start ? mThumbStart : mThumbEnd;
            setDrawableState(thumb, getDrawableState());
        } else {
            setDrawableState(mThumbStart, getDrawableState());
            setDrawableState(mThumbEnd, getDrawableState());
        }

        final Drawable tickMark = mTickMark;
        if (tickMark != null && tickMark.isStateful()
            && tickMark.setState(getDrawableState())) {
            invalidateDrawable(tickMark);
        }
    }

    protected void setDrawableState(final Drawable drawable, final int[] drawableState) {
        if (null != drawable && drawable.isStateful() && drawable.setState(drawableState)) {
            invalidateDrawable(drawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mThumbStart != null) {
            logger.verbose("setHotspot(mThumbStart, %.2f, %.2f)", x, y);
            DrawableCompat.setHotspot(mThumbStart, x, y);
        }

        if (mThumbEnd != null) {
            logger.verbose("setHotspot(mThumbEnd, %.2f, %.2f)", x, y);
            DrawableCompat.setHotspot(mThumbEnd, x, y);
        }
    }

    @Override
    public void onVisualProgressChanged(int id, float scaleStart, float scaleEnd) {
        super.onVisualProgressChanged(id, scaleStart, scaleEnd);

        if (id == android.R.id.progress) {
            if (mThumbStart != null && mThumbEnd != null) {
                setThumbPos(getWidth(), mThumbStart, scaleStart, WhichThumb.Start, Integer.MIN_VALUE);
                setThumbPos(getWidth(), mThumbEnd, scaleEnd, WhichThumb.End, Integer.MIN_VALUE);
                invalidate();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        updateThumbAndTrackPos(w, h);
    }

    private void updateThumbAndTrackPos(int w, int h) {
        final int paddedHeight = h - mPaddingTop - mPaddingBottom;
        final Drawable track = getCurrentDrawable();
        final Drawable thumb = mThumbStart;

        // The max height does not incorporate padding, whereas the height
        // parameter does.
        final int trackHeight = Math.min(mMaxHeight, paddedHeight);
        final int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();

        // Apply offset to whichever item is taller.
        final int trackOffset;
        final int thumbOffset;
        if (thumbHeight > trackHeight) {
            final int offsetHeight = (paddedHeight - thumbHeight) / 2;
            trackOffset = offsetHeight + (thumbHeight - trackHeight) / 2;
            thumbOffset = offsetHeight;
        } else {
            final int offsetHeight = (paddedHeight - trackHeight) / 2;
            trackOffset = offsetHeight;
            thumbOffset = offsetHeight + (trackHeight - thumbHeight) / 2;
        }

        if (track != null) {
            final int trackWidth = w - mPaddingRight - mPaddingLeft;
            track.setBounds(0, trackOffset, trackWidth, trackOffset + trackHeight);
        }

        if (mThumbStart != null && mThumbEnd != null) {
            setThumbPos(w, mThumbStart, getScaleStart(), WhichThumb.Start, thumbOffset);
            setThumbPos(w, mThumbEnd, getScaleEnd(), WhichThumb.End, thumbOffset);
        }

        final Drawable background = getBackground();

        if (background != null && thumb != null) {
            final Rect bounds = thumb.getBounds();
            background.setBounds(bounds);
            logger.verbose("setHotspot(background, %d, %d)", bounds.centerX(), bounds.centerY());
            DrawableCompat.setHotspotBounds(
                background, bounds.left, bounds.top, bounds.right, bounds.bottom);

        }
    }

    private float getScaleStart() {
        final float max = getMax();
        return max > 0 ? getProgressStart() / max : 0;
    }

    private float getScaleEnd() {
        final float max = getMax();
        return max > 0 ? getProgressEnd() / max : 0;
    }

    /**
     * Updates the thumb drawable bounds.
     *
     * @param w      Width of the view, including padding
     * @param thumb  Drawable used for the thumb
     * @param scale  Current progress between 0 and 1
     * @param offset Vertical offset for centering. If set to
     *               {@link Integer#MIN_VALUE}, the current offset will be used.
     */
    private void setThumbPos(int w, Drawable thumb, float scale, WhichThumb which, int offset) {
        logger.info("setThumbPos(%d, %g, %s, %d)", w, scale, which, offset);

        int available = (w - mPaddingLeft - mPaddingRight) - getProgressOffset();
        final int thumbWidth = thumb.getIntrinsicWidth();
        final int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbWidth;

        // The extra space for the thumb to move on the track
        available += mThumbOffset * 2;

        final int thumbPos = (int) (scale * available + 0.5f);

        final int top, bottom;
        if (offset == Integer.MIN_VALUE) {
            final Rect oldBounds = thumb.getBounds();
            top = oldBounds.top;
            bottom = oldBounds.bottom;
        } else {
            top = offset;
            bottom = offset + thumbHeight;
        }

        int left = thumbPos;
        int right = left + thumbWidth;

        if (which == WhichThumb.End) {
            left += getProgressOffset();
            right += getProgressOffset();
        }

        final Drawable background = getBackground();

        if (background != null && which == mWhichThumb) {
            final int offsetX = mPaddingLeft - mThumbOffset;
            final int offsetY = mPaddingTop;

            background.setBounds(
                left + offsetX,
                top + offsetY,
                right + offsetX,
                bottom + offsetY
            );

            logger.verbose("DrawableCompat.setHotspotBounds(background)");
            DrawableCompat.setHotspotBounds(
                background,
                left + offsetX,
                top + offsetY,
                right + offsetX,
                bottom + offsetY
            );
        }

        thumb.setBounds(left, top, right, bottom);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawThumb(canvas);

    }

    @Override
    void drawTrack(Canvas canvas) {
        if (mThumbStart != null && mSplitTrack) {
            final Rect tempRect = mTempRect1;

            mThumbStart.copyBounds(tempRect);
            tempRect.offset(mPaddingLeft - mThumbOffset, mPaddingTop);
            tempRect.left += mThumbClipInset;
            tempRect.right -= mThumbClipInset;

            final int saveCount = canvas.save();
            canvas.clipRect(tempRect, Op.DIFFERENCE);

            mThumbEnd.copyBounds(tempRect);
            tempRect.offset(mPaddingLeft - mThumbOffset, mPaddingTop);
            tempRect.left += mThumbClipInset;
            tempRect.right -= mThumbClipInset;

            canvas.clipRect(tempRect, Op.DIFFERENCE);

            super.drawTrack(canvas);
            drawTickMarks(canvas);
            canvas.restoreToCount(saveCount);

        } else {
            super.drawTrack(canvas);
            drawTickMarks(canvas);
        }
    }

    void drawTickMarks(Canvas canvas) {
        if (mTickMark != null) {
            final float count = getMax();
            if (count > 1) {
                final int w = mTickMark.getIntrinsicWidth();
                final int h = mTickMark.getIntrinsicHeight();
                final int halfW = w >= 0 ? w / 2 : 1;
                final int halfH = h >= 0 ? h / 2 : 1;
                mTickMark.setBounds(-halfW, -halfH, halfW, halfH);

                final float spacing = (getWidth() - mPaddingLeft - mPaddingRight) / count;
                final int saveCount = canvas.save();
                canvas.translate(mPaddingLeft, getHeight() / 2f);
                for (int i = 0; i <= count; i++) {
                    mTickMark.draw(canvas);
                    canvas.translate(spacing, 0);
                }
                canvas.restoreToCount(saveCount);
            }
        }
    }

    void drawThumb(Canvas canvas) {
        if (mThumbStart != null && mThumbEnd != null) {
            final int saveCount = canvas.save();
            canvas.translate(mPaddingLeft - mThumbOffset, mPaddingTop);
            mThumbStart.draw(canvas);
            mThumbEnd.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getCurrentDrawable();

        int thumbHeight = mThumbStart == null ? 0 : mThumbStart.getIntrinsicHeight();
        int dw = 0;
        int dh = 0;
        if (d != null) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, d.getIntrinsicWidth()));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, d.getIntrinsicHeight()));
            dh = Math.max(thumbHeight, dh);
        }
        dw += mPaddingLeft + mPaddingRight;
        dh += mPaddingTop + mPaddingBottom;

        setMeasuredDimension(
            resolveSizeAndState(dw, widthMeasureSpec, 0),
            resolveSizeAndState(dh, heightMeasureSpec, 0)
        );
    }

    @SuppressLint ("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                logger.info("ACTION_DOWN");
                if (SephirothViewCompat.isInScrollingContainer(this)) {
                    logger.warn("isInScrollContainer");
                    mTouchDownX = event.getX();
                } else {
                    startDrag(event);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    performClick();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private WhichThumb getNearestThumb(float x, float y) {

        mThumbStart.copyBounds(mTempRect1);
        mTempRect1.inset(mTempRect1.width() / 4, mTempRect1.height() / 4);

        mThumbEnd.copyBounds(mTempRect2);
        mTempRect2.inset(mTempRect2.width() / 4, mTempRect2.height() / 4);

        float diff1 = Math.abs((x) - mTempRect1.centerX());
        float diff2 = Math.abs((x) - mTempRect2.centerX());

        if (mTempRect1.contains((int) x, (int) y)) {
            return WhichThumb.Start;
        }
        if (mTempRect2.contains((int) x, (int) y)) {
            return WhichThumb.End;
        }

        return diff1 < diff2 ? WhichThumb.Start : WhichThumb.End;
    }

    private void startDrag(MotionEvent event) {
        logger.info("startDrag");

        if (null == mThumbStart || null == mThumbEnd) {
            logger.error("missing one of the thumbs!");
            return;
        }

        mWhichThumb = getNearestThumb(event.getX(), event.getY());
        logger.verbose("mWhichThumb: %s", mWhichThumb);

        setPressed(true);

        if (mWhichThumb != WhichThumb.None) {
            final Drawable thumb = mWhichThumb == WhichThumb.Start ? mThumbStart : mThumbEnd;

            if (thumb != null) {
                final float scale = mWhichThumb == WhichThumb.Start ? getScaleStart() : getScaleEnd();
                setThumbPos(getWidth(), thumb, scale, mWhichThumb, Integer.MIN_VALUE);
                invalidate(thumb.getBounds());
            }
        }

        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    @Override
    public void setPressed(final boolean pressed) {
        logger.debug("setPressed(%b, %s)", pressed, mWhichThumb);

        if (!pressed) {
            mWhichThumb = WhichThumb.None;
        }

        super.setPressed(pressed);
    }

    private void setHotspot(float x, float y) { }

    private void trackTouchEvent(MotionEvent event) {
        if (null == mThumbStart || null == mThumbEnd) {
            return;
        }

        float x = event.getX();
        float y = event.getY();
        final int width = getWidth();

        if (mWhichThumb == WhichThumb.End) {
            x -= getProgressOffset();
        }

        final int thumbWidth = mThumbStart.getIntrinsicWidth();
        final int availableWidth = width - mPaddingLeft - mPaddingRight - getProgressOffset() - thumbWidth + mThumbOffset * 2;

        x -= thumbWidth / 2f;
        x += mThumbOffset;

        final float scale;
        float progress = 0.0f;

        if (x < mPaddingLeft) {
            scale = 0.0f;
        } else if (x > width - mPaddingRight) {
            scale = 1.0f;
        } else {
            scale = (x - mPaddingLeft) / (float) availableWidth;
            progress = mTouchProgressOffset;
        }

        final float max = getMax();
        progress += scale * max;

        setHotspot(x, y);

        if (mWhichThumb == WhichThumb.Start) {
            progress = MathUtils.constrain(progress, 0, getProgressStartMaxValue());
            setProgressInternal(Math.round(progress), getProgressEnd(), true, false);
        } else if (mWhichThumb == WhichThumb.End) {
            progress = MathUtils.constrain(progress, getProgressEndMinValue(), getMax());
            setProgressInternal(getProgressStart(), Math.round(progress), true, false);
        }
    }

    @Override
    synchronized boolean setProgressInternal(
        int startValue, int endValue, final boolean fromUser, final boolean animate) {

        if (mStepSize > 1) {
            final int remainderStart = startValue % mStepSize;

            if (remainderStart > 0) {
                if ((float) remainderStart / mStepSize > 0.5) {
                    // value + (step-(value%step))
                    startValue = startValue + (mStepSize - remainderStart);
                } else {
                    // value - (value%step)
                    startValue = startValue - remainderStart;
                }
            }

            int remainderEnd = endValue % mStepSize;

            if (remainderEnd > 0) {
                if ((float) remainderEnd / mStepSize > 0.5) {
                    endValue = endValue + (mStepSize - remainderEnd);
                } else {
                    endValue = endValue - remainderEnd;
                }
            }
        }

        return super.setProgressInternal(startValue, endValue, fromUser, animate);
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
        if (mOnRangeSeekBarChangeListener != null) {
            mOnRangeSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
        if (mOnRangeSeekBarChangeListener != null) {
            mOnRangeSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    void onKeyChange() {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            int increment = mKeyProgressIncrement;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_MINUS:
                    increment = -increment;
                    // fallthrough
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_PLUS:
                case KeyEvent.KEYCODE_EQUALS:
                    if (setProgressInternal(getProgressStart() - increment, getProgressEnd() + increment, true, true)) {
                        onKeyChange();
                        return true;
                    }
                    break;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return RangeSeekBar.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        return super.performAccessibilityAction(action, arguments);
        // TODO: to be implemented
    }
}
