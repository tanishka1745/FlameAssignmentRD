#include <opencv2/opencv.hpp>
using namespace cv;

/**
 * Converts NV21 byte array to OpenCV Mat
 */
Mat nv21ToMat(jbyte* nv21, int width, int height) {
    Mat yuv(height + height/2, width, CV_8UC1, (unsigned char*)nv21);
    Mat rgb;
    cvtColor(yuv, rgb, COLOR_YUV2RGB_NV21);
    return rgb;
}

/**
 * Apply Canny Edge Detection
 */
Mat applyCanny(Mat &input) {
    Mat gray, edges;
    cvtColor(input, gray, COLOR_RGB2GRAY); // Convert to grayscale
    Canny(gray, edges, 80, 100);
    cvtColor(edges, edges, COLOR_GRAY2RGB); // Convert back to RGB for display
    return edges;
}