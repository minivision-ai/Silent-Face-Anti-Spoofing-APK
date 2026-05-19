package com.mv.livebodyexample

import android.graphics.Rect
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.mv.engine.FaceBox

/**
 * DetectionResult holds the output data of the AI engine.
 * It inherits from BaseObservable to allow Android Data Binding to automatically
 * update the UI whenever these variables change.
 */
class DetectionResult(): BaseObservable() {

    // Left coordinate of the face bounding box
    @get:Bindable
    var left: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.left) // Notify UI that the 'left' property changed
        }

    // Top coordinate of the face bounding box
    @get:Bindable
    var top: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.top)
        }

    // Right coordinate of the face bounding box
    @get:Bindable
    var right: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.right)
        }

    // Bottom coordinate of the face bounding box
    @get:Bindable
    var bottom: Int = 0
        set(value) {
            field = value
            notifyPropertyChanged(BR.bottom)
        }

    // Liveness confidence score (0.0 to 1.0) indicating how likely the face is Real
    @get:Bindable
    var confidence: Float = 0.toFloat()
        set(value) {
            field = value
            notifyPropertyChanged(BR.confidence)
        }

    // The time taken by the AI models to process the frame (in milliseconds)
    var time: Long = 0

    // The threshold value used to distinguish Real vs Fake
    var threshold: Float = 0F

    // Flag indicating if a face was detected in the frame
    @get:Bindable
    var hasFace: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.hasFace)
        }

    /**
     * Constructor to create a DetectionResult from a native FaceBox object.
     */
    constructor(faceBox: FaceBox, time: Long, hasFace: Boolean) : this() {
        this.left = faceBox.left
        this.top = faceBox.top
        this.right = faceBox.right
        this.bottom = faceBox.bottom
        this.confidence = faceBox.confidence
        this.time = time
        this.hasFace = hasFace
    }

    /**
     * Updates the UI coordinates with the calculated screen coordinates.
     * (Because camera resolution is different from screen resolution)
     */
    fun updateLocation(rect: Rect): DetectionResult {
        this.left = rect.left
        this.top = rect.top
        this.right = rect.right
        this.bottom = rect.bottom

        return this
    }

}

