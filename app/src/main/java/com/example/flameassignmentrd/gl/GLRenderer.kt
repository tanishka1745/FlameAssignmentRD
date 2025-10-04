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
    private var frameData: ByteArray? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var textureId = -1

    var showProcessed = true   // Toggle raw/processed
    var invert = false         // Bonus invert effect

    fun getSurfaceTexture(): SurfaceTexture? = surfaceTexture

    fun updateProcessed(data: ByteArray, width: Int, height: Int) {
        frameData = data
        frameWidth = width
        frameHeight = height
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        // Generate texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        frameData?.let {
            val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(it))

            if (invert) {
                val invertBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                for (x in 0 until invertBitmap.width) {
                    for (y in 0 until invertBitmap.height) {
                        val pixel = invertBitmap.getPixel(x, y)
                        val r = 255 - ((pixel shr 16) and 0xFF)
                        val g = 255 - ((pixel shr 8) and 0xFF)
                        val b = 255 - (pixel and 0xFF)
                        invertBitmap.setPixel(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
                    }
                }
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, invertBitmap, 0)
                invertBitmap.recycle()
            } else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            }

            bitmap.recycle()

            // Draw full screen quad (simple for now)
            GLES20.glEnable(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }
    }
}