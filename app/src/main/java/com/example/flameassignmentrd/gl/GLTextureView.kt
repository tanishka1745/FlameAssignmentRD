package com.example.flameassignmentrd.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 * Custom GLSurfaceView for displaying camera frames.
 * Connects to GLRenderer.
 */
class GLTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    init {
        // Use OpenGL ES 2.0
        setEGLContextClientVersion(2)
    }

    override fun setRenderer(renderer: Renderer) {
        // Attach renderer
        super.setRenderer(renderer)
        // Render only when data changes to save GPU
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}