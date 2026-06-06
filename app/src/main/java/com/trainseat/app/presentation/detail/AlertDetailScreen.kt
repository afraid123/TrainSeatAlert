package com.trainseat.app.presentation.detail

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.data.model.CheckResult
import com.trainseat.app.presentation.theme.StatusActive
import com.trainseat.app.presentation.theme.StatusPaused
import com.trainseat.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    onNavigateBack: () -> Unit,
    onEditAlert: (Long) -> Unit,
    viewModel: AlertDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Alert") },
            text = { Text("Are you sure you want to delete this alert? All history will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAlert()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.alert?.let { alert ->
                        IconButton(
                            onClick = { onEditAlert(alert.id) },
                            modifier = Modifier.semantics { contentDescription = "Edit alert" }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.semantics { contentDescription = "Delete alert" }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val alert = uiState.alert
        if (alert == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Alert not found")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { AlertInfoCard(alert = alert) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = viewModel::checkNow,
                        enabled = !uiState.isCheckingNow,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Check now" }
                    ) {
                        if (uiState.isCheckingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Check Now")
                        }
                    }
                    OutlinedButton(
                        onClick = viewModel::togglePause,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = if (alert.status == AlertConfig.STATUS_PAUSED) "Resume alert" else "Pause alert"
                        }
                    ) {
                        Text(if (alert.status == AlertConfig.STATUS_PAUSED) "Resume" else "Pause")
                    }
                }
            }

            if (uiState.checkResults.isNotEmpty()) {
                item {
                    Text(
                        "Availability Chart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SeatAvailabilityChart(
                        checkResults = uiState.checkResults,
                        threshold = alert.threshold
                    )
                }

                item {
                    Text(
                        "Check History (${uiState.checkResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(uiState.checkResults) { result ->
                    CheckResultRow(result = result)
                }
            } else {
                item {
                    Text(
                        "No check history yet. Tap 'Check Now' or wait for the scheduled check.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertInfoCard(alert: AlertConfig) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Train ${alert.trainNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    color = (if (alert.status == AlertConfig.STATUS_ACTIVE) StatusActive else StatusPaused).copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        alert.status,
                        color = if (alert.status == AlertConfig.STATUS_ACTIVE) StatusActive else StatusPaused,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (alert.trainName.isNotBlank()) {
                Text(alert.trainName, style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            InfoRow("Route", "${alert.fromStation} → ${alert.toStation}")
            InfoRow("Date", DateUtils.formatIsoDate(alert.journeyDate))
            InfoRow("Class", alert.travelClass)
            InfoRow("Quota", alert.quota)
            InfoRow("Threshold", "${alert.threshold} seats")
            InfoRow("Check Interval", "${alert.intervalMinutes} min")
            InfoRow("Sound", alert.alertSound)
            InfoRow("Last Checked", DateUtils.formatTimestamp(alert.lastChecked))
            alert.lastAvailable?.let { InfoRow("Last Available", "$it seats") }
            if (alert.notes.isNotBlank()) InfoRow("Notes", alert.notes)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CheckResultRow(result: CheckResult) {
    val statusColor = when {
        result.statusCode.startsWith("AVL") -> MaterialTheme.colorScheme.primary
        result.statusCode.startsWith("WL") -> MaterialTheme.colorScheme.secondary
        result.statusCode == "REGRET" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = "Check at ${DateUtils.formatTimestamp(result.timestamp)}, status ${result.statusCode}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(DateUtils.formatTimestamp(result.timestamp), style = MaterialTheme.typography.labelSmall)
        }
        Text(
            result.statusCode,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
            fontWeight = FontWeight.SemiBold
        )
    }
    HorizontalDivider()
}

@Composable
private fun SeatAvailabilityChart(checkResults: List<CheckResult>, threshold: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val errorColor = MaterialTheme.colorScheme.error.toArgb()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    400
                )
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawLabels(false)
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = checkResults
                .filter { it.availableSeats > 0 }
                .reversed()
                .mapIndexed { index, result ->
                    Entry(index.toFloat(), result.availableSeats.toFloat())
                }

            if (entries.isEmpty()) return@AndroidView

            val dataSet = LineDataSet(entries, "Available Seats").apply {
                color = primaryColor
                setCircleColor(primaryColor)
                lineWidth = 2f
                circleRadius = 3f
                setDrawFilled(true)
                fillColor = primaryColor
                fillAlpha = 50
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            val thresholdEntries = listOf(
                Entry(0f, threshold.toFloat()),
                Entry((entries.size - 1).toFloat(), threshold.toFloat())
            )
            val thresholdSet = LineDataSet(thresholdEntries, "Threshold").apply {
                color = errorColor
                lineWidth = 1.5f
                enableDashedLine(10f, 5f, 0f)
                setDrawCircles(false)
                setDrawFilled(false)
            }

            chart.data = LineData(dataSet, thresholdSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = "Seat availability chart" }
    )
}
