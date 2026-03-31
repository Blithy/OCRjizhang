cmake_minimum_required(VERSION 3.22.1)

project(ocr_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(THIRD_PARTY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party)
set(PADDLE_LITE_INCLUDE_DIR ${THIRD_PARTY_DIR}/paddlelite/include)
set(PADDLE_LITE_LIB_DIR ${THIRD_PARTY_DIR}/paddlelite/lib)
set(OPENCV_ROOT ${THIRD_PARTY_DIR}/opencv4.1.0/${ANDROID_ABI})
set(OPENCV_INCLUDE_DIR ${OPENCV_ROOT})
set(OPENCV_LIB_DIR ${OPENCV_ROOT}/libs)
set(OPENCV_THIRD_PARTY_LIB_DIR ${OPENCV_ROOT}/3rdparty/libs)

add_library(
    ocr_native
    SHARED
    ocr_native.cpp
    paddleocr/cls_process.cc
    paddleocr/crnn_process.cc
    paddleocr/db_post_process.cc
    paddleocr/clipper.cpp
)

target_include_directories(
    ocr_native
    PRIVATE
    ${PADDLE_LITE_INCLUDE_DIR}
    ${OPENCV_INCLUDE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/paddleocr
)

target_compile_options(
    ocr_native
    PRIVATE
    -O3
    -fexceptions
    -frtti
    -fopenmp
)

target_link_options(
    ocr_native
    PRIVATE
    -fopenmp
    -static-openmp
)

target_link_libraries(
    ocr_native
    ${PADDLE_LITE_LIB_DIR}/libpaddle_api_light_bundled.a
    ${OPENCV_LIB_DIR}/libopencv_imgcodecs.a
    ${OPENCV_LIB_DIR}/libopencv_imgproc.a
    ${OPENCV_LIB_DIR}/libopencv_core.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/libtegra_hal.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/liblibjpeg-turbo.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/liblibwebp.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/liblibpng.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/liblibjasper.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/liblibtiff.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/libIlmImf.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/libtbb.a
    ${OPENCV_THIRD_PARTY_LIB_DIR}/libcpufeatures.a
    android
    log
    z
    m
    dl
    atomic
)
