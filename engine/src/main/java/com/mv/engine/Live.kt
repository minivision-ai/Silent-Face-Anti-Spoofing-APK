package com.mv.engine

import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException

class Live : Component() {

    @Keep
    private var nativeHandler: Long

    init {
        nativeHandler = createInstance()
    }

    override fun createInstance(): Long = allocate()

    override fun destroy() {
        deallocate()
    }

    fun loadModel(assetManager: AssetManager): Int {
        val configs = parseConfig(assetManager)

        if (configs.isEmpty()) {
            Log.e(tag, "parse model config failed")
            return -1
        }

        return nativeLoadModel(assetManager, configs)
    }

    fun detect(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int,
        faceBox: FaceBox
    ): Float {
        if (previewWidth * previewHeight * 3 / 2 != yuv.size) {
            throw IllegalArgumentException("Invalid yuv data")
        }

        return nativeDetectYuv(
            yuv,
            previewWidth,
            previewHeight,
            orientation,
            faceBox.left,
            faceBox.top,
            faceBox.right,
            faceBox.bottom
        )
    }

    private fun parseConfig(assetManager: AssetManager): List<ModelConfig> {
        val inputStream = assetManager.open("live/config.json")
        val br = BufferedReader(InputStreamReader(inputStream))
        val line = br.readLine()

        val jsonArray = JSONArray(line)

        val list = mutableListOf<ModelConfig>()
        for (i in 0 until jsonArray.length()) {
            val config: JSONObject = jsonArray.getJSONObject(i)
            ModelConfig().apply {
                name = config.optString("name")
                width = config.optInt("width")
                height = config.optInt("height")
                scale = config.optDouble("scale").toFloat()
                shift_x = config.optDouble("shift_x").toFloat()
                shift_y = config.optDouble("shift_y").toFloat()
                org_resize = config.optBoolean("org_resize")

                list.add(this)
            }
        }
        return list
    }


    companion object {
        const val tag = "Live"
    }


    ///////////////////////////////////// Native ////////////////////////////////////
    @Keep
    private external fun allocate(): Long

    @Keep
    private external fun deallocate()

    @Keep
    private external fun nativeLoadModel(
        assetManager: AssetManager,
        configs: List<ModelConfig>
    ): Int

    @Keep
    private external fun nativeDetectYuv(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Float
}