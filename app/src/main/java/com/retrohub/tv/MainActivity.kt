package com.retrohub.tv

import com.retrohub.tv.i18n.I18n
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.retrohub.tv.i18n.AppLanguage
import com.retrohub.tv.ui.screens.LanguageScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.screens.DiscoverScreen
import com.retrohub.tv.ui.screens.EditSystemScreen
import com.retrohub.tv.ui.screens.GameDetailScreen
import com.retrohub.tv.ui.screens.HomeScreen
import com.retrohub.tv.ui.screens.LibraryScreen
import com.retrohub.tv.ui.screens.ProfilesScreen
import com.retrohub.tv.ui.screens.SearchScreen
import com.retrohub.tv.ui.screens.SettingsScreen
import com.retrohub.tv.ui.screens.SystemsScreen
import com.retrohub.tv.ui.theme.RH
import com.retrohub.tv.ui.theme.RetroHubTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android resets the default locale from the system configuration when an activity starts,
        // so dates and the clock would fall back to the system language. Re-apply the app language.
        reapplyLanguage()
        val lite = (application as RetroHubApp).isLite
        setContent {
            RetroHubTheme(lite = lite) { AppRoot(vm) }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reapplyLanguage()
    }

    private fun reapplyLanguage() {
        AppLanguage.fromCode(vm.settings.value.language)?.let(I18n::apply)
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val holder = rememberSaveableStateHolder()
    val top = vm.stack.last()

    BackHandler(enabled = vm.stack.size > 1) { vm.back() }

    // The whole UI follows the app language: right to left for Arabic, rebuilt when it changes.
    val settings by vm.settings.collectAsStateWithLifecycle()
    val lang = AppLanguage.fromCode(settings.language) ?: AppLanguage.ENGLISH

    // Drop saved UI state of screens that were popped, so the holder never grows unbounded.
    val visited = remember { mutableSetOf<String>() }
    visited += top.key
    LaunchedEffect(top.key) {
        val live = vm.stack.mapTo(HashSet()) { it.key }
        visited.filter { it !in live }.forEach { holder.removeState(it); visited.remove(it) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
    key(lang) {
    Box(Modifier.fillMaxSize().background(RH.Bg)) {
        holder.SaveableStateProvider(top.key) {
            when (top) {
                Screen.Home -> HomeScreen(vm)
                is Screen.Library -> LibraryScreen(vm, top)
                is Screen.Game -> GameDetailScreen(vm, top.gameId)
                Screen.Search -> SearchScreen(vm)
                Screen.Systems -> SystemsScreen(vm)
                is Screen.EditSystem -> EditSystemScreen(vm, top)
                Screen.Settings -> SettingsScreen(vm)
                is Screen.Profiles -> ProfilesScreen(vm, top)
                Screen.Discover -> DiscoverScreen(vm)
                is Screen.Language -> LanguageScreen(vm, top)
            }
        }
        if (top !is Screen.Language) StatusOverlay(vm, Modifier.align(Alignment.BottomCenter))
    }
    }
    }
}

@Composable
private fun StatusOverlay(vm: MainViewModel, modifier: Modifier) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val text = busy ?: message ?: return
    Row(
        modifier
            .padding(bottom = 28.dp, start = 48.dp, end = 48.dp)
            .widthIn(max = 920.dp)
            .background(RH.SurfaceHi, RoundedCornerShape(50))
            .border(1.dp, RH.Line, RoundedCornerShape(50))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy != null) {
            Box(Modifier.size(8.dp).background(RH.Ok, CircleShape))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
