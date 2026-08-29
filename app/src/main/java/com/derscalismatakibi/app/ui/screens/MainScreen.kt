package com.derscalismatakibi.app.ui.screens

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.camera.CameraAnalyzer
import com.derscalismatakibi.app.camera.FaceLandmarkerHelper
import com.derscalismatakibi.app.core.PomodoroState
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.StudyState
import com.derscalismatakibi.app.core.fmtHms
import com.derscalismatakibi.app.util.AppLogger
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import java.util.concurrent.Executors

@Composable
fun MainScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cfg by viewModel.configState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLogger.log("MainScreen", "Kamera izni sonucu: $granted")
        hasCameraPermission = granted
    }

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
    var showNotesDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetNotesDialog by remember { mutableStateOf(false) }
    var showAdminRequiredNotice by remember { mutableStateOf(false) }
    var weeklySeconds by remember { mutableStateOf(0.0) }
    LaunchedEffect(uiState.studyingSeconds.toInt() / 60) {
        weeklySeconds = viewModel.weeklyStudySeconds() + uiState.studyingSeconds
    }

    // Kamera onizleme karti (3:4 oran) + alttaki tum kartlar tek ekrana sigmiyor -
    // kaydirma olmadan "Arkaplanda Takip" ve altindaki her sey ekran disinda,
    // ULASILAMAZ kaliyordu. Bu, ozelligin "hic yok" gibi gorunmesinin sebebiydi.
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)

        val cameraErrorFallback = stringResource(R.string.main_camera_error_fallback)
        Card(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            when {
                backgroundActive -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.main_background_tracking_active),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                !cfg.cameraAnalysisEnabled -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.main_camera_analysis_disabled),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                hasCameraPermission -> CameraPreview(
                    useFrontCamera = viewModel.currentConfig().useFrontCamera,
                    onAnalyzed = { points, w, h -> viewModel.onFrameAnalyzed(points, w, h) },
                    onError = {
                        AppLogger.logError("Kamera", "MainScreen onizleme hatasi", it)
                        viewModel.reportCameraError("${it::class.simpleName}: ${it.message ?: cameraErrorFallback}")
                    },
                )
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.main_camera_permission_needed))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StateBadge(uiState.currentState)
            SpeakingBadge(isSpeaking = uiState.isSpeaking, confirmed = uiState.speakingConfirmed)
        }
        Text(uiState.infoText, style = MaterialTheme.typography.bodySmall)
        uiState.cameraError?.let { Text(stringResource(R.string.main_camera_error, it), color = MaterialTheme.colorScheme.error) }

        // "Arkaplanda Takip" anahtari Ayarlar > Calisan Sistemler'e tasindi -
        // buradaki kart sadece DURUMU (backgroundActive uzerinden yukarida)
        // gosterir, kontrolu artik Ayarlar'da.

        // Asagidaki 3 kart ayri composable'lara cikarildi: uiState kamera calisirken
        // saniyede 15-30 kez YENI nesne kimligiyle guncelleniyor - hepsi tek bir
        // Column icinde MainScreen'in govdesinde kalsaydi, sadece StateBadge/infoText
        // degisse bile Pomodoro/haftalik/gunluk kartlarinin TAMAMI (ilerleme cubugu,
        // butonlar - hicbiri degismese bile) her karede yeniden olculup ciziliyordu.
        // Artik her kart SADECE kendi parametreleri degisince yeniden ciziliyor.
        ProgressCard(
            studyingSeconds = uiState.studyingSeconds,
            dailyGoalHours = uiState.dailyGoalHours,
            productivityScore = uiState.productivityScore,
            speakingSeconds = uiState.speakingSeconds,
        )

        WeeklyProgressCard(weeklySeconds = weeklySeconds, weeklyGoalHours = viewModel.currentConfig().weeklyGoalHours)

        PomodoroCard(
            state = uiState.pomodoroState,
            remainingSeconds = uiState.pomodoroRemainingSeconds,
            pauseNotice = uiState.pauseNotice,
            onToggle = { viewModel.togglePomodoro() },
            onManualBreak = { viewModel.manualBreak() },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showNotesDialog = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.main_notes))
            }
            OutlinedButton(
                onClick = {
                    if (role == Role.ADMIN) showResetConfirm = true else showAdminRequiredNotice = true
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.main_reset_goal))
            }
        }
        if (showAdminRequiredNotice) {
            Text(
                stringResource(R.string.schedule_admin_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    // study_tracker2.py -> MainWindow._open_notes(): oturumu sifirlamadan not/etiket gunceller.
    if (showNotesDialog) {
        NotesDialog(
            title = stringResource(R.string.main_session_notes_title),
            confirmLabel = stringResource(R.string.action_save),
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
            title = { Text(stringResource(R.string.main_reset_goal)) },
            text = { Text(stringResource(R.string.main_reset_goal_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    if (viewModel.currentConfig().sessionNotePrompt) {
                        showResetNotesDialog = true
                    } else {
                        viewModel.resetGoal()
                    }
                }) { Text(stringResource(R.string.action_yes)) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showResetNotesDialog) {
        NotesDialog(
            title = stringResource(R.string.main_session_notes_title),
            confirmLabel = stringResource(R.string.main_save_and_reset),
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
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.main_notes_field_label)) })
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text(stringResource(R.string.main_tags_field_label)) })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(notes, tags) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ProgressCard(studyingSeconds: Double, dailyGoalHours: Double, productivityScore: Double, speakingSeconds: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.main_today_progress), style = MaterialTheme.typography.titleMedium)
            Text(fmtHms(studyingSeconds), style = MaterialTheme.typography.headlineMedium)
            val goalSeconds = dailyGoalHours * 3600
            val progress = if (goalSeconds > 0) (studyingSeconds / goalSeconds).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.main_productivity_speaking, productivityScore, fmtHms(speakingSeconds)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WeeklyProgressCard(weeklySeconds: Double, weeklyGoalHours: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.stats_this_week), style = MaterialTheme.typography.titleMedium)
            val weeklyGoalSeconds = weeklyGoalHours * 3600
            val weeklyProgress = if (weeklyGoalSeconds > 0) (weeklySeconds / weeklyGoalSeconds).toFloat().coerceIn(0f, 1f) else 0f
            Text(fmtHms(weeklySeconds), style = MaterialTheme.typography.headlineSmall)
            LinearProgressIndicator(progress = { weeklyProgress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PomodoroCard(
    state: PomodoroState,
    remainingSeconds: Double,
    pauseNotice: String,
    onToggle: () -> Unit,
    onManualBreak: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.main_pomodoro), style = MaterialTheme.typography.titleMedium)
            Text(pomodoroLabel(state, remainingSeconds, stringResource(R.string.main_pomodoro_paused)), style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onToggle) {
                    Text(if (state == PomodoroState.WORKING) stringResource(R.string.main_pomodoro_stop) else stringResource(R.string.main_pomodoro_start))
                }
                OutlinedButton(onClick = onManualBreak) {
                    Text(stringResource(R.string.main_pomodoro_break))
                }
            }
            if (pauseNotice.isNotBlank()) {
                Text(pauseNotice, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StateBadge(state: StudyState) {
    val (label, color) = when (state) {
        StudyState.STUDYING -> stringResource(R.string.main_state_studying) to Color(0xFF3DDC84)
        StudyState.AWAY -> stringResource(R.string.main_state_away) to Color(0xFFFF5D6C)
        StudyState.SLEEPING -> stringResource(R.string.main_state_sleeping) to Color(0xFFF5A623)
    }
    Surface(color = color.copy(alpha = 0.18f), contentColor = color, shape = RoundedCornerShape(999.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

/** study_tracker2.py -> speaking_badge (StatusBadge): "Sessiz" / "Konusuyor" / "Konusuyor (esik asildi)". */
@Composable
private fun SpeakingBadge(isSpeaking: Boolean, confirmed: Boolean) {
    val (label, color) = when {
        confirmed -> stringResource(R.string.main_speaking_confirmed) to Color(0xFFF5A623)
        isSpeaking -> stringResource(R.string.main_speaking) to Color(0xFF5B8CFF)
        else -> stringResource(R.string.main_speaking_silent) to Color(0xFF8A8F9C)
    }
    Surface(color = color.copy(alpha = 0.18f), contentColor = color, shape = RoundedCornerShape(999.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
    }
}

private fun pomodoroLabel(state: PomodoroState, remaining: Double, pausedLabel: String): String = when (state) {
    PomodoroState.IDLE -> "--:--:--"
    PomodoroState.PAUSED -> pausedLabel
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
    // cameraProvider.bindToLifecycle Activity'nin yasam dongusune baglanir, Compose'un
    // bu composable'i kaldirmasiyla KENDILIGINDEN unbind OLMAZ - Calisan Sistemler'den
    // Kamera/MediaPipe Analizi kapatilinca ekran degissin diye bu composable kaldirilsa
    // bile kamera/analiz arkaplanda calismaya devam ediyordu. onDispose'ta unbindAll()
    // ile bunu artik acikca durduruyoruz.
    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        faceLandmarkerHelper = try {
            FaceLandmarkerHelper(context)
        } catch (t: Throwable) {
            onError(t)
            null
        }
        onDispose {
            boundCameraProvider?.unbindAll()
            faceLandmarkerHelper?.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    LaunchedEffect(useFrontCamera, faceLandmarkerHelper) {
        val helper = faceLandmarkerHelper ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        boundCameraProvider = cameraProvider

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
