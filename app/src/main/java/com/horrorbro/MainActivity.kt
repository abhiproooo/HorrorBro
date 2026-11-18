package com.horrorbro

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

private val MohoGreen = Color(0xFF4CAF50)
private val PanelDark = Color(0xFF161618)
private val PanelDarker = Color(0xFF101012)
private val EditorBackground = Color(0xFF0B0B0D)
private val BorderGrey = Color(0xFF2C2C2E)
private val TextMuted = Color(0xFFB0B0B4)
private val Peach = Color(0xFFFFDAB9)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorrorBroTheme {
                EditorScreen()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        NativeRenderer.nativeTrimMemory(level)
    }
}

@Composable
private fun HorrorBroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = MohoGreen,
            onPrimary = Color.Black,
            background = EditorBackground,
            onBackground = Color.White,
            surface = PanelDark,
            onSurface = Color.White,
            surfaceVariant = PanelDarker,
            onSurfaceVariant = TextMuted
        ),
        content = content
    )
}

private enum class EditorTool(val label: String) {
    AddPoint("Add\nPoint"),
    Weld("Weld"),
    Bone("Bone")
}

@Composable
private fun EditorScreen() {
    var autoWeld by rememberSaveable { mutableStateOf(false) }
    var autoFill by rememberSaveable { mutableStateOf(true) }
    var autoStroke by rememberSaveable { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf(EditorTool.AddPoint) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        EditorTopBar(
            autoWeld = autoWeld,
            autoFill = autoFill,
            autoStroke = autoStroke,
            onAutoWeldChange = { autoWeld = it },
            onAutoFillChange = { autoFill = it },
            onAutoStrokeChange = { autoStroke = it }
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LeftToolDock(
                selectedTool = selectedTool,
                onToolSelected = { selectedTool = it }
            )
            CanvasArea(modifier = Modifier.weight(1f).fillMaxHeight())
            RightPanel(modifier = Modifier.width(168.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun EditorTopBar(
    autoWeld: Boolean,
    autoFill: Boolean,
    autoStroke: Boolean,
    onAutoWeldChange: (Boolean) -> Unit,
    onAutoFillChange: (Boolean) -> Unit,
    onAutoStrokeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(PanelDark)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AutoToggle("Auto Weld", autoWeld, onAutoWeldChange)
        AutoToggle("Auto Fill", autoFill, onAutoFillChange)
        AutoToggle("Auto Stroke", autoStroke, onAutoStrokeChange)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {}, enabled = false) {
            Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
        }
        IconButton(onClick = {}, enabled = false) {
            Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo")
        }
    }
}

@Composable
private fun AutoToggle(label: String, active: Boolean, onToggle: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (active) MohoGreen else Color.Transparent)
            .border(1.dp, if (active) MohoGreen else BorderGrey, shape)
            .clickable { onToggle(!active) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LeftToolDock(
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(PanelDark)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EditorTool.values().forEach { tool ->
            ToolButton(
                label = tool.label,
                active = selectedTool == tool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
private fun ToolButton(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (active) MohoGreen else PanelDarker)
            .border(1.dp, if (active) MohoGreen else BorderGrey, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else Color(0xFFE0E0E0),
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CanvasArea(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            preserveEGLContextOnPause = true
            setRenderer(ThorVGRenderer())
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    Box(modifier = modifier.background(EditorBackground)) {
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize(),
            update = { it.requestRender() }
        )
        FloatingActionButton(
            onClick = {
                NativeRenderer.nativeRecenter()
                glView.requestRender()
            },
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp),
            shape = CircleShape,
            containerColor = Color(0xFF2A2A2C),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Recenter canvas",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RightPanel(modifier: Modifier = Modifier) {
    var strokeWidth by rememberSaveable { mutableStateOf(6f) }

    Column(
        modifier = modifier
            .background(PanelDark)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PanelHeader("Style")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Swatch("Fill", Peach)
            Swatch("Stroke", Color.Black)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Width ${strokeWidth.roundToInt()}",
            color = TextMuted,
            fontSize = 11.sp
        )
        Slider(
            value = strokeWidth,
            onValueChange = { strokeWidth = it },
            valueRange = 1f..24f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderGrey)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PanelHeader("Layers", modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add layer",
                tint = TextMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LayerRow("Character", visible = true)
        LayerRow("Background", visible = false)
    }
}

@Composable
private fun PanelHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun Swatch(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .border(1.dp, BorderGrey, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun LayerRow(name: String, visible: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = if (visible) MohoGreen else Color(0xFF555555),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = name, color = Color(0xFFD5D5D5), fontSize = 12.sp)
    }
}

private class ThorVGRenderer : GLSurfaceView.Renderer {
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

private object NativeRenderer {
    init {
        System.loadLibrary("horrorbro")
    }

    external fun nativeInit(): Boolean
    external fun nativeResize(width: Int, height: Int)
    external fun nativeRender()
    external fun nativeRecenter()
    external fun nativeTrimMemory(level: Int)
}
