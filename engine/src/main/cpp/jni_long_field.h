//
// Created by yuanhao on 20-6-9.
//

#ifndef LIVEBODYEXAMPLE_JNI_LONG_FIELD_H
#define LIVEBODYEXAMPLE_JNI_LONG_FIELD_H

#include <jni.h>

class JniLongField {
public:
    explicit JniLongField(const char* field_name)
            : field_name_(field_name),
              field_id_(nullptr) {
    }

    int64_t get(JNIEnv* env, jobject instance) {
        if(field_id_ == nullptr) {
            jclass cls = env->GetObjectClass(instance);
            field_id_ = env->GetFieldID(cls, field_name_, "J");
            env->DeleteLocalRef(cls);
        }
        return env->GetLongField(instance, field_id_);
    }

    void set(JNIEnv* env, jobject instance, int64_t value) {
        if(field_id_ == nullptr) {
            jclass cls = env->GetObjectClass(instance);
            field_id_ = env->GetFieldID(cls, field_name_, "J");
            env->DeleteLocalRef(cls);
        }
        env->SetLongField(instance, field_id_, value);
    }

private:
    const char* const field_name_;
    jfieldID field_id_;
};

#endif //LIVEBODYEXAMPLE_JNI_LONG_FIELD_H
