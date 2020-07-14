package com.mv.livebodyexample

import android.graphics.Rect
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.mv.engine.FaceBox

class DetectionResult(): BaseObservable() {

    @get:Bindable
    var left: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.left)
        }

    @get:Bindable
    var top: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.top)
        }

    @get:Bindable
    var right: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.right)
        }

    @get:Bindable
    var bottom: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.bottom)
        }

    @get:Bindable
    var confidence: Float = 0.toFloat()
        set(value) {
            field = value
            notifyPropertyChanged(BR.confidence)
        }

    var time: Long = 0

    var threshold: Float = 0F

    @get:Bindable
    var hasFace: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.hasFace)
        }

    constructor(faceBox: FaceBox, time: Long, hasFace: Boolean) : this() {
        this.left = faceBox.left
        this.top = faceBox.top
        this.right = faceBox.right
        this.bottom = faceBox.bottom
        this.confidence = faceBox.confidence
        this.time = time
        this.hasFace = hasFace
    }

    fun updateLocation(rect: Rect): DetectionResult {
        this.left = rect.left
        this.top = rect.top
        this.right = rect.right
        this.bottom = rect.bottom

        return this
    }


}

