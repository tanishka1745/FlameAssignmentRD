package com.example.flameassignmentrd.app

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.flameassignmentrd.R
import com.example.flameassignmentrd.gl.GLRenderer
import com.example.flameassignmentrd.gl.GLTextureView
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.jar.Manifest
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var glView: GLTextureView
    private lateinit var renderer: GLRenderer

    companion object {
        private const val TAG = "MainActivity"
        private const val REQ_CAMERA = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glView)
        renderer = GLRenderer()
        Log.d(TAG, "Setting renderer")
        glView.setRenderer(renderer)

        // Wait for SurfaceTexture before starting CameraX
        renderer.onSurfaceCreatedCallback = {
            Log.d(TAG, "SurfaceTexture ready")
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Camera permission granted, starting CameraX")
                startCameraX()
            } else {
                Log.d(TAG, "Camera permission not granted, requesting")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQ_CAMERA
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: $requestCode, $grantResults")
        if (requestCode == REQ_CAMERA && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Camera permission granted in onRequestPermissionsResult")
            startCameraX()
        } else {
            Log.e(TAG, "Camera permission denied")
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCameraX() {
        Log.d(TAG, "startCameraX called")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            Log.d(TAG, "CameraProvider ready")
            val cameraProvider = cameraProviderFuture.get()

            val surfaceTexture = renderer.getSurfaceTexture()
            if (surfaceTexture == null) {
                Log.e(TAG, "SurfaceTexture is null, cannot start preview")
                return@addListener
            }

            surfaceTexture.setDefaultBufferSize(640, 480)
            val surface = Surface(surfaceTexture)
            Log.d(TAG, "Surface created from SurfaceTexture")

            val preview = Preview.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .also {
                    it.setSurfaceProvider { request ->
                        Log.d(TAG, "Providing surface to Preview")
                        request.provideSurface(surface, ContextCompat.getMainExecutor(this)) {}
                    }
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        Log.d(TAG, "Analyzing frame")
                        processImageProxy(imageProxy)
                    }
                }

            try {
                Log.d(TAG, "Unbinding all use cases")
                cameraProvider.unbindAll()
                Log.d(TAG, "Binding lifecycle")
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                Log.d(TAG, "CameraX bound successfully")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            Log.d(TAG, "Processing frame from ImageProxy")
            val nv21 = yuv420ToNV21(imageProxy)
            val processedBytes = NativeBridge.processNV21(nv21, imageProxy.width, imageProxy.height)

            val rawBitmap = nv21ToBitmap(nv21, imageProxy.width, imageProxy.height)
            val processedBitmap = nv21ToBitmap(processedBytes, imageProxy.width, imageProxy.height)

            glView.updateFrames(rawBitmap, processedBitmap)
            Log.d(TAG, "Frame updated on GL view")
        } catch (ex: Exception) {
            Log.e(TAG, "Error processing frame", ex)
        } finally {
            imageProxy.close()
        }
    }

    private fun yuv420ToNV21(image: ImageProxy): ByteArray {
        Log.d(TAG, "Converting YUV_420_888 to NV21")
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        yPlane.buffer.get(nv21, 0, ySize)

        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        var pos = ySize

        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val uIndex = row * uvRowStride + col * uvPixelStride
                val vIndex = row * uvRowStride + col * uvPixelStride
                nv21[pos++] = vPlane.buffer.getOrNull(vIndex)
                nv21[pos++] = uPlane.buffer.getOrNull(uIndex)
            }
        }
        return nv21
    }

    private fun ByteBuffer.getOrNull(index: Int): Byte = if (index in 0 until limit()) get(index) else 0

    private fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap {
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        glView.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause called")
        glView.onPause()
        super.onPause()
    }
}