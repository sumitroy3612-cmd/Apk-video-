package com.example.data.generator

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object MediaCodecVideoEncoder {

    private const val TAG = "MediaCodecEncoder"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
    private const val I_FRAME_INTERVAL = 1 // 1 second between keyframes

    /**
     * Encodes a sequence of Bitmaps into an H.264 MP4 video file.
     *
     * @param outputFile destination .mp4 file
     * @param width width in pixels (must be even)
     * @param height height in pixels (must be even)
     * @param fps frames per second (e.g. 24 or 30)
     * @param totalFrames total number of frames to generate
     * @param frameRenderer callback receiving (frameIndex, totalFrames) and returning a rendered Bitmap
     * @param onProgress callback reporting progress percentage (0 to 100)
     */
    fun encodeFramesToMp4(
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        totalFrames: Int,
        frameRenderer: (frameIndex: Int, totalFrames: Int) -> Bitmap,
        onProgress: (Int) -> Unit
    ): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            // Ensure width and height are even
            val validWidth = (width / 2) * 2
            val validHeight = (height / 2) * 2

            val bitRate = (validWidth * validHeight * fps * 0.15).toInt().coerceIn(1_200_000, 4_000_000)

            val format = MediaFormat.createVideoFormat(MIME_TYPE, validWidth, validHeight).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                )
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val yuvBuffer = ByteArray(validWidth * validHeight * 3 / 2)
            val argbPixels = IntArray(validWidth * validHeight)
            val frameDurationUs = 1_000_000L / fps

            var frameIndex = 0
            var isInputDone = false

            while (true) {
                // Feed input frames
                if (!isInputDone) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer: ByteBuffer? = codec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()

                        if (frameIndex < totalFrames) {
                            val bitmap = frameRenderer(frameIndex, totalFrames)
                            val scaledBitmap = if (bitmap.width != validWidth || bitmap.height != validHeight) {
                                Bitmap.createScaledBitmap(bitmap, validWidth, validHeight, true)
                            } else {
                                bitmap
                            }

                            scaledBitmap.getPixels(argbPixels, 0, validWidth, 0, 0, validWidth, validHeight)
                            convertArgbToYuv420sp(argbPixels, yuvBuffer, validWidth, validHeight)

                            inputBuffer?.put(yuvBuffer)
                            val presentationTimeUs = frameIndex * frameDurationUs
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                yuvBuffer.size,
                                presentationTimeUs,
                                0
                            )

                            frameIndex++
                            val progress = (frameIndex * 100) / totalFrames
                            onProgress(progress.coerceIn(0, 95))
                        } else {
                            // Signal End of Stream
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                frameIndex * frameDurationUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputDone = true
                        }
                    }
                }

                // Drain output buffers
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        Log.e(TAG, "Format changed after muxer started")
                    } else {
                        val newFormat = codec.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break // Encoding finished!
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER && isInputDone) {
                    // Give codec a few iterations to flush
                    Thread.sleep(10)
                }
            }

            onProgress(100)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding frames to MP4", e)
            return false
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Converts an ARGB_8888 pixel array to YUV420SemiPlanar (NV12).
     */
    private fun convertArgbToYuv420sp(argb: IntArray, yuv420sp: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var pixelIndex = 0

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[pixelIndex++]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff

                // ITU-R BT.601 conversion
                var y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                if (y < 16) y = 16
                if (y > 235) y = 235
                yuv420sp[yIndex++] = y.toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    var u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    var v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                    if (u < 16) u = 16
                    if (u > 240) u = 240
                    if (v < 16) v = 16
                    if (v > 240) v = 240

                    yuv420sp[uvIndex++] = u.toByte()
                    yuv420sp[uvIndex++] = v.toByte()
                }
            }
        }
    }
}
