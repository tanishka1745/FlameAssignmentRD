package com.example.flameassignmentrd.app

object NativeBridge {
    init { System.loadLibrary("native-lib") }
    external fun processNV21(nv21: ByteArray, width: Int, height: Int): ByteArray
}