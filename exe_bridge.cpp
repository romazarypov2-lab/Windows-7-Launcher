#include <jni.h>
#include <string>
#include <unistd.h>
#include <android/log.h>

#define LOG_TAG "EXE_LAUNCHER_CORE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_winexelauncher_app_ExeEngine_runExeFile(
        JNIEnv* env,
        jobject /* this */,
        jstring exePath,
        jstring winePrefixPath) {

    const char* nativeExePath = env->GetStringUTFChars(exePath, nullptr);
    const char* nativeWinePrefix = env->GetStringUTFChars(winePrefixPath, nullptr);

    LOGI("Инициализация окружения: %s", nativeWinePrefix);
    LOGI("Запуск бинарного файла: %s", nativeExePath);

    setenv("WINEPREFIX", nativeWinePrefix, 1);
    setenv("WINEDEBUG", "-all", 1);
    setenv("DXVK_HUD", "fps", 1);

    pid_t pid = fork();
    if (pid == 0) {
        char* args = {
            (char*)"wine", 
            (char*)"start", 
            (char*)"/unix", 
            (char*)nativeExePath, 
            nullptr
        };
        
        execvp("wine", args);
        return -1;
    }

    env->ReleaseStringUTFChars(exePath, nativeExePath);
    env->ReleaseStringUTFChars(winePrefixPath, nativeWinePrefix);

    return pid;
}
