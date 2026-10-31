package io.github.wlenaerts.mindmapper.ui.start

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.recent.RecentFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(viewModel: StartViewModel, onAbout: () -> Unit, onNew: () -> Unit) {
    val recent by viewModel.recentFiles.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()
    val error by viewModel.error.collectAsState()
    val unavailable by viewModel.unavailable.collectAsState()
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.open(uri, OpenSource.PICKER)
    }
    LaunchedEffect(Unit) { viewModel.refreshThumbnails() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.action_about))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.start_tagline), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onNew) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(stringResource(R.string.action_new), Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { openLauncher.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.action_open))
                        }
                    }
                    Text(stringResource(R.string.recent_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    if (recent.entries.isEmpty()) {
                        Text(stringResource(R.string.recent_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(recent.ordered, key = { it.uri }) { file ->
                LaunchedEffect(file.uri, thumbnails.containsKey(file.uri)) { viewModel.loadThumbnail(file.uri) }
                RecentCard(
                    file = file,
                    thumbnail = thumbnails[file.uri],
                    onOpen = { viewModel.open(android.net.Uri.parse(file.uri), OpenSource.RECENT) },
                    onPin = { viewModel.setPinned(file.uri, !file.pinned) },
                    onRemove = { viewModel.remove(file.uri) },
                )
            }
        }
    }

    error?.let { res ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            text = { Text(stringResource(res)) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.action_ok)) } },
        )
    }
    unavailable?.let { file ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(R.string.file_unavailable_title)) },
            text = { Text(stringResource(R.string.file_unavailable_text, file.name)) },
            confirmButton = { TextButton(onClick = { viewModel.remove(file.uri) }) { Text(stringResource(R.string.recent_remove)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun RecentCard(file: RecentFile, thumbnail: Thumbnail?, onOpen: () -> Unit, onPin: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("recent_${file.name}")) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.6f).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            when (thumbnail) {
                is Thumbnail.Image -> Image(
                    thumbnail.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
                Thumbnail.Unavailable -> Text(stringResource(R.string.recent_unavailable), color = Color.Gray)
                else -> Unit
            }
            if (file.pinned) {
                Icon(
                    painterResource(R.drawable.ic_push_pin),
                    contentDescription = stringResource(R.string.recent_unpin),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                )
            }
        }
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(file.name.removeSuffix(".mindmap"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                val justNow = stringResource(R.string.recent_just_now)
                Text(
                    if (System.currentTimeMillis() - file.lastUsed < DateUtils.MINUTE_IN_MILLIS) justNow
                    else DateUtils.getRelativeTimeSpanString(file.lastUsed).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.recent_more, file.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (file.pinned) R.string.recent_unpin else R.string.recent_pin)) },
                        onClick = { menu = false; onPin() },
                    )
                    DropdownMenuItem(text = { Text(stringResource(R.string.recent_remove)) }, onClick = { menu = false; onRemove() })
                }
            }
        }
    }
}
