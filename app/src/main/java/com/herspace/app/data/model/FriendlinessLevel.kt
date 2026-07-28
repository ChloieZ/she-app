package com.herspace.app.data.model

/**
 * 女性友好度等级
 * 友好票占比 ≥ 80% → 绿色
 * 友好票占比 50%-79% → 黄色
 * 友好票占比 < 50% → 红色
 * 无人投票 → 灰色（待探索）
 */
enum class FriendlinessLevel(val label: String, val colorHex: Long) {
    FRIENDLY("友好", 0xFF4CAF50),
    NEUTRAL("一般", 0xFFFFC107),
    UNFRIENDLY("不友好", 0xFFF44336),
    UNKNOWN("待探索", 0xFF9E9E9E);

    companion object {
        fun fromRatio(friendlyRatio: Float): FriendlinessLevel {
            return when {
                friendlyRatio >= 0.80f -> FRIENDLY
                friendlyRatio >= 0.50f -> NEUTRAL
                else -> UNFRIENDLY
            }
        }
    }
}
