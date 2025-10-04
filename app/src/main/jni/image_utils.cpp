#include "image_utils.h"
using namespace cv;

cv::Mat nv21ToMat(jbyte* nv21, int width, int height) {
    cv::Mat yuv(height + height / 2, width, CV_8UC1, (unsigned char*)nv21);
    cv::Mat bgr;
    cv::cvtColor(yuv, bgr, cv::COLOR_YUV2BGR_NV21);
    return bgr;
}

cv::Mat applyCanny(const cv::Mat &input) {
    cv::Mat gray, edges;
    cv::cvtColor(input, gray, cv::COLOR_BGR2GRAY);
    cv::Canny(gray, edges, 100, 200);
    cv::cvtColor(edges, edges, cv::COLOR_GRAY2BGR);
    return edges;
}