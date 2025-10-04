package com.example.flameassignmentrd.app

import android.graphics.SurfaceTexture
import android.media.Image
import android.os.Bundle
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.flameassignmentrd.R
import com.example.flameassignmentrd.gl.GLRenderer
import com.example.flameassignmentrd.gl.GLTextureView


class MainActivity : AppCompatActivity() {

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

        renderer = GLRenderer()
        glView.setRenderer(renderer)

        cameraHelper = Camera2Helper(this)

        findViewById<Button>(R.id.btnToggle).setOnClickListener {
            renderer.showProcessed = !renderer.showProcessed
            renderer.invert = !renderer.invert
        }

        // Start camera after renderer surface is ready
        Thread {
            var st: SurfaceTexture? = null
            while (st == null) {
                st = renderer.getSurfaceTexture()
                Thread.sleep(50)
            }
            val surface = Surface(st)
            runOnUiThread {
                cameraHelper.start(surface, { image -> onImageAvailable(image) }, 640, 480)
            }
        }.start()

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
    }

    private fun onImageAvailable(image: Image) {
        val nv21 = imageToNV21(image)
        val out = NativeBridge.processNV21(nv21, image.width, image.height)
        renderer.updateProcessed(out, image.width, image.height)
        frames++
        image.close()
    }

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

        yBuffer.get(nv21, 0, ySize)

        val row = ByteArray(uPlane.rowStride)
        var offset = ySize
        for (i in 0 until height / 2) {
            vBuffer.get(row, 0, row.size)
            System.arraycopy(row, 0, nv21, offset, width / 2)
            offset += width / 2

            uBuffer.get(row, 0, row.size)
            System.arraycopy(row, 0, nv21, offset, width / 2)
            offset += width / 2
        }
        return nv21
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        cameraHelper.stop()
        glView.onPause()
        super.onPause()
    }
}