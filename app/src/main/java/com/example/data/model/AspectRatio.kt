package com.example.data.model

enum class VideoAspectRatio(
    val id: String,
    val label: String,
    val subtitle: String,
    val width: Int,
    val height: Int,
    val ratioValue: Float
) {
    PORTRAIT_9_16(
        id = "9:16",
        label = "9:16",
        subtitle = "Shorts / Reels",
        width = 576,
        height = 1024,
        ratioValue = 9f / 16f
    ),
    LANDSCAPE_16_9(
        id = "16:9",
        label = "16:9",
        subtitle = "YouTube / Landscape",
        width = 1024,
        height = 576,
        ratioValue = 16f / 9f
    ),
    SQUARE_1_1(
        id = "1:1",
        label = "1:1",
        subtitle = "Square / Post",
        width = 720,
        height = 720,
        ratioValue = 1f
    );

    companion object {
        fun fromId(id: String): VideoAspectRatio =
            entries.firstOrNull { it.id == id } ?: PORTRAIT_9_16
    }
}
