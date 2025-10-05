cmake_minimum_required(VERSION 3.10.2)
project("FlameAssignmentRD")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -lc++_shared")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17")
set(ANDROID_STL c++_shared)

# Set your source files
add_library(
        native-lib
        SHARED
        native-lib.cpp
        image_utils.cpp
)

# Include OpenCV headers
include_directories(
        /Users/tanishkasharma/Downloads/OpenCV-android-sdk/sdk/native/jni/include
)

# Find Android log library
find_library(log-lib log)

# Link native-lib with OpenCV and log
target_link_libraries(
        native-lib
        ${log-lib}
        c++_shared
        /Users/tanishkasharma/Downloads/OpenCV-android-sdk/sdk/native/libs/${ANDROID_ABI}/libopencv_java4.so
)

