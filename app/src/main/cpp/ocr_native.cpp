#include <android/log.h>
#include <arm_neon.h>
#include <jni.h>

#include <algorithm>
#include <chrono>
#include <cstring>
#include <fstream>
#include <map>
#include <memory>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include "paddle_api.h"
#include "paddle_place.h"
#include "paddle_use_kernels.h"
#include "paddle_use_ops.h"
#include "cls_process.h"
#include "crnn_process.h"
#include "db_post_process.h"

using namespace paddle::lite_api;  // NOLINT

namespace {

constexpr const char* kLogTag = "PaddleOcrNative";
constexpr const char* kDetModelName = "ch_PP-OCRv3_det_infer.nb";
constexpr const char* kRecModelName = "ch_PP-OCRv3_rec_infer.nb";
constexpr const char* kClsModelName = "ch_ppocr_mobile_v2.0_cls_infer_opt.nb";
constexpr const char* kConfigName = "config.txt";
constexpr const char* kDictName = "ppocr_keys_v1.txt";

std::mutex g_engine_mutex;
std::string g_runtime_dir;
std::unique_ptr<class PaddleOcrEngine> g_engine;

void LogError(const std::string& message) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "%s", message.c_str());
}

void LogInfo(const std::string& message) {
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "%s", message.c_str());
}

void ThrowIllegalState(JNIEnv* env, const std::string& message) {
    jclass exception_class = env->FindClass("java/lang/IllegalStateException");
    if (exception_class != nullptr) {
        env->ThrowNew(exception_class, message.c_str());
    }
}

std::string JoinPath(const std::string& base, const std::string& name) {
    if (base.empty()) {
        return name;
    }
    if (base.back() == '/') {
        return base + name;
    }
    return base + "/" + name;
}

std::vector<std::string> Split(const std::string& text, const std::string& delimiter) {
    std::vector<std::string> result;
    if (text.empty()) {
        return result;
    }

    std::unique_ptr<char[]> input(new char[text.size() + 1]);
    std::strcpy(input.get(), text.c_str());
    std::unique_ptr<char[]> delims(new char[delimiter.size() + 1]);
    std::strcpy(delims.get(), delimiter.c_str());

    char* token = std::strtok(input.get(), delims.get());
    while (token != nullptr) {
        result.emplace_back(token);
        token = std::strtok(nullptr, delims.get());
    }
    return result;
}

std::map<std::string, double> LoadConfigTxt(const std::string& config_path) {
    const auto config_lines = ReadDict(config_path);
    std::map<std::string, double> config;
    for (const auto& line : config_lines) {
        const auto parts = Split(line, " ");
        if (parts.size() < 2) {
            continue;
        }
        config[parts[0]] = std::stod(parts[1]);
    }
    return config;
}

std::shared_ptr<PaddlePredictor> LoadModel(const std::string& model_file, int num_threads) {
    MobileConfig config;
    config.set_model_from_file(model_file);
    config.set_threads(num_threads);
    return CreatePaddlePredictor<MobileConfig>(config);
}

void NeonMeanScale(
    const float* din,
    float* dout,
    int size,
    const std::vector<float>& mean,
    const std::vector<float>& scale
) {
    if (mean.size() != 3 || scale.size() != 3) {
        throw std::runtime_error("mean or scale size must equal to 3");
    }

    float32x4_t vmean0 = vdupq_n_f32(mean[0]);
    float32x4_t vmean1 = vdupq_n_f32(mean[1]);
    float32x4_t vmean2 = vdupq_n_f32(mean[2]);
    float32x4_t vscale0 = vdupq_n_f32(scale[0]);
    float32x4_t vscale1 = vdupq_n_f32(scale[1]);
    float32x4_t vscale2 = vdupq_n_f32(scale[2]);

    float* dout_c0 = dout;
    float* dout_c1 = dout + size;
    float* dout_c2 = dout + size * 2;

    int i = 0;
    for (; i < size - 3; i += 4) {
        float32x4x3_t vin3 = vld3q_f32(din);
        float32x4_t vsub0 = vsubq_f32(vin3.val[0], vmean0);
        float32x4_t vsub1 = vsubq_f32(vin3.val[1], vmean1);
        float32x4_t vsub2 = vsubq_f32(vin3.val[2], vmean2);
        float32x4_t vs0 = vmulq_f32(vsub0, vscale0);
        float32x4_t vs1 = vmulq_f32(vsub1, vscale1);
        float32x4_t vs2 = vmulq_f32(vsub2, vscale2);
        vst1q_f32(dout_c0, vs0);
        vst1q_f32(dout_c1, vs1);
        vst1q_f32(dout_c2, vs2);

        din += 12;
        dout_c0 += 4;
        dout_c1 += 4;
        dout_c2 += 4;
    }

    for (; i < size; i++) {
        *(dout_c0++) = (*(din++) - mean[0]) * scale[0];
        *(dout_c1++) = (*(din++) - mean[1]) * scale[1];
        *(dout_c2++) = (*(din++) - mean[2]) * scale[2];
    }
}

cv::Mat DetResizeImg(const cv::Mat& image, int max_size_len, std::vector<float>* ratio_hw) {
    const int width = image.cols;
    const int height = image.rows;

    float ratio = 1.f;
    const int max_wh = std::max(width, height);
    if (max_wh > max_size_len) {
        ratio = height > width
            ? static_cast<float>(max_size_len) / static_cast<float>(height)
            : static_cast<float>(max_size_len) / static_cast<float>(width);
    }

    int resize_h = static_cast<int>(static_cast<float>(height) * ratio);
    int resize_w = static_cast<int>(static_cast<float>(width) * ratio);

    if (resize_h % 32 != 0) {
        resize_h = resize_h / 32 < 1 ? 32 : (resize_h / 32 - 1) * 32;
    }
    if (resize_w % 32 != 0) {
        resize_w = resize_w / 32 < 1 ? 32 : (resize_w / 32 - 1) * 32;
    }

    cv::Mat resize_image;
    cv::resize(image, resize_image, cv::Size(resize_w, resize_h));

    ratio_hw->push_back(static_cast<float>(resize_h) / static_cast<float>(height));
    ratio_hw->push_back(static_cast<float>(resize_w) / static_cast<float>(width));
    return resize_image;
}

cv::Mat RunClsModel(
    const cv::Mat& image,
    const std::shared_ptr<PaddlePredictor>& predictor_cls,
    float thresh = 0.9f
) {
    const std::vector<float> mean = {0.5f, 0.5f, 0.5f};
    const std::vector<float> scale = {1 / 0.5f, 1 / 0.5f, 1 / 0.5f};

    cv::Mat rotated;
    image.copyTo(rotated);
    cv::Mat resize_image = ClsResizeImg(image);
    resize_image.convertTo(resize_image, CV_32FC3, 1 / 255.f);

    const float* image_data = reinterpret_cast<const float*>(resize_image.data);

    std::unique_ptr<Tensor> input_tensor(std::move(predictor_cls->GetInput(0)));
    input_tensor->Resize({1, 3, resize_image.rows, resize_image.cols});
    auto* input = input_tensor->mutable_data<float>();

    NeonMeanScale(image_data, input, resize_image.rows * resize_image.cols, mean, scale);
    predictor_cls->Run();

    std::unique_ptr<const Tensor> softmax_out(std::move(predictor_cls->GetOutput(0)));
    auto* softmax_scores = softmax_out->data<float>();
    const auto shape = softmax_out->shape();
    float score = 0.f;
    int label = 0;
    for (int i = 0; i < shape[1]; i++) {
        if (softmax_scores[i] > score) {
            score = softmax_scores[i];
            label = i;
        }
    }

    if (label % 2 == 1 && score > thresh) {
        cv::rotate(rotated, rotated, 1);
    }
    return rotated;
}

void RunRecModel(
    const std::vector<std::vector<std::vector<int>>>& boxes,
    const cv::Mat& image,
    const std::shared_ptr<PaddlePredictor>& predictor_crnn,
    std::vector<std::string>* rec_text,
    std::vector<float>* rec_text_score,
    const std::vector<std::string>& charactor_dict,
    const std::shared_ptr<PaddlePredictor>& predictor_cls,
    int use_direction_classify,
    int rec_image_height
) {
    const std::vector<float> mean = {0.5f, 0.5f, 0.5f};
    const std::vector<float> scale = {1 / 0.5f, 1 / 0.5f, 1 / 0.5f};

    cv::Mat srcimg;
    image.copyTo(srcimg);

    for (int index = static_cast<int>(boxes.size()) - 1; index >= 0; index--) {
        cv::Mat crop_img = GetRotateCropImage(srcimg, boxes[index]);
        if (use_direction_classify >= 1 && predictor_cls != nullptr) {
            crop_img = RunClsModel(crop_img, predictor_cls);
        }

        const float width_height_ratio =
            static_cast<float>(crop_img.cols) / static_cast<float>(crop_img.rows);
        cv::Mat resize_image = CrnnResizeImg(crop_img, width_height_ratio, rec_image_height);
        resize_image.convertTo(resize_image, CV_32FC3, 1 / 255.f);

        const float* image_data = reinterpret_cast<const float*>(resize_image.data);

        std::unique_ptr<Tensor> input_tensor(std::move(predictor_crnn->GetInput(0)));
        input_tensor->Resize({1, 3, resize_image.rows, resize_image.cols});
        auto* input = input_tensor->mutable_data<float>();
        NeonMeanScale(image_data, input, resize_image.rows * resize_image.cols, mean, scale);

        predictor_crnn->Run();

        std::unique_ptr<const Tensor> output_tensor(std::move(predictor_crnn->GetOutput(0)));
        auto* predict_batch = output_tensor->data<float>();
        const auto predict_shape = output_tensor->shape();

        std::string text_result;
        int last_index = 0;
        float score = 0.f;
        int count = 0;

        for (int step = 0; step < predict_shape[1]; step++) {
            const int base_index = step * static_cast<int>(predict_shape[2]);
            const int argmax_index = static_cast<int>(
                Argmax(
                    &predict_batch[base_index],
                    &predict_batch[base_index + static_cast<int>(predict_shape[2])]
                )
            );
            const float max_value = static_cast<float>(*std::max_element(
                &predict_batch[base_index],
                &predict_batch[base_index + static_cast<int>(predict_shape[2])]
            ));

            if (argmax_index > 0 && !(step > 0 && argmax_index == last_index)) {
                score += max_value;
                count += 1;
                if (argmax_index < static_cast<int>(charactor_dict.size())) {
                    text_result += charactor_dict[argmax_index];
                }
            }
            last_index = argmax_index;
        }

        rec_text->push_back(text_result);
        rec_text_score->push_back(count == 0 ? 0.f : score / static_cast<float>(count));
    }
}

std::vector<std::vector<std::vector<int>>> RunDetModel(
    const std::shared_ptr<PaddlePredictor>& predictor,
    const cv::Mat& image,
    const std::map<std::string, double>& config
) {
    const int max_side_len = static_cast<int>(config.at("max_side_len"));
    const int det_db_use_dilate = static_cast<int>(config.at("det_db_use_dilate"));

    cv::Mat source_image;
    image.copyTo(source_image);

    std::vector<float> ratio_hw;
    cv::Mat resize_image = DetResizeImg(image, max_side_len, &ratio_hw);
    cv::Mat image_fp;
    resize_image.convertTo(image_fp, CV_32FC3, 1.0 / 255.f);

    std::unique_ptr<Tensor> input_tensor(std::move(predictor->GetInput(0)));
    input_tensor->Resize({1, 3, image_fp.rows, image_fp.cols});
    auto* input = input_tensor->mutable_data<float>();

    const std::vector<float> mean = {0.485f, 0.456f, 0.406f};
    const std::vector<float> scale = {1 / 0.229f, 1 / 0.224f, 1 / 0.225f};
    const float* image_data = reinterpret_cast<const float*>(image_fp.data);
    NeonMeanScale(image_data, input, image_fp.rows * image_fp.cols, mean, scale);

    predictor->Run();

    std::unique_ptr<const Tensor> output_tensor(std::move(predictor->GetOutput(0)));
    auto* output = output_tensor->data<float>();
    const auto shape = output_tensor->shape();
    const int output_height = static_cast<int>(shape[2]);
    const int output_width = static_cast<int>(shape[3]);
    const int output_size = output_height * output_width;

    std::vector<float> pred(output_size);
    std::vector<unsigned char> cbuf(output_size);
    for (int i = 0; i < output_size; i++) {
        pred[i] = static_cast<float>(output[i]);
        cbuf[i] = static_cast<unsigned char>(output[i] * 255);
    }

    cv::Mat cbuf_map(output_height, output_width, CV_8UC1, cbuf.data());
    cv::Mat pred_map(output_height, output_width, CV_32F, pred.data());

    const double threshold = config.at("det_db_thresh") * 255;
    cv::Mat bit_map;
    cv::threshold(cbuf_map, bit_map, threshold, 255, cv::THRESH_BINARY);
    if (det_db_use_dilate == 1) {
        cv::Mat dilation_map;
        cv::Mat dilation_element =
            cv::getStructuringElement(cv::MORPH_RECT, cv::Size(2, 2));
        cv::dilate(bit_map, dilation_map, dilation_element);
        bit_map = dilation_map;
    }

    const auto boxes = BoxesFromBitmap(pred_map, bit_map, config);
    return FilterTagDetRes(boxes, ratio_hw[0], ratio_hw[1], source_image);
}

std::vector<std::vector<std::vector<int>>> CreateFullImageBox(const cv::Mat& image) {
    const int width = image.cols;
    const int height = image.rows;
    return {
        {
            {0, 0},
            {width, 0},
            {width, height},
            {0, height},
        }
    };
}

class PaddleOcrEngine {
public:
    explicit PaddleOcrEngine(const std::string& runtime_dir)
        : runtime_dir_(runtime_dir),
          config_(LoadConfigTxt(JoinPath(runtime_dir_, kConfigName))),
          rec_image_height_(static_cast<int>(config_.at("rec_image_height"))),
          use_direction_classify_(static_cast<int>(config_.at("use_direction_classify"))) {
        charactor_dict_ = ReadDict(JoinPath(runtime_dir_, kDictName));
        charactor_dict_.insert(charactor_dict_.begin(), "#");
        charactor_dict_.push_back(" ");

        LogInfo("Loading detection model: " + JoinPath(runtime_dir_, kDetModelName));
        det_predictor_ = LoadModel(JoinPath(runtime_dir_, kDetModelName), 2);
        LogInfo("Detection model loaded");
        LogInfo("Loading recognition model: " + JoinPath(runtime_dir_, kRecModelName));
        rec_predictor_ = LoadModel(JoinPath(runtime_dir_, kRecModelName), 2);
        LogInfo("Recognition model loaded");
        if (use_direction_classify_ >= 1) {
            LogInfo("Loading classifier model: " + JoinPath(runtime_dir_, kClsModelName));
            cls_predictor_ = LoadModel(JoinPath(runtime_dir_, kClsModelName), 2);
            LogInfo("Classifier model loaded");
        }

        if (det_predictor_ == nullptr || rec_predictor_ == nullptr) {
            throw std::runtime_error("failed to create Paddle predictors");
        }
    }

    std::string RecognizeText(const std::string& image_path) const {
        cv::Mat image = cv::imread(image_path, cv::IMREAD_COLOR);
        if (!image.data) {
            throw std::runtime_error("failed to read image: " + image_path);
        }

        auto boxes = RunDetModel(det_predictor_, image, config_);
        if (boxes.empty()) {
            boxes = CreateFullImageBox(image);
        }

        std::vector<std::string> recognized_text;
        std::vector<float> recognized_scores;
        RunRecModel(
            boxes,
            image,
            rec_predictor_,
            &recognized_text,
            &recognized_scores,
            charactor_dict_,
            cls_predictor_,
            use_direction_classify_,
            rec_image_height_
        );

        std::ostringstream builder;
        bool first_line = true;
        for (const auto& line : recognized_text) {
            if (line.empty()) {
                continue;
            }
            if (!first_line) {
                builder << '\n';
            }
            builder << line;
            first_line = false;
        }
        return builder.str();
    }

private:
    std::string runtime_dir_;
    std::map<std::string, double> config_;
    std::vector<std::string> charactor_dict_;
    int rec_image_height_ = 48;
    int use_direction_classify_ = 1;
    std::shared_ptr<PaddlePredictor> det_predictor_;
    std::shared_ptr<PaddlePredictor> rec_predictor_;
    std::shared_ptr<PaddlePredictor> cls_predictor_;
};

PaddleOcrEngine& GetOrCreateEngine(const std::string& runtime_dir) {
    if (g_engine == nullptr || g_runtime_dir != runtime_dir) {
        g_engine = std::make_unique<PaddleOcrEngine>(runtime_dir);
        g_runtime_dir = runtime_dir;
    }
    return *g_engine;
}

std::string JStringToString(JNIEnv* env, jstring value) {
    const char* raw = env->GetStringUTFChars(value, nullptr);
    if (raw == nullptr) {
        throw std::runtime_error("failed to read JNI string");
    }
    const std::string result(raw);
    env->ReleaseStringUTFChars(value, raw);
    return result;
}

}  // namespace

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_ocrjizhang_data_ocr_PaddleOcrNative_recognizeNative(
    JNIEnv* env,
    jobject /* this */,
    jstring image_path,
    jstring runtime_dir
) {
    try {
        const std::string image_path_value = JStringToString(env, image_path);
        const std::string runtime_dir_value = JStringToString(env, runtime_dir);

        std::lock_guard<std::mutex> guard(g_engine_mutex);
        PaddleOcrEngine& engine = GetOrCreateEngine(runtime_dir_value);
        const std::string text = engine.RecognizeText(image_path_value);
        return env->NewStringUTF(text.c_str());
    } catch (const std::exception& exception) {
        LogError(exception.what());
        ThrowIllegalState(env, exception.what());
        return nullptr;
    }
}
