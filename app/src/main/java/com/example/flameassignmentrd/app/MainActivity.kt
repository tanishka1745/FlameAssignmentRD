package com.example.flameassignmentrd.app

import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
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
            Log.d(TAG, "Toggle clicked -> showProcessed=${renderer.showProcessed} invert=${renderer.invert}")
            // Ask GL to redraw with new toggle state
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
            // wait up to ~5 seconds for surface texture
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
                    Log.d(TAG, "Starting camera with preview surface")
                    // start preview; cameraHelper's onImage callback must NOT close the Image, MainActivity will
                    cameraHelper.start(surface, { image -> onImageAvailable(image) }, 640, 480)
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to start camera: ${ex.message}", ex)
                }
            }
        }
    }
    // Process each incoming Image. Caller MUST close the Image (we do it in finally).
    private fun onImageAvailable(image: Image) {
        try {
            Log.d(TAG, "Frame received ${image.width}x${image.height} format=${image.format}")
            if (image.format != ImageFormat.YUV_420_888) {
                Log.w(TAG, "Unexpected image format: ${image.format}")
            }
            val nv21 = imageToNV21(image) // robust conversion
            val out = NativeBridge.processNV21(nv21, image.width, image.height)
            if (out.isNotEmpty()) {
                renderer.updateProcessed(out, image.width, image.height)
                // ensure GL updates (if using RENDERMODE_WHEN_DIRTY)
                glView.requestRender()
            } else {
                Log.w(TAG, "NativeBridge returned empty output")
            }
            frames++
        } catch (ex: Exception) {
            Log.e(TAG, "Error processing frame", ex)
        } finally {
            try {
                image.close()
            } catch (ex: Exception) {
                Log.w(TAG, "Exception closing image", ex)
            }
        }
    }

    // Robust NV21 conversion for YUV_420_888 -> NV21 (VU interleaved)
    private fun imageToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val nv21 = ByteArray(ySize + ySize / 2)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        // Copy Y
        var pos = 0
        val yRow = ByteArray(yRowStride)
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            if (yPixelStride == 1) {
                yBuffer.get(nv21, pos, width)
                pos += width
            } else {
                yBuffer.get(yRow, 0, yRowStride)
                var col = 0
                while (col < width) {
                    nv21[pos++] = yRow[col * yPixelStride]
                    col++
                }
            }
        }

        // Copy interleaved VU
        pos = ySize
        val uRow = ByteArray(uvRowStride)
        val vRow = ByteArray(uvRowStride)
        val halfH = height / 2
        val halfW = width / 2
        for (row in 0 until halfH) {
            uBuffer.position(row * uvRowStride)
            vBuffer.position(row * uvRowStride)
            uBuffer.get(uRow, 0, uvRowStride)
            vBuffer.get(vRow, 0, uvRowStride)

            var col = 0
            for (i in 0 until halfW) {
                val uIndex = i * uvPixelStride
                val vIndex = i * uvPixelStride
                // NV21 expects V then U
                nv21[pos++] = vRow[vIndex]
                nv21[pos++] = uRow[uIndex]
                col += 2
            }
        }
        return nv21
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