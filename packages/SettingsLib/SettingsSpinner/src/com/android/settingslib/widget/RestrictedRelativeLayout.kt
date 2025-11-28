package com.android.settingslib.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.core.content.withStyledAttributes

class RestrictedRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var maxWidthPx = Int.MAX_VALUE

    init {
        if (attrs != null) {
            context.withStyledAttributes(
                attrs,
                intArrayOf(android.R.attr.maxWidth),
                defStyleAttr,
                0
            ) {
                maxWidthPx = getDimensionPixelSize(0, Int.MAX_VALUE)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val finalMode = if (widthMode == MeasureSpec.EXACTLY)
            MeasureSpec.EXACTLY else MeasureSpec.AT_MOST

        val cappedWidthSpec = if (maxWidthPx != Int.MAX_VALUE) {
            val cappedSize = minOf(widthSize, maxWidthPx)
            MeasureSpec.makeMeasureSpec(cappedSize, finalMode)
        } else {
            widthMeasureSpec
        }

        super.onMeasure(cappedWidthSpec, heightMeasureSpec)
    }
}