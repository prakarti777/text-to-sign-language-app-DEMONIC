# 🤟 Text-to-Sign-Language App (DEMONIC)

An Android application built with **Jetpack Compose** that translates spoken voice and text input into **Indian Sign Language (ISL)** video gestures and animated interpretations in real time — with **100% offline speech recognition** support.

---

## 🌟 Key Features

- **🎙️ Offline Speech-to-Text**: Powered by the [Vosk Offline Speech Recognition](https://alphacephei.com/vosk/) engine (`vosk-android`), allowing seamless voice translation without requiring an internet connection.
- **🧠 Indian Sign Language (ISL) Grammar Engine**: Translates natural English sentences into ISL grammar structures using custom tokenization and syntactic rule mapping (`IslGrammarEngine`).
- **📹 Seamless Gesture Playback**: Uses **AndroidX Media3 (ExoPlayer)** to smoothly sequence and render sign video gestures (`assets/gestures/`) based on detected phrases and words.
- **🎨 Animated Fallback & 3D Gestures**: Supports alternate animated gestures (`anime/`) for visual variety and accessibility.
- **📊 Pose & Hand Landmark Tracking**: Includes MediaPipe holistic, pose, and hand tracking models (`.task`) and skeletal JSON landmark data (`landmarks/`).
- **📱 Modern Jetpack Compose UI**: Clean, responsive Material 3 user interface with dark mode support, real-time transcription status, and smooth transitions.

---

## 🛠️ Tech Stack & Libraries

| Category | Technology |
|---|---|
| **Language** | Kotlin (JDK 17) |
| **Minimum SDK** | Android 7.0 (API Level 24) |
| **Target SDK** | Android 15 (API Level 36) |
| **UI Toolkit** | Jetpack Compose + Material 3 |
| **Navigation** | AndroidX Navigation 3 |
| **Audio Recognition** | Vosk Android SDK (`0.3.75`) + `vosk-model-en` |
| **Video Playback** | AndroidX Media3 ExoPlayer (`1.3.1`) |
| **Landmark Tracking** | Google MediaPipe Vision Models |
| **Data Serialization** | Kotlinx Serialization & Google Gson |

---

## 📂 Project Architecture

```text
demonic/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── gestures/         # 100+ Sign language gesture MP4 video files
│   │   │   ├── vosk-model-en/    # Offline acoustic & language speech models
│   │   │   ├── hand_landmarker.task
│   │   │   └── pose_landmarker.task
│   │   ├── java/com/example/demonic/
│   │   │   ├── data/             # Gesture entities and repository
│   │   │   ├── domain/           # ISL grammar engine & phrase matching logic
│   │   │   ├── speech/           # Vosk offline microphone audio manager
│   │   │   ├── theme/            # Compose typography, colors, and theme
│   │   │   └── ui/main/          # MainScreen Composable and ViewModel
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── anime/                        # Animated gesture demonstration videos
├── data/                         # Sign language video library
├── landmarks/                    # Precomputed skeletal landmark coordinates (JSON)
├── scratch/models/               # MediaPipe holistic landmarker models
├── build.gradle.kts              # Root build script
├── settings.gradle.kts           # Module definitions
└── gradle.properties
