# Free AI Video Maker (मुक्त AI वीडियो निर्माता)

A lightweight, mobile-friendly Android application to generate AI videos from text prompts and uploaded photos. Built specifically for mobile creators creating YouTube Shorts, Instagram Reels, and TikTok clips.

---

## 🌟 Key Features

1. **Text to Video (टेक्स्ट से वीडियो)**
   - Enter any natural language prompt (e.g. *"Cute 3D cartoon baby dragon sneezing fire"*).
   - Generates animated MP4 video clips tailored for mobile view.

2. **Image to Video & Text + Image to Video (फोटो से वीडियो)**
   - Upload any photo using the zero-permission Android Photo Picker.
   - Animates the photo with cinematic camera movement, smooth zoom, and dynamic lighting particles.

3. **6 Curated Visual Styles (6 वीडियो शैलियां)**
   - **Realistic (यथार्थवादी):** Natural cinematography and realistic lighting.
   - **3D Cartoon (3D कार्टून):** Pixar and Disney style 3D character animation.
   - **Anime (एनीमे):** Studio Ghibli and Makoto Shinkai vibrant aesthetics.
   - **Cinematic (सिनेमैटिक):** Hollywood widescreen blockbuster atmosphere.
   - **Fantasy (काल्पनिक):** Mythical enchanted realms with ethereal glowing particles.
   - **Kids Animation (बच्चों के लिए):** Cheerful, bright, bouncy characters.

4. **Aspect Ratios for Social Media**
   - **9:16:** YouTube Shorts, Instagram Reels, TikTok.
   - **16:9:** YouTube Landscape, TV, and Desktop.
   - **1:1:** Instagram Feed and Square posts.

5. **Video Durations**
   - 5 seconds, 10 seconds, or 15 seconds.

6. **Full-Featured Preview & Download**
   - Built-in video player with Play/Pause, Seekbar slider, Replay, and Looping.
   - **Download Video:** Directly saves standard H.264 MP4 to your phone's Gallery / Movies folder (`Environment.DIRECTORY_MOVIES/FreeAIVideoMaker/`).
   - **One-Tap Share:** Share directly to WhatsApp, Instagram Reels, or YouTube.
   - **Local History:** Saves generated videos on-device using Room Database for quick replay without re-generating.

---

## 💡 Free Usage & Technical Transparency

### How It Works for Free:
- **No Hidden Charges / 100% Free:** Zero subscription or credit card needed.
- **Serverless Generative Pipeline:** Keyframe diffusion synthesis combines with the device's native hardware `MediaCodec` H.264 engine to encode full MP4 videos locally on the phone.
- **Hardware Requirements:** Works on standard Android smartphones (Android 7.0 / SDK 24 through Android 15+ / SDK 36) without needing a gaming PC or dedicated GPU.
- **Generation Time:** 10 to 30 seconds per video.
- **API Limits & Credits:** The app includes a daily 10-credit counter that can be refreshed anytime in the Info dialog.
- **Custom API Key Support (Optional):** Advanced users can connect their own free Hugging Face API token (`hf_...`) or Replicate token in the Settings dialog for dedicated cloud GPUs.

---

## 🛠 Tech Stack

- **Platform:** Native Android (Kotlin)
- **UI Toolkit:** Jetpack Compose with Material Design 3 (M3)
- **Video Engine:** Android native `MediaCodec` (H.264/AVC) + `MediaMuxer`
- **Networking:** OkHttp & Retrofit
- **Image Loading:** Coil Compose
- **Local Storage:** Android Room Database & SQLite
- **Permissions:** Zero broad storage permissions; uses Android Photo Picker & MediaStore API

---

## 🚀 How to Run & Build

### Requirements
- Android Studio Ladybug / Meerkat or later
- Android SDK 24+ (targetSdk 36)
- Java 17 or Java 21 JDK

### Build APK
Run the following Gradle command in terminal:
```bash
gradle assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install on Connected Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 Online Deployment & Distribution

1. **Google Play Store / Internal Testing:**
   - Run `gradle bundleRelease` to generate an Android App Bundle (`.aab`).
   - Upload the `.aab` to Google Play Console.
2. **Direct APK Release:**
   - Host the generated APK on GitHub Releases or a cloud storage bucket.
