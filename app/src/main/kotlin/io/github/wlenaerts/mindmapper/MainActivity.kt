package io.github.wlenaerts.mindmapper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import io.github.wlenaerts.mindmapper.ui.IncomingDocument
import io.github.wlenaerts.mindmapper.ui.MindmapperApp
import io.github.wlenaerts.mindmapper.ui.theme.MindmapperTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val incoming = Channel<IncomingDocument>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only handle the launching intent once, not again after a configuration change or restore.
        if (savedInstanceState == null) handle(intent)
        setContent {
            MindmapperTheme {
                MindmapperApp(incoming.receiveAsFlow())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val uri: Uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        } ?: return
        incoming.trySend(IncomingDocument(uri, intent!!.flags))
    }
}
