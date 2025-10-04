cmake_minimum_required(VERSION 3.10.2)
project("flameassignmentrd")

# Set C++ flags here (works for Kotlin DSL)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -frtti -fexceptions")

# OpenCV include path (your Mac path)
include_directories(/Users/tanishkasharma/Downloads/OpenCV-android-sdk/sdk/native/jni/include)

# Source files
add_library(native-lib SHARED
        native-lib.cpp
        opencv_processor.cpp
)

# Link libraries
find_library(log-lib log)
target_link_libraries(native-lib ${log-lib})