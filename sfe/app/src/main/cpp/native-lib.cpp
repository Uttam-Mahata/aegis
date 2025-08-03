#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "AegisKeys"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_aegis_sfe_security_SecureKeys_getRegistrationKey(JNIEnv *env, jobject /* this */) {
    // The registration key is stored as a C++ string
    // This makes it harder to find via simple string searches in the APK
    std::string registrationKey = "bt0l3q7e67osg856choik0rrplumu7a8deq4mos3q329tubqt7k";
    
    LOGI("Registration key accessed");
    
    return env->NewStringUTF(registrationKey.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_aegis_sfe_security_SecureKeys_getClientId(JNIEnv *env, jobject /* this */) {
    // Also store the client ID securely
    std::string clientId = "MY_FINTECH_PROD";
    
    LOGI("Client ID accessed");
    
    return env->NewStringUTF(clientId.c_str());
}