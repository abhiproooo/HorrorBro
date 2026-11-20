package com.horrorbro

import android.content.Context
import android.content.ContextWrapper
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

    internal var canvasView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContent {
            HorrorBroTheme {
                WorkspaceScreen()
            }
        }
    }

    override fun onDestroy() {
        canvasView = null
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        nativeTrimMemory(level)
    }

    external fun nativeInit(): Boolean
    external fun nativeResize(width: Int, height: Int)
    external fun nativeRender()
    external fun nativeRecenter()
    external fun nativeTrimMemory(level: Int)
    external fun setActiveTool(toolId: Int)
    external fun setAutoStroke(enabled: Boolean)
    external fun setStrokeWidth(width: Float)
    external fun onTouchDown(x: Float, y: Float): Boolean
    external fun onTouchMove(x: Float, y: Float): Boolean
    external fun onTouchUp(): Boolean

    companion object {
        @Volatile
        internal var instance: MainActivity? = null

        init {
            System.loadLibrary("horrorbro")
        }
    }
}

class ThorVGRenderer : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        MainActivity.instance?.nativeInit()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        MainActivity.instance?.nativeResize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        MainActivity.instance?.nativeRender()
    }
}

private fun Context.findMainActivity(): MainActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return current as? MainActivity
}

fun createHorrorBroCanvas(context: Context): GLSurfaceView {
    val view = GLSurfaceView(context)
    val activity = context.findMainActivity()

    view.setEGLContextClientVersion(3)
    view.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
    view.preserveEGLContextOnPause = true
    view.setRenderer(ThorVGRenderer())
    view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    activity?.canvasView = view

    view.setOnTouchListener { surface, event ->
        val target = activity ?: MainActivity.instance ?: return@setOnTouchListener false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                target.onTouchDown(event.x, event.y)
                surface.requestRender()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.historySize) {
                    target.onTouchMove(
                        event.getHistoricalX(index),
                        event.getHistoricalY(index)
                    )
                }
                target.onTouchMove(event.x, event.y)
                surface.requestRender()
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                target.onTouchUp()
                surface.requestRender()
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

    fun nativeInit(): Boolean = MainActivity.instance?.nativeInit() ?: false

    fun nativeResize(width: Int, height: Int) {
        MainActivity.instance?.nativeResize(width, height)
    }

    fun nativeRender() {
        MainActivity.instance?.nativeRender()
    }

    fun nativeRecenter() {
        MainActivity.instance?.let {
            it.nativeRecenter()
            it.canvasView?.requestRender()
        }
    }

    fun nativeTrimMemory(level: Int) {
        MainActivity.instance?.nativeTrimMemory(level)
    }

    fun setActiveTool(toolId: Int) {
        MainActivity.instance?.let {
            it.setActiveTool(toolId)
            it.canvasView?.requestRender()
        }
    }

    fun setAutoStroke(enabled: Boolean) {
        MainActivity.instance?.let {
            it.setAutoStroke(enabled)
            it.canvasView?.requestRender()
        }
    }

    fun setStrokeWidth(width: Float) {
        MainActivity.instance?.let {
            it.setStrokeWidth(width)
            it.canvasView?.requestRender()
        }
    }

    fun onTouchDown(x: Float, y: Float): Boolean =
        MainActivity.instance?.onTouchDown(x, y) ?: false

    fun onTouchMove(x: Float, y: Float): Boolean =
        MainActivity.instance?.onTouchMove(x, y) ?: false

    fun onTouchUp(): Boolean =
        MainActivity.instance?.onTouchUp() ?: false
}
