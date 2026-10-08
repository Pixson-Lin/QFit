package com.pixsonlin.qfit.data.db

import androidx.annotation.StringRes
import com.pixsonlin.qfit.R

enum class SegmentWriteStatus(@StringRes val labelRes: Int) {
    PLANNED(R.string.segment_planned),
    WRITTEN(R.string.segment_written),
    FAILED(R.string.segment_failed),
    SKIPPED(R.string.segment_skipped),
}
