package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VideoRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM generated_videos ORDER BY timestamp DESC")
    fun getAllVideos(): Flow<List<VideoRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoRecord)

    @Delete
    suspend fun deleteVideo(video: VideoRecord)

    @Query("DELETE FROM generated_videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)
}
