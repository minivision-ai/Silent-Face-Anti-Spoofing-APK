package com.mv.engine

import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.Keep
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException

/**
 * Live is the core class responsible for Anti-Spoofing (Liveness Detection).
 * It evaluates whether a detected face is from a real person or a spoof (photo/video).
 */
class Live : Component() {

    // Memory pointer to the C++ Live object instance
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
     * Cleans up native resources to prevent memory leaks.
     */
    override fun destroy() {
        deallocate()
    }

    /**
     * Loads the liveness model configurations and weights from the assets folder.
     * @param assetManager The Android AssetManager to read files.
     * @return 0 if successful, -1 otherwise.
     */
    fun loadModel(assetManager: AssetManager): Int {
        // Parse the config.json file which contains model parameters
        val configs = parseConfig(assetManager)

        // Check if configuration parsing failed
        if (configs.isEmpty()) {
            Log.e(tag, "parse model config failed")
            return -1 // Return failure code
        }

        // Pass the parsed configurations to the native C++ loader
        return nativeLoadModel(assetManager, configs)
    }

    /**
     * Performs liveness detection on a cropped face from a YUV camera frame.
     * @param yuv The raw camera frame data.
     * @param previewWidth Frame width.
     * @param previewHeight Frame height.
     * @param orientation Camera orientation.
     * @param faceBox The bounding box of the face to analyze.
     * @return Float value representing the confidence score (higher means more likely to be real).
     */
    fun detect(
        yuv: ByteArray,
        previewWidth: Int,
        previewHeight: Int,
        orientation: Int,
        faceBox: FaceBox
    ): Float {
        // Validate YUV data length (NV21 format uses width * height * 1.5 bytes)
        if (previewWidth * previewHeight * 3 / 2 != yuv.size) {
            throw IllegalArgumentException("Invalid yuv data")
        }

        // Call the C++ function with the frame data and face bounding box coordinates
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

    /**
     * Reads and parses 'live/config.json' from the assets directory.
     * This JSON tells the C++ engine about the AI model shapes, scaling, and shifts.
     */
    private fun parseConfig(assetManager: AssetManager): List<ModelConfig> {
        // Open the JSON config file
        val inputStream = assetManager.open("live/config.json")
        val br = BufferedReader(InputStreamReader(inputStream))
        val line = br.readLine() // Read the single-line JSON array

        // Parse it into a JSON Array
        val jsonArray = JSONArray(line)

        val list = mutableListOf<ModelConfig>()
        
        // Loop through each model configuration in the array
        for (i in 0 until jsonArray.length()) {
            val config: JSONObject = jsonArray.getJSONObject(i)
            
            // Map the JSON keys to the ModelConfig Kotlin object
            ModelConfig().apply {
                name = config.optString("name")
                width = config.optInt("width")
                height = config.optInt("height")
                scale = config.optDouble("scale").toFloat()
                shift_x = config.optDouble("shift_x").toFloat()
                shift_y = config.optDouble("shift_y").toFloat()
                org_resize = config.optBoolean("org_resize")

                // Add to our list
                list.add(this)
            }
        }
        return list // Return the parsed configurations
    }

    companion object {
        const val tag = "Live"
    }


    ///////////////////////////////////// Native (JNI) Functions ////////////////////////////////////
    
    // Allocates the C++ Live object
    @Keep
    private external fun allocate(): Long

    // Frees the C++ Live object
    @Keep
    private external fun deallocate()

    // Loads the C++ model files based on the parsed configurations
    @Keep
    private external fun nativeLoadModel(
        assetManager: AssetManager,
        configs: List<ModelConfig>
    ): Int

    // Runs liveness inference in C++ using the frame data and coordinates
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