package com.pollecode.prezzencekotlin.nativebridge

object NativeWhisperEngine {
    init {
        System.loadLibrary("prezzence_whisper")
    }

    external fun transcribePcm(modelPath: String, pcmData: FloatArray, languageTag: String): String
}
