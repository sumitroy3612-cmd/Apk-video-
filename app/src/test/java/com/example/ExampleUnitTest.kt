package com.example

import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testVideoStylesAvailable() {
        val styles = VideoStyle.entries
        assertEquals(6, styles.size)
        assertNotNull(VideoStyle.fromId("cartoon_3d"))
        assertNotNull(VideoStyle.fromId("realistic"))
        assertNotNull(VideoStyle.fromId("anime"))
        assertNotNull(VideoStyle.fromId("cinematic"))
        assertNotNull(VideoStyle.fromId("fantasy"))
        assertNotNull(VideoStyle.fromId("kids_animation"))
    }

    @Test
    fun testAspectRatios() {
        val portrait = VideoAspectRatio.fromId("9:16")
        assertEquals(VideoAspectRatio.PORTRAIT_9_16, portrait)
        val landscape = VideoAspectRatio.fromId("16:9")
        assertEquals(VideoAspectRatio.LANDSCAPE_16_9, landscape)
        val square = VideoAspectRatio.fromId("1:1")
        assertEquals(VideoAspectRatio.SQUARE_1_1, square)
    }
}
