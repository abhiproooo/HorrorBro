package com.horrorbro

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.horrorbro.ui.HorrorBroTheme
import com.horrorbro.ui.WorkspaceScreen
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorrorBroTheme {
                WorkspaceScreen()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        NativeRenderer.nativeTrimMemory(level)
    }
}

class ThorVGRenderer : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        NativeRenderer.nativeInit()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        NativeRenderer.nativeResize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        NativeRenderer.nativeRender()
    }
}

fun createHorrorBroCanvas(context: Context): GLSurfaceView {
    val view = GLSurfaceView(context)
    view.setEGLContextClientVersion(3)
    view.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
    view.preserveEGLContextOnPause = true
    view.setRenderer(ThorVGRenderer())
    view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    view.setOnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (NativeRenderer.nativeTouchDown(event.x, event.y)) {
                    view.requestRender()
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {
                var changed = false
                for (index in 0 until event.historySize) {
                    changed = NativeRenderer.nativeTouchMove(
                        event.getHistoricalX(index),
                        event.getHistoricalY(index)
                    ) || changed
                }
                changed = NativeRenderer.nativeTouchMove(event.x, event.y) || changed
                if (changed) {
                    view.requestRender()
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (NativeRenderer.nativeTouchUp()) {
                    view.requestRender()
                }
                true
            }
            else -> false
        }
    }
    return view
}

object NativeRenderer {
    init {
        System.loadLibrary("horrorbro")
    }

    external fun nativeInit(): Boolean
    external fun nativeResize(width: Int, height: Int)
    external fun nativeRender()
    external fun nativeRecenter()
    external fun nativeTrimMemory(level: Int)
    external fun nativeSetTool(toolId: Int)
    external fun nativeTouchDown(x: Float, y: Float): Boolean
    external fun nativeTouchMove(x: Float, y: Float): Boolean
    external fun nativeTouchUp(): Boolean
}
