package com.example.flameassignmentrd.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 * Custom GLSurfaceView for displaying camera frames.
 * Connects to GLRenderer. */

class GLTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var renderer: GLRenderer? = null

    init {
        setEGLContextClientVersion(2)
    }

    override fun setRenderer(renderer: Renderer) {
        super.setRenderer(renderer)
        this.renderer = renderer as? GLRenderer
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateFrames(raw: Bitmap, processed: Bitmap) {
        renderer?.updateFrames(raw, processed)
        requestRender()
    }

    fun toggleProcessed() {
        renderer?.showProcessed = !renderer?.showProcessed!!
        requestRender()
    }

    fun toggleInvert() {
        renderer?.invert = !renderer?.invert!!
        requestRender()
    }
}