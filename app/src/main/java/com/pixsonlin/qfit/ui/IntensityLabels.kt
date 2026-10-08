package com.pixsonlin.qfit.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixsonlin.qfit.R
import com.pixsonlin.qfit.data.IntensityLevel
import com.pixsonlin.qfit.data.db.RunEntity
import com.pixsonlin.qfit.data.db.RunStatus
import com.pixsonlin.qfit.data.db.SegmentWriteStatus

@Composable
fun IntensityLevel.label(): String = stringResource(labelRes)

@Composable
fun IntensityLevel.menuLabel(): String =
    stringResource(R.string.intensity_menu_format, label(), cadenceSpm)

@Composable
fun intensityLabelForRun(run: RunEntity): String {
    val fromKey = runCatching { IntensityLevel.valueOf(run.intensityName) }.getOrNull()
    if (fromKey != null) return fromKey.label()
    // Legacy rows may store a localized name in intensityDisplayName.
    return run.intensityDisplayName.ifBlank { run.intensityName }
}

@Composable
fun runStatusLabel(statusKey: String): String =
    runCatching { stringResource(RunStatus.valueOf(statusKey).labelRes) }
        .getOrDefault(statusKey)

@Composable
fun segmentStatusLabel(statusKey: String, successFallback: Boolean): String =
    runCatching { stringResource(SegmentWriteStatus.valueOf(statusKey).labelRes) }
        .getOrElse {
            stringResource(
                if (successFallback) R.string.segment_written else R.string.segment_failed,
            )
        }

fun IntensityLevel.label(context: Context): String = context.getString(labelRes)
