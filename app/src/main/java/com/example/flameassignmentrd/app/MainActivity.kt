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
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    companion object {
        private const val TAG = "MainActivity"
        private const val REQ_CAMERA = 100
    }

    private lateinit var glView: GLTextureView
    private lateinit var renderer: GLRenderer
    private lateinit var cameraHelper: Camera2Helper
    private lateinit var tvFps: TextView

    private var frames = 0
    private var lastTs = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glView)
        tvFps = findViewById(R.id.tvFps)

        // initialize renderer and GL view
        renderer = GLRenderer()
        glView.setRenderer(renderer)

        cameraHelper = Camera2Helper(this)

        findViewById<Button>(R.id.btnToggle).setOnClickListener {
            renderer.showProcessed = !renderer.showProcessed
            renderer.invert = !renderer.invert
            glView.requestRender()
        }

        // FPS counter updater
        Thread {
            while (true) {
                Thread.sleep(500)
                runOnUiThread {
                    val now = System.currentTimeMillis()
                    val fps = if (lastTs > 0) (frames * 1000 / (now - lastTs)) else 0
                    tvFps.text = "FPS: $fps"
                    frames = 0
                    lastTs = now
                }
            }
        }.start()

        // Check permission and start camera when ready
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQ_CAMERA)
        } else {
            startCameraWhenReady()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCameraWhenReady()
        } else {
            Log.e(TAG, "Camera permission not granted")
        }
    }

    // Wait for renderer SurfaceTexture to be available, then start camera
    private fun startCameraWhenReady() {
        thread {
            val timeoutMs = 5000L
            val start = System.currentTimeMillis()
            var st: SurfaceTexture? = null
            while (System.currentTimeMillis() - start < timeoutMs && st == null) {
                st = renderer.getSurfaceTexture()
                Thread.sleep(50)
            }

            if (st == null) {
                Log.e(TAG, "SurfaceTexture not available after timeout")
                return@thread
            }

            val surface = Surface(st)
            runOnUiThread {
                try {
                    cameraHelper.start(surface, { image -> onImageAvailable(image) }, 640, 480)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to start camera: ${ex.message}", ex)
                }
            }
        }
    }
    // Process each incoming Image. Caller MUST close the Image (we do it in finally).
    private fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap {
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val byteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    private fun onImageAvailable(image: Image) {
        try {
            val nv21 = imageToNV21(image)
            val processedBytes = NativeBridge.processNV21(nv21, image.width, image.height)

            val rawBitmap = nv21ToBitmap(nv21, image.width, image.height)
            val processedBitmap = nv21ToBitmap(processedBytes, image.width, image.height)

            renderer.updateFrames(rawBitmap, processedBitmap)
            glView.requestRender()

            frames++
        } catch (ex: Exception) {
            Log.e(TAG, "Error processing frame", ex)
        } finally {
            image.close()
        }
    }

    // Robust NV21 conversion for YUV_420_888 -> NV21 (VU interleaved)
    private fun imageToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        // Copy Y plane
        yPlane.buffer.get(nv21, 0, ySize)

        // Copy UV planes safely
        val uvBuffer = ByteArray(uPlane.buffer.remaining() * 2) // temporary buffer
        var pos = ySize
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val uIndex = row * uvRowStride + col * uvPixelStride
                val vIndex = row * uvRowStride + col * uvPixelStride
                val vByte = vPlane.buffer.getOrNull(vIndex) ?: 0
                val uByte = uPlane.buffer.getOrNull(uIndex) ?: 0
                nv21[pos++] = vByte
                nv21[pos++] = uByte
            }
        }

        return nv21
    }

    // Safe ByteBuffer getOrNull extension
    private fun ByteBuffer.getOrNull(index: Int): Byte {
        return if (index >= 0 && index < limit()) get(index) else 0
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        // stop camera before pausing GL
        cameraHelper.stop()
        glView.onPause()
        super.onPause()
    }
}