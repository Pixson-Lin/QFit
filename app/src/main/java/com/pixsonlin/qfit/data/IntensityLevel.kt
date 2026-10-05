package com.pixsonlin.qfit.data

enum class IntensityLevel(
    val displayName: String,
    val cadenceSpm: Int,
    val strideMeters: Double,
) {
    STROLL("散步", 80, 0.60),
    SUPER_SLOW_JOG("超慢跑", 140, 0.67),
    JOG("慢跑", 165, 0.70),
    MARATHON("馬拉松", 180, 0.78),
    SPRINT("衝刺", 210, 1.00),
    ;

    fun menuLabel(): String = "$displayName(${cadenceSpm}步/分)"
}
