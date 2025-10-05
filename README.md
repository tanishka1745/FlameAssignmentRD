# 🔥 Flame - Android + Web Frame Viewer

**Flame** is a cross-platform demo app that captures camera frames on **Android** using **NDK + OpenCV**, and visualizes them on a **TypeScript-based web viewer**. It demonstrates native image processing, JNI integration, and real-time frame streaming to a web interface.

---

## ✅ Features Implemented

### 📱 Android
- Real-time camera frame capture using **Camera2 API**
- Native frame processing via **OpenCV** (NDK integration)
- JNI bridge to transfer frames from C++ → Java layer
- Base64 encoding for frame transmission
- Frame rate (FPS) display overlay
- Error handling for camera/permission issues
- Toggle between raw and processed frames

### 🌐 Web (TypeScript)
- Renders Base64-encoded frames on an HTML5 canvas
- Displays **FPS** and frame dimensions
- Supports drag-and-drop to preview custom images
- Simulated frame stream (~15 FPS) for testing
- Lightweight UI built in TypeScript + HTML + CSS

---
### ScreenShot of implemented functionality.
<img width="1348" height="447" alt="Screenshot 2025-10-05 at 6 26 11 PM" src="https://github.com/user-attachments/assets/815223f1-2e8b-48fc-9fc0-1513a6f7bb05" />





## ⚙ Setup Instructions

### 🧩 Android Setup (NDK + OpenCV)

#### Requirements
- **Android Studio** (Arctic Fox or newer)
- **Android NDK 25.1+**
- **OpenCV Android SDK** (`OpenCV-android-sdk`)
- Gradle configured for native builds

  #### Clone & Build
  git clone https://github.com/tanishka1745/FlameAssignmentRD.git 
  cd FlameAssignmentRD

#### 🧠 Architecture Flow

Camera2 (Java)          --> Captures frames from device camera  
↓  
NDK Layer (C++ / JNI)  --> Receives NV21 frames, converts to OpenCV Mat  
↓  
OpenCV Processing       --> Applies filters (Canny, invert, etc.)  
↓  
Base64 Encoding (Java)  --> Converts processed frame to Base64  
↓  
WebSocket / Local Bridge--> Sends frame to web viewer (or simulates locally)  
↓  
TypeScript Canvas Renderer --> Draws frames, calculates FPS, handles drag-and-drop

#### Step 1: Install NDK
1. Open Android Studio → **SDK Manager** → **SDK Tools**
2. Check **NDK (Side by side)** → Apply  
3. Note the installation path (e.g., `/Users/username/Library/Android/sdk/ndk/25.2.9519653/`)

#### Step 2: Add OpenCV Android SDK
1. Download [OpenCV Android SDK](https://opencv.org/releases/)
2. Extract and place it in your project under:#### Step 3: Configure CMake for OpenCV
`CMakeLists.txt` example:
```cmake
cmake_minimum_required(VERSION 3.18.1)
project("flame")

# OpenCV include directory
set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/src/main/jniLibs/OpenCV-android-sdk/sdk/native/jni")
include_directories(${OpenCV_DIR}/include)

add_library(native-lib SHARED native-lib.cpp image_utils.cpp)

# Link OpenCV and Android log library
find_library(log-lib log)
target_link_libraries(native-lib ${log-lib} ${OpenCV_DIR}/libs/${ANDROID_ABI}/libopencv_java4.so)
