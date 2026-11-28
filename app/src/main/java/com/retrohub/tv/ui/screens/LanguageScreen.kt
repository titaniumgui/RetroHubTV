package com.retrohub.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.retrohub.tv.i18n.AppLanguage
import com.retrohub.tv.i18n.I18n
import com.retrohub.tv.i18n.tr
import com.retrohub.tv.ui.MainViewModel
import com.retrohub.tv.ui.Screen
import com.retrohub.tv.ui.components.BrandMark
import com.retrohub.tv.ui.components.RhSurface
import com.retrohub.tv.ui.theme.RH

/**
 * Language picker. On a fresh install it appears before anything else, so its own text is shown
 * in both languages. The device's language (or the current choice) has focus first.
 */
@Composable
fun LanguageScreen(vm: MainViewModel, screen: Screen.Language) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val chosen = AppLanguage.fromCode(settings.language)
    val preselect = chosen ?: I18n.deviceLanguage()
    val focus = rememberFocusRestore(
        vm, screen.key, "lang:${preselect.code}", AppLanguage.entries.mapTo(HashSet()) { "lang:${it.code}" },
    )

    Column(Modifier.fillMaxSize().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark()
        Spacer(Modifier.height(56.dp))
        Text("Choose your language", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text("اختر لغتك", style = MaterialTheme.typography.headlineMedium, color = RH.TextDim)
        Spacer(Modifier.height(44.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            AppLanguage.entries.forEach { lang ->
                RhSurface(
                    onClick = { vm.chooseLanguage(lang) },
                    modifier = focus.modifier("lang:${lang.code}").size(width = 300.dp, height = 168.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(lang.nativeName, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(lang.altName, style = MaterialTheme.typography.bodyLarge, color = RH.TextDim)
                        if (lang == chosen) {
                            Spacer(Modifier.height(6.dp))
                            Text("✓ " + tr("Current"), style = MaterialTheme.typography.labelMedium, color = RH.Ok)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        // One line per language, each laid out in its own direction.
        val hint = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content, textAlign = TextAlign.Center)
        Text("You can change this later in Settings.", style = hint, color = RH.TextFaint)
        Text("يمكنك تغييرها لاحقًا من الإعدادات.", style = hint, color = RH.TextFaint)
    }
}
