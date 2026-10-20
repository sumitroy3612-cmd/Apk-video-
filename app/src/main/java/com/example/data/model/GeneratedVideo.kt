package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_videos")
data class VideoRecord(
    @PrimaryKey val id: String,
    val prompt: String,
    val enhancedPrompt: String,
    val styleId: String,
    val durationSeconds: Int,
    val aspectRatioId: String,
    val videoFilePath: String,
    val originalImageUri: String?,
    val timestamp: Long,
    val sourceModel: String
)

data class GeneratedVideo(
    val id: String,
    val prompt: String,
    val enhancedPrompt: String,
    val style: VideoStyle,
    val durationSeconds: Int,
    val aspectRatio: VideoAspectRatio,
    val videoFilePath: String,
    val originalImageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceModel: String = "Free AI Engine"
)
