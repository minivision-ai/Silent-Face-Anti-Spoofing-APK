//
// Created by yuanhao on 20-6-12.
//

#include <android/asset_manager_jni.h>
#include "jni_long_field.h"
#include "live/live.h"
#include "android_log.h"
#include "img_process.h"

JniLongField live_field("nativeHandler");

Live* get_live(JNIEnv* env, jobject instance) {
    Live* const live = reinterpret_cast<Live*>(live_field.get(env, instance));
    return live;
}

void set_live(JNIEnv* env, jobject instance, Live* live) {
    live_field.set(env, instance, reinterpret_cast<intptr_t>(live));
}


extern "C" {

JNIEXPORT jlong JNICALL
LIVE_METHOD(allocate)(JNIEnv *env, jobject instance);

JNIEXPORT void JNICALL
LIVE_METHOD(deallocate)(JNIEnv *env, jobject instance);


JNIEXPORT jint JNICALL
LIVE_METHOD(nativeLoadModel)(JNIEnv *env, jobject instance, jobject asset_manager,
                             jobject configs);

JNIEXPORT jfloat JNICALL
LIVE_METHOD(nativeDetectYuv)(JNIEnv *env, jobject instance, jbyteArray yuv, jint preview_width,
                             jint preview_height, jint orientation, jint left, jint top, jint right, jint bottom);

}


void ConvertAndroidConfig2NativeConfig(JNIEnv *env,jobject model_configs, std::vector<ModelConfig>& modelConfigs) {
    modelConfigs.clear();

    jclass list_clz = env->GetObjectClass(model_configs);
    jmethodID list_size = env->GetMethodID(list_clz, "size", "()I");
    jmethodID list_get = env->GetMethodID(list_clz, "get", "(I)Ljava/lang/Object;");

    env->DeleteLocalRef(list_clz);

    int len = env->CallIntMethod(model_configs, list_size);
    for(int i = 0; i < len; i++) {
        jobject config = env->CallObjectMethod(model_configs, list_get, i);
        jclass config_clz = env->GetObjectClass(config);
        jfieldID config_name         = env->GetFieldID(config_clz, "name" ,"Ljava/lang/String;");
        jfieldID config_width        = env->GetFieldID(config_clz, "width", "I");
        jfieldID config_height       = env->GetFieldID(config_clz, "height", "I");
        jfieldID config_scale        = env->GetFieldID(config_clz, "scale", "F");
        jfieldID config_shift_x      = env->GetFieldID(config_clz, "shift_x", "F");
        jfieldID config_shift_y      = env->GetFieldID(config_clz, "shift_y", "F");
        jfieldID config_org_resize   = env->GetFieldID(config_clz, "org_resize", "Z");

        env->DeleteLocalRef(config_clz);

        ModelConfig modelConfig;
        modelConfig.width       = env->GetIntField(config, config_width);
        modelConfig.height      = env->GetIntField(config, config_height);
        modelConfig.scale       = env->GetFloatField(config, config_scale);
        modelConfig.shift_x     = env->GetFloatField(config, config_shift_x);
        modelConfig.shift_y     = env->GetFloatField(config, config_shift_y);
        modelConfig.org_resize  = env->GetBooleanField(config, config_org_resize);
        jstring model_name_jstr   = static_cast<jstring>(env->GetObjectField(config, config_name));
        const char *name = env->GetStringUTFChars(model_name_jstr, 0);

        std::string nameStr(name);
        modelConfig.name = nameStr;
        modelConfigs.push_back(modelConfig);

        env->ReleaseStringUTFChars(model_name_jstr, name);

    }
}


JNIEXPORT jlong JNICALL
LIVE_METHOD(allocate)(JNIEnv *env, jobject instance) {
    auto * const live = new Live();
    set_live(env, instance, live);
    return reinterpret_cast<intptr_t> (live);
}


JNIEXPORT void JNICALL
LIVE_METHOD(deallocate)(JNIEnv *env, jobject instance) {
    delete get_live(env, instance);
    set_live(env, instance, nullptr);
}

JNIEXPORT jint JNICALL
LIVE_METHOD(nativeLoadModel)(JNIEnv *env, jobject instance, jobject asset_manager,
        jobject configs) {
    std::vector<ModelConfig> model_configs;
    ConvertAndroidConfig2NativeConfig(env, configs, model_configs);

    AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
    return get_live(env, instance)->LoadModel(mgr, model_configs);
}


JNIEXPORT jfloat JNICALL
LIVE_METHOD(nativeDetectYuv)(JNIEnv *env, jobject instance, jbyteArray yuv, jint preview_width,
        jint preview_height, jint orientation, jint left, jint top, jint right, jint bottom) {
    jbyte *yuv_ = env->GetByteArrayElements(yuv, nullptr);

    cv::Mat bgr;
    Yuv420sp2bgr(reinterpret_cast<unsigned char *>(yuv_), preview_width, preview_height, orientation, bgr);

    FaceBox faceBox;
    faceBox.x1 = left;
    faceBox.y1 = top;
    faceBox.x2 = right;
    faceBox.y2 = bottom;

    float confidence = get_live(env, instance)->Detect(bgr, faceBox);
    env->ReleaseByteArrayElements(yuv, yuv_, 0);
    return confidence;
}

