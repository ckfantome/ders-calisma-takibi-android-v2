package com.derscalismatakibi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.core.Role
import com.derscalismatakibi.app.core.SLOT_KIND_BREAK
import com.derscalismatakibi.app.core.SLOT_KIND_LABELS
import com.derscalismatakibi.app.core.SLOT_KIND_WORK
import com.derscalismatakibi.app.core.WEEKDAY_NAMES
import com.derscalismatakibi.app.data.ScheduleSlotEntity
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.launch

/**
 * study_tracker2.py -> ScheduleDialog (Ctrl+H) + MainWindow._toggle_schedule_tracking()
 * karsiligi: haftalik Calisma/Mola araliklarini duzenleme + Takvim Takip
 * modunu baslatma/durdurma.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: StudyViewModel) {
    val role by viewModel.role.collectAsState()
    val slots by viewModel.scheduleSlots.collectAsState()
    val trackingEnabled by viewModel.scheduleTrackingEnabled.collectAsState()
    var selectedDay by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf<String?>(null) } // "calisma" / "mola" / null
    var showStartConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isAdmin = role == Role.ADMIN

    fun requireAdmin(): Boolean {
        if (!isAdmin) {
            scope.launch { snackbarHostState.showSnackbar("Bu islem icin yonetici modu gerekiyor.") }
        }
        return isAdmin
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Zaman Cizelgesi", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (trackingEnabled) "Takvim takibi ACIK" else "Takvim takibi kapali",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = {
                        if (trackingEnabled) {
                            viewModel.stopScheduleTracking()
                        } else {
                            showStartConfirm = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (trackingEnabled) "Takvim Takibini Durdur" else "Takvim Takibini Baslat")
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedDay) {
            WEEKDAY_NAMES.forEachIndexed { index, name ->
                Tab(selected = selectedDay == index, onClick = { selectedDay = index }, text = { Text(name) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (requireAdmin()) showAddDialog = SLOT_KIND_WORK }) { Text("+ Calisma") }
            OutlinedButton(onClick = { if (requireAdmin()) showAddDialog = SLOT_KIND_BREAK }) { Text("+ Mola") }
        }

        val daySlots = slots.filter { it.day == selectedDay }.sortedBy { it.startTime }
        if (daySlots.isEmpty()) {
            Text("Bu gun icin planlanmis aralik yok.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(daySlots) { slot -> ScheduleRow(slot, isAdmin) { if (requireAdmin()) viewModel.deleteScheduleSlot(slot) } }
            }
        }

        SnackbarHost(hostState = snackbarHostState) { data -> Snackbar(snackbarData = data) }
    }

    showAddDialog?.let { kind ->
        AddSlotDialog(
            kind = kind,
            onDismiss = { showAddDialog = null },
            onConfirm = { start, end ->
                viewModel.addScheduleSlot(selectedDay, start, end, kind)
                showAddDialog = null
            },
        )
    }

    if (showStartConfirm) {
        val summary = viewModel.todaysScheduleSummary()
        AlertDialog(
            onDismissRequest = { showStartConfirm = false },
            title = { Text("Takvim Takibini Baslat") },
            text = {
                Text(
                    if (summary.isBlank()) {
                        "Bugun icin tanimli bir zaman cizelgesi yok. Once bir aralik ekle."
                    } else {
                        "Bugunku program:\n$summary\n\nCalisma araligi baslayinca Pomodoro otomatik baslar; " +
                            "mola sirasinda calismaya devam edersen bu ayrica not edilir; planlanandan az " +
                            "calisilirsa aralik bitince bildirilir."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = summary.isNotBlank(),
                    onClick = { viewModel.startScheduleTracking(); showStartConfirm = false },
                ) { Text("Evet") }
            },
            dismissButton = { TextButton(onClick = { showStartConfirm = false }) { Text("Iptal") } },
        )
    }
}

@Composable
private fun ScheduleRow(slot: ScheduleSlotEntity, isAdmin: Boolean, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${slot.startTime}–${slot.endTime}  (${SLOT_KIND_LABELS[slot.kind] ?: slot.kind})")
            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Sil")
                }
            }
        }
    }
}

@Composable
private fun AddSlotDialog(kind: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var start by remember { mutableStateOf(if (kind == SLOT_KIND_WORK) "09:00" else "11:00") }
    var end by remember { mutableStateOf(if (kind == SLOT_KIND_WORK) "11:00" else "11:15") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == SLOT_KIND_WORK) "Calisma Araligi Ekle" else "Mola Araligi Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Baslangic (SS:DD)") })
                OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("Bitis (SS:DD)") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(start, end) }) { Text("Ekle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Iptal") } },
    )
}
