package com.android.purebilibili.navigation3.predictiveback

internal enum class BiliPaiPredictiveBackAnimationStyle(val storageValue: String) {
    NONE("none"),
    AOSP("aosp"),
    MIUIX("miuix"),
    SCALE("scale"),
    CLASSIC("bilipai_classic");

    companion object {
        fun fromStorageValue(value: String?): BiliPaiPredictiveBackAnimationStyle {
            return when (value) {
                "default" -> MIUIX
                "classic" -> CLASSIC
                else -> entries.find { it.storageValue == value } ?: MIUIX
            }
        }

        fun resolveAdaptiveDefault(
            uiStyle: com.android.purebilibili.core.theme.AppUiStyle,
            value: String?,
        ): BiliPaiPredictiveBackAnimationStyle {
            if (value.isNullOrBlank() || value == "default" || value == "miuix") {
                return when (uiStyle) {
                    com.android.purebilibili.core.theme.AppUiStyle.MATERIAL3 -> AOSP
                    com.android.purebilibili.core.theme.AppUiStyle.MIUIX -> MIUIX
                }
            }
            return fromStorageValue(value)
        }
    }
}
