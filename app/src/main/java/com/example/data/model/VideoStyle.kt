package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class VideoStyle(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val description: String,
    val promptModifier: String,
    val icon: ImageVector
) {
    REALISTIC(
        id = "realistic",
        titleEn = "Realistic",
        titleHi = "यथार्थवादी",
        description = "Photorealistic with natural physics & real lighting",
        promptModifier = "photorealistic 8k, realistic cinematography, natural lighting, 35mm lens, hyper-detailed textures, 60fps",
        icon = Icons.Default.CameraAlt
    ),
    CARTOON_3D(
        id = "cartoon_3d",
        titleEn = "3D Cartoon",
        titleHi = "3D कार्टून",
        description = "Pixar & Disney style 3D animation",
        promptModifier = "Pixar style 3D render, Disney animation aesthetic, Octane 3D render, expressive smooth motion, vibrant colors, 4k",
        icon = Icons.Default.Face
    ),
    ANIME(
        id = "anime",
        titleEn = "Anime",
        titleHi = "एनीमे",
        description = "Japanese anime aesthetic & hand-drawn charm",
        promptModifier = "Japanese anime movie style, Studio Ghibli and Makoto Shinkai aesthetic, vibrant anime keyframes, beautiful background art",
        icon = Icons.Default.Palette
    ),
    CINEMATIC(
        id = "cinematic",
        titleEn = "Cinematic",
        titleHi = "सिनेमैटिक",
        description = "Hollywood film look with dramatic moody lighting",
        promptModifier = "Hollywood cinematic shot, dramatic lighting, anamorphic lens flares, shallow depth of field, slow motion, IMAX grade",
        icon = Icons.Default.Movie
    ),
    FANTASY(
        id = "fantasy",
        titleEn = "Fantasy",
        titleHi = "काल्पनिक",
        description = "Magical ethereal wonderland with glowing particles",
        promptModifier = "magical mythical fantasy realm, ethereal glowing particles, enchanted atmosphere, mystical lighting, Unreal Engine 5 render",
        icon = Icons.Default.AutoAwesome
    ),
    KIDS_ANIMATION(
        id = "kids_animation",
        titleEn = "Kids Animation",
        titleHi = "बच्चों के लिए",
        description = "Fun, playful, bouncy colorful characters",
        promptModifier = "cute colorful kids animation, playful cheerful characters, bright pastel palette, bouncy fun motion, charming cartoon",
        icon = Icons.Default.Star
    );

    companion object {
        fun fromId(id: String): VideoStyle = entries.firstOrNull { it.id == id } ?: REALISTIC
    }
}
