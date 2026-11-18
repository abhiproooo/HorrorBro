package com.horrorbro

import android.opengl.GLSurfaceView
import android.os.Bundle
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
    external fun nativeTouchDown(x: Float, y: Float)
    external fun nativeTouchMove(x: Float, y: Float)
    external fun nativeTouchUp()
}
