#include <jni.h>
#include <string>

// Returns a simple version string to verify native build and linkage
extern "C" JNIEXPORT jstring JNICALL
Java_com_wristlingo_app_nativebridge_NativeBridge_nativeVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "WristLingo-Native 0.1.0";
    return env->NewStringUTF(version.c_str());
}

// No-op JNI function to prove linkage; does nothing
extern "C" JNIEXPORT void JNICALL
Java_com_wristlingo_app_nativebridge_NativeBridge_nativeNoop(
        JNIEnv* /* env */, jobject /* this */) {
}


