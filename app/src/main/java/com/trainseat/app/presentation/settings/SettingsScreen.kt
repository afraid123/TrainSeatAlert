package com.trainseat.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainseat.app.BuildConfig
import com.trainseat.app.presentation.theme.ThemeMode
import com.trainseat.app.util.INTERVAL_OPTIONS
import com.trainseat.app.util.toIntervalLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val defaultInterval by viewModel.defaultIntervalMinutes.collectAsStateWithLifecycle()
    val alarmVolume by viewModel.alarmVolume.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val exportResult by viewModel.exportResult.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val rapidApiKey by viewModel.rapidApiKey.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exportResult) {
        if (exportResult != null) showExportDialog = true
    }

    LaunchedEffect(importResult) {
        if (importResult != null) viewModel.clearImportResult()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
            if (json.isNotBlank()) viewModel.importAlerts(json)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSectionHeader("Seat Data API (RapidAPI – IRCTC)")

            Text(
                text = "✓ Seat checks are active using the built-in API key. " +
                        "You can optionally use your own RapidAPI key below (e.g. for a higher quota).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Your RapidAPI Key (optional)") },
                placeholder = { Text("paste X-RapidAPI-Key here") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "RapidAPI key input field" }
            )

            Button(
                onClick = {
                    viewModel.setRapidApiKey(apiKeyInput)
                    apiKeyInput = ""
                },
                enabled = apiKeyInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Save API key" }
            ) {
                Text("Use My Own Key")
            }

            HorizontalDivider()
            SettingsSectionHeader("Monitoring")

            SettingsClickItem(
                title = "Default Polling Interval",
                subtitle = defaultInterval.toIntervalLabel(),
                onClick = { showIntervalDialog = true },
                modifier = Modifier.semantics { contentDescription = "Default polling interval: ${defaultInterval.toIntervalLabel()}" }
            )

            HorizontalDivider()
            SettingsSectionHeader("Alarm")

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alarm Volume: $alarmVolume%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = alarmVolume.toFloat(),
                    onValueChange = { viewModel.setAlarmVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.semantics { contentDescription = "Alarm volume slider, current value $alarmVolume percent" }
                )
            }

            SettingsSwitchItem(
                title = "Keep Screen On When Alarm Fires",
                checked = keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) },
                modifier = Modifier.semantics { contentDescription = "Keep screen on when alarm fires" }
            )

            HorizontalDivider()
            SettingsSectionHeader("Appearance")

            SettingsClickItem(
                title = "Dark Mode",
                subtitle = when (themeMode) {
                    ThemeMode.SYSTEM -> "System Default"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                onClick = { showThemeDialog = true },
                modifier = Modifier.semantics { contentDescription = "Dark mode setting" }
            )

            HorizontalDivider()
            SettingsSectionHeader("Notifications")

            SettingsClickItem(
                title = "Notification Settings",
                subtitle = "Open system notification settings",
                icon = Icons.Default.Notifications,
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.semantics { contentDescription = "Open notification settings" }
            )

            HorizontalDivider()
            SettingsSectionHeader("Data")

            SettingsClickItem(
                title = "Export Alerts",
                subtitle = "Save all alerts to a JSON file",
                onClick = { viewModel.exportAlerts() },
                modifier = Modifier.semantics { contentDescription = "Export alerts to JSON" }
            )

            SettingsClickItem(
                title = "Import Alerts",
                subtitle = "Load alerts from a JSON file",
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.semantics { contentDescription = "Import alerts from JSON" }
            )

            HorizontalDivider()
            SettingsSectionHeader("About")

            SettingsInfoItem(
                title = "Version",
                value = BuildConfig.VERSION_NAME
            )

            SettingsInfoItem(
                title = "Build",
                value = BuildConfig.VERSION_CODE.toString()
            )

            SettingsClickItem(
                title = "GitHub",
                subtitle = "https://github.com/trainseat/app",
                icon = Icons.Default.Info,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/trainseat/app"))
                    context.startActivity(intent)
                },
                modifier = Modifier.semantics { contentDescription = "Open GitHub repository" }
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Dark Mode") },
            text = {
                Column {
                    listOf(ThemeMode.SYSTEM to "System Default", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                        .forEach { (mode, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                Text(label)
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text("Default Polling Interval") },
            text = {
                Column {
                    INTERVAL_OPTIONS.forEach { minutes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultInterval == minutes,
                                onClick = {
                                    viewModel.setDefaultInterval(minutes)
                                    showIntervalDialog = false
                                }
                            )
                            Text(minutes.toIntervalLabel())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportDialog && exportResult != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                viewModel.clearExportResult()
            },
            title = { Text("Export Data") },
            text = {
                Column {
                    Text("Copy the JSON below or save it manually:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportResult ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    viewModel.clearExportResult()
                }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsInfoItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
