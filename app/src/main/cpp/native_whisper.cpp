#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <thread>
#include "whisper.h"

static std::string jstring_to_string(JNIEnv *env, jstring value) {
    if (value == nullptr) return "";
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string result = chars == nullptr ? "" : chars;
    if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pollecode_prezzencekotlin_nativebridge_NativeWhisperEngine_transcribePcm(
        JNIEnv *env,
        jobject,
        jstring modelPath,
        jfloatArray pcmData,
        jstring languageTag) {
    const std::string model = jstring_to_string(env, modelPath);
    std::string language = jstring_to_string(env, languageTag);
    const size_t dash = language.find('-');
    if (dash != std::string::npos) language = language.substr(0, dash);
    std::transform(language.begin(), language.end(), language.begin(), ::tolower);
    if (language.empty()) language = "en";

    const jsize sampleCount = env->GetArrayLength(pcmData);
    if (model.empty() || sampleCount < 1600) {
        return env->NewStringUTF("");
    }

    std::vector<float> samples(static_cast<size_t>(sampleCount));
    env->GetFloatArrayRegion(pcmData, 0, sampleCount, samples.data());

    whisper_context_params contextParams = whisper_context_default_params();
    whisper_context *ctx = whisper_init_from_file_with_params(model.c_str(), contextParams);
    if (ctx == nullptr) {
        return env->NewStringUTF("");
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.no_context = true;
    params.single_segment = false;
    params.language = language.c_str();
    unsigned int hardwareThreads = std::thread::hardware_concurrency();
    params.n_threads = static_cast<int>(std::max(1u, std::min(4u, hardwareThreads == 0 ? 2u : hardwareThreads)));

    int code = whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size()));
    std::string text;
    if (code == 0) {
        const int segments = whisper_full_n_segments(ctx);
        for (int i = 0; i < segments; ++i) {
            const char *segment = whisper_full_get_segment_text(ctx, i);
            if (segment != nullptr) text += segment;
        }
    }

    whisper_free(ctx);
    return env->NewStringUTF(text.c_str());
}
