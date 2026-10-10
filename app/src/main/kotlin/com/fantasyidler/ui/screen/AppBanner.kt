package com.fantasyidler.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Identity equality on purpose: banners ride a StateFlow, which drops equal values. A data
// class made two same-text banners "equal", so advancing between them never re-emitted and
// the display timer never restarted, wedging the banner on screen (issue #1692).
class QueuedBanner(val message: String, val onConsumed: () -> Unit)

/**
 * App-wide replacement for both Android Toasts and per-screen Snackbars. Enqueuing is a
 * plain non-suspending call, so unlike SnackbarHostState.showSnackbar() there's no
 * cancellable suspend point for a later message to race with -- that race is what caused
 * messages to "annihilate" each other under the old per-screen Snackbar approach (#1105).
 */
object AppBannerCenter {
    private val pending = ArrayDeque<QueuedBanner>()
    private val _current = MutableStateFlow<QueuedBanner?>(null)
    val current: StateFlow<QueuedBanner?> = _current.asStateFlow()

    @Synchronized
    fun enqueue(message: String, onConsumed: () -> Unit = {}) {
        // Coalesce identical consecutive messages: the text is already (about to be) on
        // screen, and stacking copies just chains display time (issue #1692). The dropped
        // copy's consumer still runs so callers' message state clears as usual.
        val tail = pending.lastOrNull() ?: _current.value
        if (tail?.message == message) {
            onConsumed()
            return
        }
        pending.addLast(QueuedBanner(message, onConsumed))
        if (_current.value == null) advance()
    }

    @Synchronized
    fun dismissCurrent() {
        _current.value?.onConsumed?.invoke()
        advance()
    }

    private fun advance() {
        _current.value = pending.removeFirstOrNull()
    }
}

/** Drop-in replacement for the old Toast-based effect -- same signature, same call sites. */
@Composable
fun AppBannerEffect(message: String?, onConsumed: () -> Unit) {
    LaunchedEffect(message) {
        message?.let { AppBannerCenter.enqueue(it, onConsumed) }
    }
}

/**
 * Keying on the message alone drops a message re-posted with identical text before the
 * previous one was consumed (rapid mercantile queue taps): the state flow skips the
 * equal value, so the effect never re-runs. Callers that can re-post the same text pass
 * a counter bumped on every post as [key] so each one shows.
 */
@Composable
fun AppBannerEffect(message: String?, key: Any?, onConsumed: () -> Unit) {
    LaunchedEffect(key) {
        message?.let { AppBannerCenter.enqueue(it, onConsumed) }
    }
}

private const val BANNER_DISPLAY_MS = 2_500L

@Composable
fun AppBannerHost() {
    val current by AppBannerCenter.current.collectAsState()

    LaunchedEffect(current) {
        current?.let {
            delay(BANNER_DISPLAY_MS)
            AppBannerCenter.dismissCurrent()
        }
    }

    val banner = current ?: return

    Popup(
        alignment = Alignment.TopCenter,
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                color           = MaterialTheme.colorScheme.primary,
                contentColor    = MaterialTheme.colorScheme.onPrimary,
                shape           = RoundedCornerShape(12.dp),
                tonalElevation  = 8.dp,
                shadowElevation = 8.dp,
                modifier        = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        onClick           = { AppBannerCenter.dismissCurrent() },
                    ),
            ) {
                Text(
                    text     = banner.message,
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
