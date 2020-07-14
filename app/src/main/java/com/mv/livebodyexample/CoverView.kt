package com.mv.livebodyexample

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CoverView: View {

    private var paint: Paint
    private var backColor: Int

    private var horizontalPadding: Int
    private var verticalPadding: Int

    private var porterDuffXfermode: PorterDuffXfermode

    private var rect: Rect

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CoverView)

        horizontalPadding = typedArray.getDimension(R.styleable.CoverView_horizontalPadding, 0F).toInt()
        verticalPadding = typedArray.getDimension(R.styleable.CoverView_verticalPadding, 0F).toInt()
        backColor = typedArray.getColor(R.styleable.CoverView_backgroundColor, resources.getColor(R.color.colorAccent))

        typedArray.recycle()

        porterDuffXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)

        paint = Paint()
        paint.color = backColor
        paint.xfermode = porterDuffXfermode

        rect = Rect()

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas?) {
        val layerId = canvas?.saveLayer(0F, 0F, width.toFloat(), height.toFloat(), null, Canvas.ALL_SAVE_FLAG)

        canvas?.drawRect(0F, 0F, width.toFloat(), height.toFloat(), paint)

        rect.apply {
            left = horizontalPadding
            top = verticalPadding
            right = width.minus(horizontalPadding)
            bottom = height.minus(verticalPadding)

            paint.color = Color.TRANSPARENT
            canvas?.drawRect(this, paint)
        }

        if (layerId != null) {
            canvas.restoreToCount(layerId)
        }
    }
}