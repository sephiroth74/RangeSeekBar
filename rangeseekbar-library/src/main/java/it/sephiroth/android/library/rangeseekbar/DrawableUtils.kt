package it.sephiroth.android.library.rangeseekbar

import android.graphics.PorterDuff

object DrawableUtils {
    private const val VECTOR_DRAWABLE_CLAZZ_NAME = "android.graphics.drawable.VectorDrawable"

    fun parseTintMode(value: Int, defaultMode: PorterDuff.Mode): PorterDuff.Mode {
        when (value) {
            3 -> return PorterDuff.Mode.SRC_OVER
            5 -> return PorterDuff.Mode.SRC_IN
            9 -> return PorterDuff.Mode.SRC_ATOP
            14 -> return PorterDuff.Mode.MULTIPLY
            15 -> return PorterDuff.Mode.SCREEN
            16 -> return PorterDuff.Mode.ADD
            else -> return defaultMode
        }
    }
}
