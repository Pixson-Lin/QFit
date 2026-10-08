package com.pixsonlin.qfit.data.db

import androidx.annotation.StringRes
import com.pixsonlin.qfit.R

enum class RunStatus(@StringRes val labelRes: Int) {
    RUNNING(R.string.status_running),
    COMPLETED(R.string.status_completed),
    CANCELLED(R.string.status_cancelled),
}
