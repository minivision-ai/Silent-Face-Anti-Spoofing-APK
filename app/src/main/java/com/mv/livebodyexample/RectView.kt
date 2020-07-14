package com.mv.livebodyexample

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.DecimalFormat

class RectView : View {

    private var confidence: Float = 0F
    private var rect: RectF
    private var paint: Paint
    private var color: Int
    private var radius: Float

    private var textPadding: Int

    private var decimalFormat: DecimalFormat = DecimalFormat("0.000")
    private var lineLength: Float

    private var leftTopAcrRectF: RectF = RectF()
    private var rightTopAcrRectF: RectF = RectF()
    private var leftBottomAcrRectF: RectF = RectF()
    private var rightBottomAcrRectF: RectF = RectF()

    private var textBackgroundRect: Rect = Rect()
    private var textBoundsRect: Rect = Rect()
    private var textWidth: Int
    private var textHeight: Int

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RectView)

        val left = typedArray.getInt(R.styleable.RectView_x1, defaultLeft)
        val top = typedArray.getInt(R.styleable.RectView_y1, defaultTop)
        val right = typedArray.getInt(R.styleable.RectView_x2, defaultRight)
        val bottom = typedArray.getInt(R.styleable.RectView_y2, defaultBottom)
        color = typedArray.getColor(R.styleable.RectView_color, defaultColor)
        val textSize = typedArray.getDimension(R.styleable.RectView_textSize, defaultTextSize)
        textPadding = typedArray.getDimensionPixelOffset(R.styleable.RectView_textPadding, defaultTextPadding)
        radius = typedArray.getDimension(R.styleable.RectView_radius, defaultRadius)
        lineLength = typedArray.getDimension(R.styleable.RectView_lineLength, defaultLineLength)

        typedArray.recycle()

        rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3F
            this.textSize = textSize
        }

        textBoundsRect.apply {
            paint.getTextBounds(sampleText, 0, sampleText.length, this)
            textWidth = this.width()
            textHeight = this.height()
        }

    }

    fun setX1(v: Int) {
        rect.left = v.toFloat()
    }

    fun setX2(v: Int) {
        rect.right = v.toFloat()
    }

    fun setY1(v: Int) {
        rect.top = v.toFloat()
    }

    fun setY2(v: Int) {
        rect.bottom = v.toFloat()
    }

    fun setColor(v: Int) {
        color = v
        paint.color = color
    }

    fun setConfidence(v: Float) {
        confidence = v
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        paint.style = Paint.Style.STROKE
        paint.color = color

        // left top
        canvas?.drawLine(rect.left, rect.top + lineLength, rect.left, rect.top + radius, paint)
        leftTopAcrRectF.apply {
            left = rect.left
            top = rect.top
            right = rect.left + radius * 2
            bottom = rect.top + radius * 2
            canvas?.drawArc(this, 180F, 90F, false, paint)
        }
        canvas?.drawLine(rect.left + radius, rect.top, rect.left + lineLength, rect.top, paint)

        // right top
        canvas?.drawLine(rect.right - lineLength, rect.top, rect.right - radius, rect.top, paint)
        rightTopAcrRectF.apply {
            left = rect.right - radius * 2
            top = rect.top
            right = rect.right
            bottom = rect.top + radius * 2
            canvas?.drawArc(this, 270F, 90F, false, paint)
        }
        canvas?.drawLine(rect.right, rect.top + radius, rect.right, rect.top + lineLength, paint)

        //left bottom
        canvas?.drawLine(rect.left, rect.bottom - lineLength, rect.left, rect.bottom - radius, paint)
        leftBottomAcrRectF.apply {
            left = rect.left
            top = rect.bottom - radius * 2
            right = rect.left + radius * 2
            bottom = rect.bottom
            canvas?.drawArc(this, 180F, -90F, false, paint)
        }
        canvas?.drawLine(rect.left + radius, rect.bottom, rect.left + lineLength, rect.bottom, paint)

        // right bottom
        canvas?.drawLine(rect.right - lineLength, rect.bottom, rect.right - radius, rect.bottom, paint)
        rightBottomAcrRectF.apply {
            left = rect.right - radius * 2
            top = rect.bottom - radius * 2
            right = rect.right
            bottom = rect.bottom
            canvas?.drawArc(this, 90F, -90F, false, paint)
        }
        canvas?.drawLine(rect.right, rect.bottom - radius, rect.right, rect.bottom - lineLength, paint)

        val text = decimalFormat.format(confidence)

        textBackgroundRect.apply {
            left = rect.left.toInt()
            top = rect.top.toInt() - textHeight - 2 * textPadding
            right = rect.left.toInt() + textWidth + 2 * textPadding
            bottom = rect.top.toInt()

            paint.style = Paint.Style.FILL
            canvas?.drawRect(this, paint)
        }

        textBoundsRect.apply {
            left = rect.left.toInt() + textPadding
            top = rect.top.toInt() - textPadding - textHeight
            right = rect.left.toInt() + textPadding + textWidth
            bottom = rect.top.toInt() - textPadding

            paint.color = Color.WHITE
            canvas?.drawText(text, this.left.toFloat(), this.bottom.toFloat(), paint)
        }

    }

    companion object {
        const val sampleText = "0.789"
        const val defaultLeft = 0
        const val defaultTop = 0
        const val defaultRight = 0
        const val defaultBottom = 0
        const val defaultTextSize = 50F
        const val defaultTextPadding = 8
        const val defaultRadius = 5F
        const val defaultLineLength = 45F
        const val defaultColor = Color.RED
    }
}