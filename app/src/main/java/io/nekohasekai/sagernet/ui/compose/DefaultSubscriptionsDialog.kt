package io.nekohasekai.sagernet.ui.compose

import android.content.Context
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.group.DefaultSubscriptions
import io.nekohasekai.sagernet.utils.Theme

/** Simple "loading subscriptions" modal for the first launch; closes itself when the download ends. */
internal fun Context.showDefaultSubscriptionsDialog(): ComponentDialog? {
    if (!DefaultSubscriptions.progress.value.running) return null
    lateinit var dialog: ComponentDialog
    val content = ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            NekoComposeTheme {
                DefaultSubscriptionsContent(onFinished = { dialog.dismiss() })
            }
        }
    }
    return ComponentDialog(this, Theme.getDialogTheme()).also { created ->
        dialog = created
        created.requestWindowFeature(Window.FEATURE_NO_TITLE)
        created.setCancelable(false)
        created.setCanceledOnTouchOutside(false)
        created.setContentView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        created.prepareAsPreferenceDialog(this)
        created.show()
    }
}

@Composable
private fun DefaultSubscriptionsContent(onFinished: () -> Unit) {
    val progress by DefaultSubscriptions.progress.collectAsState()
    LaunchedEffect(progress.running) {
        if (!progress.running) onFinished()
    }
    PreferenceDialogSurface(
        title = null,
        showButtons = true,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = DefaultSubscriptions::skip) {
                    Text(stringResource(R.string.default_subscriptions_skip))
                }
            }
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_download_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(R.string.default_subscriptions_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.default_subscriptions_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            val total = progress.total.coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { (progress.done.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
            Text(
                text = stringResource(
                    R.string.default_subscriptions_progress,
                    progress.done.coerceAtMost(total),
                    total,
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.default_subscriptions_skip_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
