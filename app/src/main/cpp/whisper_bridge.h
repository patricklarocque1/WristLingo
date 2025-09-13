#pragma once

#include <jni.h>
#include <string>

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_create(
        JNIEnv* env,
        jobject thiz,
        jstring modelPath,
        jint sampleRate);

JNIEXPORT void JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_feedPcm(
        JNIEnv* env,
        jobject thiz,
        jlong ctxPtr,
        jshortArray pcm,
        jint length);

JNIEXPORT jstring JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_finalizeStream(
        JNIEnv* env,
        jobject thiz,
        jlong ctxPtr);

JNIEXPORT void JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_destroy(
        JNIEnv* env,
        jobject thiz,
        jlong ctxPtr);

JNIEXPORT jstring JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_nativePartial(
        JNIEnv* env,
        jobject thiz,
        jlong ctxPtr,
        jint windowMs);

}


