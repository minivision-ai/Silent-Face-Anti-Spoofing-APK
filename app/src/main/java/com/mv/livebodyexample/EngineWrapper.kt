package com.mv.livebodyexample

import android.content.res.AssetManager
import com.mv.engine.FaceBox
import com.mv.engine.FaceDetector
import com.mv.engine.Live

/**
 * EngineWrapper acts as a bridge between the Android UI/Camera and the underlying C++ Engine.
 * It manages the initialization, execution, and cleanup of the FaceDetector and Liveness models.
 */
class EngineWrapper(private var assetManager: AssetManager) {

    // Instance of FaceDetector used to find face bounding boxes in the camera frame
    private var faceDetector: FaceDetector = FaceDetector()
    
    // Instance of the Live model used to determine if the detected face is real or a spoof (fake)
    private var live: Live = Live()

    /**
     * Initializes the native C++ models by loading them from the Android assets folder.
     * @return true if both models load successfully, false otherwise.
     */
    fun init(): Boolean {
        // Load the face detection model first. Returns 0 on success.
        var ret = faceDetector.loadModel(assetManager)
        
        // If face detection model loaded successfully
        if (ret == 0) {
            // Load the liveness (anti-spoofing) model
            ret = live.loadModel(assetManager)
            
            // Return true if liveness model also loaded successfully (ret == 0)
            return ret == 0 
        }

        // Return false if initialization failed at any step
        return false 
    }

    /**
     * Cleans up native C++ resources when the engine is no longer needed.
     * This is crucial to prevent memory leaks in the C++ layer.
     */
    fun destroy() {
        // Free the face detector native memory
        faceDetector.destroy()
        
        // Free the liveness model native memory
        live.destroy()
    }

    /**
     * Performs face detection and liveness checking on a given YUV camera frame.
     * @param yuv The raw camera frame data in YUV 420 format.
     * @param width The width of the camera frame.
     * @param height The height of the camera frame.
     * @param orientation The rotation/orientation of the camera frame.
     * @return DetectionResult containing the face box, inference time, and liveness confidence.
     */
    fun detect(yuv: ByteArray, width: Int, height: Int, orientation: Int): DetectionResult {
        // Step 1: Call native face detector to find faces in the given frame
        val boxes = detectFace(yuv, width, height, orientation)
        
        // Step 2: If at least one face is detected in the frame
        if (boxes.isNotEmpty()) {
            
            // Record start time to measure how long the liveness check takes
            val begin = System.currentTimeMillis() 
            
            // Step 3: Take the first detected face (index 0) and apply liveness check
            val box = boxes[0].apply {
                // Run liveness detection on the cropped area of this specific face
                val c = detectLive(yuv, width, height, orientation, this)
                
                // Store the calculated liveness confidence score inside the FaceBox object
                confidence = c
            }
            
            // Record end time after inference is complete
            val end = System.currentTimeMillis() 
            
            // Return result wrapper containing: The face box, time taken (end - begin), and true (face detected)
            return DetectionResult(box, end - begin, true)
        }

        // Return an empty/default result if no face was found in the frame
        return DetectionResult()
    }

    /**
     * Helper function to detect face bounding boxes from YUV data.
     * Delegates the call to the FaceDetector component.
     */
    private fun detectFace(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int
    ): List<FaceBox> = faceDetector.detect(yuv, width, height, orientation)

    /**
     * Helper function to determine liveness confidence score for a specific face box.
     * Delegates the call to the Live component.
     */
    private fun detectLive(
        yuv: ByteArray,
        width: Int,
        height: Int,
        orientation: Int,
        faceBox: FaceBox
    ): Float = live.detect(yuv, width, height, orientation, faceBox)

}