#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "AegisKeys"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_aegis_sfe_security_SecureKeys_getRegistrationKey(JNIEnv *env, jobject /* this */) {
    // The registration key is stored as a C++ string
    // This makes it harder to find via simple string searches in the APK
    std::string registrationKey = "1s1hvhdqbvgq4s4u4om55gbfdf27tiqin4981quuk0571t1n8hf8";
    
    LOGI("Registration key accessed");
    
    return env->NewStringUTF(registrationKey.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aegis_sfe_security_SecureKeys_getClientId(JNIEnv *env, jobject /* this */) {
    // Also store the client ID securely
    std::string clientId = "MY_BANK_ANDROID";
    
    LOGI("Client ID accessed");
    
    return env->NewStringUTF(clientId.c_str());
}