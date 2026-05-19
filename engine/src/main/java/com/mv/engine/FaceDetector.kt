package com.mv.engine

import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.annotation.Keep
import java.lang.IllegalArgumentException

/**
 * FaceDetector is responsible for finding face bounding boxes in an image or camera frame.
 * It interfaces directly with the C++ native layer via JNI (Java Native Interface).
 */
@Keep // @Keep prevents ProGuard/R8 from obfuscating this class, which is needed for JNI
class FaceDetector : Component() {

    // Stores the memory address (pointer) of the C++ FaceDetector object instance
    @Keep
    private var nativeHandler: Long

    init {
        // Automatically allocate memory in C++ when the Java object is created
        nativeHandler = createInstance()
    }

    /**
     * Calls the native allocate() function to create a C++ instance.
     */
    override fun createInstance(): Long = allocate()

    /**
     * Loads the face detection model (e.g., NCNN/TFLite) from the Android assets folder.
     * @param assetsManager Android AssetManager to read files.
     * @return Status code from C++ (usually 0 means success).
     */
    fun loadModel(assetsManager: AssetManager): Int = nativeLoadModel(assetsManager)

    /**
     * Detects faces in an Android Bitmap image.
     * Only ARGB_8888 config is supported by the native layer.
     */
    fun detect(bitmap: Bitmap): List<FaceBox> = when (bitmap.config) {
        // If bitmap is in standard ARGB_8888 format, pass it to C++
        Bitmap.Config.ARGB_8888 -> nativeDetectBitmap(bitmap)
        // Otherwise, throw an error because C++ expects exactly ARGB_8888 memory layout
        else -> throw IllegalArgumentException("Invalid bitmap config value")
    }

    /**
     * Detects faces in a raw YUV camera frame (typically from Camera1/Camera2 APIs).
     * @param yuv The raw byte array of YUV data.
     * @param previewWidth The width of the camera frame.
     * @param previewHeight The height of the camera frame.
     * @param orientation The rotation of the device/camera.
     * @return A list of FaceBox objects representing detected faces.
     */
    fun detect(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int
    ): List<FaceBox> {
        // Validate that the YUV data size matches NV21/YUV420 format expectations (width * height * 1.5)
        if (previewWidth * previewHeight * 3 / 2 != yuv.size) {
            throw IllegalArgumentException("Invalid yuv data")
        }
        // Pass the validated data to the C++ layer
        return nativeDetectYuv(yuv, previewWidth, previewHeight, orientation)
    }

    /**
     * Cleans up the native C++ memory when this object is destroyed.
     */
    override fun destroy() = deallocate()

    //////////////////////////////// Native (JNI) Functions ////////////////////////////////////
    
    // Allocates the C++ detector object and returns its memory address pointer
    @Keep
    private external fun allocate(): Long

    // Frees the C++ memory associated with this detector
    @Keep
    private external fun deallocate()

    // C++ function to load the model file from assets
    @Keep
    private external fun nativeLoadModel(assetsManager: AssetManager): Int

    // C++ function to run inference on a Bitmap
    @Keep
    private external fun nativeDetectBitmap(bitmap: Bitmap): List<FaceBox>

    // C++ function to run inference on raw YUV bytes
    @Keep
    private external fun nativeDetectYuv(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int
    ): List<FaceBox>

}