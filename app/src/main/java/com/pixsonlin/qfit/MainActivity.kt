package com.pixsonlin.qfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PocScreen(
                        openHealthConnect = {
                            startActivity(HealthConnectPoc.settingsIntent())
                        },
                        openPlayStoreForHc = {
                            startActivity(HealthConnectPoc.playStoreIntent())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PocScreen(
    openHealthConnect: () -> Unit,
    openPlayStoreForHc: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<PocUiState>(PocUiState.Checking) }
    var attempt by remember { mutableIntStateOf(0) }
    var pendingWriteAfterGrant by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.containsAll(HealthConnectPoc.requiredPermissions)) {
            pendingWriteAfterGrant = true
        } else {
            state = PocUiState.PermissionDenied
        }
    }

    fun writeSteps() {
        scope.launch {
            state = PocUiState.Writing
            runCatching {
                val client = HealthConnectPoc.getClient(context)
                HealthConnectPoc.writePocSteps(client)
            }.onSuccess {
                state = PocUiState.Written()
            }.onFailure { error ->
                state = PocUiState.Error(error.message ?: error::class.java.simpleName)
            }
        }
    }

    fun startFlow() {
        scope.launch {
            state = PocUiState.Checking
            when (HealthConnectPoc.sdkStatus(context)) {
                HealthConnectClient.SDK_UNAVAILABLE -> {
                    state = PocUiState.Unavailable
                    return@launch
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    state = PocUiState.NeedInstall
                    return@launch
                }
            }

            val client = HealthConnectPoc.getClient(context)
            if (HealthConnectPoc.hasWritePermission(client)) {
                writeSteps()
            } else {
                state = PocUiState.RequestingPermission
                permissionLauncher.launch(HealthConnectPoc.requiredPermissions)
            }
        }
    }

    LaunchedEffect(attempt) {
        startFlow()
    }

    LaunchedEffect(pendingWriteAfterGrant) {
        if (pendingWriteAfterGrant) {
            pendingWriteAfterGrant = false
            writeSteps()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = statusText(state),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (state) {
            is PocUiState.NeedInstall -> {
                Button(
                    onClick = openPlayStoreForHc,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.status_need_install))
                }
            }
            is PocUiState.PermissionDenied,
            is PocUiState.Error,
            is PocUiState.Unavailable,
            -> {
                Button(
                    onClick = { attempt += 1 },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }
            is PocUiState.Written -> {
                Button(
                    onClick = openHealthConnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.action_open_hc))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { writeSteps() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.action_write_again))
                }
            }
            else -> {
                // Checking / requesting / writing — no primary action yet.
            }
        }

        if (state !is PocUiState.Written && state !is PocUiState.Checking) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = openHealthConnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_open_hc))
            }
        }
    }
}

@Composable
private fun statusText(state: PocUiState): String = when (state) {
    PocUiState.Checking -> stringResource(R.string.status_checking)
    PocUiState.Unavailable -> stringResource(R.string.status_unavailable)
    PocUiState.NeedInstall -> stringResource(R.string.status_need_install)
    PocUiState.RequestingPermission -> stringResource(R.string.status_requesting)
    PocUiState.PermissionDenied -> stringResource(R.string.status_denied)
    PocUiState.Writing -> stringResource(R.string.status_writing)
    is PocUiState.Written -> stringResource(R.string.status_written)
    is PocUiState.Error -> stringResource(R.string.status_error, state.message)
}
