package com.example.flameassignmentrd.app

import android.content.pm.PackageManager
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

    private lateinit var glView: GLTextureView      // Keep your existing GLTextureView type
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
        // If your GL view supports render modes, you can use RENDERMODE_WHEN_DIRTY and call requestRender()
        // glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

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