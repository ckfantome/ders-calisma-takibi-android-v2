package com.derscalismatakibi.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.camera.CameraAnalyzer
import com.derscalismatakibi.app.camera.FaceLandmarkerHelper
import com.derscalismatakibi.app.core.PomodoroState
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.StudyState
import com.derscalismatakibi.app.core.fmtHms
import com.derscalismatakibi.app.service.StudyForegroundService
import com.derscalismatakibi.app.util.BatteryOptimizationHelper
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import java.util.concurrent.Executors

@Composable
fun MainScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    // Android 13+ icin bildirim izni ayrica istenir (Pomodoro/Takvim bildirimleri).
    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val role by viewModel.role.collectAsState()
    val backgroundActive by viewModel.backgroundTrackingActive.collectAsState()
    // Sonuc gozardi edilir (kullanici reddetse de servisi baslatiyoruz - sadece
    // OEM tarafindan oldurulme ihtimali artmis olur, bloklayici degil).
    val batteryOptimizationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetNotesDialog by remember { mutableStateOf(false) }
    var showAdminRequiredNotice by remember { mutableStateOf(false) }
    var weeklySeconds by remember { mutableStateOf(0.0) }
    LaunchedEffect(uiState.studyingSeconds.toInt() / 60) {
        weeklySeconds = viewModel.weeklyStudySeconds() + uiState.studyingSeconds
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ders Calisma Takibi", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            when {
                backgroundActive -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Arkaplanda takip calisiyor - onizleme (pil tasarrufu icin) kapali.\nDurumu bildirimden takip edebilirsin.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
                hasCameraPermission -> CameraPreview(
                    useFrontCamera = viewModel.currentConfig().useFrontCamera,
                    onAnalyzed = { points, w, h -> viewModel.onFrameAnalyzed(points, w, h) },
                    onError = { viewModel.reportCameraError(it.message ?: "Kamera hatasi") },
                )
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Kamera izni gerekiyor")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StateBadge(uiState.currentState)
            SpeakingBadge(isSpeaking = uiState.isSpeaking, confirmed = uiState.speakingConfirmed)
        }
        Text(uiState.infoText, style = MaterialTheme.typography.bodySmall)
        uiState.cameraError?.let { Text("Kamera hatasi: $it", color = MaterialTheme.colorScheme.error) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Arkaplanda Takip", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (backgroundActive) "Acik - baska uygulamalara gecince de takip devam eder."
                            else "Kapali - uygulamadan cikinca takip durur.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = backgroundActive,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                                    batteryOptimizationLauncher.launch(
                                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizationsIntent(context)
                                    )
                                }
                                androidx.core.content.ContextCompat.startForegroundService(context, StudyForegroundService.startIntent(context))
                            } else {
                                context.startService(StudyForegroundService.stopIntent(context))
                            }
                        },
                    )
                }
                Text(
                    "Bazı telefon markalarında (Xiaomi/MIUI, Oppo/ColorOS, Huawei/EMUI, Samsung) " +
                        "arkaplan takibinin kesintisiz çalışması için Ayarlar > Uygulamalar > Ders " +
                        "Çalışma Takibi > Pil/Otomatik başlatma bölümünden uygulamaya izin vermen gerekebilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bugunku ilerleme", style = MaterialTheme.typography.titleMedium)
                Text(fmtHms(uiState.studyingSeconds), style = MaterialTheme.typography.headlineMedium)
                val goalSeconds = uiState.dailyGoalHours * 3600
                val progress = if (goalSeconds > 0) (uiState.studyingSeconds / goalSeconds).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Verimlilik: %${uiState.productivityScore} · Konusma: ${fmtHms(uiState.speakingSeconds)}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bu hafta", style = MaterialTheme.typography.titleMedium)
                val weeklyGoalSeconds = viewModel.currentConfig().weeklyGoalHours * 3600
                val weeklyProgress = if (weeklyGoalSeconds > 0) (weeklySeconds / weeklyGoalSeconds).toFloat().coerceIn(0f, 1f) else 0f
                Text(fmtHms(weeklySeconds), style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(progress = { weeklyProgress }, modifier = Modifier.fillMaxWidth())
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pomodoro", style = MaterialTheme.typography.titleMedium)
                Text(pomodoroLabel(uiState.pomodoroState, uiState.pomodoroRemainingSeconds), style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.togglePomodoro() }) {
                        Text(if (uiState.pomodoroState == PomodoroState.WORKING) "Pomodoro Durdur" else "Pomodoro Baslat")
                    }
                    OutlinedButton(onClick = { viewModel.manualBreak() }) {
                        Text("Mola Ver")
                    }
                }
                if (uiState.pauseNotice.isNotBlank()) {
                    Text(uiState.pauseNotice, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showNotesDialog = true }, modifier = Modifier.weight(1f)) {
                Text("Notlar")
            }
            OutlinedButton(
                onClick = {
                    if (role == Role.ADMIN) showResetConfirm = true else showAdminRequiredNotice = true
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Hedefi Sifirla")
            }
        }
        if (showAdminRequiredNotice) {
            Text(
                "Bu islem icin yonetici modu gerekiyor.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    // study_tracker2.py -> MainWindow._open_notes(): oturumu sifirlamadan not/etiket gunceller.
    if (showNotesDialog) {
        NotesDialog(
            title = "Oturum Notlari",
            confirmLabel = "Kaydet",
            initialNotes = uiState.sessionNotes,
            initialTags = uiState.sessionTags,
            onDismiss = { showNotesDialog = false },
            onSave = { notes, tags ->
                viewModel.updateSessionNotes(notes, tags)
                showNotesDialog = false
            },
        )
    }

    // study_tracker2.py -> MainWindow._reset_goal(): onay + (SESSION_NOTE_PROMPT acikken) not istemi.
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Hedefi Sifirla") },
            text = { Text("Bugunku ilerleme kaydedilip sifirlanacak. Devam edilsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    if (viewModel.currentConfig().sessionNotePrompt) {
                        showResetNotesDialog = true
                    } else {
                        viewModel.resetGoal()
                    }
                }) { Text("Evet") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Iptal") } },
        )
    }

    if (showResetNotesDialog) {
        NotesDialog(
            title = "Oturum Notlari",
            confirmLabel = "Kaydet ve Sifirla",
            initialNotes = uiState.sessionNotes,
            initialTags = uiState.sessionTags,
            onDismiss = {
                showResetNotesDialog = false
                viewModel.resetGoal()
            },
            onSave = { notes, tags ->
                viewModel.resetGoal(notes = notes, tags = tags)
                showResetNotesDialog = false
            },
        )
    }
}

/** study_tracker2.py -> SessionNotesDialog (Ctrl+N): mevcut not/etiketle ONCEDEN DOLU acilir (uzerine yazmaz). */
@Composable
private fun NotesDialog(
    title: String,
    confirmLabel: String,
    initialNotes: String,
    initialTags: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var notes by remember { mutableStateOf(initialNotes) }
    var tags by remember { mutableStateOf(initialTags) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Bugun ne calistin?") })
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Etiketler (virgulle ayir)") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(notes, tags) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Iptal") } },
    )
}

@Composable
private fun StateBadge(state: StudyState) {
    val (label, color) = when (state) {
        StudyState.STUDYING -> "Calisiyor" to Color(0xFF3DDC84)
        StudyState.AWAY -> "Uzakta / Dikkatsiz" to Color(0xFFFF5D6C)
        StudyState.SLEEPING -> "Uykulu" to Color(0xFFF5A623)
    }
    Surface(color = color.copy(alpha = 0.18f), contentColor = color, shape = RoundedCornerShape(999.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

/** study_tracker2.py -> speaking_badge (StatusBadge): "Sessiz" / "Konusuyor" / "Konusuyor (esik asildi)". */
@Composable
private fun SpeakingBadge(isSpeaking: Boolean, confirmed: Boolean) {
    val (label, color) = when {
        confirmed -> "🎤 Konusuyor (esik asildi)" to Color(0xFFF5A623)
        isSpeaking -> "🎤 Konusuyor" to Color(0xFF5B8CFF)
        else -> "🎤 Sessiz" to Color(0xFF8A8F9C)
    }
    Surface(color = color.copy(alpha = 0.18f), contentColor = color, shape = RoundedCornerShape(999.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

private fun pomodoroLabel(state: PomodoroState, remaining: Double): String = when (state) {
    PomodoroState.IDLE -> "--:--:--"
    PomodoroState.PAUSED -> "DURAKLATILDI"
    else -> fmtHms(remaining)
}

@Composable
private fun CameraPreview(
    useFrontCamera: Boolean,
    onAnalyzed: (points: List<com.derscalismatakibi.app.core.Point2D>?, w: Int, h: Int) -> Unit,
    onError: (Throwable) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var faceLandmarkerHelper by remember { mutableStateOf<FaceLandmarkerHelper?>(null) }

    DisposableEffect(Unit) {
        faceLandmarkerHelper = try {
            FaceLandmarkerHelper(context)
        } catch (t: Throwable) {
            onError(t)
            null
        }
        onDispose {
            faceLandmarkerHelper?.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    LaunchedEffect(useFrontCamera, faceLandmarkerHelper) {
        val helper = faceLandmarkerHelper ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        imageAnalysis.setAnalyzer(
            cameraExecutor,
            CameraAnalyzer(helper, onResult = onAnalyzed, onError = onError),
        )

        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
        } catch (t: Throwable) {
            onError(t)
        }
    }
}
