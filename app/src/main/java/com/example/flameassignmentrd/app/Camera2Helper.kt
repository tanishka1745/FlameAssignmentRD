package com.example.flameassignmentrd.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface

class Camera2Helper(private val context: Context) {

    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var handler: Handler
    private var imageReader: ImageReader? = null

    init {
        // Background thread for camera callbacks
        val thread = HandlerThread("camera-thread")
        thread.start()
        handler = Handler(thread.looper)
    }

    @SuppressLint("MissingPermission")
    fun start(previewSurface: Surface, onImage: (Image) -> Unit, width: Int = 640, height: Int = 480) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = manager.cameraIdList.firstOrNull() ?: run {
            Log.e("Camera2Helper", "No camera found")
            return
        }

        // Initialize ImageReader
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    onImage(image)
                    image.close()
                }
            }, handler)
        }

        manager.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val targets = listOf(previewSurface, imageReader!!.surface)
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        this@Camera2Helper.session = session
                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(previewSurface)
                            addTarget(imageReader!!.surface)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        }
                        session.setRepeatingRequest(request.build(), null, handler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("Camera2Helper", "Capture session configuration failed")
                    }
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                Log.e("Camera2Helper", "Camera error: $error")
            }
        }, handler)
    }

    fun stop() {
        session?.close()
        session = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }
}