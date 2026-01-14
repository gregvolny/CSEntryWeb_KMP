// Stub jni.h for WASM builds
// This provides minimal JNI type definitions for compilation purposes

#pragma once

#ifdef __EMSCRIPTEN__

// JNI type stubs for WASM compilation
typedef void* jobject;
typedef void* jstring;
typedef void* jclass;
typedef void* jthrowable;
typedef void* jweak;
typedef void* jarray;
typedef void* jbooleanArray;
typedef void* jbyteArray;
typedef void* jcharArray;
typedef void* jshortArray;
typedef void* jintArray;
typedef void* jlongArray;
typedef void* jfloatArray;
typedef void* jdoubleArray;
typedef void* jobjectArray;
typedef void* jfieldID;
typedef void* jmethodID;

typedef unsigned char jboolean;
typedef signed char jbyte;
typedef unsigned short jchar;
typedef short jshort;
typedef int jint;
typedef long long jlong;
typedef float jfloat;
typedef double jdouble;
typedef jint jsize;

// JNI environment stub
struct JNIEnv;
struct JavaVM;
typedef JNIEnv* JNIEnv_t;
typedef JavaVM* JavaVM_t;

#endif
