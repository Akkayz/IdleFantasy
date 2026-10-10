package com.fantasyidler

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.hilt.navigation.compose.DesktopInjector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fantasyidler.notification.SessionNotificationManager
import com.fantasyidler.repository.BackupScheduler
import com.fantasyidler.repository.PlayerRepository
import com.fantasyidler.repository.QueuedSessionStarter
import com.fantasyidler.repository.SaveSlotRepository
import com.fantasyidler.repository.SessionRepository
import com.fantasyidler.repository.WorkerQueuedSessionStarter
import com.fantasyidler.ui.navigation.AppNavigation
import com.fantasyidler.ui.theme.FantasyIdlerTheme
import com.fantasyidler.ui.theme.LocalAppFontScale
import com.fantasyidler.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

fun main() = application {
    val windowState = rememberWindowState(width = 480.dp, height = 860.dp)
    val pendingNavigateTo = remember { mutableStateOf<String?>(null) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Idle Fantasy",
        state = windowState,
        icon = painterResource(R.drawable.ic_launcher_foreground)
    ) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val notifManager = DesktopInjector.get(SessionNotificationManager::class.java)
                notifManager.createChannels()
                notifManager.setAppInForeground(true)

                val saveSlotRepo = DesktopInjector.get(SaveSlotRepository::class.java)
                val playerRepo = DesktopInjector.get(PlayerRepository::class.java)
                val backupScheduler = DesktopInjector.get(BackupScheduler::class.java)
                val sessionRepo = DesktopInjector.get(SessionRepository::class.java)
                val queuedStarter = DesktopInjector.get(QueuedSessionStarter::class.java)
                val workerStarter = DesktopInjector.get(WorkerQueuedSessionStarter::class.java)

                try {
                    saveSlotRepo.recoverInterruptedSwitch()
                    val flags = playerRepo.getFlags()
                    if (flags.backupFrequency.isNotEmpty()) {
                        backupScheduler.schedule(flags.backupFrequency)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("DesktopMain", "Startup recovery or backup scheduling failed", e)
                }

                while (isActive) {
                    try {
                        sessionRepo.completeOverdueSessions(queuedStarter, workerStarter)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("DesktopMain", "Failed completing overdue sessions", e)
                    }
                    delay(5_000L)
                }
            }
        }

        val settingsViewModel: SettingsViewModel = hiltViewModel()
        val systemDark = isSystemInDarkTheme()
        LaunchedEffect(systemDark) { settingsViewModel.setSystemDark(systemDark) }
        val colourScheme by settingsViewModel.colourScheme.collectAsStateWithLifecycle()
        val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
        val baseDensity = LocalDensity.current

        FantasyIdlerTheme(colorScheme = colourScheme) {
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale),
                LocalAppFontScale provides fontScale,
            ) {
                AppNavigation(
                    pendingNavigateTo = pendingNavigateTo.value,
                    onNavigateConsumed = { pendingNavigateTo.value = null },
                )
            }
        }
    }
}
