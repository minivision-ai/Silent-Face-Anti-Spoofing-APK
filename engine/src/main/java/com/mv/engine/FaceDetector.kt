package com.mv.engine

import android.content.res.AssetManager
import android.graphics.Bitmap
import androidx.annotation.Keep
import java.lang.IllegalArgumentException

@Keep
class FaceDetector : Component() {

    @Keep
    private var nativeHandler: Long

    init {
        nativeHandler = createInstance()
    }

    override fun createInstance(): Long = allocate()

    fun loadModel(assetsManager: AssetManager): Int = nativeLoadModel(assetsManager)

    fun detect(bitmap: Bitmap): List<FaceBox> = when (bitmap.config) {
        Bitmap.Config.ARGB_8888 -> nativeDetectBitmap(bitmap)
        else -> throw IllegalArgumentException("Invalid bitmap config value")
    }

    fun detect(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int
    ): List<FaceBox> {
        if (previewWidth * previewHeight * 3 / 2 != yuv.size) {
            throw IllegalArgumentException("Invalid yuv data")
        }
        return nativeDetectYuv(yuv, previewWidth, previewHeight, orientation)
    }


    override fun destroy() = deallocate()

    //////////////////////////////// Native ////////////////////////////////////
    @Keep
    private external fun allocate(): Long

    @Keep
    private external fun deallocate()

    @Keep
    private external fun nativeLoadModel(assetsManager: AssetManager): Int

    @Keep
    private external fun nativeDetectBitmap(bitmap: Bitmap): List<FaceBox>

    @Keep
    private external fun nativeDetectYuv(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int
    ): List<FaceBox>


}