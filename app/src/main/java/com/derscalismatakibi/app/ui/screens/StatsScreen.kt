package com.derscalismatakibi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.derscalismatakibi.app.R
import com.derscalismatakibi.app.core.fmtHms
import com.derscalismatakibi.app.data.DailyTotal
import com.derscalismatakibi.app.viewmodel.StudyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * study_tracker2.py -> StatsDialog'un Android karsiligi: gunluk toplam calisma
 * surelerini hem cubuk grafik (Vico) hem de liste olarak gosterir.
 */
@Composable
fun StatsScreen(viewModel: StudyViewModel) {
    var totals by remember { mutableStateOf<List<DailyTotal>>(emptyList()) }
    var weeklySeconds by remember { mutableStateOf(0.0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    // Grafikte en fazla son 14 gun gosterilir - okunabilirlik icin (liste asagida
    // hala tum 30 gunu gosterir).
    val chartDays = remember(totals) { totals.take(14).asReversed() }
    val chartDateFmt = remember { SimpleDateFormat("dd.MM", Locale.US) }
    val isoDateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    LaunchedEffect(Unit) {
        totals = viewModel.dailyTotals(30)
        weeklySeconds = viewModel.weeklyStudySeconds()
    }
    LaunchedEffect(chartDays) {
        val entries = chartDays.mapIndexed { index, row ->
            FloatEntry(x = index.toFloat(), y = (row.total / 3600.0).toFloat())
        }
        chartEntryModelProducer.setEntries(entries)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.stats_this_week), style = MaterialTheme.typography.titleMedium)
                Text(fmtHms(weeklySeconds), style = MaterialTheme.typography.headlineSmall)
            }
        }

        if (chartDays.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.stats_chart_title), style = MaterialTheme.typography.titleMedium)
                    val bottomLabels = remember(chartDays) {
                        chartDays.map { row ->
                            try {
                                chartDateFmt.format(isoDateFmt.parse(row.date) ?: return@map row.date)
                            } catch (t: Throwable) {
                                row.date
                            }
                        }
                    }
                    val chartStyle = ChartStyle.fromColors(
                        axisLabelColor = MaterialTheme.colorScheme.onSurface,
                        axisGuidelineColor = MaterialTheme.colorScheme.outlineVariant,
                        axisLineColor = MaterialTheme.colorScheme.outline,
                        entityColors = listOf(MaterialTheme.colorScheme.primary),
                        elevationOverlayColor = MaterialTheme.colorScheme.onSurface,
                    )
                    ProvideChartStyle(chartStyle) {
                        Chart(
                            chart = columnChart(),
                            chartModelProducer = chartEntryModelProducer,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(
                                valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                                    bottomLabels.getOrNull(value.toInt()) ?: ""
                                },
                            ),
                            modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.stats_chart_y_axis_note),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    val intent = viewModel.buildExportIntent()
                    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.stats_export_chooser_title)))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.stats_export_csv)) }

        if (totals.isEmpty()) {
            Card(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.stats_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.stats_empty_body),
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
