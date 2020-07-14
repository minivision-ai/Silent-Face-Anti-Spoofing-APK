//
// Created by yuanhao on 20-6-10.
//

#ifndef LIVEBODYEXAMPLE_ANDROID_LOG_H
#define LIVEBODYEXAMPLE_ANDROID_LOG_H

#include <android/log.h>

#define LOG_TAG     "Engine"

#define LOG_INFO(...)       __android_log_print(ANDROID_LOG_INFO,   LOG_TAG, __VA_ARGS__)
#define LOG_DEBUG(...)      __android_log_print(ANDROID_LOG_DEBUG,  LOG_TAG, __VA_ARGS__)
#define LOG_WARN(...)       __android_log_print(ANDROID_LOG_WARN,   LOG_TAG, __VA_ARGS__)
#define LOG_ERR(...)        __android_log_print(ANDROID_LOG_ERROR,  LOG_TAG, __VA_ARGS__)

#endif //LIVEBODYEXAMPLE_ANDROID_LOG_H
