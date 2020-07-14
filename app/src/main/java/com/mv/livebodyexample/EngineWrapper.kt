package com.mv.livebodyexample

import android.content.res.AssetManager
import com.mv.engine.FaceBox
import com.mv.engine.FaceDetector
import com.mv.engine.Live


class EngineWrapper(private var assetManager: AssetManager) {

    private var faceDetector: FaceDetector = FaceDetector()
    private var live: Live = Live()

    fun init(): Boolean {
        var ret = faceDetector.loadModel(assetManager)
        if (ret == 0) {
            ret = live.loadModel(assetManager)
            return ret == 0
        }

        return false
    }

    fun destroy() {
        faceDetector.destroy()
        live.destroy()
    }

    fun detect(yuv: ByteArray, width: Int, height: Int, orientation: Int): DetectionResult {
        val boxes = detectFace(yuv, width, height, orientation)
        if (boxes.isNotEmpty()) {
            val begin = System.currentTimeMillis()
            val box = boxes[0].apply {
                val c = detectLive(yuv, width, height, orientation, this)
                confidence = c
            }
            val end = System.currentTimeMillis()
            return DetectionResult(box, end - begin, true)
        }

        return DetectionResult()
    }

    private fun detectFace(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int
    ): List<FaceBox> = faceDetector.detect(yuv, width, height, orientation)

    private fun detectLive(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int,
        faceBox: FaceBox
    ): Float = live.detect(yuv, width, height, orientation, faceBox)

}