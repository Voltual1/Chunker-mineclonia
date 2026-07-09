#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <chrono>
#include <thread>
#include "MCMap.hpp"
#include "MTMap.hpp"
#include "threads.hpp"

static jobject g_callback_obj = nullptr;
static jmethodID g_on_progress_mid = nullptr;
static JavaVM* g_jvm = nullptr;

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

void report_progress(int64_t groups_done, int64_t total_groups, int64_t blocks_done) {
    if (g_jvm == nullptr || g_callback_obj == nullptr || g_on_progress_mid == nullptr) {
        return;
    }
    JNIEnv* env = nullptr;
    bool is_attached = false;
    jint res = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            is_attached = true;
        }
    }
    if (env != nullptr) {
        env->CallVoidMethod(g_callback_obj, g_on_progress_mid, 
                            (jlong)groups_done, (jlong)total_groups, (jlong)blocks_done);
        if (is_attached) {
            g_jvm->DetachCurrentThread();
        }
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_voltual_mc2mt_MC2MTLib_convertMap(JNIEnv *env, jobject thiz, jstring input_path, jstring output_path, jobject callback) {
    const char *in_path = env->GetStringUTFChars(input_path, nullptr);
    const char *out_path = env->GetStringUTFChars(output_path, nullptr);

    std::string input(in_path);
    std::string output(out_path);

    env->ReleaseStringUTFChars(input_path, in_path);
    env->ReleaseStringUTFChars(output_path, out_path);

    if (callback != nullptr) {
        g_callback_obj = env->NewGlobalRef(callback);
        jclass cb_class = env->GetObjectClass(g_callback_obj);
        g_on_progress_mid = env->GetMethodID(cb_class, "onProgress", "(JJJ)V");
    }

    groups_done = 0;
    blocks_done = 0;

    MCMap mc_map(input);
    MTMap mt_map(output);

    std::vector<MCGroup*> groups;
    mc_map.listGroups(groups);

    if (groups.empty()) {
        if (g_callback_obj != nullptr) {
            env->DeleteGlobalRef(g_callback_obj);
            g_callback_obj = nullptr;
        }
        return JNI_FALSE;
    }

    for (MCGroup* g : groups) {
        convert_queue.q.push(g);
    }

    init_threads(&mc_map, &mt_map);

    size_t total_groups = groups.size();
    while (groups_done < total_groups) {
        report_progress(groups_done, total_groups, blocks_done);
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }

    report_progress(groups_done, total_groups, blocks_done);

    deinit_threads();

    if (g_callback_obj != nullptr) {
        env->DeleteGlobalRef(g_callback_obj);
        g_callback_obj = nullptr;
        g_on_progress_mid = nullptr;
    }

    return JNI_TRUE;
}