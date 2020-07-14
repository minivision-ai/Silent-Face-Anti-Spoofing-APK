//
// Created by yuanhao on 20-6-9.
//

#include "jni_long_field.h"
#include "definition.h"
#include "detection/face_detector.h"
#include "img_process.h"
#include <android/asset_manager_jni.h>


JniLongField face_detector_field("nativeHandler");

FaceDetector* get_face_detector(JNIEnv* env, jobject instance) {
    FaceDetector* const detector =
            reinterpret_cast<FaceDetector*>(face_detector_field.get(env, instance));
    return detector;
}

void set_face_detector(JNIEnv* env, jobject instance, FaceDetector* detector) {
    face_detector_field.set(env, instance, reinterpret_cast<intptr_t>(detector));
}


extern "C" {

JNIEXPORT jlong JNICALL
FACE_DETECTOR_METHOD(allocate)(JNIEnv *env, jobject instance);


JNIEXPORT void JNICALL
FACE_DETECTOR_METHOD(deallocate)(JNIEnv *env, jobject instance);

JNIEXPORT jint JNICALL
FACE_DETECTOR_METHOD(nativeLoadModel)(JNIEnv *env, jobject instance, jobject assets_manager);


JNIEXPORT jobject JNICALL
FACE_DETECTOR_METHOD(nativeDetectBitmap)(JNIEnv *env, jobject instance, jobject bitmap);


JNIEXPORT jobject JNICALL
FACE_DETECTOR_METHOD(nativeDetectYuv)(JNIEnv *env, jobject instance, jbyteArray yuv,
                                      jint preview_width, jint preview_height,jint orientation);

}


jobject ConvertFaceBoxVector2List(JNIEnv *env, std::vector<FaceBox>& boxes) {
    jclass list_clz = env->FindClass(JAVA_ARRAY_LIST_CLASSPATH);

    jmethodID init_method = env->GetMethodID(list_clz, "<init>", "()V");
    jmethodID add_method = env->GetMethodID(list_clz, "add", "(Ljava/lang/Object;)Z");

    jobject list = env->NewObject(list_clz, init_method);
    env->DeleteLocalRef(list_clz);

    jclass face_clz = env->FindClass(ANDROID_FACE_BOX_CLASSPATH);

    jmethodID face_init_method = env->GetMethodID(face_clz, "<init>", "(IIIIF)V");

    for (auto& box : boxes) {
        int left = static_cast<int>(box.x1);
        int top = static_cast<int>(box.y1);
        int right = static_cast<int>(box.x2);
        int bottom = static_cast<int>(box.y2);


        jobject face = env->NewObject(face_clz, face_init_method, left, top, right, bottom, 0.f);
        env->CallBooleanMethod(list, add_method, face);

        env->DeleteLocalRef(face);
    }

    env->DeleteLocalRef(face_clz);
    return list;
}


JNIEXPORT jlong JNICALL
FACE_DETECTOR_METHOD(allocate)(JNIEnv *env, jobject instance) {
    auto * const detector = new FaceDetector();
    set_face_detector(env, instance, detector);
    return reinterpret_cast<intptr_t> (detector);
}

JNIEXPORT void JNICALL
FACE_DETECTOR_METHOD(deallocate)(JNIEnv *env, jobject instance) {
    delete get_face_detector(env, instance);
    set_face_detector(env, instance, nullptr);
}



JNIEXPORT jint JNICALL
FACE_DETECTOR_METHOD(nativeLoadModel)(JNIEnv *env, jobject instance, jobject assets_manager) {
    AAssetManager* mgr = AAssetManager_fromJava(env, assets_manager);
    return get_face_detector(env, instance)->LoadModel(mgr);
}

JNIEXPORT jobject JNICALL
FACE_DETECTOR_METHOD(nativeDetectBitmap)(JNIEnv *env, jobject instance, jobject bitmap) {
    cv::Mat img;
    int ret = ConvertBitmap2Mat(env, bitmap, img);
    if(ret != 0)
        return nullptr;

    std::vector<FaceBox> boxes;
    get_face_detector(env, instance)->Detect(img, boxes);

    AndroidBitmap_unlockPixels(env, bitmap);

    if(boxes.empty()) return nullptr;

    return ConvertFaceBoxVector2List(env, boxes);
}

JNIEXPORT jobject JNICALL
FACE_DETECTOR_METHOD(nativeDetectYuv)(JNIEnv *env, jobject instance, jbyteArray yuv,
        jint preview_width, jint preview_height,jint orientation) {
    jbyte *yuv_ = env->GetByteArrayElements(yuv, nullptr);

    cv::Mat bgr;
    Yuv420sp2bgr(reinterpret_cast<unsigned char *>(yuv_), preview_width, preview_height, orientation, bgr);

    std::vector<FaceBox> boxes;
    get_face_detector(env, instance)->Detect(bgr, boxes);

    env->ReleaseByteArrayElements(yuv, yuv_, 0);

    return ConvertFaceBoxVector2List(env, boxes);
}
