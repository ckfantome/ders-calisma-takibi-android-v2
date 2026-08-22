package com.derscalismatakibi.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.UpdateChecker
import com.derscalismatakibi.app.ui.screens.LogsScreen
import com.derscalismatakibi.app.ui.screens.MainScreen
import com.derscalismatakibi.app.ui.screens.ScheduleScreen
import com.derscalismatakibi.app.ui.screens.SettingsScreen
import com.derscalismatakibi.app.ui.screens.StatsScreen
import com.derscalismatakibi.app.ui.screens.UsageStatsScreen
import com.derscalismatakibi.app.util.UpdateInstaller
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val destinations = listOf(
    Destination("main", "Ana Ekran", Icons.Filled.Home),
    Destination("schedule", "Takvim", Icons.Filled.CalendarMonth),
    Destination("stats", "Istatistikler", Icons.Filled.BarChart),
    Destination("usage", "Kullanim", Icons.Filled.Apps),
    Destination("logs", "Loglar", Icons.Filled.Article),
    Destination("settings", "Ayarlar", Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: StudyViewModel = viewModel()
    val role by viewModel.role.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showStudentConfirm by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUnknownSourcesDialog by remember { mutableStateOf(false) }

    // Acilista bir kere, sessizce (kullaniciyi rahatsiz etmeden) guncelleme kontrolu.
    LaunchedEffect(Unit) {
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
        val info = withContext(Dispatchers.IO) { UpdateChecker.checkForUpdate(currentVersion) }
        if (info != null) pendingUpdate = info
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ders Calisma Takibi") },
                actions = {
                    IconButton(onClick = {
                        if (role == Role.ADMIN) showStudentConfirm = true else showPinDialog = true
                    }) {
                        Icon(
                            if (role == Role.ADMIN) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = "Yonetici / Ogrenci modu",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                destinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController = navController, startDestination = "main", modifier = Modifier.padding(padding)) {
            composable("main") { MainScreen(viewModel) }
            composable("schedule") { ScheduleScreen(viewModel) }
            composable("stats") { StatsScreen(viewModel) }
            composable("usage") { UsageStatsScreen() }
            composable("logs") { LogsScreen() }
            composable("settings") { SettingsScreen(viewModel) }
        }
    }

    if (showStudentConfirm) {
        AlertDialog(
            onDismissRequest = { showStudentConfirm = false },
            title = { Text("Ogrenci Moduna Gec") },
            text = { Text("Ogrenci moduna gecilecek: ayarlar, takvim duzenleme ve hedef sifirlama kilitlenecek. Devam edilsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.switchToStudent()
                    showStudentConfirm = false
                }) { Text("Evet") }
            },
            dismissButton = { TextButton(onClick = { showStudentConfirm = false }) { Text("Iptal") } },
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Yonetici Modu") },
            text = {
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it },
                    label = { Text("PIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = viewModel.tryUnlockAdmin(pin)
                    showPinDialog = false
                    if (!ok) {
                        scope.launch { snackbarHostState.showSnackbar("Hatali PIN") }
                    }
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Iptal") } },
        )
    }

    pendingUpdate?.let { info ->
        UpdateAvailableDialog(
            info = info,
            onDismiss = { pendingUpdate = null },
            onInstall = {
                if (UpdateInstaller.canInstallUnknownApps(context)) {
                    UpdateInstaller.downloadAndInstall(context, info)
                    pendingUpdate = null
                } else {
                    showUnknownSourcesDialog = true
                }
            },
        )
    }

    if (showUnknownSourcesDialog) {
        UnknownSourcesDialog(
            onGoToSettings = {
                showUnknownSourcesDialog = false
                context.startActivity(UpdateInstaller.unknownAppsSettingsIntent(context))
            },
            onDismiss = { showUnknownSourcesDialog = false },
        )
    }
}
