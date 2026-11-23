package com.retrohub.tv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.flow.first
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.retrohub.tv.ui.MainViewModel
import kotlinx.coroutines.delay

/**
 * Remembers which element (by string key) had focus on a screen and gives it focus again
 * when the user comes back, which is what a console dashboard is expected to do.
 */
class FocusRestore(
    private val vm: MainViewModel,
    private val screenKey: String,
    val target: String?,
    val requester: FocusRequester,
    private val ready: Boolean,
) {
    fun modifier(key: String): Modifier =
        (if (key == target) Modifier.focusRequester(requester) else Modifier)
            // Ignore focus the framework hands out while loading; only real user positions are remembered.
            .onFocusChanged { if (it.isFocused && ready) vm.focusMemory[screenKey] = key }
}

/**
 * @param ready false while the screen's data is still loading. No focus is requested until it
 *              is true; otherwise a fallback button would grab focus first and be remembered.
 */
@Composable
fun rememberFocusRestore(
    vm: MainViewModel,
    screenKey: String,
    defaultKey: String?,
    availableKeys: Set<String>,
    ready: Boolean = true,
): FocusRestore {
    val remembered = vm.focusMemory[screenKey]
    val target = when {
        !ready -> null
        remembered != null && remembered in availableKeys -> remembered
        else -> defaultKey
    }
    val requester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        // When the window gains focus Compose hands focus to the first focusable; wait for that first.
        snapshotFlow { windowInfo.isWindowFocused }.first { it }
        // Lazy items may need a frame or two to be composed, and the window may not have focus yet
        // (Compose then moves focus to the first button). Retry until the target really holds focus,
        // which the onFocusChanged listener above reports through focusMemory.
        repeat(12) {
            runCatching { requester.requestFocus() }
            delay(50)
            if (vm.focusMemory[screenKey] == target) return@LaunchedEffect
        }
    }
    return FocusRestore(vm, screenKey, target, requester, ready)
}
