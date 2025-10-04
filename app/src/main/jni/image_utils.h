#ifndef IMAGE_UTILS_H
#define IMAGE_UTILS_H

#include <opencv2/opencv.hpp>
#include <jni.h>

cv::Mat nv21ToMat(jbyte* nv21, int width, int height);
cv::Mat applyCanny(const cv::Mat &input);

#endif // IMAGE_UTILS_H