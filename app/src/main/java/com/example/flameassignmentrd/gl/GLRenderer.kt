package com.example.flameassignmentrd.gl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renderer that draws camera frames as OpenGL texture.
 * Supports showing processed frames and invert toggle.
 */
class GLRenderer : GLSurfaceView.Renderer {

    private var surfaceTexture: SurfaceTexture? = null
    private var textureId = -1

    private var rawBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null

    var showProcessed = true
    var invert = false

    var onSurfaceCreatedCallback: (() -> Unit)? = null

    fun getSurfaceTexture(): SurfaceTexture? = surfaceTexture

    fun updateFrames(raw: Bitmap, processed: Bitmap) {
        rawBitmap = raw
        processedBitmap = processed
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        surfaceTexture = SurfaceTexture(textureId)
        onSurfaceCreatedCallback?.invoke()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        surfaceTexture?.updateTexImage()  // ✅ Must update the SurfaceTexture

        val bitmap = if (showProcessed) processedBitmap else rawBitmap
        bitmap?.let {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, it, 0)
        }
    }
}