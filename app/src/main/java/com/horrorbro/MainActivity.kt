package com.horrorbro

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : ComponentActivity() {
    private var glView: GLSurfaceView? = null

    external fun initThorVG()
    external fun drawFrame(width: Int, height: Int)
    external fun onTouchDown(x: Float, y: Float)
    external fun onTouchMove(x: Float, y: Float)
    external fun setActiveTool(toolId: Int)
    external fun setAutoStroke(enabled: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("horrorbro")
        setContent { HorrorBroApp() }
    }

    fun getGLView(context: android.content.Context): GLSurfaceView {
        if (glView == null) {
            glView = GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                var screenWidth = 0
                var screenHeight = 0
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) { initThorVG() }
                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        screenWidth = width
                        screenHeight = height
                        gl?.glViewport(0, 0, width, height)
                    }
                    override fun onDrawFrame(gl: GL10?) { drawFrame(screenWidth, screenHeight) }
                })
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> onTouchDown(event.x, v.size.height - event.y)
                        MotionEvent.ACTION_MOVE -> onTouchMove(event.x, v.size.height - event.y)
                    }
                    v.performClick()
                    requestRender()
                    true
                }
            }
        }
        return glView!!
    }
}

@Composable
fun HorrorBroApp() {
    var activeTool by remember { mutableStateOf(0) } // 0=None, 1=AddPoint, 2=Freehand
    var autoStroke by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as MainActivity

    // Sync state to C++
    LaunchedEffect(activeTool) { activity.setActiveTool(activeTool) }
    LaunchedEffect(autoStroke) { activity.setAutoStroke(autoStroke) }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF2B2B2B))) {
        Column(modifier = Modifier.width(64.dp).fillMaxHeight().background(Color(0xFF1E1E1E)), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            ToolButton(1, activeTool, { activeTool = it }, Icons.Filled.Add)
            ToolButton(2, activeTool, { activeTool = it }, Icons.Filled.Edit)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF1E1E1E)), verticalAlignment = Alignment.CenterVertically) {
                Text("  ", color = Color.Gray)
                Checkbox(checked = autoStroke, onCheckedChange = { autoStroke = it })
                Text("Auto-stroke", color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { }) { Icon(Icons.Filled.Menu, "Undo", tint = Color.White) }
                IconButton(onClick = { }) { Icon(Icons.Filled.Save, "Save", tint = Color.White) }
            }

            Box(modifier = Modifier.weight(1f)) {
                AndroidView(factory = { activity.getGLView(it) }, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { }, modifier = Modifier.align(Alignment.Center), enabled = false) {
                    Icon(Icons.Filled.Add, contentDescription = "Center", tint = Color.Gray.copy(alpha = 0.3f))
                }
            }
        }

        Column(modifier = Modifier.width(200.dp).fillMaxHeight().background(Color(0xFF1E1E1E))) {
            Text("Style", color = Color.White, modifier = Modifier.padding(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF333333)))
            Text("Layers", color = Color.White, modifier = Modifier.padding(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF333333)))
        }
    }
}

@Composable
fun ToolButton(id: Int, activeTool: Int, onClick: (Int) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Button(
        onClick = { onClick(id) },
        colors = ButtonDefaults.buttonColors(containerColor = if (id == activeTool) Color(0xFF4CAF50) else Color(0xFF333333)),
        modifier = Modifier.padding(4.dp).size(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) { Icon(icon, contentDescription = null) }
}
