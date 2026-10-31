package io.github.wlenaerts.mindmapper.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.TopicImage
import io.github.wlenaerts.mindmapper.export.ExportFormat
import io.github.wlenaerts.mindmapper.render.IconSet

@Composable
fun TextDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_text_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("topic_text"),
                minLines = 1,
                maxLines = 6,
                // The keyboard's action key confirms; long text still wraps on the map.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(value.text) }),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun IconDialog(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.icon_title)) },
        text = {
            LazyVerticalGrid(columns = GridCells.Adaptive(52.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                items(IconSet.icons.entries.toList(), key = { it.key }) { (name, res) ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(44.dp)
                            .background(
                                if (name == current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { onPick(name) }
                            .semantics { contentDescription = name.replace('_', ' ') },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(res), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        dismissButton = { TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.icon_none)) } },
    )
}

@Composable
fun ImageDialog(image: TopicImage, onWidth: (Float) -> Unit, onReplace: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    var width by remember { mutableFloatStateOf(image.width) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.image_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.image_width))
                Slider(
                    value = width,
                    onValueChange = { width = it },
                    onValueChangeFinished = { onWidth(width) },
                    valueRange = TopicImage.MIN_WIDTH..TopicImage.MAX_WIDTH,
                    modifier = Modifier.testTag("image_width"),
                )
                TextButton(onClick = onReplace) { Text(stringResource(R.string.image_replace)) }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.image_remove)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

val CentralColors: List<Int> = listOf(
    0xFF3D5A80, 0xFF1D3557, 0xFF2A9D8F, 0xFF264653, 0xFF6A4C93, 0xFF8E44AD,
    0xFFE63946, 0xFFC0392B, 0xFFF4A261, 0xFFE9C46A, 0xFF52B788, 0xFF2D6A4F,
    0xFF457B9D, 0xFF0077B6, 0xFF495057, 0xFF212529,
).map { it.toInt() }

fun shapeLabel(shape: CentralShape): Int = when (shape) {
    CentralShape.CIRCLE -> R.string.shape_circle
    CentralShape.ELLIPSE -> R.string.shape_ellipse
    CentralShape.RECTANGLE -> R.string.shape_rectangle
    CentralShape.ROUNDED_RECTANGLE -> R.string.shape_rounded_rectangle
    CentralShape.DIAMOND -> R.string.shape_diamond
    CentralShape.HEXAGON -> R.string.shape_hexagon
    CentralShape.CAPSULE -> R.string.shape_capsule
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleDialog(shape: CentralShape, color: Int, onShape: (CentralShape) -> Unit, onColor: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.style_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.style_shape), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CentralShape.entries.forEach { s ->
                        FilterChip(selected = s == shape, onClick = { onShape(s) }, label = { Text(stringResource(shapeLabel(s))) })
                    }
                }
                Text(stringResource(R.string.style_colour), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CentralColors.forEachIndexed { i, c ->
                        val swatchLabel = stringResource(R.string.colour_swatch, (i + 1).toString())
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Color(c), CircleShape)
                                .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { onColor(c) }
                                .semantics { contentDescription = swatchLabel },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

@Composable
fun UnsavedDialog(onSave: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.unsaved_title)) },
        text = { Text(stringResource(R.string.unsaved_text)) },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row2 {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.action_discard)) }
            }
        },
    )
}

@Composable
private fun Row2(content: @Composable () -> Unit) = androidx.compose.foundation.layout.Row { content() }

@Composable
fun ExportDialog(format: ExportFormat, onSave: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (format == ExportFormat.PDF) R.string.export_title_pdf else R.string.export_title_png)) },
        text = { Text(stringResource(R.string.export_text)) },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.export_save)) } },
        dismissButton = { TextButton(onClick = onShare) { Text(stringResource(R.string.export_share)) } },
    )
}
