package io.github.wlenaerts.mindmapper.ui.editor

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.styleOrStandard
import io.github.wlenaerts.mindmapper.core.view.Viewport
import io.github.wlenaerts.mindmapper.export.ExportFormat
import io.github.wlenaerts.mindmapper.ui.map.MapCanvas
import io.github.wlenaerts.mindmapper.ui.map.fitViewport
import kotlinx.coroutines.launch

private sealed interface EditorDialog {
    data class EditText(val topicId: String) : EditorDialog
    data class ChooseIcon(val topicId: String) : EditorDialog
    data class EditImage(val topicId: String) : EditorDialog
    data object Style : EditorDialog
    data class BranchStyle(val topicId: String) : EditorDialog
    data object Rename : EditorDialog
    data object Unsaved : EditorDialog
    data class Export(val format: ExportFormat) : EditorDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onClose: () -> Unit, onPresent: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var viewport by remember { mutableStateOf<Viewport?>(null) }
    var canvasSize by remember { mutableStateOf(0f to 0f) }
    var imageTarget by remember { mutableStateOf<String?>(null) }
    var closeAfterSave by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ExportFormat?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val savedText = stringResource(R.string.saved)
    val exportedText = stringResource(R.string.exported)

    fun fit() {
        val layout = state.layout ?: return
        val (w, h) = canvasSize
        if (w > 0 && h > 0) viewport = fitViewport(layout, w, h, density)
    }

    val saveAsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) {
            closeAfterSave = false
        } else {
            viewModel.saveAs(uri) { ok ->
                if (ok && closeAfterSave) onClose() else if (ok) scope.launch { snackbar.showSnackbar(savedText) }
                closeAfterSave = false
            }
        }
    }
    fun save(thenClose: Boolean = false) {
        closeAfterSave = thenClose
        val started = viewModel.save { ok ->
            if (ok && thenClose) onClose() else if (ok) scope.launch { snackbar.showSnackbar(savedText) }
        }
        if (!started) saveAsLauncher.launch(viewModel.saveFileName())
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = imageTarget
        if (uri != null && target != null) viewModel.importImage(target, uri)
        imageTarget = null
    }
    fun chooseImage(topicId: String) {
        imageTarget = topicId
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)) { uri ->
        if (uri != null) viewModel.export(ExportFormat.PDF, uri) { ok -> if (ok) scope.launch { snackbar.showSnackbar(exportedText) } }
    }
    val exportPng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PNG.mimeType)) { uri ->
        if (uri != null) viewModel.export(ExportFormat.PNG, uri) { ok -> if (ok) scope.launch { snackbar.showSnackbar(exportedText) } }
    }

    fun leave() {
        when {
            !state.dirty -> onClose()
            state.autosaveActive -> save(thenClose = true)
            else -> dialog = EditorDialog.Unsaved
        }
    }
    BackHandler(enabled = !state.loading) { leave() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.autosaveNow() }

    state.message?.let { res ->
        val text = stringResource(res)
        LaunchedEffect(res) {
            snackbar.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.loadFailed) { if (state.loadFailed) onClose() }
    LaunchedEffect(state.editTextOf) {
        state.editTextOf?.let {
            dialog = EditorDialog.EditText(it)
            viewModel.consumeEditText()
        }
    }
    // Fit on open and whenever the available size changes (rotation, split screen).
    LaunchedEffect(state.layout != null, canvasSize) { fit() }

    val title = state.title.ifBlank { stringResource(R.string.untitled) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (state.dirty && !state.autosaveActive) "$title •" else title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        IconButton(onClick = { dialog = EditorDialog.Rename }, enabled = state.map != null) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_rename))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.action_undo))
                    }
                    IconButton(onClick = viewModel::redo, enabled = state.canRedo) {
                        Icon(painterResource(R.drawable.ic_redo), contentDescription = stringResource(R.string.action_redo))
                    }
                    IconButton(onClick = { save() }, enabled = state.map != null) {
                        Icon(painterResource(R.drawable.ic_save), contentDescription = stringResource(R.string.action_save))
                    }
                    IconButton(onClick = { viewModel.autosaveNow(); onPresent() }, enabled = state.map != null) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.action_present))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_save_as)) }, onClick = {
                                menuOpen = false
                                saveAsLauncher.launch(viewModel.saveFileName())
                            })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_autosave)) },
                                trailingIcon = { Checkbox(checked = state.autosave, onCheckedChange = null) },
                                onClick = { viewModel.setAutosave(!state.autosave) },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_fit)) }, onClick = { menuOpen = false; fit() })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_layout_radial)) },
                                trailingIcon = { RadioButton(selected = state.map?.layout == LayoutKind.RADIAL, onClick = null) },
                                onClick = { menuOpen = false; viewModel.setLayout(LayoutKind.RADIAL) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_layout_right)) },
                                trailingIcon = { RadioButton(selected = state.map?.layout == LayoutKind.RIGHT, onClick = null) },
                                onClick = { menuOpen = false; viewModel.setLayout(LayoutKind.RIGHT) },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_export_pdf)) }, onClick = {
                                menuOpen = false
                                dialog = EditorDialog.Export(ExportFormat.PDF)
                            })
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_export_png)) }, onClick = {
                                menuOpen = false
                                dialog = EditorDialog.Export(ExportFormat.PNG)
                            })
                        }
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            LaunchedEffect(widthPx, heightPx) { canvasSize = widthPx to heightPx }
            val map = state.map
            val layout = state.layout
            val vp = viewport
            if (map == null || layout == null || vp == null) {
                if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                MapCanvas(
                    map = map,
                    layout = layout,
                    renderer = viewModel.renderer,
                    images = viewModel.imageCache,
                    viewport = vp,
                    onViewportChange = { viewport = it },
                    selectedId = state.selectedId,
                    description = stringResource(R.string.mindmap_canvas),
                    onTap = viewModel::select,
                    onDoubleTap = { id -> viewModel.select(id); dialog = EditorDialog.EditText(id) },
                    onDrop = viewModel::drop,
                    onDragActiveChange = { dragging = it },
                )
                val selectedBox = state.selectedId?.let { layout[it] }
                if (selectedBox != null && !dragging) {
                    val selected = selectedBox.topicId
                    TopicActions(
                        box = selectedBox,
                        viewport = vp,
                        radial = map.layout == LayoutKind.RADIAL,
                        onAddChild = viewModel::addChild,
                        onAddSibling = viewModel::addSibling,
                        onEditText = { dialog = EditorDialog.EditText(selected) },
                        onImage = {
                            if (map.find(selected)?.image != null) dialog = EditorDialog.EditImage(selected) else chooseImage(selected)
                        },
                        onIcon = { dialog = EditorDialog.ChooseIcon(selected) },
                        onStyle = { dialog = if (selectedBox.depth == 0) EditorDialog.Style else EditorDialog.BranchStyle(selected) },
                        onDelete = viewModel::deleteSelected,
                    )
                    // Every topic action is on the map now; the toolbar is kept for actions that belong there.
                    TopicToolbar(
                        actions = emptyList(),
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                    )
                }
            }
        }
    }

    val map = state.map
    when (val d = dialog) {
        is EditorDialog.EditText -> map?.find(d.topicId)?.let { topic ->
            val format = viewModel.textFormat(d.topicId) ?: return@let
            TextDialog(
                topic.text,
                format,
                onConfirm = { text, newFormat -> viewModel.setTextAndFormat(d.topicId, text, newFormat); dialog = null },
                onDismiss = { dialog = null },
            )
        }
        is EditorDialog.ChooseIcon -> IconDialog(
            current = map?.find(d.topicId)?.icon,
            onPick = { viewModel.setIcon(d.topicId, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is EditorDialog.EditImage -> map?.find(d.topicId)?.image?.let { image ->
            ImageDialog(
                image = image,
                onWidth = { viewModel.setImageWidth(d.topicId, it) },
                onReplace = { dialog = null; chooseImage(d.topicId) },
                onRemove = { viewModel.removeImage(d.topicId); dialog = null },
                onDismiss = { dialog = null },
            )
        }
        EditorDialog.Style -> map?.let {
            StyleDialog(
                shape = it.centralShape,
                color = it.centralColor,
                trunkWidth = it.trunkWidth,
                trunkTaper = it.trunkTaper,
                onShape = viewModel::setCentralShape,
                onColor = viewModel::setCentralColor,
                onTrunk = viewModel::setTrunk,
                onDismiss = { dialog = null },
            )
        }
        is EditorDialog.BranchStyle -> map?.find(d.topicId)?.let { topic ->
            BranchStyleDialog(
                current = topic.styleOrStandard,
                colorSlot = state.layout?.get(d.topicId)?.colorSlot ?: 0,
                renderer = viewModel.renderer,
                onPick = { viewModel.setBranchStyle(d.topicId, it) },
                onDismiss = { dialog = null },
            )
        }
        EditorDialog.Rename -> RenameDialog(
            initial = state.title.ifBlank { viewModel.saveFileName().removeSuffix(".mindmap") },
            onConfirm = { viewModel.rename(it); dialog = null },
            onDismiss = { dialog = null },
        )
        EditorDialog.Unsaved -> UnsavedDialog(
            onSave = { dialog = null; save(thenClose = true) },
            onDiscard = { dialog = null; onClose() },
            onDismiss = { dialog = null },
        )
        is EditorDialog.Export -> ExportDialog(
            format = d.format,
            onSave = {
                dialog = null
                val name = viewModel.exportFileName(d.format)
                if (d.format == ExportFormat.PDF) exportPdf.launch(name) else exportPng.launch(name)
            },
            onShare = {
                dialog = null
                viewModel.exportForSharing(d.format) { file ->
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
                        val send = Intent(Intent.ACTION_SEND)
                            .setType(d.format.mimeType)
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(send, null))
                    }
                }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

/** An action in the bottom toolbar. */
class ToolbarAction(val label: String, val icon: @Composable () -> Unit, val onClick: () -> Unit)

/** The bottom toolbar for the selected topic; shows nothing while it has no [actions]. */
@Composable
private fun TopicToolbar(actions: List<ToolbarAction>, modifier: Modifier) {
    if (actions.isEmpty()) return
    Surface(modifier = modifier, shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp, shadowElevation = 6.dp) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { ToolbarButton(it.label, it.onClick, it.icon) }
        }
    }
}

@Composable
private fun ToolbarButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    TextButton(onClick = onClick) {
        icon()
        Text(label, modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}
