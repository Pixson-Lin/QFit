package com.pixsonlin.qfit.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixsonlin.qfit.R
import com.pixsonlin.qfit.data.IntensityLevel
import com.pixsonlin.qfit.data.RunConfigStore
import com.pixsonlin.qfit.domain.BatterySettingsOpenResult
import com.pixsonlin.qfit.domain.EnvironmentChecker
import com.pixsonlin.qfit.domain.EnvironmentStatus
import com.pixsonlin.qfit.domain.HealthConnectWriter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAbout: () -> Unit,
    onOpenHistory: () -> Unit,
    onStarted: () -> Unit,
    onStartRun: (IntensityLevel, Int, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val config = remember { RunConfigStore(context) }
    val envChecker = remember { EnvironmentChecker(context) }

    var intensity by remember {
        val initial = config.getIntensityOrDefault()
        if (config.getIntensityOrNull() == null) {
            config.setIntensity(initial)
        }
        mutableStateOf(initial)
    }
    var durationMinutes by remember { mutableIntStateOf(config.getDurationMinutes()) }
    var backgroundRun by remember { mutableStateOf(config.isBackgroundRunEnabled()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var env by remember {
        mutableStateOf(
            EnvironmentStatus(
                healthConnectReady = false,
                notificationsReady = false,
                batteryReady = false,
                exactAlarmReady = false,
            ),
        )
    }
    var refreshTick by remember { mutableIntStateOf(0) }
    var didAutoRequestHc by remember { mutableStateOf(false) }


    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { refreshTick += 1 }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { refreshTick += 1 }

    LaunchedEffect(refreshTick) {
        env = envChecker.status()
    }

    // After system settings / permission dialogs, re-read checkbox state on resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        env = envChecker.status()
        val status = HealthConnectWriter.sdkStatus(context)
        if (
            !didAutoRequestHc &&
            status == HealthConnectClient.SDK_AVAILABLE &&
            !env.healthConnectReady
        ) {
            didAutoRequestHc = true
            hcPermissionLauncher.launch(HealthConnectWriter.requiredPermissions)
        }
    }

    val estimatedSteps = intensity.cadenceSpm * durationMinutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_bar_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            SectionBox {
                Text(
                    text = stringResource(R.string.label_type),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = intensity.menuLabel(),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.label_choose_type)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        IntensityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.menuLabel()) },
                                onClick = {
                                    intensity = level
                                    config.setIntensity(level)
                                    dropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            SectionBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_duration),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            R.string.estimate_steps,
                            durationMinutes,
                            estimatedSteps.toString(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                    )
                }
                DurationMinutesSlider(
                    valueMinutes = durationMinutes,
                    onValueMinutesChange = {
                        durationMinutes = it
                        config.setDurationMinutes(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    if (!env.healthConnectReady) {
                        scope.launch {
                            snackbar.showSnackbar(context.getString(R.string.err_hc_required))
                        }
                        hcPermissionLauncher.launch(HealthConnectWriter.requiredPermissions)
                        return@Button
                    }
                    if (backgroundRun &&
                        Build.VERSION.SDK_INT >= 33 &&
                        !env.notificationsReady
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onStartRun(intensity, durationMinutes, backgroundRun)
                    onStarted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.action_start),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            // Extra space under Start only; gap from divider to checkboxes stays spacedBy(8).
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // 2×2 grid, each cell left-aligned
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusCheck(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.check_hc),
                        checked = env.healthConnectReady,
                        onClick = {
                            if (HealthConnectWriter.sdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                                context.startActivity(HealthConnectWriter.settingsIntent())
                            } else if (!env.healthConnectReady) {
                                hcPermissionLauncher.launch(HealthConnectWriter.requiredPermissions)
                            } else {
                                context.startActivity(HealthConnectWriter.settingsIntent())
                            }
                            refreshTick += 1
                        },
                    )
                    StatusCheck(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.check_background_run),
                        checked = backgroundRun,
                        onClick = {
                            val next = !backgroundRun
                            backgroundRun = next
                            config.setBackgroundRunEnabled(next)
                            if (next &&
                                Build.VERSION.SDK_INT >= 33 &&
                                !env.notificationsReady
                            ) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                            refreshTick += 1
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusCheck(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.check_battery),
                        checked = env.batteryReady,
                        onClick = {
                            // Samsung: SubSettings not exported; public battery trampoline
                            // crashes → open app info + snackbar「請點選電池」.
                            val result = envChecker.openAppBatterySettings()
                            if (result == BatterySettingsOpenResult.OPENED_APP_DETAILS) {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        context.getString(R.string.hint_tap_battery),
                                    )
                                }
                            }
                            refreshTick += 1
                        },
                    )
                    StatusCheck(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.check_alarm),
                        checked = env.exactAlarmReady,
                        onClick = {
                            context.startActivity(envChecker.exactAlarmIntent())
                            refreshTick += 1
                        },
                    )
                }
            }
            } // end scrollable column

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Button(
                    onClick = onOpenHistory,
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_history))
                }
            }
        }
    }
}

@Composable
private fun SectionBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        content = content,
    )
}

/**
 * Equal-spaced duration slider over [RunConfigStore.DURATION_STOPS].
 * Track position is by stop index (not wall-clock minutes), so each tick
 * gap looks the same; thumb snaps to the stop list.
 */
@Composable
private fun DurationMinutesSlider(
    valueMinutes: Int,
    onValueMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stops = RunConfigStore.DURATION_STOPS
    val lastIndex = (stops.size - 1).coerceAtLeast(0)
    val index = stops.indexOf(valueMinutes).let { if (it >= 0) it else stops.indexOf(RunConfigStore.snapToStep(valueMinutes)) }
        .coerceIn(0, lastIndex)

    Slider(
        value = index.toFloat(),
        onValueChange = { raw ->
            val next = raw.roundToInt().coerceIn(0, lastIndex)
            onValueMinutesChange(stops[next])
        },
        valueRange = 0f..lastIndex.toFloat(),
        steps = (lastIndex - 1).coerceAtLeast(0),
        modifier = modifier,
    )
}

@Composable
private fun StatusCheck(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
