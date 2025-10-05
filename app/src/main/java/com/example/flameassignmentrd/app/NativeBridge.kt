package com.example.flameassignmentrd.app



// Singleton class for initiate one time object

object NativeBridge {
    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib") }
    external fun processNV21(nv21: ByteArray, width: Int, height: Int): ByteArray
}