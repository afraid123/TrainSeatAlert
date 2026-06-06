package com.trainseat.app.presentation.addedit

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainseat.app.data.model.AlertConfig
import com.trainseat.app.util.INTERVAL_OPTIONS
import com.trainseat.app.util.QUOTA_OPTIONS
import com.trainseat.app.util.TRAVEL_CLASS_OPTIONS
import com.trainseat.app.util.toIntervalLabel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlertScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.trainNumber.isBlank()) "New Alert" else "Edit Alert") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Train Information")

            OutlinedTextField(
                value = uiState.trainNumber,
                onValueChange = viewModel::onTrainNumberChange,
                label = { Text("Train Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.validationErrors.containsKey("trainNumber"),
                supportingText = {
                    uiState.validationErrors["trainNumber"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Train number input" },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.trainName,
                onValueChange = viewModel::onTrainNameChange,
                label = { Text("Train Name") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Train name input" },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.fromStation,
                    onValueChange = viewModel::onFromStationChange,
                    label = { Text("From Station *") },
                    isError = uiState.validationErrors.containsKey("fromStation"),
                    supportingText = {
                        uiState.validationErrors["fromStation"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "From station code" },
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.toStation,
                    onValueChange = viewModel::onToStationChange,
                    label = { Text("To Station *") },
                    isError = uiState.validationErrors.containsKey("toStation"),
                    supportingText = {
                        uiState.validationErrors["toStation"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "To station code" },
                    singleLine = true
                )
            }

            // Date picker
            val cal = remember { Calendar.getInstance() }
            OutlinedTextField(
                value = uiState.journeyDate,
                onValueChange = {},
                label = { Text("Journey Date *") },
                readOnly = true,
                isError = uiState.validationErrors.containsKey("journeyDate"),
                supportingText = {
                    uiState.validationErrors["journeyDate"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                trailingIcon = {
                    IconButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                viewModel.onJourneyDateChange(
                                    "%04d-%02d-%02d".format(year, month + 1, day)
                                )
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.minDate = System.currentTimeMillis() - 1000
                        }.show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Journey date" },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownSelector(
                    label = "Travel Class *",
                    options = TRAVEL_CLASS_OPTIONS,
                    selected = uiState.travelClass,
                    onSelect = viewModel::onTravelClassChange,
                    modifier = Modifier.weight(1f)
                )
                DropdownSelector(
                    label = "Quota",
                    options = QUOTA_OPTIONS,
                    selected = uiState.quota,
                    onSelect = viewModel::onQuotaChange,
                    modifier = Modifier.weight(1f)
                )
            }

            SectionHeader("Alert Configuration")

            OutlinedTextField(
                value = uiState.threshold,
                onValueChange = viewModel::onThresholdChange,
                label = { Text("Seat Threshold *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.validationErrors.containsKey("threshold"),
                supportingText = {
                    uiState.validationErrors["threshold"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("Alert when seats drop below this number")
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Seat threshold" },
                singleLine = true
            )

            DropdownSelector(
                label = "Polling Interval",
                options = INTERVAL_OPTIONS.map { it.toIntervalLabel() },
                selected = uiState.intervalMinutes.toIntervalLabel(),
                onSelect = { label ->
                    val minutes = INTERVAL_OPTIONS.firstOrNull { it.toIntervalLabel() == label } ?: 30
                    viewModel.onIntervalChange(minutes)
                },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownSelector(
                label = "Alert Sound",
                options = listOf(
                    AlertConfig.SOUND_ALARM to "Alarm Ringtone",
                    AlertConfig.SOUND_NOTIFICATION to "Notification Sound",
                    AlertConfig.SOUND_SILENT to "Silent"
                ).map { it.second },
                selected = when (uiState.alertSound) {
                    AlertConfig.SOUND_ALARM -> "Alarm Ringtone"
                    AlertConfig.SOUND_NOTIFICATION -> "Notification Sound"
                    else -> "Silent"
                },
                onSelect = { label ->
                    val code = when (label) {
                        "Alarm Ringtone" -> AlertConfig.SOUND_ALARM
                        "Notification Sound" -> AlertConfig.SOUND_NOTIFICATION
                        else -> AlertConfig.SOUND_SILENT
                    }
                    viewModel.onAlertSoundChange(code)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibrate", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.vibrate,
                    onCheckedChange = viewModel::onVibrateChange,
                    modifier = Modifier.semantics { contentDescription = "Vibrate toggle" }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Repeat Until Dismissed", style = MaterialTheme.typography.bodyLarge)
                    Text("Re-alarm every 2 min", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.repeatUntilDismissed,
                    onCheckedChange = viewModel::onRepeatUntilDismissedChange,
                    modifier = Modifier.semantics { contentDescription = "Repeat until dismissed toggle" }
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes / Label") },
                supportingText = { Text("${uiState.notes.length}/100") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Notes" },
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Cancel" }
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = viewModel::saveAlert,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Save and start monitoring" }
                ) {
                    Text("Save & Start")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
                .semantics { contentDescription = "$label selector" }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
