cmake_minimum_required(VERSION 3.22.1)

project(prezzence_whisper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/whisper)

include_directories(
    ${WHISPER_DIR}
    ${WHISPER_DIR}/ggml-cpu
)

set(WHISPER_SOURCES
    ${WHISPER_DIR}/ggml.c
    ${WHISPER_DIR}/ggml.cpp
    ${WHISPER_DIR}/ggml-alloc.c
    ${WHISPER_DIR}/ggml-backend.cpp
    ${WHISPER_DIR}/ggml-backend-meta.cpp
    ${WHISPER_DIR}/ggml-backend-reg.cpp
    ${WHISPER_DIR}/ggml-backend-dl.cpp
    ${WHISPER_DIR}/ggml-cpu/amx/amx.cpp
    ${WHISPER_DIR}/ggml-cpu/amx/mmq.cpp
    ${WHISPER_DIR}/ggml-cpu/ggml-cpu.c
    ${WHISPER_DIR}/ggml-cpu/ggml-cpu.cpp
    ${WHISPER_DIR}/ggml-cpu/quants.c
    ${WHISPER_DIR}/ggml-cpu/traits.cpp
    ${WHISPER_DIR}/ggml-cpu/repack.cpp
    ${WHISPER_DIR}/ggml-cpu/unary-ops.cpp
    ${WHISPER_DIR}/ggml-cpu/binary-ops.cpp
    ${WHISPER_DIR}/ggml-cpu/vec.cpp
    ${WHISPER_DIR}/ggml-cpu/ops.cpp
    ${WHISPER_DIR}/ggml-opt.cpp
    ${WHISPER_DIR}/ggml-threading.cpp
    ${WHISPER_DIR}/ggml-quants.c
    ${WHISPER_DIR}/gguf.cpp
    ${WHISPER_DIR}/whisper.cpp
    native_whisper.cpp
)

if (${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "armeabi-v7a")
    list(APPEND WHISPER_SOURCES
        ${WHISPER_DIR}/ggml-cpu/arch/arm/quants.c
        ${WHISPER_DIR}/ggml-cpu/arch/arm/repack.cpp
    )
elseif (${ANDROID_ABI} STREQUAL "x86_64")
    list(APPEND WHISPER_SOURCES
        ${WHISPER_DIR}/ggml-cpu/arch/x86/quants.c
        ${WHISPER_DIR}/ggml-cpu/arch/x86/repack.cpp
    )
endif()

add_library(prezzence_whisper SHARED ${WHISPER_SOURCES})

find_library(LOG_LIB log)

target_link_libraries(prezzence_whisper ${LOG_LIB} android)
target_compile_options(prezzence_whisper PRIVATE -DWSP_GGML_USE_CPU -DWSP_GGML_USE_CPU_REPACK -O3 -DNDEBUG -pthread)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_options(prezzence_whisper PRIVATE -march=armv8-a)
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    target_compile_options(prezzence_whisper PRIVATE -mfpu=neon-vfpv4)
elseif (${ANDROID_ABI} STREQUAL "x86_64")
    target_compile_options(prezzence_whisper PRIVATE -march=x86-64 -msse4.2 -mpopcnt)
endif()
