package com.pixsonlin.qfit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixsonlin.qfit.R
import com.pixsonlin.qfit.service.RunSessionState
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InProgressScreen(
    onOpenAbout: () -> Unit,
    onCancelConfirmed: () -> Unit,
    onFinishedNavigateHome: () -> Unit,
) {
    val active by RunSessionState.active.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(active?.finished) {
        if (active?.finished == true) {
            delay(400)
            onFinishedNavigateHome()
        }
    }

    LaunchedEffect(active?.startTimeMillis) {
        while (active != null && active?.finished != true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val run = active
    val elapsedMs = if (run == null) 0L else (now - run.startTimeMillis).coerceAtLeast(0L)
    val totalMs = if (run == null) 0L else (run.endTimeMillis - run.startTimeMillis)

    Scaffold(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.in_progress_title),
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProgressRow(
                        headline = stringResource(R.string.label_type),
                        supporting = run?.intensity?.label() ?: "—",
                    )
                    ProgressRow(
                        headline = stringResource(R.string.label_current_steps),
                        supporting = (run?.stepsWritten ?: 0).toString(),
                    )
                    ProgressRow(
                        headline = stringResource(R.string.label_elapsed_total),
                        supporting = "${formatMmSs(elapsedMs)}/${formatMmSs(totalMs)}",
                    )
                    run?.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // When session UI is missing or already finished, Cancel is meaningless.
            // Offer an explicit Home escape so a desynced Run screen is never a dead end.
            val canCancel = run != null && run.finished != true
            Button(
                onClick = {
                    if (canCancel) {
                        showCancelConfirm = true
                    } else {
                        onFinishedNavigateHome()
                    }
                },
                modifier = Modifier.width(182.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (canCancel) R.string.action_cancel else R.string.action_back_home,
                    ),
                )
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(stringResource(R.string.cancel_title)) },
            text = { Text(stringResource(R.string.cancel_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        onCancelConfirmed()
                    },
                ) {
                    Text(stringResource(R.string.cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(stringResource(R.string.cancel_dismiss))
                }
            },
        )
    }
}

@Composable
private fun ProgressRow(headline: String, supporting: String) {
    ListItem(
        headlineContent = { Text(headline) },
        supportingContent = { Text(supporting) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

private fun formatMmSs(millis: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(millis).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
