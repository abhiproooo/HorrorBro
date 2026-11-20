package com.horrorbro.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.horrorbro.NativeRenderer
import com.horrorbro.createHorrorBroCanvas
import kotlin.math.roundToInt

private val MohoGreen = Color(0xFF4CAF50)
private val PanelDark = Color(0xFF161618)
private val PanelDarker = Color(0xFF101012)
private val EditorBackground = Color(0xFF0B0B0D)
private val BorderGrey = Color(0xFF2C2C2E)
private val TextMuted = Color(0xFFB0B0B4)
private val TextDim = Color(0xFF7C7C82)
private val ToolIconGrey = Color(0xFFD5D5D5)
private val Peach = Color(0xFFFFDAB9)

@Composable
fun HorrorBroTheme(content: @Composable () -> Unit) {
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

private enum class ToolGroup(val label: String) {
    Draw("Draw"),
    Fill("Fill"),
    Layer("Layer")
}

private enum class EditorTool(
    val label: String,
    val group: ToolGroup,
    val icon: ImageVector,
    val nativeId: Int
) {
    AddPoint("Add Point", ToolGroup.Draw, Icons.Default.Add, 0),
    Freehand("Freehand", ToolGroup.Draw, Icons.Default.Edit, 1),
    Weld("Weld", ToolGroup.Draw, Icons.Default.Link, 2),
    Bone("Bone", ToolGroup.Draw, Icons.Default.Polyline, -1),
    Fill("Fill", ToolGroup.Fill, Icons.Default.FormatColorFill, -1),
    Stroke("Stroke", ToolGroup.Fill, Icons.Default.Brush, -1),
    Layer("Layer", ToolGroup.Layer, Icons.Default.Layers, -1)
}

@Composable
fun WorkspaceScreen() {
    var autoWeld by rememberSaveable { mutableStateOf(false) }
    var autoFill by rememberSaveable { mutableStateOf(true) }
    var autoStroke by rememberSaveable { mutableStateOf(false) }
    var strokeWidth by rememberSaveable { mutableStateOf(6f) }
    var selectedTool by remember { mutableStateOf(EditorTool.AddPoint) }

    LaunchedEffect(selectedTool) {
        NativeRenderer.setActiveTool(selectedTool.nativeId)
    }

    LaunchedEffect(autoStroke) {
        NativeRenderer.setAutoStroke(autoStroke)
    }

    LaunchedEffect(strokeWidth) {
        NativeRenderer.setStrokeWidth(strokeWidth)
    }

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
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LeftToolPanel(
                selectedTool = selectedTool,
                onToolSelected = { selectedTool = it }
            )
            CanvasArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            RightPanel(
                strokeWidth = strokeWidth,
                onStrokeWidthChange = { strokeWidth = it },
                modifier = Modifier
                    .width(148.dp)
                    .fillMaxHeight()
            )
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
            .height(40.dp)
            .background(PanelDark)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MohoCheckField("Auto-weld", autoWeld, onAutoWeldChange)
        MohoCheckField("Auto-fill", autoFill, onAutoFillChange)
        MohoCheckField("Auto-stroke", autoStroke, onAutoStrokeChange)
        Spacer(modifier = Modifier.weight(1f))
        SmallIconButton(
            icon = Icons.Default.Undo,
            description = "Undo",
            enabled = false,
            onClick = {}
        )
        SmallIconButton(
            icon = Icons.Default.Redo,
            description = "Redo",
            enabled = false,
            onClick = {}
        )
    }
}

@Composable
private fun MohoCheckField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MohoCheckbox(checked = checked)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MohoCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .size(13.dp)
            .clip(shape)
            .background(if (checked) MohoGreen else Color.Transparent)
            .border(1.dp, if (checked) MohoGreen else BorderGrey, shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) TextMuted else BorderGrey,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun LeftToolPanel(
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
            .background(PanelDark)
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            ToolGroupSection(ToolGroup.Draw, selectedTool, onToolSelected)
            ToolGroupSection(ToolGroup.Fill, selectedTool, onToolSelected)
            ToolGroupSection(ToolGroup.Layer, selectedTool, onToolSelected)
        }
        BottomLeftDock()
    }
}

@Composable
private fun ToolGroupSection(
    group: ToolGroup,
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit
) {
    val tools = EditorTool.values().filter { it.group == group }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Text(
            text = group.label,
            color = TextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tools.chunked(2).forEach { rowTools ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowTools.forEach { tool ->
                        ToolIconButton(
                            tool = tool,
                            active = selectedTool == tool,
                            onClick = { onToolSelected(tool) }
                        )
                    }
                    if (rowTools.size == 1) {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    tool: EditorTool,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(if (active) MohoGreen else PanelDarker)
            .border(1.dp, if (active) MohoGreen else BorderGrey, shape),
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = tool.label,
            tint = if (active) Color.Black else ToolIconGrey,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BottomLeftDock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 6.dp)
            .height(1.dp)
            .background(BorderGrey)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReservedTool(Icons.Default.Videocam, "Camera")
        ReservedTool(Icons.Default.Work, "Workspace")
    }
}

@Composable
private fun ReservedTool(icon: ImageVector, label: String) {
    Column(
        modifier = Modifier.alpha(0.35f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, BorderGrey, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun CanvasArea(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember(context) { createHorrorBroCanvas(context) }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    glView.onResume()
                    glView.requestRender()
                }
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

    Box(modifier = modifier.background(Color.White)) {
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(0.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = {
                    NativeRenderer.nativeRecenter()
                    glView.requestRender()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Recenter canvas",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = {
                    NativeRenderer.nativeRecenter()
                    glView.requestRender()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Auto center",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RightPanel(
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(PanelDark)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PanelHeader("Style")
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Swatch("Fill", Peach)
            Swatch("Stroke", Color.Black)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Width ${strokeWidth.roundToInt()}",
            color = TextMuted,
            fontSize = 10.sp
        )
        Slider(
            value = strokeWidth,
            onValueChange = onStrokeWidthChange,
            valueRange = 1f..24f,
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            colors = SliderDefaults.colors(
                thumbColor = MohoGreen,
                activeTrackColor = MohoGreen,
                inactiveTrackColor = BorderGrey
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        PanelDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PanelHeader("Layers", modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add layer",
                tint = TextMuted,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LayerRow("Character", initialVisible = true)
        LayerRow("Background", initialVisible = false)
    }
}

@Composable
private fun PanelHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PanelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderGrey)
    )
}

@Composable
private fun Swatch(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .border(1.dp, BorderGrey, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun LayerRow(name: String, initialVisible: Boolean) {
    var visible by rememberSaveable { mutableStateOf(initialVisible) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(3.dp))
                .clickable { visible = !visible },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (visible) "Hide $name" else "Show $name",
                tint = if (visible) MohoGreen else TextDim,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = name,
            color = Color(0xFFD5D5D5),
            fontSize = 10.sp
        )
    }
}
