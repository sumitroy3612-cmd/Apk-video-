package com.example.data.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.GeneratedVideo
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

sealed class GenerationProgress {
    object Idle : GenerationProgress()
    data class Progress(val stepEn: String, val stepHi: String, val percentage: Int) : GenerationProgress()
    data class Success(val video: GeneratedVideo) : GenerationProgress()
    data class Error(val messageEn: String, val messageHi: String) : GenerationProgress()
}

class AiVideoEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Generates an MP4 video from text and/or an optional uploaded image.
     */
    suspend fun generateVideo(
        prompt: String,
        style: VideoStyle,
        durationSeconds: Int,
        aspectRatio: VideoAspectRatio,
        uploadedImageUri: Uri?,
        customApiKey: String? = null,
        onProgress: (GenerationProgress) -> Unit
    ): GeneratedVideo? = withContext(Dispatchers.IO) {
        val videoId = UUID.randomUUID().toString()
        val videoDir = File(context.filesDir, "videos").apply { if (!exists()) mkdirs() }
        val outputFile = File(videoDir, "video_$videoId.mp4")

        try {
            onProgress(
                GenerationProgress.Progress(
                    stepEn = "Analyzing prompt & cinematic style...",
                    stepHi = "प्रॉम्प्ट और वीडियो शैली का विश्लेषण हो रहा है...",
                    percentage = 10
                )
            )

            val fullEnhancedPrompt = buildEnhancedPrompt(prompt, style, uploadedImageUri != null)

            // Determine dimensions for encoding (multiples of 16 for optimal codec compatibility)
            val (encWidth, encHeight) = when (aspectRatio) {
                VideoAspectRatio.PORTRAIT_9_16 -> Pair(480, 854)
                VideoAspectRatio.LANDSCAPE_16_9 -> Pair(854, 480)
                VideoAspectRatio.SQUARE_1_1 -> Pair(640, 640)
            }

            onProgress(
                GenerationProgress.Progress(
                    stepEn = "Synthesizing AI visual keyframes...",
                    stepHi = "AI विज़ुअल की-फ्रेम तैयार हो रहे हैं...",
                    percentage = 30
                )
            )

            // Step 1: Obtain the base visual bitmap (from uploaded photo or AI cloud synthesis)
            val baseBitmap: Bitmap = if (uploadedImageUri != null) {
                // Image to Video mode: use uploaded image
                val loaded = ImageProcessor.loadScaledBitmap(context, uploadedImageUri, encWidth, encHeight)
                loaded ?: createProceduralFallbackBitmap(prompt, style, encWidth, encHeight)
            } else {
                // Text to Video mode: fetch from AI video visual synthesis
                fetchAiImageKeyframe(fullEnhancedPrompt, encWidth, encHeight)
                    ?: createProceduralFallbackBitmap(prompt, style, encWidth, encHeight)
            }

            onProgress(
                GenerationProgress.Progress(
                    stepEn = "Rendering motion dynamics & camera pan...",
                    stepHi = "कैमरा मोशन और एनीमेशन फ्रेम बन रहे हैं...",
                    percentage = 55
                )
            )

            // Step 2: Encode multi-frame video with camera movement & style-specific visual dynamics
            val fps = 24
            val totalFrames = durationSeconds * fps

            // Cache particle coordinates for stylistic animation
            val particles = List(40) {
                Particle(
                    x = (Math.random() * encWidth).toFloat(),
                    y = (Math.random() * encHeight).toFloat(),
                    size = (Math.random() * 6 + 2).toFloat(),
                    speed = (Math.random() * 2 + 1).toFloat(),
                    phase = (Math.random() * Math.PI * 2).toFloat()
                )
            }

            val success = MediaCodecVideoEncoder.encodeFramesToMp4(
                outputFile = outputFile,
                width = encWidth,
                height = encHeight,
                fps = fps,
                totalFrames = totalFrames,
                frameRenderer = { frameIndex, total ->
                    renderAnimatedFrame(
                        baseBitmap = baseBitmap,
                        frameIndex = frameIndex,
                        totalFrames = total,
                        width = encWidth,
                        height = encHeight,
                        style = style,
                        particles = particles
                    )
                },
                onProgress = { encPercent ->
                    val totalPercent = 55 + (encPercent * 0.40).toInt()
                    onProgress(
                        GenerationProgress.Progress(
                            stepEn = "Encoding H.264 MP4 video ($encPercent%)...",
                            stepHi = "H.264 MP4 वीडियो एन्कोड हो रहा है ($encPercent%)...",
                            percentage = totalPercent.coerceIn(55, 96)
                        )
                    )
                }
            )

            if (!success || !outputFile.exists() || outputFile.length() == 0L) {
                throw IllegalStateException("Failed to encode MP4 video stream.")
            }

            onProgress(
                GenerationProgress.Progress(
                    stepEn = "Finalizing video...",
                    stepHi = "वीडियो को अंतिम रूप दिया जा रहा है...",
                    percentage = 100
                )
            )

            val generatedVideo = GeneratedVideo(
                id = videoId,
                prompt = prompt,
                enhancedPrompt = fullEnhancedPrompt,
                style = style,
                durationSeconds = durationSeconds,
                aspectRatio = aspectRatio,
                videoFilePath = outputFile.absolutePath,
                originalImageUri = uploadedImageUri?.toString(),
                timestamp = System.currentTimeMillis(),
                sourceModel = if (uploadedImageUri != null) "AI Image-To-Video Engine" else "AI Text-To-Video Engine"
            )

            onProgress(GenerationProgress.Success(generatedVideo))
            generatedVideo

        } catch (e: Exception) {
            Log.e("AiVideoEngine", "Generation failed", e)
            val errorEn = e.localizedMessage ?: "Unknown video generation error"
            val errorHi = "वीडियो निर्माण में त्रुटि आई: कृपया पुनः प्रयास करें"
            onProgress(GenerationProgress.Error(errorEn, errorHi))
            null
        }
    }

    /**
     * Renders an individual animated frame with cinematic zoom, camera pan, and stylistic lighting particles
     */
    private fun renderAnimatedFrame(
        baseBitmap: Bitmap,
        frameIndex: Int,
        totalFrames: Int,
        width: Int,
        height: Int,
        style: VideoStyle,
        particles: List<Particle>
    ): Bitmap {
        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frameBitmap)

        val progress = frameIndex.toFloat() / totalFrames.toFloat()

        // Smooth sinusoidal easing
        val ease = (1f - cos(progress * Math.PI.toFloat())) / 2f

        // Camera Dynamics: Subtle slow zoom (1.00 -> 1.15) and horizontal drift
        val zoom = 1.0f + (0.15f * ease)
        val panX = sin(progress * Math.PI.toFloat()) * (width * 0.05f)
        val panY = cos(progress * Math.PI.toFloat()) * (height * 0.03f)

        val matrix = Matrix().apply {
            postScale(zoom, zoom, width / 2f, height / 2f)
            postTranslate(panX, panY)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(baseBitmap, matrix, paint)

        // Add style-specific atmospheric lighting and motion dynamics
        drawStyleAtmosphere(canvas, width, height, progress, style, particles)

        return frameBitmap
    }

    /**
     * Overlays stylistic atmosphere matching the chosen aesthetic
     */
    private fun drawStyleAtmosphere(
        canvas: Canvas,
        width: Int,
        height: Int,
        progress: Float,
        style: VideoStyle,
        particles: List<Particle>
    ) {
        val atmospherePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (style) {
            VideoStyle.CINEMATIC -> {
                // Dramatic anamorphic lighting sweep and subtle vignette
                val flareX = width * (0.2f + 0.6f * progress)
                val flareY = height * 0.35f
                val radialGradient = RadialGradient(
                    flareX, flareY, width * 0.45f,
                    intArrayOf(Color.argb(55, 255, 220, 150), Color.argb(15, 200, 180, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                atmospherePaint.shader = radialGradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), atmospherePaint)

                // Cinematic letterbox vignette
                atmospherePaint.shader = null
                atmospherePaint.color = Color.argb(40, 0, 0, 0)
                canvas.drawRect(0f, 0f, width.toFloat(), height * 0.04f, atmospherePaint)
                canvas.drawRect(0f, height * 0.96f, width.toFloat(), height.toFloat(), atmospherePaint)
            }
            VideoStyle.FANTASY -> {
                // Glowing ethereal sparkles floating upward
                atmospherePaint.style = Paint.Style.FILL
                for (p in particles) {
                    val currentY = (p.y - progress * height * p.speed + height * 2) % height
                    val currentX = p.x + sin(progress * 4f + p.phase) * 15f
                    val alpha = ((sin(progress * 6f + p.phase) + 1f) * 0.5f * 180).toInt().coerceIn(30, 240)
                    atmospherePaint.color = Color.argb(alpha, 216, 180, 254) // Ethereal lilac glow
                    canvas.drawCircle(currentX, currentY, p.size, atmospherePaint)
                }
            }
            VideoStyle.CARTOON_3D, VideoStyle.KIDS_ANIMATION -> {
                // Bright warm sunlight vignette and bouncy sparkle accents
                val sunGlow = RadialGradient(
                    width * 0.85f, height * 0.15f, width * 0.5f,
                    intArrayOf(Color.argb(45, 255, 245, 180), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                atmospherePaint.shader = sunGlow
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), atmospherePaint)
            }
            VideoStyle.ANIME -> {
                // Anime speed lines / light rays
                atmospherePaint.shader = null
                atmospherePaint.color = Color.argb(25, 255, 255, 255)
                atmospherePaint.strokeWidth = 2f
                for (i in 0 until 5) {
                    val rayOffset = ((progress * 300f + i * 80) % width)
                    canvas.drawLine(rayOffset, 0f, rayOffset + 40f, height.toFloat(), atmospherePaint)
                }
            }
            VideoStyle.REALISTIC -> {
                // Subtle natural film exposure pulse
                val exposureAlpha = (sin(progress * Math.PI.toFloat() * 2f) * 10 + 10).toInt()
                atmospherePaint.color = Color.argb(exposureAlpha, 255, 250, 240)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), atmospherePaint)
            }
        }
    }

    /**
     * Downloads an AI visual keyframe from high-speed generative endpoints
     */
    private fun fetchAiImageKeyframe(fullPrompt: String, width: Int, height: Int): Bitmap? {
        val seed = (Math.random() * 999999).toInt()
        val encoded = URLEncoder.encode(fullPrompt, "UTF-8")
        val url = "https://image.pollinations.ai/prompt/$encoded?width=$width&height=$height&seed=$seed&nologo=true"

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FreeAIVideoMaker/1.0 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else null
            }
        } catch (e: Exception) {
            Log.w("AiVideoEngine", "Cloud visual keyframe fetch timed out or failed: ${e.message}")
            null
        }
    }

    /**
     * Generates a procedural artwork if offline or external API is unresponsive
     */
    private fun createProceduralFallbackBitmap(prompt: String, style: VideoStyle, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = when (style) {
                VideoStyle.CINEMATIC -> LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(15, 23, 42), Color.rgb(30, 41, 59), Color.rgb(2, 6, 23)),
                    null, Shader.TileMode.CLAMP
                )
                VideoStyle.CARTOON_3D, VideoStyle.KIDS_ANIMATION -> LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(99, 102, 241), Color.rgb(236, 72, 153), Color.rgb(245, 158, 11)),
                    null, Shader.TileMode.CLAMP
                )
                VideoStyle.ANIME -> LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(79, 70, 229), Color.rgb(192, 132, 252), Color.rgb(253, 164, 175)),
                    null, Shader.TileMode.CLAMP
                )
                VideoStyle.FANTASY -> LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(17, 24, 39), Color.rgb(88, 28, 135), Color.rgb(13, 148, 136)),
                    null, Shader.TileMode.CLAMP
                )
                VideoStyle.REALISTIC -> LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(30, 41, 59), Color.rgb(51, 65, 85), Color.rgb(15, 23, 42)),
                    null, Shader.TileMode.CLAMP
                )
            }
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw decorative art or prompt preview
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(style.titleEn + " AI Video", width / 2f, height * 0.45f, textPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val cleanPrompt = if (prompt.length > 35) prompt.take(32) + "..." else prompt
        canvas.drawText("\"$cleanPrompt\"", width / 2f, height * 0.52f, subPaint)

        return bitmap
    }

    private fun buildEnhancedPrompt(prompt: String, style: VideoStyle, isImageToVideo: Boolean): String {
        return if (isImageToVideo) {
            "$prompt, animated photo with smooth camera dynamics, ${style.promptModifier}, continuous fluid motion"
        } else {
            "$prompt, ${style.promptModifier}, cinematic motion, masterpiece composition"
        }
    }

    /**
     * Saves the generated video into the user's public Movies / Downloads folder
     * so it shows up in phone Gallery, WhatsApp, Instagram, etc.
     */
    fun exportVideoToPublicGallery(videoFile: File): Uri? {
        val displayName = "AIVideo_${System.currentTimeMillis()}.mp4"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FreeAIVideoMaker")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        videoFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                uri
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(publicDir, displayName)
                videoFile.copyTo(targetFile, overwrite = true)
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            Log.e("AiVideoEngine", "Export to gallery failed", e)
            null
        }
    }

    private data class Particle(
        val x: Float,
        val y: Float,
        val size: Float,
        val speed: Float,
        val phase: Float
    )
}
