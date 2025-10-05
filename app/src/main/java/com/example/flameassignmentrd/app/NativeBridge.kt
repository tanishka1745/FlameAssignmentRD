package com.example.flameassignmentrd.app



// Singleton class for initiate one time object

object NativeBridge {
    init {
        System.loadLibrary("c++_shared")    // C++ runtime
        System.loadLibrary("opencv_java4")  // OpenCV
        System.loadLibrary("native-lib")    // Your custom C++ code with processNV21
    }

    external fun processNV21(nv21: ByteArray, width: Int, height: Int): ByteArray
}