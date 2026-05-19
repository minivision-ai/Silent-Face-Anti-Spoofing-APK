@file:Suppress("DEPRECATION")

package com.mv.livebodyexample

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.mv.livebodyexample.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.IOException

/**
 * MainActivity is the primary screen of the application.
 * It handles camera preview, requests permissions, and passes camera frames to the EngineWrapper for AI inference.
 */
@ObsoleteCoroutinesApi
class MainActivity : AppCompatActivity(), SetThresholdDialogFragment.ThresholdDialogListener {

    // Data binding object for activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // Flag to check if the C++ AI Engine is successfully loaded and ready
    private var enginePrepared: Boolean = false
    
    // Wrapper class to communicate with FaceDetector and Live C++ models
    private lateinit var engineWrapper: EngineWrapper
    
    // Confidence threshold required to classify a face as "Real"
    private var threshold: Float = defaultThreshold

    // Android Hardware Camera instance (using older Camera1 API)
    private var camera: Camera? = null
    
    // Default to front-facing camera
    private var cameraId: Int = Camera.CameraInfo.CAMERA_FACING_FRONT
    
    // Requested camera preview width and height
    private val previewWidth: Int = 640
    private val previewHeight: Int = 480

    // Reference to the preview callback
    private var previewCallback: Camera.PreviewCallback? = null

    /**
     * Orientation value (7) indicates how the frame data should be rotated before processing.
     *    1       2       3       4        5          6          7            8
     * <p>
     * 888888  888888      88  88      8888888888  88                  88  8888888888
     * 88          88      88  88      88  88      88  88          88  88      88  88
     * 8888      8888    8888  8888    88          8888888888  8888888888          88
     * 88          88      88  88
     * 88          88  888888  888888
     */
    private var frameOrientation: Int = 7

    // Dimensions of the device screen
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Scaling factors to map camera coordinates (previewWidth x previewHeight) to screen coordinates
    private var factorX: Float = 0F
    private var factorY: Float = 0F

    // A single background thread dedicated to running AI inference so it doesn't freeze the UI
    private val detectionContext = newSingleThreadContext("detection")
    
    // Flag to prevent overlapping inference jobs (drops frames if AI is still processing the previous one)
    private var working: Boolean = false

    // Animator for the scanning line visual effect
    private lateinit var scaleAnimator: ObjectAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if camera permission is granted
        if (hasPermissions()) {
            init() // Initialize app if permissions exist
        } else {
            requestPermission() // Prompt user for permissions
        }

    }

    /**
     * Checks if the required permissions (Camera) are granted by the user.
     */
    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    // Requests camera permissions natively
    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermission() = requestPermissions(permissions, permissionReqCode)

    /**
     * Main initialization function for UI and Camera setup.
     */
    private fun init() {
        // Setup data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.result = DetectionResult() // Initialize empty result to UI

        // Get screen dimensions
        calculateSize()

        // Setup the SurfaceView which displays the camera preview
        binding.surface.holder.let {
            val surfaceCallback = object : SurfaceHolder.Callback, Camera.PreviewCallback {
                
                // Called when the surface size or format changes
                override fun surfaceChanged(
                    holder: SurfaceHolder?,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    if (holder?.surface == null || camera == null) return

                    try {
                        camera?.stopPreview() // Stop preview before making changes
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }

                    // Set camera preview resolution
                    val parameters = camera?.parameters
                    parameters?.setPreviewSize(previewWidth, previewHeight)

                    // Calculate scaling factors: Camera is rotated 90 degrees, so we map previewHeight to screenWidth
                    factorX = screenWidth / previewHeight.toFloat()
                    factorY = screenHeight / previewWidth.toFloat()

                    camera?.parameters = parameters

                    camera?.startPreview() // Restart camera preview
                    camera?.setPreviewCallback(this) // Set this class to receive the raw camera frames

                    setCameraDisplayOrientation() // Fix rotation for display
                }

                // Called when the surface is destroyed (e.g., app goes to background)
                override fun surfaceDestroyed(holder: SurfaceHolder?) {
                    camera?.setPreviewCallback(null) // Stop sending frames
                    camera?.release() // Release camera hardware
                    camera = null
                }

                // Called when the surface is first created
                override fun surfaceCreated(holder: SurfaceHolder?) {
                    try {
                        // Try opening front camera
                        camera = Camera.open(cameraId)
                    } catch (e: Exception) {
                        // If front camera fails, try back camera
                        cameraId = Camera.CameraInfo.CAMERA_FACING_BACK
                        camera = Camera.open(cameraId)
                    }

                    try {
                        camera!!.setPreviewDisplay(binding.surface.holder) // Link camera to the SurfaceView
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                /**
                 * This function is called continuously whenever the camera captures a new frame.
                 * @param data The raw YUV byte array frame.
                 */
                override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
                    // Only process if the AI engine is ready and data exists
                    if (enginePrepared && data != null) {
                        
                        // If we are not already processing a previous frame
                        if (!working) {
                            // Launch detection on a background thread
                            GlobalScope.launch(detectionContext) {
                                working = true // Lock to drop incoming frames
                                
                                // Perform face and liveness detection via EngineWrapper
                                val result = engineWrapper.detect(
                                    data,
                                    previewWidth,
                                    previewHeight,
                                    frameOrientation
                                )
                                result.threshold = threshold // Attach current threshold

                                // Map the detected bounding box (from camera resolution) to phone screen resolution
                                val rect = calculateBoxLocationOnScreen(
                                    result.left,
                                    result.top,
                                    result.right,
                                    result.bottom
                                )

                                // Update the UI data binding with the new coordinates
                                binding.result = result.updateLocation(rect)

                                Log.d(
                                    tag,
                                    "threshold:${result.threshold}, confidence: ${result.confidence}"
                                )

                                // Redraw the rectangle view on the screen
                                binding.rectView.postInvalidate()
                                working = false // Unlock to process the next frame
                            }
                        }
                    }
                }
            }
            previewCallback = surfaceCallback
            it.setFormat(ImageFormat.NV21) // Set format to NV21 (YUV420)
            it.addCallback(surfaceCallback)
        }

        // Create an animation that moves the "scan" line up and down infinitely
        scaleAnimator = ObjectAnimator.ofFloat(binding.scan, View.SCALE_Y, 1F, -1F, 1F).apply {
            this.duration = 3000
            this.repeatCount = ValueAnimator.INFINITE
            this.repeatMode = ValueAnimator.REVERSE
            this.interpolator = LinearInterpolator()
            this.start() // Start animation
        }

    }

    /**
     * Gets the current screen width and height in pixels.
     */
    private fun calculateSize() {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }

    /**
     * Scales the bounding box coordinates from camera preview size to actual screen size.
     */
    private fun calculateBoxLocationOnScreen(left: Int, top: Int, right: Int, bottom: Int): Rect =
        Rect(
            (left * factorX).toInt(),
            (top * factorY).toInt(),
            (right * factorX).toInt(),
            (bottom * factorY).toInt()
        )

    /**
     * Adjusts the camera preview rotation based on device orientation so it doesn't look sideways.
     */
    private fun setCameraDisplayOrientation() {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera!!.setDisplayOrientation(result)
    }

    /**
     * Triggered when the user clicks the "Settings" button. Shows the threshold adjustment dialog.
     */
    fun setting(@Suppress("UNUSED_PARAMETER") view: View) =
        SetThresholdDialogFragment().show(supportFragmentManager, "dialog")

    /**
     * Triggered when the user clicks the "Switch Camera" button.
     */
    fun switchCamera(@Suppress("UNUSED_PARAMETER") view: View) {
        try {
            camera?.stopPreview()
            camera?.setPreviewCallback(null)
            camera?.release()
            camera = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cameraId = if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            frameOrientation = 6
            Camera.CameraInfo.CAMERA_FACING_BACK
        } else {
            frameOrientation = 7
            Camera.CameraInfo.CAMERA_FACING_FRONT
        }

        try {
            camera = Camera.open(cameraId)
            camera?.setPreviewDisplay(binding.surface.holder)
            
            val parameters = camera?.parameters
            parameters?.setPreviewSize(previewWidth, previewHeight)
            camera?.parameters = parameters
            
            setCameraDisplayOrientation()
            camera?.startPreview()
            camera?.setPreviewCallback(previewCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Callback from SetThresholdDialogFragment. Updates the global confidence threshold.
     */
    override fun onDialogPositiveClick(t: Float) {
        threshold = t
    }

    /**
     * Handles the result of the permission request.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionReqCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init() // Start camera if granted
            } else {
                // Show a toast message if permission is denied
                Toast.makeText(this, "Please grant camera permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Called when the activity comes to the foreground. Initializes the AI engine.
     */
    override fun onResume() {
        engineWrapper = EngineWrapper(assets)
        enginePrepared = engineWrapper.init() // Load models

        if (!enginePrepared) {
            Toast.makeText(this, "Engine init failed.", Toast.LENGTH_LONG).show()
        }

        super.onResume()
    }

    /**
     * Called when the activity is destroyed. Cleans up native memory and stops animations.
     */
    override fun onDestroy() {
        engineWrapper.destroy() // Free C++ memory
        scaleAnimator.cancel() // Stop scan animation
        super.onDestroy()
    }

    companion object {
        const val tag = "MainActivity"
        
        // Default cutoff threshold for classifying a face as Real vs Fake
        const val defaultThreshold = 0.915F 

        val permissions: Array<String> = arrayOf(Manifest.permission.CAMERA)
        const val permissionReqCode = 1
    }

}
