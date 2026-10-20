package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.generator.AiVideoEngine
import com.example.data.generator.GenerationProgress
import com.example.data.local.AppDatabase
import com.example.data.model.GeneratedVideo
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoRecord
import com.example.data.model.VideoStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

data class VideoMakerUiState(
    val prompt: String = "",
    val selectedImageUri: Uri? = null,
    val selectedStyle: VideoStyle = VideoStyle.CARTOON_3D,
    val selectedDurationSeconds: Int = 5,
    val selectedAspectRatio: VideoAspectRatio = VideoAspectRatio.PORTRAIT_9_16,
    val generationProgress: GenerationProgress = GenerationProgress.Idle,
    val currentVideo: GeneratedVideo? = null,
    val history: List<VideoRecord> = emptyList(),
    val remainingFreeCredits: Int = 10,
    val showInfoDialog: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val customApiKey: String = "",
    val isDownloading: Boolean = false
)

class VideoMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val videoDao = database.videoDao()
    private val videoEngine = AiVideoEngine(context)

    private val prefs = context.getSharedPreferences("ai_video_maker_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        VideoMakerUiState(
            remainingFreeCredits = prefs.getInt("remaining_credits", 10),
            customApiKey = prefs.getString("custom_api_key", "") ?: ""
        )
    )
    val uiState: StateFlow<VideoMakerUiState> = _uiState.asStateFlow()

    init {
        // Observe history records from Room database
        viewModelScope.launch {
            videoDao.getAllVideos().collectLatest { list ->
                _uiState.value = _uiState.value.copy(history = list)
            }
        }
    }

    fun onPromptChanged(newPrompt: String) {
        _uiState.value = _uiState.value.copy(prompt = newPrompt)
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun onStyleSelected(style: VideoStyle) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun onDurationSelected(duration: Int) {
        _uiState.value = _uiState.value.copy(selectedDurationSeconds = duration)
    }

    fun onAspectRatioSelected(aspectRatio: VideoAspectRatio) {
        _uiState.value = _uiState.value.copy(selectedAspectRatio = aspectRatio)
    }

    fun useSamplePrompt(sample: String) {
        _uiState.value = _uiState.value.copy(prompt = sample)
    }

    fun generateVideo() {
        val currentState = _uiState.value
        val promptText = currentState.prompt.trim()

        if (promptText.isEmpty() && currentState.selectedImageUri == null) {
            Toast.makeText(
                context,
                "Please describe your video or upload an image / कृपया वीडियो का विवरण लिखें या फोटो जोड़ें",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        viewModelScope.launch {
            val generated = videoEngine.generateVideo(
                prompt = if (promptText.isNotEmpty()) promptText else "A beautiful living scene",
                style = currentState.selectedStyle,
                durationSeconds = currentState.selectedDurationSeconds,
                aspectRatio = currentState.selectedAspectRatio,
                uploadedImageUri = currentState.selectedImageUri,
                customApiKey = currentState.customApiKey.ifEmpty { null },
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(generationProgress = progress)
                }
            )

            if (generated != null) {
                // Save record into Room database
                val record = VideoRecord(
                    id = generated.id,
                    prompt = generated.prompt,
                    enhancedPrompt = generated.enhancedPrompt,
                    styleId = generated.style.id,
                    durationSeconds = generated.durationSeconds,
                    aspectRatioId = generated.aspectRatio.id,
                    videoFilePath = generated.videoFilePath,
                    originalImageUri = generated.originalImageUri,
                    timestamp = generated.timestamp,
                    sourceModel = generated.sourceModel
                )
                videoDao.insertVideo(record)

                // Update remaining free credits
                val newCredits = (_uiState.value.remainingFreeCredits - 1).coerceAtLeast(0)
                prefs.edit().putInt("remaining_credits", newCredits).apply()

                _uiState.value = _uiState.value.copy(
                    currentVideo = generated,
                    remainingFreeCredits = newCredits
                )
            }
        }
    }

    fun downloadCurrentVideo() {
        val video = _uiState.value.currentVideo ?: return
        val videoFile = File(video.videoFilePath)
        if (!videoFile.exists()) {
            Toast.makeText(context, "Video file not found", Toast.LENGTH_SHORT).show()
            return
        }

        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch {
            val exportedUri = videoEngine.exportVideoToPublicGallery(videoFile)
            _uiState.value = _uiState.value.copy(isDownloading = false)

            if (exportedUri != null) {
                Toast.makeText(
                    context,
                    "Video saved to Movies & Gallery! / वीडियो गैलरी में सेव हो गया!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Failed to save video to Gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playFromHistory(record: VideoRecord) {
        val generated = GeneratedVideo(
            id = record.id,
            prompt = record.prompt,
            enhancedPrompt = record.enhancedPrompt,
            style = VideoStyle.fromId(record.styleId),
            durationSeconds = record.durationSeconds,
            aspectRatio = VideoAspectRatio.fromId(record.aspectRatioId),
            videoFilePath = record.videoFilePath,
            originalImageUri = record.originalImageUri,
            timestamp = record.timestamp,
            sourceModel = record.sourceModel
        )
        _uiState.value = _uiState.value.copy(
            currentVideo = generated,
            showHistorySheet = false
        )
    }

    fun deleteHistory(record: VideoRecord) {
        viewModelScope.launch {
            videoDao.deleteVideo(record)
            val file = File(record.videoFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun toggleInfoDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInfoDialog = show)
    }

    fun toggleHistorySheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showHistorySheet = show)
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = show)
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key.trim()).apply()
        _uiState.value = _uiState.value.copy(
            customApiKey = key.trim(),
            showSettingsDialog = false
        )
        Toast.makeText(context, "API Key updated", Toast.LENGTH_SHORT).show()
    }

    fun resetDailyCredits() {
        prefs.edit().putInt("remaining_credits", 10).apply()
        _uiState.value = _uiState.value.copy(remainingFreeCredits = 10)
        Toast.makeText(context, "Free credits refreshed to 10!", Toast.LENGTH_SHORT).show()
    }
}
