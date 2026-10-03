package com.pixsonlin.qfit.data.db

enum class SegmentWriteStatus(val displayName: String) {
    PLANNED("待寫入"),
    WRITTEN("成功"),
    FAILED("失敗"),
    SKIPPED("略過"),
}
