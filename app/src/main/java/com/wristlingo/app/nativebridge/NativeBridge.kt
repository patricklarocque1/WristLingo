package com.wristlingo.app.nativebridge

object NativeBridge {
    init {
        // Library name corresponds to add_library name in CMake (libwristlingo_native.so)
        System.loadLibrary("wristlingo_native")
    }

    external fun nativeVersion(): String
    external fun nativeNoop()
}


