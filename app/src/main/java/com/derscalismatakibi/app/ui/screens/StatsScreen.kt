package com.derscalismatakibi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.derscalismatakibi.app.core.fmtHms
import com.derscalismatakibi.app.data.DailyTotal
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.launch

/**
 * study_tracker2.py -> StatsDialog'un basitlestirilmis Android karsiligi:
 * matplotlib grafigi yerine gunluk toplam calisma surelerini listeleyen
 * bir LazyColumn (bu turda grafik kapsam disi, README'de belirtildi).
 */
@Composable
fun StatsScreen(viewModel: StudyViewModel) {
    var totals by remember { mutableStateOf<List<DailyTotal>>(emptyList()) }
    var weeklySeconds by remember { mutableStateOf(0.0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        totals = viewModel.dailyTotals(30)
        weeklySeconds = viewModel.weeklyStudySeconds()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Istatistikler", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bu hafta", style = MaterialTheme.typography.titleMedium)
                Text(fmtHms(weeklySeconds), style = MaterialTheme.typography.headlineSmall)
            }
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    val intent = viewModel.buildExportIntent()
                    context.startActivity(android.content.Intent.createChooser(intent, "Verileri Paylas"))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Disa Aktar (CSV)") }

        if (totals.isEmpty()) {
            Card(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Henuz veri yok", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Bir calisma oturumu tamamladiginda istatistikler burada gorunecek.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(totals) { row ->
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(row.date, style = MaterialTheme.typography.bodyLarge)
                            Text(fmtHms(row.total), style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
