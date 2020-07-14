//
// Created by yuanhao on 20-6-10.
//

#include <ncnn/cpu.h>
#include "face_detector.h"
#include "../android_log.h"

static bool AreaComp(FaceBox& l, FaceBox& r) {
    return ((l.x2 - l.x1 + 1) * (l.y2 - l.y1 + 1)) > ((r.x2 - r.x1 + 1) * (r.y2 - r.y1 + 1));
}

FaceDetector::FaceDetector() : threshold_(0.6f), thread_num_(2){
    min_face_size_ = 64;

    option_.lightmode = true;
    option_.num_threads = thread_num_;
}

FaceDetector::~FaceDetector() {
    net_.clear();
}

void FaceDetector::SetMinFaceSize(int size) {
    min_face_size_ = size;
}

int FaceDetector::LoadModel(AAssetManager* assetManager) {
    net_.opt = option_;
    int ret = net_.load_param(assetManager, "detection/detection.param");
    if(ret != 0) {
        LOG_ERR("FaceDetector load param failed. %d", ret);
        return -1;
    }

    ret = net_.load_model(assetManager, "detection/detection.bin");
    if(ret != 0) {
        LOG_ERR("FaceDetector load model failed. %d", ret);
        return -2;
    }
    return 0;
}


int FaceDetector::Detect(cv::Mat &src, std::vector<FaceBox> &boxes) {
    int w = src.cols;
    int h = src.rows;

    float aspect_ratio = w / (float)h;

    int input_width = static_cast<int>(input_size_ * sqrt(aspect_ratio));
    int input_height = static_cast<int>(input_size_ / sqrt(aspect_ratio));

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(src.data, ncnn::Mat::PIXEL_BGR, src.cols, src.rows,
                                                 input_width, input_height);

    in.substract_mean_normalize(mean_val_, nullptr);

    ncnn::Extractor extractor = net_.create_extractor();
    extractor.set_num_threads(thread_num_);
    extractor.input(net_input_name_.c_str(), in);

    ncnn::Mat out;
    extractor.extract(net_output_name_.c_str(), out);

    boxes.clear();
    for (int i = 0; i < out.h; ++i) {
        const float* values = out.row(i);
        float confidence = values[1];

        if(confidence < threshold_) continue;

        FaceBox box;
        box.confidence = confidence;
        box.x1 = values[2] * w;
        box.y1 = values[3] * h;
        box.x2 = values[4] * w;
        box.y2 = values[5] * h;

        // square
        float box_width = box.x2 - box.x1 + 1;
        float box_height = box.y2 - box.y1 + 1;

        float size = (box_width + box_height) * 0.5f;

        if(size < min_face_size_) continue;

        float cx = box.x1 + box_width * 0.5f;
        float cy = box.y1 + box_height * 0.5f;

        box.x1 = cx - size * 0.5f;
        box.y1 = cy - size * 0.5f;
        box.x2 = cx + size * 0.5f - 1;
        box.y2 = cy + size * 0.5f - 1;

        boxes.emplace_back(box);
    }

    // sort
    std::sort(boxes.begin(), boxes.end(), AreaComp);
    return 0;
}
