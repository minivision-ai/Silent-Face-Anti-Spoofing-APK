//
// Created by yuanhao on 20-5-15.
//

#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include "live.h"
#include "../android_log.h"

LiveBody::LiveBody() {
    thread_num_ = 2;

    option_.lightmode = true;
    option_.num_threads = thread_num_;
}

LiveBody::~LiveBody() {
    for (int i = 0; i < nets_.size(); ++i) {
        nets_[i]->clear();
        delete nets_[i];
    }
    nets_.clear();
}

int LiveBody::loadModel(AAssetManager *assetManager, std::vector<ModelConfig> &configs) {
    configs_ = configs;
    model_num_ = static_cast<int>(configs_.size());
    for (int i = 0; i < model_num_; ++i) {
        ncnn::Net *net = new ncnn::Net();
        net->opt = option_;
        std::string param = "live/" + configs_[i].name + ".param";
        std::string model = "live/" + configs_[i].name + ".bin";
        int ret = net->load_param(assetManager, param.c_str());
        if (ret != 0) {
            LOG_ERR("LiveBody load param failed.");
            return -2 * (i) - 1;
        }

        ret = net->load_model(assetManager, model.c_str());
        if (ret != 0) {
            LOG_ERR("LiveBody load model failed.");
            return -2 * (i + 1);
        }
        nets_.emplace_back(net);
    }
    return 0;
}

int LiveBody::loadModel(std::string &dir, std::vector<ModelConfig> &configs) {
    configs_ = configs;
    model_num_ = static_cast<int>(configs_.size());
    for (int i = 0; i < model_num_; ++i) {
        ncnn::Net *net = new ncnn::Net();
        net->opt = option_;
        std::string param = dir + "/" + configs_[i].name + ".param";
        std::string model = dir + "/" + configs_[i].name + ".bin";
        int ret = net->load_param(param.c_str());
        if (ret != 0) {
            LOG_ERR("LiveBody load param failed.");
            return -2 * (i) - 1;
        }

        ret = net->load_model(model.c_str());
        if (ret != 0) {
            LOG_ERR("LiveBody load model failed.");
            return -2 * (i + 1);
        }
        nets_.emplace_back(net);
    }
    return 0;
}

float LiveBody::detect(cv::Mat &src, FaceBox &box) {
    float c = 0.f;
    for (int i = 0; i < model_num_; i++) {
        cv::Mat roi;
        if(configs_[i].org_resize) {
            cv::resize(src, roi, cv::Size(configs_[i].width, configs_[i].height));
        } else {
            cv::Rect rect = calculateBox(box, src.cols, src.rows, configs_[i]);
            // roi resize
            cv::resize(src(rect), roi, cv::Size(configs_[i].width, configs_[i].height));
        }

        ncnn::Mat in = ncnn::Mat::from_pixels(roi.data, ncnn::Mat::PIXEL_BGR, roi.cols, roi.rows);

        // inference
        ncnn::Extractor extractor = nets_[i]->create_extractor();
        extractor.set_light_mode(true);
        extractor.set_num_threads(thread_num_);

        extractor.input(net_input_name_.c_str(), in);
        ncnn::Mat out;
        extractor.extract(net_output_name_.c_str(), out);

        c += out.row(0)[1];
    }
    return c;
}

cv::Rect LiveBody::calculateBox(FaceBox &box, int w, int h, ModelConfig &config) {
    int x = static_cast<int>(box.x1);
    int y = static_cast<int>(box.y1);
    int boxWidth = static_cast<int>(box.x2 - box.x1 + 1);
    int boxHeight = static_cast<int>(box.y2 - box.y1 + 1);

    int shiftX = static_cast<int>(boxWidth * config.shift_x);
    int shiftY = static_cast<int>(boxHeight * config.shift_y);

    float scale = std::min(
            config.scale,
            std::min((w - 1) / (float) boxWidth, (h - 1) / (float) boxHeight)
    );

    int boxCenterX = boxWidth / 2 + x;
    int boxCenterY = boxHeight / 2 + y;

    int newWidth = static_cast<int>(boxWidth * scale);
    int newHeight = static_cast<int>(boxHeight * scale);

    int leftTopX = boxCenterX - newWidth / 2 + shiftX;
    int leftTopY = boxCenterY - newHeight / 2 + shiftY;
    int rightBottomX = boxCenterX + newWidth / 2 + shiftX;
    int rightBottomY = boxCenterY + newHeight / 2 + shiftY;

    if (leftTopX < 0) {
        rightBottomX -= leftTopX;
        leftTopX = 0;
    }

    if (leftTopY < 0) {
        rightBottomY -= leftTopY;
        leftTopY = 0;
    }

    if (rightBottomX >= w) {
        int s = rightBottomX - w + 1;
        leftTopX -= s;
        rightBottomX -= s;
    }

    if (rightBottomY >= h) {
        int s = rightBottomY - h + 1;
        leftTopY -= s;
        rightBottomY -= s;
    }

    return cv::Rect(leftTopX, leftTopY, newWidth, newHeight);
}




