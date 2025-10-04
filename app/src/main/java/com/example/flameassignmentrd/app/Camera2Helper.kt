package com.example.flameassignmentrd.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface

class Camera2Helper(private val context: Context) {


    // Reference to the opened camera
    private var cameraDevice: CameraDevice? = null

    // Reference to capture session
    private var session: CameraCaptureSession? = null

    // Background handler for camera operations
    private var handler: Handler

    // ImageReader to receive frames
    private var imageReader: ImageReader? = null

    init {
        // Create a background thread to handle camera callbacks
        val thread = HandlerThread("camera-thread")
        thread.start()
        handler = Handler(thread.looper)
    }

    @SuppressLint("MissingPermission")
    fun start(previewSurface: Surface, onImage: (Image) -> Unit, width: Int = 640, height: Int = 480) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = manager.cameraIdList[0]



        // Create ImageReader to receive frames in YUV_420_888 format
        imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.YUV_420_888, 2)
        // Set listener to receive each frame
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            onImage(image)
        }, handler)

        manager.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val targets = listOf(previewSurface, imageReader!!.surface)
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        this@Camera2Helper.session = session
                        val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(previewSurface)
                            addTarget(imageReader!!.surface)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        }
                        session.setRepeatingRequest(req.build(), null, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) { camera.close() }
            override fun onError(camera: CameraDevice, error: Int) { camera.close() }
        }, handler)
    }
     //Stop camera and release resources
    fun stop() {
        session?.close()
        cameraDevice?.close()
        imageReader?.close()
    }
}