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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.derscalismatakibi.app.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.UpdateChecker
import com.derscalismatakibi.app.legal.PrivacyConsent
import com.derscalismatakibi.app.ui.screens.AppBlockScreen
import com.derscalismatakibi.app.ui.screens.CallLogScreen
import com.derscalismatakibi.app.ui.screens.KeyboardLogScreen
import com.derscalismatakibi.app.ui.screens.LocationScreen
import com.derscalismatakibi.app.ui.screens.LogsScreen
import com.derscalismatakibi.app.ui.screens.DeviceReportScreen
import com.derscalismatakibi.app.ui.screens.PrivacyConsentScreen
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

@Composable
private fun destinations(): List<Destination> = listOf(
    Destination("main", stringResource(R.string.nav_main), Icons.Filled.Home),
    Destination("schedule", stringResource(R.string.schedule_title), Icons.Filled.CalendarMonth),
    Destination("stats", stringResource(R.string.stats_title), Icons.Filled.BarChart),
    Destination("usage", stringResource(R.string.nav_usage), Icons.Filled.Apps),
    Destination("logs", stringResource(R.string.logs_title), Icons.Filled.Article),
    Destination("calllog", stringResource(R.string.call_log_title), Icons.Filled.Phone),
    Destination("location", stringResource(R.string.location_title), Icons.Filled.Place),
    Destination("device", stringResource(R.string.nav_device), Icons.Filled.BatteryStd),
    Destination("appblock", stringResource(R.string.app_block_title), Icons.Filled.Lock),
    Destination("keyboardlog", stringResource(R.string.keyboard_log_title), Icons.Filled.Keyboard),
    Destination("settings", stringResource(R.string.settings_title), Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: StudyViewModel = viewModel()

    // KVKK/gizlilik onayi verilmeden (veya metin surumu yukseltilip yeniden onay
    // gerekince) ASAGIDAKI Scaffold/NavHost'un TAMAMI erisilemez - once bu.
    val consentCfg by viewModel.configState.collectAsState()
    if (!consentCfg.privacyConsentAccepted || consentCfg.privacyConsentVersion < PrivacyConsent.VERSION) {
        PrivacyConsentScreen(viewModel)
        return
    }

    val role by viewModel.role.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 9 hedef bir alt navigasyon cubuguna sigmiyordu (etiketler 2 satira kirilip
    // kalabaliklasiyordu) - cekmece (drawer) menude sinir yok, daha okunakli.
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showPinDialog by remember { mutableStateOf(false) }
    var showStudentConfirm by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUnknownSourcesDialog by remember { mutableStateOf(false) }

    // Sinav Modu + Ekran Sabitleme ikisi de aciksa telefonu bu uygulamaya kilitler
    // (istisna listesi calismaz, bkz. AppBlockScreen aciklamasi) - ikisinden biri
    // kapaninca kilit acilir. ponytail: kullanici sistemin kendi "uzun bas geri+genel
    // gorunum" jestiyle elle acarsa burada otomatik tekrar kilitlenmiyor - sadece
    // anahtar degisince veya bu ekran yeniden olusunca tekrar dener.
    val shouldPinScreen = consentCfg.examModeEnabled && consentCfg.screenPinningEnabled
    LaunchedEffect(shouldPinScreen) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect
        val am = activity.getSystemService(android.app.ActivityManager::class.java)
        val isPinned = am?.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
        if (shouldPinScreen && !isPinned) {
            try {
                activity.startLockTask()
                com.derscalismatakibi.app.util.AppLogger.log("EkranSabitleme", "Kilitlendi")
            } catch (t: Throwable) {
                com.derscalismatakibi.app.util.AppLogger.logError("EkranSabitleme", "Kilitlenemedi", t)
            }
        } else if (!shouldPinScreen && isPinned) {
            try {
                activity.stopLockTask()
                com.derscalismatakibi.app.util.AppLogger.log("EkranSabitleme", "Kilit acildi")
            } catch (t: Throwable) {
                com.derscalismatakibi.app.util.AppLogger.logError("EkranSabitleme", "Kilit acilamadi", t)
            }
        }
    }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                // Ogrenci (denetim) modunda Loglar cekmeceden gizlenir - ham log kaydi
                // ebeveyn-denetim amacli, ogrenciye gorunmemesi gerekiyor.
                val allDestinations = destinations()
                val visibleDestinations = if (role == Role.STUDENT) allDestinations.filterNot { it.route == "logs" } else allDestinations
                visibleDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (role == Role.ADMIN) showStudentConfirm = true else showPinDialog = true
                        }) {
                            Icon(
                                if (role == Role.ADMIN) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = stringResource(R.string.nav_admin_student_mode),
                            )
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(navController = navController, startDestination = "main", modifier = Modifier.padding(padding)) {
                composable("main") { MainScreen(viewModel) }
                composable("schedule") { ScheduleScreen(viewModel) }
                composable("stats") { StatsScreen(viewModel) }
                composable("usage") { UsageStatsScreen() }
                composable("logs") { LogsScreen() }
                composable("calllog") { CallLogScreen() }
                composable("location") { LocationScreen(viewModel) }
                composable("device") { DeviceReportScreen() }
                composable("appblock") { AppBlockScreen(viewModel) }
                composable("keyboardlog") { KeyboardLogScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
            }
        }
    }

    if (showStudentConfirm) {
        AlertDialog(
            onDismissRequest = { showStudentConfirm = false },
            title = { Text(stringResource(R.string.nav_switch_to_student)) },
            text = { Text(stringResource(R.string.nav_switch_to_student_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.switchToStudent()
                    showStudentConfirm = false
                }) { Text(stringResource(R.string.action_yes)) }
            },
            dismissButton = { TextButton(onClick = { showStudentConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        val wrongPinMessage = stringResource(R.string.nav_wrong_pin)
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(stringResource(R.string.nav_admin_mode)) },
            text = {
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it },
                    label = { Text(stringResource(R.string.nav_pin_label)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = viewModel.tryUnlockAdmin(pin)
                    showPinDialog = false
                    if (!ok) {
                        scope.launch { snackbarHostState.showSnackbar(wrongPinMessage) }
                    }
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
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
