package androidx.activity.compose

import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackHandler(
    @Suppress("UNUSED_PARAMETER") enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit
) {
    // Desktop navigation uses in-app back buttons; system back gesture is a no-op.
}

fun AppCompatActivity.setContent(
    @Suppress("UNUSED_PARAMETER") content: @Composable () -> Unit
) {
    // DesktopMain.kt hosts the Compose Window directly.
}

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ActivityResultLauncher<I> {
    val currentOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    return remember(contract) {
        ActivityResultLauncher { input ->
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    contract.invokeDesktop(input)
                }
                currentOnResult(result)
            }
        }
    }
}
